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
package nars.storage;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.io.IInferenceRecorder;
import nars.language.Term;
import nars.main_nogui.Parameters;
import nars.main_nogui.ReasonerBatch;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The memory of the system.
 */
public class BackingStore implements MemoryOps {

    public final MemoryState memoryState = new MemoryState();


    /* ---------- Constructor ---------- */

    /**
     * Create a new memory <p> Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public BackingStore(ReasonerBatch reasoner) {
        this.memoryState.setReasoner(reasoner);
        memoryState.setRecorder(new NullInferenceRecorder());
        memoryState.setConcepts(new ConceptBag(this));
        memoryState.setNovelTasks(new NovelTaskBag(this));
        memoryState.setNewTasks(new ArrayList<>());
        memoryState.setExportStrings(new ArrayList<String>());
    }

    @Override
    public void clear() {
        memoryState.getConcepts().init();
        memoryState.getNovelTasks().init();
        memoryState.getNewTasks().clear();
        getExportStrings().clear();
        memoryState.getReasoner().initTimer();
        getRecorder().append("\n-----RESET-----\n");
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     *
     * @return
     */ /* ---------- access utilities ---------- */
    @Override
    public ArrayList<String> getExportStrings() {
        return (ArrayList<String>) memoryState.getExportStrings();
    }

    /**
     * Inference record text to be written into a log file
     */
    @Override
    public IInferenceRecorder getRecorder() {
        return memoryState.getRecorder();
    }

    @Override
    public void setRecorder(IInferenceRecorder recorder) {
        memoryState.setRecorder(recorder);
    }

//    public MainWindow getMainWindow() {
//        return reasoner.getMainWindow();
//    }

    @Override
    public long getTime() {
        return memoryState.getReasoner().getTime();
    }

    /* ---------- conversion utilities ---------- */

    /**
     * Actually means that there are no new Tasks
     */
    @Override
    public boolean noResult() {
        return memoryState.getNewTasks().isEmpty();
    }

    /**
     * Get an existing Concept for a given name <p> called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    @Override
    public Concept nameToConcept(String name) {
        return memoryState.getConcepts().get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator <p> called in
     * StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    @Override
    public Term nameToListedTerm(String name) {
        var concept = memoryState.getConcepts().get(name);
        if (concept != null) {
            return concept.getTerm();
        }
        return null;
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    @Override
    public Concept termToConcept(Term term) {
        return nameToConcept(term.getName());
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    @Override
    public Concept getConcept(Term term) {
        if (!term.isConstant()) {
            return null;
        }
        var n = term.getName();
        var concept = memoryState.getConcepts().get(n);
        if (concept == null) {
            concept = new Concept(term, this); // the only place to make a new Concept
            var created = memoryState.getConcepts().putIn(concept);
            if (!created) {
                return null;
            }
        }
        return concept;
    }

    /* ---------- adjustment functions ---------- */

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    @Override
    public float getConceptActivation(Term t) {
        var c = termToConcept(t);
        return Optional.ofNullable(c).map(ImmutableItemIdentity::getPriority).orElse(0f);
    }

    /* ---------- new task entries ---------- */

    /* There are several types of new tasks, all added into the
     newTasks list, to be processed in the next workCycle.
     Some of them are reported and/or logged. */

    /**
     * Adjust the activation level of a Concept <p> called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    @Override
    public void activateConcept(Concept c, BudgetValue b) {
        memoryState.getConcepts().pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        memoryState.getConcepts().putBack(c);
    }

    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
    @Override
    public void inputTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            getRecorder().append("!!! Perceived: " + task + "\n");
            report(task.getSentence(), true);    // report input
            memoryState.getNewTasks().add(task);       // wait to be processed in the next workCycle
        } else {
            getRecorder().append("!!! Neglected: " + task + "\n");
        }
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget          The budget value of the new Task
     * @param sentence        The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     *                        forward/backward correspondence
     */
    @Override
    public void activatedTask(BudgetValue budget, Sentence sentence, Sentence candidateBelief) {
        var task = new Task(sentence, budget, memoryState.getCurrentTask(), sentence, candidateBelief);
        getRecorder().append("!!! Activated: " + task.toString() + "\n");
        if (sentence.isQuestion()) {
            var s = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            var minSilent = memoryState.getReasoner().getSilenceValue().get() / 100.0f;
            if (s > minSilent) {  // only report significant derived Tasks
                report(task.getSentence(), false);
            }
        }
        memoryState.getNewTasks().add(task);
    }

