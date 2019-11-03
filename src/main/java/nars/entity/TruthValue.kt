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
package nars.entity

import nars.io.Symbols

/**
 * Frequency and confidence.
 */
class TruthValue : Cloneable  { // implements Cloneable {
    /**
     * The frequency factor of the truth value
     */
    private var frequency: Float
    /**
     * The confidence factor of the truth value
     */
    private var confidence: Float
    /**
     * Get the isAnalytic flag
     *
     * @return The isAnalytic value
     */
    /**
     * Whether the truth value is derived from a definition
     */
    var analytic = false
        private set

    /**
     * Constructor with two Floats
     *
     * @param f The frequency value
     * @param c The confidence value
     */
    constructor(f: Float, c: Float) {
        frequency = f
        confidence = if (c < 1) c else 0.9999f
    }

    /**
     * Constructor with two Floats
     *
     * @param f The frequency value
     * @param c The confidence value
     */
    constructor(f: Float, c: Float, b: Boolean) {
        frequency = f
        confidence = if (c < 1) c else 0.9999f
        analytic = b
    }

    /**
     * Constructor with a TruthValue to clone
     *
     * @param v The truth value to be cloned
     */
    constructor(v: TruthValue) {
        frequency = v.getFrequency()
        confidence = v.getConfidence()
        analytic = v.analytic
    }

    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    fun getFrequency(): Float {
        return frequency
    }

    /**
     * Get the confidence value
     *
     * @return The confidence value
     */
    fun getConfidence(): Float {
        return confidence
    }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    val expectation: Float
        get() = (confidence * (frequency - 0.5) + 0.5).toFloat()

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     *
     * @param t The given value
     * @return The absolute difference
     */
    fun getExpDifAbs(t: TruthValue): Float {
        return Math.abs(expectation - t.expectation)
    }

    /**
     * Check if the truth value is negative
     *
     * @return True if the frequence is less than 1/2
     */
    val isNegative: Boolean
        get() = getFrequency() < 0.5

    /**
     * Compare two truth values
     *
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */

    override fun equals(that: Any?): Boolean {
        return (that is TruthValue
                && getFrequency() == that.getFrequency()
                && getConfidence() == that.getConfidence())
    }

    /**
     * The hash code of a TruthValue
     *
     * @return The hash code
     */

    override fun hashCode(): Int {
        return (expectation * 37).toInt()
    }

    public override fun clone(): Any {
        return TruthValue(getFrequency(), getConfidence(), analytic)
    }

    /**
     * The String representation of a TruthValue
     *
     * @return The String
     */

    override fun toString(): String {
        return DELIMITER.toString() + frequency.toString() + SEPARATOR.toString() + confidence.toString() + DELIMITER
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     *
     * @return The String
     */
    fun toStringBrief(): String {
        val s1 = DELIMITER.toString() + frequency.toString() + SEPARATOR
        val s2 = confidence.toString()
        return s1 + (if (s2 == "1.00") "0.99" else s2) + DELIMITER
    }

    companion object {

        /**
         * The character that marks the two ends of a truth value
         */
        private const val DELIMITER = Symbols.TRUTH_VALUE_MARK
        /**
         * The character that separates the factors in a truth value
         */
        private const val SEPARATOR = Symbols.VALUE_SEPARATOR
    }
}