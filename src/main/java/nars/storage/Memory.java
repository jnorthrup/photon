/*
 * Memory.java
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
public class Memory {

    public final MemoryState memoryState = new MemoryState();


    /* ---------- Constructor ---------- */

    /**
     * Create a new memory <p> Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public Memory(ReasonerBatch reasoner) {
        this.memoryState.setReasoner(reasoner);
        memoryState.setRecorder(new NullInferenceRecorder());
        memoryState.setConcepts(new ConceptBag(this));
        memoryState.setNovelTasks(new NovelTaskBag(this));
        memoryState.setNewTasks(new ArrayList<>());
        memoryState.setExportStrings(new ArrayList<>());
    }

    public void init() {
        memoryState.getConcepts().init();
        memoryState.getNovelTasks().init();
        memoryState.getNewTasks().clear();
        getExportStrings().clear();
        memoryState.getReasoner().initTimer();
        getRecorder().append("\n-----RESET-----\n");
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     */ /* ---------- access utilities ---------- */
    public List<String> getExportStrings() {
        return memoryState.getExportStrings();
    }

    /**
     * Inference record text to be written into a log file
     */
    public IInferenceRecorder getRecorder() {
        return memoryState.getRecorder();
    }

    public long getTime() {
        return memoryState.getReasoner().getTime();
    }

