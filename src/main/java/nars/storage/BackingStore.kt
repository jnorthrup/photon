/*
 * BackingStore.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.storage

import nars.entity.*
import nars.inference.BudgetFunctions.activate
import nars.io.IInferenceRecorder
import nars.language.Term
import nars.main_nogui.Parameters
import nars.main_nogui.ReasonerBatch
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * The memory of the system.
 */
class BackingStore(reasoner: ReasonerBatch ) : MemoryOps {
    val memoryState = MemoryState()
    override fun clear() {
        memoryState.concepts!!.init()
        memoryState.novelTasks!!.init()
        memoryState.newTasks.clear()
        exportStrings.clear()
        memoryState.reasoner!!.initTimer()
        recorder.append("\n-----RESET-----\n")
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     *
     * @return
     */
/* ---------- access utilities ---------- */
    override val exportStrings: ArrayList<String>
        get() = memoryState.exportStrings as ArrayList<String>

    /**
     * Inference record text to be written into a log file
     */
    override var recorder: IInferenceRecorder
        get() = memoryState.recorder
        set(recorder) {
            memoryState.recorder = recorder
        }

    //    public MainWindow getMainWindow() {
//        return reasoner.getMainWindow();
//    }
    override val time: Long
        get() = memoryState.reasoner!!.time
    /* ---------- conversion utilities ---------- */
    /**
     * Actually means that there are no new Tasks
     */
    override fun noResult(): Boolean {
        return memoryState.newTasks.isEmpty()
    }

    /**
     * Get an existing Concept for a given name
     *
     * called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    override fun nameToConcept(name: String): Concept {
        return memoryState.concepts!![name]!!
    }

    /**
     * Get a Term for a given name of a Concept or Operator
     *
     * called in
     * StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    override fun nameToListedTerm(name: String): Term? {
        val concept = memoryState.concepts!![name]
        return concept?.term
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    override fun termToConcept(term: Term): Concept {
        return nameToConcept(term.name)
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    override fun getConcept(term: Term): Concept? {
        if (term.constant) {
            val n = term.name
            var concept = memoryState.concepts!![n]
            if (concept == null) {
                concept = Concept(term, this) // the only place to make a new Concept
                val created = memoryState.concepts!!.putIn(concept)
                if (!created) {
                    return null
                }
            }
            return concept
        }
        return null
    }
    /* ---------- adjustment functions ---------- */
    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    override fun getConceptActivation(t: Term): Float {
        val c = termToConcept(t)
        return Optional.ofNullable(c).map(ImmutableItemIdentity::priority).orElse(0f)
    }
    /* ---------- new task entries ---------- */ /* There are several types of new tasks, all added into the
     newTasks list, to be processed in the next workCycle.
     Some of them are reported and/or logged. */
    /**
     * Adjust the activation level of a Concept
     *
     * called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    override fun activateConcept(c: Concept, b: BudgetValue) {
        memoryState.concepts!!.pickOut(c.key)
        activate(c, b)
        memoryState.concepts!!.putBack(c)
    }

    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
    override fun inputTask(task: Task) {
        if (task.budget.aboveThreshold()) {
            recorder.append("!!! Perceived: $task\n")
            report(task.sentence, true) // report input
            memoryState.newTasks.add(task) // wait to be processed in the next workCycle
        } else {
            recorder.append("!!! Neglected: $task\n")
        }
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget          The budget value of the new Task
     * @param sentence        The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     * forward/backward correspondence
     */
    override fun activatedTask(budget: BudgetValue, sentence: Sentence, candidateBelief: Sentence) {
        val task = Task(sentence, budget, memoryState.currentTask!!, sentence, candidateBelief)
        recorder.append("!!! Activated: $task\n")
        if (sentence.isQuestion) {
            val s = task.budget.summary()
            //            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            val minSilent = memoryState.reasoner!!.silenceValue.get() / 100.0f
            if (s > minSilent) { // only report significant derived Tasks
                report(task.sentence, false)
            }
        }
        memoryState.newTasks.add(task)
    }
    /* --------------- new task building --------------- */
    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    override fun derivedTask(task: Task) {
        if (task.budget.aboveThreshold()) {
            recorder.append("!!! Derived: $task\n")
            val budget = task.budget.summary()
            //            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            val minSilent = memoryState.reasoner!!.silenceValue.get() / 100.0f
            if (budget > minSilent) { // only report significant derived Tasks
                report(task.sentence, false)
            }
            memoryState.newTasks.add(task)
        } else {
            recorder.append("!!! Ignored: $task\n")
        }
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    override fun doublePremiseTask(newContent: Term, newTruth: TruthValue, newBudget: BudgetValue) {
        if (newContent != null) {
            val newSentence = Sentence(newContent, memoryState.currentTask!!.sentence.punctuation, newTruth, memoryState.newStamp!!)
            val newTask = Task(newSentence, newBudget, memoryState.currentTask!!, memoryState.currentBelief!!)
            derivedTask(newTask)
        }
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param revisible  Whether the sentence is revisible
     */
    override fun doublePremiseTask(newContent: Term, newTruth: TruthValue, newBudget: BudgetValue, revisible: Boolean) {
        if (newContent != null) {
            val taskSentence = memoryState.currentTask!!.sentence
            val newSentence = Sentence(newContent, taskSentence.punctuation, newTruth, memoryState.newStamp!!, revisible)
            val newTask = Task(newSentence, newBudget, memoryState.currentTask!!, memoryState.currentBelief!!)
            derivedTask(newTask)
        }
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    override fun singlePremiseTask(newContent: Term, newTruth: TruthValue, newBudget: BudgetValue) {
        singlePremiseTask(newContent, memoryState.currentTask!!.sentence.punctuation, newTruth, newBudget)
    }
    /* ---------- system working workCycle ---------- */
    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    override fun singlePremiseTask(newContent: Term, punctuation: Any, newTruth: TruthValue, newBudget: BudgetValue) {
        val parentTask = memoryState.currentTask!!.parentTask
        if (parentTask != null && newContent == parentTask.content) { // circular structural inference
            return
        }
        val taskSentence = memoryState.currentTask!!.sentence
        if (taskSentence.isJudgment || memoryState.currentBelief == null) {
            memoryState.newStamp = Stamp(taskSentence.stamp, time)
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            memoryState.newStamp = Stamp(memoryState.currentBelief!!.stamp, time)
        }
        val newSentence = Sentence(newContent, punctuation, newTruth, memoryState.newStamp!!, taskSentence.revisible)
        val newTask = Task(newSentence, newBudget, memoryState.currentTask!!, null)
        derivedTask(newTask)
    }

    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept
     *
     * Called from Reasoner.tick only
     *
     * @param clock The current time to be displayed
     */
    override fun workCycle(clock: Long) {
        recorder.append(" --- $clock ---\n")
        processNewTask()
        if (noResult()) { // necessary?
            processNovelTask()
        }
        if (noResult()) { // necessary?
            processConcept()
        }
        memoryState.novelTasks!!.refresh()
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */
    override fun processNewTask() {
        var task: Task
        var counter = memoryState.newTasks.size // don't include new tasks produced in the current workCycle
        while (counter > 0) {
            counter--
            task = memoryState.newTasks.removeAt(0)
            if (!task.isInput && termToConcept(task.content!!) == null) {
                val s = task.sentence
                if (s.isJudgment) {
                    val d = s.truth!!.expectation.toDouble()
                    if (d > Parameters.DEFAULT_CREATION_EXPECTATION) {
                        memoryState.novelTasks!!.putIn(task) // new concept formation
                    } else {
                        recorder.append("!!! Neglected: $task\n")
                    }
                }
            } else { // new input or existing concept
                immediateProcess(task)
            }
        }
        counter--
    }

    /**
     * Select a novel task to process.
     */
    override fun processNovelTask() {
        val task = memoryState.novelTasks!!.takeOut() // select a task from novelTasks
        task?.let { immediateProcess(it) }
    }
    /* ---------- task processing ---------- */
    /**
     * Select a concept to fire.
     */
    override fun processConcept() {
        memoryState.currentConcept = memoryState.concepts!!.takeOut()
        if (memoryState.currentConcept != null) {
            memoryState.currentTerm = memoryState.currentConcept!!.term
            recorder.append(" * Selected Concept: " + memoryState.currentTerm + "\n")
            memoryState.concepts!!.putBack(memoryState.currentConcept) // current Concept remains in the bag all the time
            memoryState.currentConcept!!.fire() // a working workCycle
        }
    }
    /* ---------- display ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param task the task to be accepted
     */
    override fun immediateProcess(task: Task) {
        memoryState.currentTask = task // one of the two places where this variable is set
        recorder.append("!!! Insert: $task\n")
        memoryState.currentTerm = task.content
        memoryState.currentConcept = getConcept(memoryState.currentTerm!!)
        if (memoryState.currentConcept != null) {
            activateConcept(memoryState.currentConcept!!, task.budget)
            memoryState.currentConcept!!.directProcess(task)
        }
    }

    //    /**
//     * Display input/output sentence in the output channels. The only place to
//     * add Objects into exportStrings. Currently only Strings are added, though
//     * in the future there can be outgoing Tasks; also if exportStrings is empty
//     * display the current value of timer ( exportStrings is emptied in
//     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
//     *
//     * @param sentence the sentence to be displayed
//     * @param input    whether the task is input
//     */
    override fun report(sentence: Sentence, input: Boolean) {
        if (ReasonerBatch.DEBUG) {
            println("// report( clock " + memoryState.reasoner!!.time
                    + ", input " + input
                    + ", timer " + memoryState.reasoner!!.timer
                    + ", Sentence " + sentence
                    + ", exportStrings " + exportStrings)
            System.out.flush()
        }
        if (exportStrings.isEmpty()) {
            val timer = memoryState.reasoner!!.updateTimer()
            if (timer > 0) {
                exportStrings.add(timer.toString())
            }
        }
        var s: String
        s = if (input) {
            "  IN: "
        } else {
            " OUT: "
        }
        s += sentence.toStringBrief()
        exportStrings.add(s)
    }

    override fun toString(): String {
        return (toStringLongIfNotNull(memoryState.concepts, "concepts")
                + toStringLongIfNotNull(memoryState.novelTasks, "novelTasks")
                + toStringIfNotNull(memoryState.newTasks, "newTasks")
                + toStringLongIfNotNull(memoryState.currentTask, "currentTask")
                + toStringLongIfNotNull(memoryState.currentBeliefLink, "currentBeliefLink")
                + toStringIfNotNull(memoryState.currentBelief, "currentBelief"))
    }

    fun toStringLongIfNotNull(item: Bag<*>?, title: String): String = item?.let {
        val toStringLong = it.toStringLong()
        "\n $title:\n$toStringLong" }
            ?: ""

    private fun toStringLongIfNotNull(item: ItemIdentity?, title: String): String {
        return Optional.ofNullable(item).map { item1: ItemIdentity ->
            ("\n " + title + ":\n"
                    + item1.toStringLong())
        }.orElse("")
    }

    private fun toStringIfNotNull(item: Any?, title: String): String {
        return Optional.ofNullable(item).map { o: Any ->
            ("\n " + title + ":\n"
                    + o.toString())
        }.orElse("")
    }

    override val taskForgettingRate: AtomicInteger
        get() = memoryState.taskForgettingRate

    override val beliefForgettingRate: AtomicInteger
        get() = memoryState.beliefForgettingRate

    override val conceptForgettingRate: AtomicInteger
        get() = memoryState.conceptForgettingRate

    /**
     * Backward pointer to the reasoner
     */
    override val reasoner: ReasonerBatch
        get() = memoryState.reasoner

    /**
     * Concept bag. Containing all Concepts of the system
     */
    override val concepts: ConceptBag
        get() = memoryState.concepts

    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    override val novelTasks: NovelTaskBag?
        get() = memoryState.novelTasks

    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    override val newTasks: MutableCollection<Task>
        get() = memoryState.newTasks

    /**
     * The selected Term
     */
    override var currentTerm: Term?
        get() = memoryState.currentTerm
        set(currentTerm) {
            memoryState.currentTerm = currentTerm
        }

    /**
     * The selected Concept
     */
    override var currentConcept: Concept?
        get() = memoryState.currentConcept
        set(currentConcept) {
            memoryState.currentConcept = currentConcept
        }

    /**
     * The selected TaskLink
     */
    override var currentTaskLink: TaskLink?
        get() = memoryState.currentTaskLink
        set(currentTaskLink) {
            memoryState.currentTaskLink = currentTaskLink
        }

    /**
     * The selected Task
     */
    override var currentTask: Task?
        get() = memoryState.currentTask
        set(currentTask) {
            memoryState.currentTask = currentTask
        }

    /**
     * The selected TermLink
     */
    override var currentBeliefLink: TermLink?
        get() = memoryState.currentBeliefLink
        set(currentBeliefLink) {
            memoryState.currentBeliefLink = currentBeliefLink
        }

    /**
     * The selected belief
     */
    override var currentBelief: Sentence?
        get() = memoryState.currentBelief
        set(currentBelief) {
            memoryState.currentBelief = currentBelief
        }

    /**
     * The new Stamp
     */
    override var newStamp: Stamp?
        get() = memoryState.newStamp
        set(newStamp) {
            memoryState.newStamp = newStamp
        }

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    override fun getSubstitute(): Map<Term, Term> {
        return memoryState.substitute
    }

    override fun setSubstitute(substitute: Map<Term, Term>) {
        memoryState.substitute = substitute
    }
    /* ---------- Constructor ---------- */
    /**
     * Create a new memory
     *
     * Called in Reasoner.reset only
     *
     * @param reasoner
     */
    init {
        memoryState.reasoner = reasoner
        memoryState.recorder = NullInferenceRecorder()
        memoryState.concepts = ConceptBag(this)
        memoryState.novelTasks = NovelTaskBag(this)
        memoryState.newTasks= mutableListOf()
        memoryState.exportStrings = mutableListOf()
    }
}