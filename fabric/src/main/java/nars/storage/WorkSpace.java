package nars.storage;

import nars.entity.*;
import nars.language.Term;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jim on 1/14/16.
 */
public interface WorkSpace {
    /* ---------- access utilities ---------- */
    List<String> getExportStrings();

    AtomicInteger getTaskForgettingRate();

    AtomicInteger getBeliefForgettingRate();

    AtomicInteger getConceptForgettingRate();

    void setBeliefForgettingRate(AtomicInteger beliefForgettingRate);

    void setTaskForgettingRate(AtomicInteger taskForgettingRate);

    void setConceptForgettingRate(AtomicInteger conceptForgettingRate);

    List<Task> getNewTasks();

    void setNewTasks(List<Task> newTasks);

    void setExportStrings(List<String> exportStrings);

    Term getCurrentTerm();

    void setCurrentTerm(Term currentTerm);

    TaskLink getCurrentTaskLink();

    void setCurrentTaskLink(TaskLink currentTaskLink);

    Task getCurrentTask();

    void setCurrentTask(Task currentTask);

    TermLink getCurrentBeliefLink();

    void setCurrentBeliefLink(TermLink currentBeliefLink);

    Sentence getCurrentBelief();

    void setCurrentBelief(Sentence currentBelief);

    Stamp getNewStamp();

    void setNewStamp(Stamp newStamp);
}