    /* --------------- new task building --------------- */

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    @Override
    public void derivedTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            getRecorder().append("!!! Derived: " + task + "\n");
            var budget = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            var minSilent = memoryState.getReasoner().getSilenceValue().get() / 100.0f;
            if (budget > minSilent) {  // only report significant derived Tasks
                report(task.getSentence(), false);
            }
            memoryState.getNewTasks().add(task);
        } else {
            getRecorder().append("!!! Ignored: " + task + "\n");
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
    @Override
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        if (newContent != null) {
            var newSentence = new Sentence(newContent, memoryState.getCurrentTask().getSentence().getPunctuation(), newTruth, memoryState.getNewStamp());
            var newTask = new Task(newSentence, newBudget, memoryState.getCurrentTask(), memoryState.getCurrentBelief());
            derivedTask(newTask);
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
    @Override
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {
        if (newContent != null) {
            var taskSentence = memoryState.getCurrentTask().getSentence();
            var newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, memoryState.getNewStamp(), revisible);
            var newTask = new Task(newSentence, newBudget, memoryState.getCurrentTask(), memoryState.getCurrentBelief());
            derivedTask(newTask);
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
    @Override
    public void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(newContent, memoryState.getCurrentTask().getSentence().getPunctuation(), newTruth, newBudget);
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
    @Override
    public void singlePremiseTask(Term newContent, Object  punctuation, TruthValue newTruth, BudgetValue newBudget) {
        var parentTask = memoryState.getCurrentTask().getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }
        var taskSentence = memoryState.getCurrentTask().getSentence();
        if (taskSentence.isJudgment() || memoryState.getCurrentBelief() == null) {
            memoryState.setNewStamp(new Stamp(taskSentence.getStamp(), getTime()));
        } else {    // to answer a question with negation in NAL-5 --- move to activated task?
            memoryState.setNewStamp(new Stamp(memoryState.getCurrentBelief().getStamp(), getTime()));
        }
        var newSentence = new Sentence(newContent, punctuation, newTruth, memoryState.getNewStamp(), taskSentence.getRevisible());
        var newTask = new Task(newSentence, newBudget, memoryState.getCurrentTask(), null);
        derivedTask(newTask);
    }

    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept <p> Called from Reasoner.tick only
     *
     * @param clock The current time to be displayed
     */
    @Override
    public void workCycle(long clock) {
        getRecorder().append(" --- " + clock + " ---\n");
        processNewTask();
        if (noResult()) {       // necessary?
            processNovelTask();
        }
        if (noResult()) {       // necessary?
            processConcept();
        }
        memoryState.getNovelTasks().refresh();
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */
    @Override
    public void processNewTask() {
        Task task;
        var counter = memoryState.getNewTasks().size();  // don't include new tasks produced in the current workCycle
        while (counter > 0) {
            counter--;
            task = memoryState.getNewTasks().remove(0);
            if (!task.isInput() && (termToConcept(task.getContent()) == null)) {
                var s = task.getSentence();
                if (s.isJudgment()) {
                    double d = s.getTruth().getExpectation();
                    if (d > Parameters.DEFAULT_CREATION_EXPECTATION) {
                        memoryState.getNovelTasks().putIn(task);    // new concept formation
                    } else {
                        getRecorder().append("!!! Neglected: " + task + "\n");
                    }
                }
            } else { // new input or existing concept
                immediateProcess(task);
            }
        }
        counter--;
    }

    /**
     * Select a novel task to process.
     */
    @Override
    public void processNovelTask() {
        var task = memoryState.getNovelTasks().takeOut();       // select a task from novelTasks
        if (task != null) {
            immediateProcess(task);
        }
    }

    /* ---------- task processing ---------- */

    /**
     * Select a concept to fire.
     */
    @Override
    public void processConcept() {
        memoryState.setCurrentConcept(memoryState.getConcepts().takeOut());
        if (memoryState.getCurrentConcept() != null) {
            memoryState.setCurrentTerm(memoryState.getCurrentConcept().getTerm());
            getRecorder().append(" * Selected Concept: " + memoryState.getCurrentTerm() + "\n");
            memoryState.getConcepts().putBack(memoryState.getCurrentConcept());   // current Concept remains in the bag all the time
            memoryState.getCurrentConcept().fire();              // a working workCycle
        }
    }

    /* ---------- display ---------- */

    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param task the task to be accepted
     */
    @Override
    public void immediateProcess(Task task) {
        memoryState.setCurrentTask(task); // one of the two places where this variable is set
        getRecorder().append("!!! Insert: " + task + "\n");
        memoryState.setCurrentTerm(task.getContent());
        memoryState.setCurrentConcept(getConcept(memoryState.getCurrentTerm()));
        if (memoryState.getCurrentConcept() != null) {
            activateConcept(memoryState.getCurrentConcept(), task.getBudget());
            memoryState.getCurrentConcept().directProcess(task);
        }
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
     *
     * @param sentence the sentence to be displayed
     * @param input    whether the task is input
     */
    @Override
    public void report(Sentence sentence, boolean input) {
        if (ReasonerBatch.DEBUG) {
            System.out.println("// report( clock " + memoryState.getReasoner().getTime()
                    + ", input " + input
                    + ", timer " + memoryState.getReasoner().getTimer()
                    + ", Sentence " + sentence
                    + ", exportStrings " + getExportStrings());
            System.out.flush();
        }
        if (getExportStrings().isEmpty()) {
            var timer = memoryState.getReasoner().updateTimer();
            if (timer > 0) {
                getExportStrings().add(String.valueOf(timer));
            }
        }
        String s;
        if (input) {
            s = "  IN: ";
        } else {
            s = " OUT: ";
        }
        s += sentence.toStringBrief();
        getExportStrings().add(s);
    }

    public String toString() {
        return toStringLongIfNotNull(memoryState.getConcepts(), "concepts")
                + toStringLongIfNotNull(memoryState.getNovelTasks(), "novelTasks")
                + toStringIfNotNull(memoryState.getNewTasks(), "newTasks")
                + toStringLongIfNotNull(memoryState.getCurrentTask(), "currentTask")
                + toStringLongIfNotNull(memoryState.getCurrentBeliefLink(), "currentBeliefLink")
                + toStringIfNotNull(memoryState.getCurrentBelief(), "currentBelief");
    }

    private String toStringLongIfNotNull(Bag<?> item, String title) {
        return Optional.ofNullable(item).map(bag -> "\n " + title + ":\n"
                + bag.toStringLong()).orElse("");
    }

    private String toStringLongIfNotNull(ItemIdentity item, String title) {
        return Optional.ofNullable(item).map(item1 -> "\n " + title + ":\n"
                + item1.toStringLong()).orElse("");
    }

    private String toStringIfNotNull(Object item, String title) {
        return Optional.ofNullable(item).map(o -> "\n " + title + ":\n"
                + o.toString()).orElse("");
    }

    @Override
    public AtomicInteger getTaskForgettingRate() {
        return memoryState.getTaskForgettingRate();
    }

    @Override
    public AtomicInteger getBeliefForgettingRate() {
        return memoryState.getBeliefForgettingRate();
    }

    @Override
    public AtomicInteger getConceptForgettingRate() {
        return memoryState.getConceptForgettingRate();
    }

    /**
     * Backward pointer to the reasoner
     */
    @Override
    public ReasonerBatch getReasoner() {
        return memoryState.getReasoner();
    }

    /**
     * Concept bag. Containing all Concepts of the system
     */
    @Override
    public ConceptBag getConcepts() {
        return memoryState.getConcepts();
    }

    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    @Override
    public NovelTaskBag getNovelTasks() {
        return memoryState.getNovelTasks();
    }

    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    @Override
    public List<Task> getNewTasks() {
        return memoryState.getNewTasks();
    }

    /**
     * The selected Term
     */
    @Override
    public Term getCurrentTerm() {
        return memoryState.getCurrentTerm();
    }

    @Override
    public void setCurrentTerm(Term currentTerm) {
        memoryState.setCurrentTerm(currentTerm);
    }

    /**
     * The selected Concept
     */
    @Override
    public Concept getCurrentConcept() {
        return memoryState.getCurrentConcept();
    }

    @Override
    public void setCurrentConcept(Concept currentConcept) {
        memoryState.setCurrentConcept(currentConcept);
    }

    /**
     * The selected TaskLink
     */
    @Override
    public TaskLink getCurrentTaskLink() {
        return memoryState.getCurrentTaskLink();
    }

    @Override
    public void setCurrentTaskLink(TaskLink currentTaskLink) {
        memoryState.setCurrentTaskLink(currentTaskLink);
    }

    /**
     * The selected Task
     */
    @Override
    public Task getCurrentTask() {
        return memoryState.getCurrentTask();
    }

    @Override
    public void setCurrentTask(Task currentTask) {
        memoryState.setCurrentTask(currentTask);
    }

    /**
     * The selected TermLink
     */
    @Override
    @org.jetbrains.annotations.Nullable
    public TermLink getCurrentBeliefLink() {
        return memoryState.getCurrentBeliefLink();
    }

    @Override
    public void setCurrentBeliefLink(@org.jetbrains.annotations.Nullable TermLink currentBeliefLink) {
        memoryState.setCurrentBeliefLink(currentBeliefLink);
    }

    /**
     * The selected belief
     */
    @Override
    @org.jetbrains.annotations.Nullable
    public Sentence getCurrentBelief() {
        return memoryState.getCurrentBelief();
    }

    @Override
    public void setCurrentBelief(@org.jetbrains.annotations.Nullable Sentence currentBelief) {
        memoryState.setCurrentBelief(currentBelief);
    }

    /**
     * The new Stamp
     */
    @Override
    public Stamp getNewStamp() {
        return memoryState.getNewStamp();
    }

    @Override
    public void setNewStamp(Stamp newStamp) {
        memoryState.setNewStamp(newStamp);
    }

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    @Override
    public Map<Term, Term> getSubstitute() {
        return memoryState.getSubstitute();
    }

    @Override
    public void setSubstitute(@Nullable Map<Term, ? extends Term> substitute) {
        memoryState.setSubstitute(substitute);
    }

}
