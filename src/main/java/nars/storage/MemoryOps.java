package nars.storage;

import nars.entity.*;
import nars.io.IInferenceRecorder;
import nars.language.Term;
import nars.main_nogui.ReasonerBatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface MemoryOps {
    void clear(); /* ---------- access utilities ---------- */

    List<String> getExportStrings();

    IInferenceRecorder getRecorder();

    long getTime();

    boolean noResult();

    Concept nameToConcept(String name);

    Term nameToListedTerm(String name);

    Concept termToConcept(Term term);

    Concept getConcept(Term term);

    float getConceptActivation(Term t);

    void activateConcept(Concept c, BudgetValue b);

    void inputTask(Task task);

    void activatedTask(BudgetValue budget, Sentence sentence, Sentence candidateBelief);

    void derivedTask(Task task);

    void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget);

    void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible);

    void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget);

    void singlePremiseTask(Term newContent, char punctuation, TruthValue newTruth, BudgetValue newBudget);

    void workCycle(long clock);

    void processNewTask();

    void processNovelTask();

    void processConcept();

    void immediateProcess(Task task);

    void report(Sentence sentence, boolean input);

    AtomicInteger getTaskForgettingRate();

    AtomicInteger getBeliefForgettingRate();

    AtomicInteger getConceptForgettingRate();

    ReasonerBatch getReasoner();

    ConceptBag getConcepts();

    NovelTaskBag getNovelTasks();

    List<Task> getNewTasks();

    Term getCurrentTerm();

    void setCurrentTerm(Term currentTerm);

    Concept getCurrentConcept();

    void setCurrentConcept(Concept currentConcept);

    TaskLink getCurrentTaskLink();

    void setCurrentTaskLink(TaskLink currentTaskLink);

    Task getCurrentTask();

    void setCurrentTask(Task currentTask);

    @org.jetbrains.annotations.Nullable
    TermLink getCurrentBeliefLink();

    void setCurrentBeliefLink(@org.jetbrains.annotations.Nullable TermLink currentBeliefLink);

    @org.jetbrains.annotations.Nullable
    Sentence getCurrentBelief();

    void setCurrentBelief(@org.jetbrains.annotations.Nullable Sentence currentBelief);

    Stamp getNewStamp();

    void setNewStamp(Stamp newStamp);

    Map<Term, Term> getSubstitute();

    void setSubstitute(Map<Term, Term> substitute);

    void setRecorder(IInferenceRecorder recorder);
}
