package nars.entity;

import nars.language.Term;

/**
 * Created by jim on 1/2/2016.
 */
public interface Sentence {
    Term getContent();

    char getPunctuation();

    void setContent(Term t);

    TruthValueRefier getTruth();

    StampHandle getStamp();

    boolean isJudgment();

    boolean isQuestion();

    boolean getRevisible();

    void setRevisible(boolean b);
}
