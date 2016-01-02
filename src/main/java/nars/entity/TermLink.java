package nars.entity;

import nars.language.Term;

/**
 * Created by jim on 1/2/2016.
 */
public interface TermLink extends Item {
    void setKey();

    Term getTarget();

    short getType();

    short[] getIndices();

    short getIndex(int i);
}
