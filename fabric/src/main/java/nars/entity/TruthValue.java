/*
 * TruthValue.java
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

import nars.data.TruthHandle;
import nars.io.Symbols;

/**
 * Frequency and confidence.
 */
public class TruthValue implements Cloneable, TruthHandle { // implements Cloneable {
    /** The charactor that marks the two ends of a truth value */
    private static final char DELIMITER = Symbols.TRUTH_VALUE_MARK;
    /** The charactor that separates the factors in a truth value */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
    private float frequency;
    private float confidence;

    /**
     * Constructor with two ShortFloats
     * @param f The frequency value
     * @param c The confidence value
     */
    public TruthValue(float f, float c) {
        setFrequency(f);
        setConfidence((c < 1) ?  (c) :  (0.9999f));
    }

    /**
     * Constructor with a TruthValue to clone
     * @param v The truth value to be cloned
     */
    public TruthValue(TruthHandle v) {
        setFrequency(v.getFrequency());
        setConfidence(v.getConfidence());
    }

    /**
     * Get the frequency value
     * @return The frequency value
     */
    @Override
    public float getFrequency() {
        return frequency ;
    }

    /** The confidence factor of the truth value */ /**
     * Get the confidence value
     * @return The confidence value
     */
    @Override
    public float getConfidence() {
        return confidence ;
    }

    /** 
     * Calculate the expectation value of the truth value
     * @return The expectation value
     */
     public float getExpectation() {
        return (float) (getConfidence() * (getFrequency() - 0.5) + 0.5);
    }



    /** 
     * Calculate the absolute difference of the expectation value and that of a given truth value
     * @param t The given value
     * @return The absolute difference
     */
     public float isExpDifAbs(TruthValue t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }

    /**
     * Check if the truth value is negative
     * @return True if the frequence is less than 1/2
     */
     public boolean isNegative() {
        return getFrequency() < 0.5;
    }

    /**
     * Compare two truth values
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof TruthValue)
                && (getFrequency() == ((TruthValue) that).getFrequency())
                && (getConfidence() == ((TruthValue) that).getConfidence()));
    }

    /**
     * The hash code of a TruthValue
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return (int) (getExpectation() * 37);
    }

    @Override
    public Object clone() {
        return new TruthValue(getFrequency(), getConfidence());
    }
    
    /**
     * The String representation of a TruthValue
     * @return The String
     */
    @Override
    public String toString() {
        return ""+DELIMITER + getFrequency() + SEPARATOR + getConfidence() + DELIMITER;
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is accruate to 1%
     * @return The String
     */
    public String toStringBrief() {
        String s1 = "" +DELIMITER + getFrequency() + SEPARATOR;
        Float s2 = getConfidence();
        return s2 == (1.0f) ? s1 + "0.99" + DELIMITER : s1 + s2 + DELIMITER;
    }

    @Override
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    @Override
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
