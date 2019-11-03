/*
 * AbstractItem.java
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

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 * <p>
 * It has a key and a budget. Cannot be cloned
 */
public abstract class AbstractItem implements BudgetTriple {

    /**
     * The key of the AbstractItem, unique in a Bag
     */
    protected String key;
    /**
     * The budget of the AbstractItem, consisting of 3 numbers
     */
    protected BudgetValue budget;

    /**
     * The default constructor
     */
    protected AbstractItem() {
    }

    /**
     * Constructor with default budget
     *
     * @param key The key value
     */
    protected AbstractItem(String key) {
        this.key = key;
        this.budget = new BudgetValue();
    }

    /**
     * Constructor with initial budget
     *
     * @param key    The key value
     * @param budget The initial budget
     */
    protected AbstractItem(String key, BudgetValue budget) {
        this.key = key;
        this.budget = new BudgetValue(budget);  // clone, not assignment
    }

    /**
     * Get the current key
     *
     * @return Current key value
     */
    public String getKey() {
        return key;
    }

    /**
     * Get BudgetValue
     *
     * @return Current BudgetValue
     */
    public BudgetValue getBudget() {
        return budget;
    }

    /**
     * Get priority value
     *
     * @return Current priority value
     */
    @Override
    public float getPriority() {
        return budget.getPriority();
    }

    /**
     * Set priority value
     *
     * @param v Set a new priority value
     */
    @Override
    public void setPriority(float v) {
        budget.setPriority(v);
    }

    /**
     * Increase priority value
     *
     * @param v The amount of increase
     */
    @Override
    public void incPriority(float v) {
        budget.incPriority(v);
    }

    /**
     * Decrease priority value
     *
     * @param v The amount of decrease
     */
    @Override
    public void decPriority(float v) {
        budget.decPriority(v);
    }

    /**
     * Get durability value
     *
     * @return Current durability value
     */
    @Override
    public float getDurability() {
        return budget.getDurability();
    }

    /**
     * Set durability value
     *
     * @param v The new durability value
     */
    @Override
    public void setDurability(float v) {
        budget.setDurability(v);
    }

    /**
     * Increase durability value
     *
     * @param v The amount of increase
     */
    @Override
    public void incDurability(float v) {
        budget.incDurability(v);
    }

    /**
     * Decrease durability value
     *
     * @param v The amount of decrease
     */
    @Override
    public void decDurability(float v) {
        budget.decDurability(v);
    }

    /**
     * Get quality value
     *
     * @return The quality value
     */
    @Override
    public float getQuality() {
        return budget.getQuality();
    }

    /**
     * Set quality value
     *
     * @param v The new quality value
     */
    @Override
    public void setQuality(float v) {
        budget.setQuality(v);
    }

    /**
     * Merge with another AbstractItem with identical key
     *
     * @param that The AbstractItem to be merged
     */
    public void merge(AbstractItem that) {
        budget.merge(that.getBudget());
    }

    /**
     * Return a String representation of the AbstractItem
     *
     * @return The String representation of the full content
     */
    @Override
    public String toString() {
        return budget + " " + key;
    }

    /**
     * Return a String representation of the AbstractItem after simplification
     *
     * @return A simplified String representation of the content
     */
    public String toStringBrief() {
        return budget.toStringBrief() + " " + key;
    }

    public String toStringLong() {
        return toString();
    }

}
