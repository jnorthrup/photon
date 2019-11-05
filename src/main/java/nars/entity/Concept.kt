/*
 * Concept.java
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
package nars.entity

import nars.inference.BudgetFunctions
import nars.inference.LocalRules
import nars.inference.RuleTables
import nars.inference.UtilityFunctions
import nars.language.CompoundTerm
import nars.language.Term
import nars.main_nogui.NARSBatch
import nars.main_nogui.Parameters
import nars.storage.*
import java.util.*

/**
 * A concept contains information associated with a term, including directly and
 * indirectly related tasks and beliefs.
 *
 *
 * To make sure the space will be released, the only allowed reference to a
 * concept are those in a ConceptBag. All other access go through the Term that
 * names the concept.
 */
class Concept(
        /**
         * The term is the unique ID of the concept
         */
        val term: Term,
        /**
         * Reference to the memory
         */
        internal var memory: BackingStore) : ImmutableItemIdentity(term.name) {
    /**
     * Return the associated term, called from BackingStore only
     *
     * @return The associated term
     */
    /**
     * Task links for indirect processing
     */
    private val taskLinks: TaskLinkBag
    /**
     * Term links between the term and its components and compounds
     */
    private val termLinks: TermLinkBag
    /**
     * Question directly asked about the term
     */
    private val questions: MutableList<Task>?
    /**
     * Sentences directly made about the term, with non-future tense
     */
    private val beliefs: MutableList<Sentence>
    /**
     * Link templates of TermLink, only in concepts with CompoundTerm jmv TODO
     * explain more
     */
    private var termLinkTemplates: List<TermLink>? = null
    /**
     * The display window
     */
    private val entityObserver: EntityObserver = NullEntityObserver()


    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     *
     *
     * called in BackingStore.immediateProcess only
     *
     * @param task The task to be processed
     */
    fun directProcess(task: Task) {
        if (task.sentence.isJudgment) {
            processJudgment(task)
        } else {
            processQuestion(task)
        }
        if (task.budget.aboveThreshold()) {    // still need to be processed

            linkToTask(task)
        }
        entityObserver.refresh(displayContent())
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and
     * solutions
     *
     * @param judg The judgment to be accepted
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private fun processJudgment(task: Task) {
        val judg: Sentence = task.sentence
        val oldBelief = evaluation(judg, beliefs)
        if (oldBelief != null) {
            val newStamp = judg.stamp
            val oldStamp = oldBelief.stamp
            if (newStamp == oldStamp) {
                if (task.parentTask!!.sentence.isJudgment) {
                    task.budget.decPriority(0f)    // duplicated task
                }   // else: activated belief

                return
            } else if (LocalRules.revisible(judg, oldBelief)) {
                memory.newStamp = Stamp.make(newStamp, oldStamp, memory.time)
                if (memory.newStamp != null) {
                    memory.currentBelief = oldBelief
                    LocalRules.revision(judg, oldBelief, false, memory)
                }
            }
        }
        if (task.budget.aboveThreshold()) {
            //                LocalRules.trySolution(ques.getSentence(), judg, ques, memory);

            for (ques in questions!!) {
                LocalRules.trySolution(judg, ques, memory)
            }
            addToTable(judg, beliefs, Parameters.MAXIMUM_BELIEF_LENGTH)
        }
    }

    /**
     * To answer a question by existing beliefs
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private fun processQuestion(task: Task): Float {
        var ques: Sentence = task.sentence
        var newQuestion = true
        if (questions != null) {
            for (t in questions) {
                val q: Sentence = t.sentence
                if (q.content == ques.content) {
                    ques = q
                    newQuestion = false
                    break
                }
            }
        }
        if (newQuestion) {
            questions!!.add(task)
        }
        if (questions!!.size > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
            questions.removeAt(0)    // FIFO
        }
        val newAnswer = evaluation(ques, beliefs)
        return if (newAnswer != null) {
//            LocalRules.trySolution(ques, newAnswer, task, memory);

            LocalRules.trySolution(newAnswer, task, memory)
            newAnswer.truth!!.expectation
        } else {
            0.5f
        }
    }

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     *
     *
     * The only method that calls the TaskLink constructor.
     *
     * @param task    The task to be linked
     * @param content The content of the task
     */
    private fun linkToTask(task: Task) {
        val taskBudget = task.budget
        var taskLink = TaskLink(task, null, taskBudget)   // link type: SELF

        insertTaskLink(taskLink)
        if (term is CompoundTerm) {
            if (termLinkTemplates!!.isNotEmpty()) {
                val subBudget: BudgetValue = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates!!.size)
                if (subBudget.aboveThreshold()) {
                    var componentTerm: Term
                    var componentConcept: Concept?
                    for (termLink in termLinkTemplates!!) {
//                        if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) { // avoid circular transform
                        taskLink = TaskLink(task, termLink, subBudget)
                        componentTerm = termLink.target
                        componentConcept = memory.getConcept(componentTerm)
                        componentConcept?.insertTaskLink(taskLink)
//                        }

                    }
                    buildTermLinks(taskBudget)  // recursively insert TermLink
                }
            }
        }
    }

    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     * @param newSentence The judgment to be processed
     * @param table       The table to be revised
     * @param capacity    The capacity of the table
     */
    private fun addToTable(newSentence: Sentence, table: MutableList<Sentence>, capacity: Int) {
        val rank1 = BudgetFunctions.rankBelief(newSentence)    // for the new isBelief

        var judgment2: Sentence
        var rank2: Float
        var i = 0

        while (i < table.size) {
            judgment2 = table[i]
            rank2 = BudgetFunctions.rankBelief(judgment2)
            if (rank1 >= rank2) {
                if (!newSentence.equivalentTo(judgment2)) {
                    table.add(i, newSentence)
                    break
                }
                return
            }
            i++
        }
        if (table.size >= capacity)
            while (table.size > capacity)
                table.removeAt(table.size - 1)
        else
            if (i == table.size)
                table.add(newSentence)
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     *
     * @param query The question to be processed
     * @param list  The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private fun evaluation(query: Sentence, list: Iterable<Sentence>?): Sentence? {
        if (list == null) {
            return null
        }
        var currentBest = 0f
        var beliefQuality: Float
        var candidate: Sentence? = null
        for (judg in list) {
            beliefQuality = LocalRules.solutionQuality(query, judg)
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality
                candidate = judg
            }
        }
        return candidate
    }

    /* ---------- insert Links for indirect processing ---------- */


    /**
     * Insert a TaskLink into the TaskLink bag
     *
     *
     * called only from BackingStore.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    private fun insertTaskLink(taskLink: TaskLink) {
        val taskBudget = taskLink.budget
        taskLinks.putIn(taskLink)
        memory.activateConcept(this, taskBudget)
    }

    /**
     * Recursively build TermLinks between a compound and its components
     *
     *
     * called only from BackingStore.continuedProcess
     *
     * @param taskBudget The BudgetValue of the task
     */
    private fun buildTermLinks(taskBudget: BudgetValue) {
        var t: Term
        var concept: Concept?
        var termLink1: TermLink
        var termLink2: TermLink
        if (termLinkTemplates!!.isNotEmpty()) {
            val subBudget: BudgetValue = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates!!.size)
            if (subBudget.aboveThreshold()) {
                for (template in termLinkTemplates!!) {
                    if (template.type != TermLinkConstants.TRANSFORM) {
                        t = template.target
                        concept = memory.getConcept(t)
                        if (concept != null) {
                            termLink1 = TermLink(t, template, subBudget)
                            insertTermLink(termLink1)   // this termLink to that

                            termLink2 = TermLink(term, template, subBudget)
                            concept.insertTermLink(termLink2)   // that termLink to this

                            if (t is CompoundTerm) {
                                concept.buildTermLinks(subBudget)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     *
     *
     * called from buildTermLinks only
     *
     * @param termLink The termLink to be inserted
     */
    private fun insertTermLink(termLink: TermLink) {
        termLinks.putIn(termLink)
    }

    /* ---------- access local information ---------- */


    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */

    override fun toString(): String {  // called from concept bag

        return if (NARSBatch.standAlone) {
            super.toStringBrief() + " " + key
        } else {
            key
        }
    }

    /**
     * called from [NARSBatch]
     */

    override fun toStringLong(): String? {
        var res = (toStringBrief() + " " + key
                + toStringIfNotNull(termLinks, "termLinks")
                + toStringIfNotNull(taskLinks, "taskLinks"))
        res += toStringIfNotNull(null, "questions")
        for (t in questions!!) {
            res += t.toString()
        }
        // TODO other details?


        return res
    }

    private fun toStringIfNotNull(item: Any?, title: String): String {
        return Optional.ofNullable(item).map { o: Any -> "\n $title:$o" }.orElse("")
    }

    /**
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     *
     * @return The quality value
     */
    override var quality: Float
        get() {
            val linkPriority = termLinks.averagePriority()
            val termComplexityFactor = 1.0f / term.complexity
            return UtilityFunctions.or(linkPriority, termComplexityFactor)
        }
        set(quality) {
            super.quality = quality
        }

    /**
     * Select a isBelief to interact with the given task in inference
     *
     *
     * get the first qualified one
     *
     *
     * only called in RuleTables.reason
     *
     * @param task The selected task
     * @return The selected isBelief
     */
    fun getBelief(task: Task): Sentence? {
        val taskSentence: Sentence = task.sentence
        var belief: Sentence
        for (sentence in beliefs) {
            belief = sentence
            memory.recorder!!.append(" * Selected Belief: $belief\n")
            memory.newStamp = Stamp.make(taskSentence.stamp, belief.stamp, memory.time)
            if (memory.newStamp != null) {
                return belief.clone() as Sentence
            }
        }
        return null
    }

    /* ---------- main loop ---------- */


    /**
     * An atomic step in a concept, only called in [BackingStore.processConcept]
     */
    fun fire() {
        val currentTaskLink = taskLinks.takeOut() ?: return
        memory.currentTaskLink = currentTaskLink
        memory.currentBeliefLink = null
        memory.recorder!!.append(" * Selected TaskLink: $currentTaskLink\n")
        memory.currentTask = currentTaskLink.targetTask
//      memory.getRecorder().append(" * Selected Task: " + task + "\n");    // for debugging
        if (currentTaskLink.type == TermLinkConstants.TRANSFORM) {
            memory.currentBelief = null
            RuleTables.transformTask(currentTaskLink, memory)  // to turn this into structural inference as below?
        } else {
            var termLinkCount = Parameters.MAX_REASONED_TERM_LINK
//        while (memory.noResult() && (termLinkCount > 0)) {


            while (termLinkCount > 0) {
                val termLink: TermLink? = termLinks.takeOut(currentTaskLink, memory.time)
                if (termLink != null) {
                    memory.recorder!!.append(" * Selected TermLink: $termLink\n")
                    memory.currentBeliefLink = termLink
                    RuleTables.reason(currentTaskLink, termLink, memory)
                    termLinks.putBack(termLink)
                    termLinkCount--
                } else {
                    termLinkCount = 0
                }
            }
        }
        taskLinks.putBack(currentTaskLink)
    }

    /* ---------- display ---------- */


    /**
     * Collect direct isBelief, questions, and goals for display
     *
     * @return String representation of direct content
     */
    private fun displayContent(): String {
        val buffer = StringBuilder()
        buffer.append("\n  Beliefs:\n")
        if (beliefs.size > 0) {
            for (s in beliefs) {
                buffer.append(s).append("\n")
            }
        }
        buffer.append("\n  Question:\n")
        if (questions!!.size > 0) {
            for (t in questions) {
                buffer.append(t).append("\n")
            }
        }
        return buffer.toString()
    }

    internal inner class NullEntityObserver : EntityObserver {
        override fun post(str: String?) {}
        override fun createBagObserver(): BagObserver<TermLink?>? {
            return NullBagObserver()
        }

        override fun startPlay(concept: Concept?, showLinks: Boolean) {}
        override fun stop() {}
        override fun refresh(message: String?) {}
    }


    /**
     * Constructor, called in BackingStore.getConcept only
     *
     * @param tm     A term corresponding to the concept
     * @param memory A reference to the memory
     */

    init {
        questions = ArrayList()
        beliefs = ArrayList()
        taskLinks = TaskLinkBag(memory)
        termLinks = TermLinkBag(memory)
        if (term is CompoundTerm) {
            termLinkTemplates = term.prepareComponentLinks() as List<TermLink>?
        }
    }

}