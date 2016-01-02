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

import nars.data.BudgetStruct;
import nars.data.TermStruct;
import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.io.IInferenceRecorder;
import nars.language.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The memory of the system.
 */
public class Memory implements WorkSpace {

    private ReasonerBatch reasoner;

    /* ---------- Long-term storage for multiple cycles ---------- */
    private ConceptBag concepts;
    private NovelTaskBag novelTasks;
    private IInferenceRecorder recorder;
    private AtomicInteger beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
    private AtomicInteger taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
    private AtomicInteger conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);

    /* ---------- Short-term workspace for a single cycle ---------- */
    private List<Task> newTasks;
    private List<String> exportStrings;
    private Term currentTerm;
    private Concept currentConcept;
    private TaskLink currentTaskLink;
    private Task currentTask;
    private TermLink currentBeliefLink;
    private Sentence currentBelief;
    private Stamp newStamp;


    private Map<Term, Term> substitute;


    /* ---------- Constructor ---------- */
    /**
     * Create a new memory <p> Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public Memory(ReasonerBatch reasoner) {
        setReasoner(reasoner);
        setRecorder(new NullInferenceRecorder());
        setConcepts(new ConceptBag(this));
        setNovelTasks(new NovelTaskBag(this));
        setNewTasks(new ArrayList<>());
        setExportStrings(new ArrayList<>());
    }

    public static void init(Memory memory) {
        memory.getConcepts().init();
        memory.getNovelTasks().init();
        memory.getNewTasks().clear();
        memory.getExportStrings().clear();
//      reasoner.getMainWindow().initTimer();
        ReasonerBatch.initTimer(memory.getReasoner());
        memory.getRecorder().append("\n-----RESET-----\n");
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     */ /* ---------- access utilities ---------- */
    @Override
    public List<String> getExportStrings() {
        return exportStrings;
    }

    /**
     * Inference record text to be written into a log file
     */
    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    public static long getTime(Memory memory) {
        return ReasonerBatch.getTime(memory.getReasoner().getClock());
    }

