/*
 * BudgetValue.java
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

import nars.inference.BudgetFunctions
import nars.inference.UtilityFunctions
import nars.io.Symbols
import nars.main_nogui.Parameters

/**
 * A triple of priority (current), durability (decay), and quality (long-term average).
 */
class BudgetValue : BudgetTriple {
    /**
     * The relative share of time resource to be allocated
     */
      override var priority = 0.01f
    /**
     * The percent of priority to be kept in a constant period; All priority
     * values “decay” over time, though at different rates. Each item is given a
     * “durability” factor in (0, 1) to specify the percentage of priority level
     * left after each reevaluation
     */
    override var durability = 0.01f
    /**
     * The overall (context-independent) evaluation
     */
      override var quality = 0.01f

    /**
     * Default constructor
     */
    constructor() {}

    /**
     * Constructor with initialization
     *
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    constructor(p: Float, d: Float, q: Float) {
        priority = p
        durability = d
        quality = q
    }

    /**
     * Cloning constructor
     *
     * @param v Budget value to be cloned
     */
    constructor(v: BudgetValue) : this(v.priority, v.durability, v.quality) {}

    /**
     * Increase priority value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    override fun incPriority(v: Float) {
        priority = UtilityFunctions.or(priority, v)
    }

    /**
     * Decrease priority value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    override fun decPriority(v: Float) {
        priority = UtilityFunctions.and(priority, v)
    }

    /**
     * Increase durability value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    override fun incDurability(v: Float) {
        durability = UtilityFunctions.or(durability, v)
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    override fun decDurability(v: Float) {
        durability = UtilityFunctions.and(durability, v)
    }



    /**
     * Merge one BudgetValue into another
     *
     * @param that The other Budget
     */

    override fun merge(that: BudgetTriple?) {
        if (that != null) {
            BudgetFunctions.merge(this, that)
        }
    }

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    fun summary(): Float {
        return UtilityFunctions.aveGeo(priority, durability, quality)
    }

    /**
     * Whether the budget should get any processing at all
     *
     *
     * to be revised to depend on how busy the system is
     *
     * @return The decision on whether to process the ImmutableItemIdentity
     */
    fun aboveThreshold(): Boolean {
        return summary() >= Parameters.BUDGET_THRESHOLD
    }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */

    override fun toString(): String {
        return MARK.toString() + priority.toString() + SEPARATOR.toString() + durability.toString() + SEPARATOR.toString() + quality.toString() + MARK
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    fun toStringBrief(): String {
        return toString()
//        return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR + quality.toStringBrief() + MARK;

    }

    companion object {
        /**
         * The character that marks the two ends of a budget value
         */
        private const val MARK = Symbols.BUDGET_VALUE_MARK
        /**
         * The character that separates the factors in a budget value
         */
        private const val SEPARATOR = Symbols.VALUE_SEPARATOR
    }
}