//    public MainWindow getMainWindow() {
//        return reasoner.getMainWindow();
//    }

    /**
     * Actually means that there are no new Tasks
     */
    public boolean noResult() {
        return memoryState.getNewTasks().isEmpty();
    }

    /* ---------- conversion utilities ---------- */

    /**
     * Get an existing Concept for a given name <p> called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
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
    public Concept termToConcept(Term term) {
        return nameToConcept(term.getName());
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
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

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    public float getConceptActivation(Term t) {
        var c = termToConcept(t);
        return Optional.ofNullable(c).map(ImmutableItemIdentity::getPriority).orElse(0f);
    }

    /* ---------- adjustment functions ---------- */

    /**
     * Adjust the activation level of a Concept <p> called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(Concept c, BudgetValue b) {
        memoryState.getConcepts().pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        memoryState.getConcepts().putBack(c);
    }

    /* ---------- new task entries ---------- */

    /* There are several types of new tasks, all added into the
     newTasks list, to be processed in the next workCycle.
     Some of them are reported and/or logged. */

    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
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

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    private void derivedTask(Task task) {
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

    /* --------------- new task building --------------- */

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
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
    public void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(newContent, memoryState.getCurrentTask().getSentence().getPunctuation(), newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public void singlePremiseTask(Term newContent, char punctuation, TruthValue newTruth, BudgetValue newBudget) {
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

    /* ---------- system working workCycle ---------- */

    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept <p> Called from Reasoner.tick only
     *
     * @param clock The current time to be displayed
     */
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
    private void processNewTask() {
        Task task;
        var counter = memoryState.getNewTasks().size();  // don't include new tasks produced in the current workCycle
        while (counter > 0) {
            counter--;
            task = memoryState.getNewTasks().remove(0);
            if (task.isInput() || (termToConcept(task.getContent()) != null)) { // new input or existing concept
                immediateProcess(task);
            } else {
                var s = task.getSentence();
                if (s.isJudgment()) {
                    double d = s.getTruth().getExpectation();
                    if (d > Parameters.DEFAULT_CREATION_EXPECTATION) {
                        memoryState.getNovelTasks().putIn(task);    // new concept formation
                    } else {
                        getRecorder().append("!!! Neglected: " + task + "\n");
                    }
                }
            }
        }
        counter--;
    }

    /**
     * Select a novel task to process.
     */
    private void processNovelTask() {
        var task = memoryState.getNovelTasks().takeOut();       // select a task from novelTasks
        if (task != null) {
            immediateProcess(task);
        }
    }

    /**
     * Select a concept to fire.
     */
    private void processConcept() {
        memoryState.setCurrentConcept(memoryState.getConcepts().takeOut());
        if (memoryState.getCurrentConcept() != null) {
            memoryState.setCurrentTerm(memoryState.getCurrentConcept().getTerm());
            getRecorder().append(" * Selected Concept: " + memoryState.getCurrentTerm() + "\n");
            memoryState.getConcepts().putBack(memoryState.getCurrentConcept());   // current Concept remains in the bag all the time
            memoryState.getCurrentConcept().fire();              // a working workCycle
        }
    }

    /* ---------- task processing ---------- */

    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param task the task to be accepted
     */
    private void immediateProcess(Task task) {
        memoryState.setCurrentTask(task); // one of the two places where this variable is set
        getRecorder().append("!!! Insert: " + task + "\n");
        memoryState.setCurrentTerm(task.getContent());
        memoryState.setCurrentConcept(getConcept(memoryState.getCurrentTerm()));
        if (memoryState.getCurrentConcept() != null) {
            activateConcept(memoryState.getCurrentConcept(), task.getBudget());
            memoryState.getCurrentConcept().directProcess(task);
        }
    }

    /* ---------- display ---------- */

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

    public AtomicInteger getTaskForgettingRate() {
        return memoryState.getTaskForgettingRate();
    }

    public AtomicInteger getBeliefForgettingRate() {
        return memoryState.getBeliefForgettingRate();
    }

    public AtomicInteger getConceptForgettingRate() {
        return memoryState.getConceptForgettingRate();
    }

    /**
     * Backward pointer to the reasoner
     */
    public ReasonerBatch getReasoner() {
        return memoryState.getReasoner();
    }

    /**
     * Concept bag. Containing all Concepts of the system
     */
    public ConceptBag getConcepts() {
        return memoryState.getConcepts();
    }

    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    public NovelTaskBag getNovelTasks() {
        return memoryState.getNovelTasks();
    }

    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    public List<Task> getNewTasks() {
        return memoryState.getNewTasks();
    }

    /**
     * The selected Term
     */
    public Term getCurrentTerm() {
        return memoryState.getCurrentTerm();
    }

    public void setCurrentTerm(Term currentTerm) {
        memoryState.setCurrentTerm(currentTerm);
    }

    /**
     * The selected Concept
     */
    public Concept getCurrentConcept() {
        return memoryState.getCurrentConcept();
    }

    public void setCurrentConcept(Concept currentConcept) {
        memoryState.setCurrentConcept(currentConcept);
    }

    /**
     * The selected TaskLink
     */
    public TaskLink getCurrentTaskLink() {
        return memoryState.getCurrentTaskLink();
    }

    public void setCurrentTaskLink(TaskLink currentTaskLink) {
        memoryState.setCurrentTaskLink(currentTaskLink);
    }

    /**
     * The selected Task
     */
    public Task getCurrentTask() {
        return memoryState.getCurrentTask();
    }

    public void setCurrentTask(Task currentTask) {
        memoryState.setCurrentTask(currentTask);
    }

    /**
     * The selected TermLink
     */
    @org.jetbrains.annotations.Nullable
    public TermLink getCurrentBeliefLink() {
        return memoryState.getCurrentBeliefLink();
    }

    public void setCurrentBeliefLink(@org.jetbrains.annotations.Nullable TermLink currentBeliefLink) {
        memoryState.setCurrentBeliefLink(currentBeliefLink);
    }

    /**
     * The selected belief
     */
    @org.jetbrains.annotations.Nullable
    public Sentence getCurrentBelief() {
        return memoryState.getCurrentBelief();
    }

    public void setCurrentBelief(@org.jetbrains.annotations.Nullable Sentence currentBelief) {
        memoryState.setCurrentBelief(currentBelief);
    }

    /**
     * The new Stamp
     */
    public Stamp getNewStamp() {
        return memoryState.getNewStamp();
    }

    public void setNewStamp(Stamp newStamp) {
        memoryState.setNewStamp(newStamp);
    }

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    public Map<Term, Term> getSubstitute() {
        return memoryState.getSubstitute();
    }

    public void setSubstitute(Map<Term, Term> substitute) {
        memoryState.setSubstitute(substitute);
    }

    public void setRecorder(IInferenceRecorder recorder) {
        memoryState.setRecorder(recorder);
    }

    public static class MemoryState {
        private   ReasonerBatch reasoner;/* ---------- Long-term storage for multiple cycles ---------- */
        private   ConceptBag concepts;
        private   NovelTaskBag novelTasks;
        private   AtomicInteger beliefForgettingRate;
        private   AtomicInteger taskForgettingRate;
        private   AtomicInteger conceptForgettingRate;
        private   List<Task> newTasks;/* ---------- Short-term workspace for a single cycle ---------- */
        private   List<String> exportStrings;

        public List<String> getExportStrings() {
            return exportStrings;
        }

        public void setExportStrings(List<String> exportStrings) {
            this.exportStrings = exportStrings;
        }

        public Term currentTerm;
        public Concept currentConcept;
        public TaskLink currentTaskLink;
        public Task currentTask;
        @Nullable
        public TermLink currentBeliefLink;
        @Nullable
        public Sentence currentBelief;
        public Stamp newStamp;
        protected Map<Term, Term> substitute;
        private IInferenceRecorder recorder;

        public IInferenceRecorder getRecorder() {
            return recorder;
        }

        public MemoryState() {
            this.beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
            this.taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
            this.conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);
        }

        public AtomicInteger getTaskForgettingRate() {
            return taskForgettingRate;
        }

        public AtomicInteger getBeliefForgettingRate() {
            return beliefForgettingRate;
        }

        public AtomicInteger getConceptForgettingRate() {
            return conceptForgettingRate;
        }

        /**
         * Backward pointer to the reasoner
         */
        public ReasonerBatch getReasoner() {
            return reasoner;
        }

        /**
         * Concept bag. Containing all Concepts of the system
         */
        public ConceptBag getConcepts() {
            return concepts;
        }

        /**
         * New tasks with novel composed terms, for delayed and selective processing
         */
        public NovelTaskBag getNovelTasks() {
            return novelTasks;
        }

        /**
         * List of new tasks accumulated in one cycle, to be processed in the next
         * cycle
         */
        public List<Task> getNewTasks() {
            return newTasks;
        }

        /**
         * The selected Term
         */
        public Term getCurrentTerm() {
            return currentTerm;
        }

        public void setCurrentTerm(Term currentTerm) {
            this.currentTerm = currentTerm;
        }

        /**
         * The selected Concept
         */
        public Concept getCurrentConcept() {
            return currentConcept;
        }

        public void setCurrentConcept(Concept currentConcept) {
            this.currentConcept = currentConcept;
        }

        /**
         * The selected TaskLink
         */
        public TaskLink getCurrentTaskLink() {
            return currentTaskLink;
        }

        public void setCurrentTaskLink(TaskLink currentTaskLink) {
            this.currentTaskLink = currentTaskLink;
        }

        /**
         * The selected Task
         */
        public Task getCurrentTask() {
            return currentTask;
        }

        public void setCurrentTask(Task currentTask) {
            this.currentTask = currentTask;
        }

        /**
         * The selected TermLink
         */
        @Nullable
        public TermLink getCurrentBeliefLink() {
            return currentBeliefLink;
        }

        public void setCurrentBeliefLink(@Nullable TermLink currentBeliefLink) {
            this.currentBeliefLink = currentBeliefLink;
        }

        /**
         * The selected belief
         */
        @Nullable
        public Sentence getCurrentBelief() {
            return currentBelief;
        }

        public void setCurrentBelief(@Nullable Sentence currentBelief) {
            this.currentBelief = currentBelief;
        }

        /**
         * The new Stamp
         */
        public Stamp getNewStamp() {
            return newStamp;
        }

        public void setNewStamp(Stamp newStamp) {
            this.newStamp = newStamp;
        }

        /**
         * The substitution that unify the common term in the Task and the Belief
         * TODO unused
         */
        public Map<Term, Term> getSubstitute() {
            return substitute;
        }

        public void setSubstitute(Map<Term, Term> substitute) {
            this.substitute = substitute;
        }

        public void setRecorder(IInferenceRecorder recorder) {
            this.recorder = recorder;
        }

        public void setConcepts(ConceptBag concepts) {
            this.concepts = concepts;
        }

        public void setReasoner(ReasonerBatch reasoner) {
            this.reasoner = reasoner;
        }

        public void setNovelTasks(NovelTaskBag novelTasks) {
            this.novelTasks = novelTasks;
        }

        public <E> void setNewTasks( List<Task> newTasks) {
            this.newTasks = newTasks;
        }
    }
}