//    public MainWindow getMainWindow() {
//        return reasoner.getMainWindow();
//    }
    /**
     * Actually means that there are no new Tasks
     * @param memory
     */
    public static boolean noResult(WorkSpace memory) {
        return memory.getNewTasks().isEmpty();
    }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name <p> called from Term and
     * ConceptWindow.
     *
     * @param memory
     * @param name the name of a concept
     * @return a Concept or null
     */
    public static Concept nameToConcept(Memory memory, String name) {
        return memory.getConcepts().get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator <p> called in
     * StringParser and the make methods of compound terms.
     *
     * @param memory
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public static Term nameToListedTerm(Memory memory, String name) {
        Concept concept;
        concept = memory.getConcepts().get(name);
        return null != concept ? concept.getTerm() : null;
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param memory
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public static Concept termToConcept(Memory memory, TermStruct term) {
        return nameToConcept(memory, term.getName());
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param memory
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    public static Concept getConcept(Memory memory, Term term) {
        Concept r=null;
        e:{

            if (term.isConstant()) {
                Concept concept = memory.getConcepts().get(term.getName());

            if (null == concept) {
                concept = new Concept(term, memory); // the only place to make a new Concept
                boolean created = memory.getConcepts().putIn(concept);
                if (created) r = concept;
                break e;
            }
            r=concept;

        }
        }
        return r;
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept <p> called in
     * Concept.insertTaskLink only
     *
     * @param memory
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public static void activateConcept(Memory memory, Concept c, BudgetStruct b) {
        memory.getConcepts().pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        memory.getConcepts().putBack(c);
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
     * @param memory
     * @param task The input task
     */
    public static void inputTask(Memory memory, Task task) {
        if (task.getBudget().aboveThreshold()) {
            memory.getRecorder().append("!!! Perceived: " + task + "\n");
            Sentence sentence = task.getSentence();
            report(memory, sentence, true);    // report input
            memory.getNewTasks().add(task);       // wait to be processed in the next workCycle
        } else {
            memory.getRecorder().append("!!! Neglected: " + task + "\n");
        }
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param memory
     * @param budget The budget value of the new Task
     * @param sentence The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     * forward/backward correspondence
     */
    public static void activatedTask(Memory memory, BudgetValue budget, Sentence sentence, Sentence candidateBelief) {
        Task task = new Task(sentence, budget, memory.getCurrentTask(), sentence, candidateBelief);
        memory.getRecorder().append("!!! Activated: " + task + "\n");
        if (sentence.isQuestion()) {
            float s = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            float minSilent = ReasonerBatch.getSilenceValue(memory.getReasoner().getSilenceValue()).get() / 100.0f;
            // only report significant derived Tasks
            if (s > minSilent) report(memory, task.getSentence(), false);
        }
        memory.getNewTasks().add(task);
    }

    /**
     * Derived task comes from the inference rules.
     *
     * @param memory
     * @param task the derived task
     */
    private static void derivedTask(Memory memory, Task task) {
        if (task.getBudget().aboveThreshold()) {
            memory.getRecorder().append("!!! Derived: " + task + "\n");
            float budget = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            float minSilent = ReasonerBatch.getSilenceValue(memory.getReasoner().getSilenceValue()).get() / 100.0f;
            if (budget > minSilent) {  // only report significant derived Tasks
                report(memory, task.getSentence(), false);
            }
            memory.getNewTasks().add(task);
        } else {
            memory.getRecorder().append("!!! Ignored: " + task + "\n");
        }
    }

    /* --------------- new task building --------------- */
    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param memory
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public static void doublePremiseTask(Memory memory, Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        if (null != newContent) {
            Sentence newSentence = new Sentence(newContent, memory.getCurrentTask().getSentence().getPunctuation(), newTruth, memory.getNewStamp());
            Task newTask = new Task(newSentence, newBudget, memory.getCurrentTask(), memory.getCurrentBelief());
            Memory.derivedTask(memory, newTask);
        }
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param memory
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     * @param revisible Whether the sentence is revisible
     */
    public static void doublePremiseTask(Memory memory, Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {
        if (null != newContent) {
            Sentence taskSentence = memory.getCurrentTask().getSentence();
            Sentence newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, memory.getNewStamp(), revisible);
            Task newTask = new Task(newSentence, newBudget, memory.getCurrentTask(), memory.getCurrentBelief());
            Memory.derivedTask(memory, newTask);
        }
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param memory
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public static void singlePremiseTask(Memory memory, Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(memory, newContent, memory.getCurrentTask().getSentence().getPunctuation(), newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *  @param memory
     * @param newContent The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public static void singlePremiseTask(Memory memory, Term newContent, int punctuation, TruthValue newTruth, BudgetValue newBudget) {
        Sentence taskSentence = memory.getCurrentTask().getSentence();
        if (taskSentence.isJudgment() || null == memory.getCurrentBelief()) {
            memory.setNewStamp(Stamp.createStamp(taskSentence.getStamp(), Memory.getTime(memory)));
        } else {
            memory.setNewStamp(Stamp.createStamp(memory.getCurrentBelief().getStamp(), Memory.getTime(memory)));
        }
        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, memory.getNewStamp(), taskSentence.getRevisible());
        Task newTask = new Task(newSentence, newBudget, memory.getCurrentTask(), null);
        Memory.derivedTask(memory, newTask);
    }

    /* ---------- system working workCycle ---------- */
    /**
     * An atomic working workCycle of the system: process new Tasks, then fireFromProcessConcept a
     * concept <p> Called from Reasoner.tick only
     *
     * @param memory
     * @param clock The current time to be displayed
     */
    public static void workCycle(Memory memory, long clock) {
        memory.getRecorder().append(" --- " + clock + " ---\n");
        processNewTask(memory);
        if (noResult(memory)) {       // necessary?
            processNovelTask(memory);
        }
        if (noResult(memory)) {       // necessary?
            processConcept(memory);
        }
        memory.getNovelTasks().refresh();
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     * @param memory
     */
    private static void processNewTask(Memory memory) {
        Task task;
        int counter = memory.getNewTasks().size();  // don't include new tasks produced in the current workCycle
        while (0 < counter--) {
            task = memory.getNewTasks().remove(0);
            if (task.isInput() || null != termToConcept(memory, task.getContent())) { // new input or existing concept
                immediateProcess(memory, task);
            } else {
                Sentence  s = task.getSentence();
                if (s.isJudgment()) {
                    double d = s.getTruth().getExpectation();
                    if (Parameters.DEFAULT_CREATION_EXPECTATION < d)
                        memory.getNovelTasks().putIn(task);    // new concept formation
                    else {
                        memory.getRecorder().append("!!! Neglected: " + task + "\n");
                    }
                }
            }
        }
    }

    /**
     * Select a novel task to process.
     * @param memory
     */
    private static void processNovelTask(Memory memory) {
        Task task = memory.getNovelTasks().takeOut();       // select a task from novelTasks
        if (null != task) {
            immediateProcess(memory, task);
        }
    }

    /**
     * Select a concept to fireFromProcessConcept.
     * @param memory
     */
    private static void processConcept(Memory memory) {
        memory.setCurrentConcept(memory.getConcepts().takeOut());
        if (null != memory.getCurrentConcept()) {
            memory.setCurrentTerm(memory.getCurrentConcept().getTerm());
            memory.getRecorder().append(" * Selected Concept: " + memory.getCurrentTerm() + "\n");
            memory.getConcepts().putBack(memory.getCurrentConcept());   // current Concept remains in the bag all the time
            Concept.fireFromProcessConcept(memory.getCurrentConcept().getMemory(), memory.getCurrentConcept().getTaskLinks(), memory.getCurrentConcept().getTermLinks());              // a working workCycle
        }
    }

    /* ---------- task processing ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param memory
     * @param task the task to be accepted
     */
    private static void immediateProcess(Memory memory, Task task) {
        memory.setCurrentTask(task); // one of the two places where this variable is set
        memory.getRecorder().append("!!! Insert: " + task + "\n");
        memory.setCurrentTerm(task.getContent());
        memory.setCurrentConcept(Memory.getConcept(memory, memory.getCurrentTerm()));
        if (null != memory.getCurrentConcept()) {
            memory.getCurrentConcept().directProcess(task);
        }
    }

    /* ---------- display ---------- */
    /**
     * Display active concepts, called from MainWindow.
     *
     * we don't want to expose fields concepts and novelTasks, AND we want to
     * separate GUI and inference, so this method has become conceptsStartPlay(
     * BagObserver bagObserver, String s) and this method calls
     * concepts.addBagObserver( bagObserver, s) see design for Bag and

     *
     * @param memory
     * @param bagObserver
     * @param s the window title
     */
    public static void conceptsStartPlay(Memory memory, BagObserver bagObserver, String s) {
        bagObserver.setBag(memory.getConcepts());
        memory.getConcepts().addBagObserver(bagObserver, s);
    }

    /**
     * Display new tasks, called from MainWindow. see
     *
     * @param memory
     * @param bagObserver
     * @param s the window title
     */
    public static void taskBuffersStartPlay(Memory memory, BagObserver bagObserver, String s) {
        bagObserver.setBag(memory.getNovelTasks());
        memory.getNovelTasks().addBagObserver(bagObserver, s);
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick(ReasonerBatch)} - TODO fragile mechanism)
     *
     * @param memory
     * @param sentence the sentence to be displayed
     * @param input whether the task is input
     */
    public static void report(Memory memory, Sentence sentence, boolean input) {
        if (ReasonerBatch.isDEBUG()) {
            System.out.println("// report( clock " + ReasonerBatch.getTime(memory.getReasoner().getClock())
                    + ", input " + input
                    + ", timer " + memory.getReasoner().getTimer()
                    + ", Sentence " + sentence
                    + ", exportStrings " + memory.getExportStrings());
            System.out.flush();
        }
        if (memory.getExportStrings().isEmpty()) {
//          long timer = reasoner.getMainWindow().updateTimer();
            long timer = ReasonerBatch.updateTimer(memory.getReasoner());
            if (0 < timer) {
                memory.getExportStrings().add(String.valueOf(timer));
            }
        }

        String s;
        if (input) {
            s = "  IN: ";
        } else {
            s = " OUT: ";
        }
        s += Sentence.toStringBrief(sentence);
        memory.getExportStrings().add(s);
    }

    public static String toString(Memory memory) {
        return toStringLongIfNotNull(memory.getConcepts(), "concepts")
                + toStringLongIfNotNull(memory.getNovelTasks(), "novelTasks")
                + toStringIfNotNull(memory.getNewTasks(), "newTasks")
                + toStringLongIfNotNull(memory.getCurrentTask(), "currentTask")
                + toStringLongIfNotNull(memory.getCurrentBeliefLink(), "currentBeliefLink")
                + toStringIfNotNull(memory.getCurrentBelief(), "currentBelief");
    }

    private static String toStringLongIfNotNull(Bag<?> item, String title) {
        return null == item ? "" : "\n " + title + ":\n"
                + item.toStringLong();
    }

    private static String toStringLongIfNotNull(Item item, String title) {
        return null == item ? "" : "\n " + title + ":\n"
                + Item.toStringLong(item);
    }

    private static String toStringIfNotNull(Object item, String title) {
        return null == item ? "" : "\n " + title + ":\n"
                + item;
    }

    @Override
    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    @Override
    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    @Override
    public AtomicInteger getConceptForgettingRate() {
        return conceptForgettingRate;
    }

    /**
     * Backward pointer to the reasoner
     */
    public ReasonerBatch getReasoner() {
        return reasoner;
    }

    public void setReasoner(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
    }

    /**
     * Concept bag. Containing all Concepts of the system
     */
    public ConceptBag getConcepts() {
        return concepts;
    }

    public void setConcepts(ConceptBag concepts) {
        this.concepts = concepts;
    }

    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    public NovelTaskBag getNovelTasks() {
        return novelTasks;
    }

    public void setNovelTasks(NovelTaskBag novelTasks) {
        this.novelTasks = novelTasks;
    }

    @Override
    public void setBeliefForgettingRate(AtomicInteger beliefForgettingRate) {
        this.beliefForgettingRate = beliefForgettingRate;
    }

    @Override
    public void setTaskForgettingRate(AtomicInteger taskForgettingRate) {
        this.taskForgettingRate = taskForgettingRate;
    }

    @Override
    public void setConceptForgettingRate(AtomicInteger conceptForgettingRate) {
        this.conceptForgettingRate = conceptForgettingRate;
    }

    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    @Override
    public List<Task> getNewTasks() {
        return newTasks;
    }

    @Override
    public void setNewTasks(List<Task> newTasks) {
        this.newTasks = newTasks;
    }

    @Override
    public void setExportStrings(List<String> exportStrings) {
        this.exportStrings = exportStrings;
    }

    /**
     * The selected Term
     */
    @Override
    public Term getCurrentTerm() {
        return currentTerm;
    }

    @Override
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
    @Override
    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }

    @Override
    public void setCurrentTaskLink(TaskLink currentTaskLink) {
        this.currentTaskLink = currentTaskLink;
    }

    /**
     * The selected Task
     */
    @Override
    public Task getCurrentTask() {
        return currentTask;
    }

    @Override
    public void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    /**
     * The selected TermLink
     */
    @Override
    public TermLink getCurrentBeliefLink() {
        return currentBeliefLink;
    }

    @Override
    public void setCurrentBeliefLink(TermLink currentBeliefLink) {
        this.currentBeliefLink = currentBeliefLink;
    }

    /**
     * The selected belief
     */
    @Override
    public Sentence getCurrentBelief() {
        return currentBelief;
    }

    @Override
    public void setCurrentBelief(Sentence currentBelief) {
        this.currentBelief = currentBelief;
    }

    /**
     * The new Stamp
     */
    @Override
    public Stamp getNewStamp() {
        return newStamp;
    }

    @Override
    public void setNewStamp(Stamp newStamp) {
        this.newStamp = newStamp;
    }

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    public  Map<Term, Term> getSubstitute() {
        return substitute;
    }

    public void setSubstitute(Map<Term, Term> substitute) {
        this.substitute = substitute;
    }

}
