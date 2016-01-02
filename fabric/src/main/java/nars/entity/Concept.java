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
package nars.entity;

import nars.data.BudgetStruct;
import nars.data.SentenceStruct;
import nars.inference.LocalRules;
import nars.inference.RuleTables;
import nars.inference.UtilityFunctions;
import nars.language.CompoundTerm;
import nars.language.Term;
import nars.storage.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A concept contains information associated with a term, including directly
 * and indirectly related tasks and beliefs.
 * <p>
 * To make sure the space will be released, the only allowed reference to a concept are
 * those in a ConceptBag. All other access go through the Term that names the concept.
 */
public final class Concept extends Item {

    private Memory memory;
    private Term term;
    private TaskLinkBag taskLinks;
    private TermLinkBag termLinks;
    private List<TermLink> termLinkTemplates;
    private List<Task> questions;
    private List<Sentence> beliefs;
    private EntityObserver entityObserver = new NullEntityObserver();


    /* ---------- constructor and initialization ---------- */

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param tm     A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term tm, Memory memory) {
        super(tm.getName());
        setTerm(tm);
        this.setMemory(memory);
        setQuestions(new ArrayList<Task>());
        setBeliefs(new ArrayList<Sentence>());
        setTaskLinks(new TaskLinkBag(memory));
        setTermLinks(new TermLinkBag(memory));
        if (tm instanceof CompoundTerm) {
            setTermLinkTemplates(((CompoundTerm) tm).prepareComponentLinks());
        }
    }

    /**
     * Distribute the budget of a task among the links to it
     *
     * @param b
     *            The original budget
     * @param n
     *            Number of links
     * @return Budget value for each link
     */
    public static BudgetValue distributeAmongLinks(BudgetStruct b, int n) {
        float priority = (float) (b.getPriority() / Math.sqrt(n));
        return new BudgetValue(priority, b.getDurability(), b.getQuality());
    }

    /**
     * Determine the rank of a judgment by its confidence and originality (stamp
     * length)
     *
     * @param judg
     *            The judgment to be ranked
     * @return The rank of the judgment, according to truth value only
     */
    public static float rankBelief(SentenceStruct judg) {
        float confidence = judg.getTruth().getConfidence();
        float originality = 1.0f / (judg.getStamp().getEvidentialBase().size() + 1);
        return UtilityFunctions.or(confidence, originality);
    }

    /* ---------- direct processing of tasks ---------- */

    /**
     * Directly process a new task. Called exactly once on each task.
     * Using local information and finishing in a constant time.
     * Provide feedback in the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @param task The task to be processed
     */
    public void directProcess(Task task) {
        if (task.getSentence().isJudgment()) {
            processJudgment(task);
        } else {
            processQuestion(task);
        }
        if (task.getBudget().aboveThreshold()) {    // still need to be processed
            linkToTask(task);
        }
        getEntityObserver().refresh(displayContent());
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and solutions
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private void processJudgment(Task task) {
        Sentence judg = task.getSentence();
        Sentence oldBelief = evaluation(judg, getBeliefs());
        if (oldBelief != null) {
            Stamp newStamp = judg.getStamp(), oldStamp = oldBelief.getStamp();
            if (newStamp.equals(oldStamp)) {
                task.getBudget().decPriority(0);    // duplicated task
                return;
            } else if (LocalRules.revisible(judg, oldBelief)) {
                getMemory().setNewStamp(Stamp.make(newStamp, oldStamp, Memory.getTime(getMemory())));
                if (getMemory().getNewStamp() != null) {
                    getMemory().setCurrentBelief(oldBelief);
                    LocalRules.revision(judg, oldBelief, false, getMemory());
                }
            }
        }
        if (task.getBudget().aboveThreshold()) {
            for (Task ques : getQuestions()) {
//                LocalRules.trySolution(ques.getSentence(), judg, ques, memory);
                LocalRules.trySolution(judg, ques, getMemory());
            }
            addToTable(judg, getBeliefs(), Parameters.MAXIMUM_BELIEF_LENGTH);
        }
    }

    /**
     * To answer a question by existing beliefs
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    public float processQuestion(Task task) {
        Sentence ques = task.getSentence();
        boolean newQuestion = true;
        if (getQuestions() != null) {
            for (Task t : getQuestions()) {
                Sentence q = t.getSentence();
                if (q.getContent().equals(ques.getContent())) {
                    ques = q;
                    newQuestion = false;
                    break;
                }
            }
            if (newQuestion) {
                getQuestions().add(task);
            }
            if (getQuestions().size() > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
                getQuestions().remove(0);    // FIFO
            }
            Sentence newAnswer = evaluation(ques, getBeliefs());
            if (newAnswer != null) {
//            LocalRules.trySolution(ques, newAnswer, task, memory);
                LocalRules.trySolution(newAnswer, task, getMemory());
                return newAnswer.getTruth().getExpectation();
            } else {
                return 0.5f;
            }
        }
        throw new Error("this state was not reachable from previous code");
    }

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * <p>
     * The only method that calls the TaskLink constructor.
     *
     * @param task The task to be linked
     */
    private void linkToTask(Task task) {
        BudgetValue taskBudget = task.getBudget();
        TaskLink taskLink = new TaskLink(task, null, taskBudget);   // link type: SELF
        insertTaskLink(taskLink);
        if (getTerm() instanceof CompoundTerm) {
            if (getTermLinkTemplates().size() > 0) {
                BudgetValue subBudget = distributeAmongLinks(taskBudget, getTermLinkTemplates().size());
                if (subBudget.aboveThreshold()) {
                    Term componentTerm;
                    Concept componentConcept;
                    for (TermLink termLink : getTermLinkTemplates()) {
//                        if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) { // avoid circular transform
                        taskLink = new TaskLink(task, termLink, subBudget);
                        componentTerm = termLink.getTerm();
                        componentConcept = Memory.getConcept(getMemory(), componentTerm);
                        if (componentConcept != null) {
                            componentConcept.insertTaskLink(taskLink);
                        }
//                        }
                    }
                    buildTermLinks(taskBudget);  // recursively insert TermLink
                }
            }
        }
    }

    /**
     * Add a new belief (or goal) into the table
     * Sort the beliefs/goals by rank, and remove redundant or low rank one
     *
     * @param newSentence The judgment to be processed
     * @param table       The table to be revised
     * @param capacity    The capacity of the table
     */
    private static void addToTable(Sentence newSentence, List<Sentence> table, int capacity) {
        float rank1 = rankBelief(newSentence);    // for the new isBelief
        Sentence judgment2;
        float rank2;
        int i;
        for (i = 0; i < table.size(); i++) {
            judgment2 = table.get(i);
            rank2 = rankBelief(judgment2);
            if (rank1 >= rank2) {
                if (newSentence.equivalentTo(judgment2)) {
                    return;
                }
                table.add(i, newSentence);
                break;
            }
        }
        if (table.size() >= capacity) {
            while (table.size() > capacity) {
                table.remove(table.size() - 1);
            }
        } else if (i == table.size()) {
            table.add(newSentence);
        }
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     *
     * @param query The question to be processed
     * @param list  The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private static Sentence evaluation(SentenceStruct query, Iterable<Sentence> list) {
        if (list == null) {
            return null;
        }
        float currentBest = 0;
        float beliefQuality;
        Sentence candidate = null;
        for (Sentence judg : list) {
            beliefQuality = LocalRules.solutionQuality(query, judg);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judg;
            }
        }
        return candidate;
    }

    /* ---------- insert Links for indirect processing ---------- */

    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    public void insertTaskLink(TaskLink taskLink) {
        BudgetValue taskBudget = taskLink.getBudget();
        getTaskLinks().putIn(taskLink);
        Memory.activateConcept(getMemory(), this, taskBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskBudget The BudgetValue of the task
     */
    public void buildTermLinks(BudgetStruct taskBudget) {
        Term t;
        Concept concept;
        TermLink termLink1, termLink2;
        if (getTermLinkTemplates().size() > 0) {
            BudgetValue subBudget = distributeAmongLinks(taskBudget, getTermLinkTemplates().size());
            if (subBudget.aboveThreshold()) {
                for (TermLink template : getTermLinkTemplates()) {
                    if (template.getType() != TermLink.TRANSFORM) {
                        t = template.getTerm();
                        concept = Memory.getConcept(getMemory(), t);
                        if (concept != null) {
                            termLink1 = new TermLink(t, template, subBudget);
                            insertTermLink(termLink1);   // this termLink to that
                            termLink2 = new TermLink(getTerm(), template, subBudget);
                            concept.insertTermLink(termLink2);   // that termLink to this
                            if (t instanceof CompoundTerm) {
                                concept.buildTermLinks(subBudget);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     * <p>
     * called from buildTermLinks only
     *
     * @param termLink The termLink to be inserted
     */
    public void insertTermLink(TermLink termLink) {
        getTermLinks().putIn(termLink);
    }

    /* ---------- access local information ---------- */

    /**
     * The term is the unique ID of the concept
     */ /**
     * Return the associated term, called from Memory only
     *
     * @return The associated term
     */
    public Term getTerm() {
        return term;
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() {  // called from concept bag
        if (NARSBatch.isStandAlone()) {
            return (super.toStringBrief(getBudget(), getKey()) + " " + getKey());
        } else {
            return getKey();
        }
    }

    /**
     * called from {@link NARSBatch}
     */
    public String toStringLong() {
        String res = toStringBrief(getBudget(), getKey()) + " " + getKey()
                + toStringIfNotNull(getTermLinks(), "termLinks")
                + toStringIfNotNull(getTaskLinks(), "taskLinks");
        res += toStringIfNotNull(null, "questions");
        for (Task t : getQuestions()) {
            res += t.toString();
        }
        // TODO other details?
        return res;
    }

    private static String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":" + item.toString();
    }

    /**
     * Recalculate the quality of the concept [to be refined to show extension/intension balance]
     *
     * @return The quality value
     */
    @Override
    public float getQuality() {
        float linkPriority = getTermLinks().averagePriority();
        float termComplexityFactor = 1.0f / getTerm().getComplexity();
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /**
     * Link templates of TermLink, only in concepts with CompoundTerm
     * jmv TODO explain more
     */ /**
     * Return the templates for TermLinks, only called in Memory.continuedProcess
     *
     * @return The template get
     */
    public Collection<TermLink> getTermLinkTemplates() {
        return termLinkTemplates;
    }

    /**
     * Select a isBelief to interact with the given task in inference
     * <p>
     * get the first qualified one
     * <p>
     * only called in RuleTables.reason
     *
     * @param task The selected task
     * @return The selected isBelief
     */
    public Sentence getBelief(Task task) {
        Sentence taskSentence = task.getSentence();
        Sentence r = null;
        for (Sentence belief1 : getBeliefs()) {
            getMemory().getRecorder().append(" * Selected Belief: " + belief1 + "\n");
            getMemory().setNewStamp(Stamp.make((Stamp) taskSentence.getStamp(), belief1.getStamp(), Memory.getTime(getMemory())));
            if (getMemory().getNewStamp() != null) {
                r = (Sentence) belief1.clone();
                break;
            }
        }
        return r;
    }

    /* ---------- main loop ---------- */

    /**
     * An atomic step in a concept, only called in {@link Memory#} processConcept
     * @param memory
     * @param taskLinks
     * @param termLinks
     */
    public static void fireFromProcessConcept(Memory memory, TaskLinkBag taskLinks, TermLinkBag termLinks) {
        TaskLink currentTaskLink = taskLinks.takeOut();
        if (currentTaskLink != null) {
            memory.setCurrentTaskLink(currentTaskLink);
            memory.setCurrentBeliefLink(null);
            memory.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
            Task task = currentTaskLink.getTargetTask();
            memory.setCurrentTask(task);  // one of the two places where this variable is set
            if (currentTaskLink.getType() == TermLink.TRANSFORM) {
                RuleTables.transformTask(currentTaskLink, memory);  // to turn this into structural inference as below?
            }
            int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
            while (Memory.noResult(memory) && (termLinkCount > 0)) {
                TermLink termLink = termLinks.takeOut(currentTaskLink, Memory.getTime(memory));
                if (termLink != null) {
                    memory.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
                    memory.setCurrentBeliefLink(termLink);
                    RuleTables.reason(currentTaskLink, termLink, memory);
                    termLinks.putBack(termLink);
                    termLinkCount--;
                } else {
                    termLinkCount = 0;
                }
            }
            taskLinks.putBack(currentTaskLink);
        }
    }

    /* ---------- display ---------- */

    /**
     * Start displaying contents and links, called from ConceptWindow,
     * TermWindow
     * or Memory.processTask only
     *
     * @param entityObserver TODO make it a real observer pattern (i.e. with a plurality of observers)
     * @param showLinks      Whether to display the task links
     */
    public void startPlay(EntityObserver entityObserver, boolean showLinks) {
        this.setEntityObserver(entityObserver);
        entityObserver.startPlay(this, showLinks);
        entityObserver.post(displayContent());
        if (showLinks) {
            getTaskLinks().addBagObserver(entityObserver.createBagObserver(), "Task Links in " + getTerm());
            getTermLinks().addBagObserver(entityObserver.createBagObserver(), "Term Links in " + getTerm());
        }
    }

    /**
     * Resume display, called from ConceptWindow only
     */
    public void play() {
        getEntityObserver().post(displayContent());
    }

    /**
     * Stop display, called from ConceptWindow only
     */
    public void stop() {
        getEntityObserver().stop();
    }

    /**
     * Collect direct isBelief, questions, and goals for display
     *
     * @return String representation of direct content
     */
    public String displayContent() {
        StringBuilder buffer = new StringBuilder();
        if (getBeliefs().size() > 0) {
            buffer.append("\n  Beliefs:\n");
            for (Sentence s : getBeliefs()) {
                buffer.append(s).append("\n");
            }
        }
        if (getQuestions().size() > 0) {
            buffer.append("\n  Question:\n");
            for (Task t : getQuestions()) {
                buffer.append(t).append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Reference to the memory
     */
    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public void setTerm(Term term) {
        this.term = term;
    }

    /**
     * Task links for indirect processing
     */
    public TaskLinkBag getTaskLinks() {
        return taskLinks;
    }

    public void setTaskLinks(TaskLinkBag taskLinks) {
        this.taskLinks = taskLinks;
    }

    /**
     * Term links between the term and its components and compounds
     */
    public TermLinkBag getTermLinks() {
        return termLinks;
    }

    public void setTermLinks(TermLinkBag termLinks) {
        this.termLinks = termLinks;
    }

    public void setTermLinkTemplates(List<TermLink> termLinkTemplates) {
        this.termLinkTemplates = termLinkTemplates;
    }

    /**
     * Question directly asked about the term
     */
    public List<Task> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Task> questions) {
        this.questions = questions;
    }

    /**
     * Sentences directly made about the term, with non-future tense
     */
    public List<Sentence> getBeliefs() {
        return beliefs;
    }

    public void setBeliefs(List<Sentence> beliefs) {
        this.beliefs = beliefs;
    }

    /**
     * The display window
     */
    public EntityObserver getEntityObserver() {
        return entityObserver;
    }

    public void setEntityObserver(EntityObserver entityObserver) {
        this.entityObserver = entityObserver;
    }

    class NullEntityObserver implements EntityObserver {
        @Override
        public void post(String str) {
        }

        @SuppressWarnings("rawtypes")
        @Override
        public BagObserver createBagObserver() {
            return new NullBagObserver();
        }

        @Override
        public void startPlay(Concept concept, boolean showLinks) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void refresh(String message) {
        }
    }
}

