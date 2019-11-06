package nars.entity;

import nars.language.Term;

/**
 * Created by jim on 1/2/2016.
 */
public interface Task extends Item {
    SentenceHandle getSentence();

    Term getContent();

    long getCreationTime();

    boolean isInput();

    Sentence getBestSolution();

    void setBestSolution(SentenceHandle judg);

    SentenceHandle getParentBelief();

    Task getParentTask();
}
