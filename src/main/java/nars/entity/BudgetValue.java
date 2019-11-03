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
package nars.entity;

import nars.inference.BudgetFunctions;
import nars.inference.UtilityFunctions;
import nars.io.Symbols;
import nars.main_nogui.Parameters;

/**
 * A triple of priority (current), durability (decay), and quality (long-term average).
 */
public class BudgetValue implements BudgetTriple  {

    /**
     * The character that marks the two ends of a budget value
     */
    private static final char MARK = Symbols.BUDGET_VALUE_MARK;
    /**
     * The character that separates the factors in a budget value
     */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
    /**
     * The relative share of time resource to be allocated
     */
    protected Float priority= new Float(0.01f);;
    /**
     * The percent of priority to be kept in a constant period; All priority
     * values “decay” over time, though at different rates. Each item is given a
     * “durability” factor in (0, 1) to specify the percentage of priority level
     * left after each reevaluation
     */
    protected Float durability= new Float(0.01f);;
    /**
     * The overall (context-independent) evaluation
     */
    protected Float quality= new Float(0.01f);;

    /**
     * Default constructor
     */
    public BudgetValue() {
    }

    /**
     * Constructor with initialization
     *
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    public BudgetValue(float p, float d, float q) {
        priority = new Float(p);
        durability = new Float(d);
        quality = new Float(q);
    }

    /**
     * Cloning constructor
     *
     * @param v Budget value to be cloned
     */
    public BudgetValue(BudgetValue v) {
   this(     new Float(v.getPriority()),
        new Float(v.getDurability()),
      new Float(v.getQuality())) ;
    }

    /**
     * Get priority value
     *
     * @return The current priority
     */
    @Override
    public float getPriority() {
        return priority.floatValue();
    }

    /**
     * Change priority value
     *
     * @param v The new priority
     */
    @Override
    public void setPriority(float v) {
        priority=v;
    }

    /**
     * Increase priority value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    @Override
    public void incPriority(float v) {
        priority=UtilityFunctions.or(priority.floatValue(), v);
    }

    /**
     * Decrease priority value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    @Override
    public void decPriority(float v) {
        priority=UtilityFunctions.and(priority.floatValue(), v);
    }

    /**
     * Get durability value
     *
     * @return The current durability
     */
    @Override
    public float getDurability() {
        return durability.floatValue();
    }

    /**
     * Change durability value
     *
     * @param v The new durability
     */
    @Override
    public void setDurability(float v) {
        durability=v;
    }

    /**
     * Increase durability value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    @Override
    public void incDurability(float v) {
        durability=UtilityFunctions.or(durability.floatValue(), v);
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    @Override
    public void decDurability(float v) {
        durability=UtilityFunctions.and(durability.floatValue(), v);
    }

    /**
     * Get quality value
     *
     * @return The current quality
     */
    @Override
    public float getQuality() {
        return quality.floatValue();
    }

    /**
     * Change quality value
     *
     * @param v The new quality
     */
    @Override
    public void setQuality(float v) {
        quality=v;
    }

    /**
     * Merge one BudgetValue into another
     *
     * @param that The other Budget
     */

    @Override
    public void merge(BudgetTriple that) {
        BudgetFunctions.merge(this, that);
    }

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
     public float summary() {
        return UtilityFunctions.aveGeo(priority.floatValue(), durability.floatValue(), quality.floatValue());
    }

    /**
     * Whether the budget should get any processing at all
     * <p>
     * to be revised to depend on how busy the system is
     *
     * @return The decision on whether to process the AbstractItem
     */
     public boolean aboveThreshold() {
        return (summary() >= Parameters.BUDGET_THRESHOLD);
    }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */

    public String toString() {
        return MARK + priority.toString() + SEPARATOR + durability.toString() + SEPARATOR + quality.toString() + MARK;
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    public String toStringBrief() {return toString();
//        return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR + quality.toStringBrief() + MARK;
    }
}
