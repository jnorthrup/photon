package nars.entity;

/**
 * Created by jim on 1/2/2016.
 */
public interface TruthValue {
    float getFrequency();

    float getConfidence();

    boolean getAnalytic();

    void setAnalytic();

    float getExpectation();

    float getExpDifAbs(TruthValue t);

    boolean isNegative();
}
