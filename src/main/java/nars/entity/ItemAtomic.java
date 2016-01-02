/*
 * Item.java
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
public abstract class ItemAtomic implements Item {

    /** The key of the Item, unique in a Bag */
    public String key;
    /** The budget of the Item, consisting of 3 numbers */
    public BudgetValueAtomic budget;

    /**
     * The default constructor
     */
    public ItemAtomic() {}

    /**
     * Constructor with default budget
     * @param key The key value
     */
    public ItemAtomic(String key) {
        this.key = key;
        this.budget = new BudgetValueAtomic();
     }

    /**
     * Constructor with initial budget
     * @param key The key value
     * @param budget The initial budget
     */
    public ItemAtomic(String key, BudgetValueAtomic budget) {
        this.key = key;
        this.budget = new BudgetValueAtomic(budget);  // clone, not assignment
    }

    /**
     * Constructor with initial budget
     * @param budget The initial budget
     */
    @Override
    public void setBudget(BudgetValueAtomic budget) {
        this.budget = budget;
    }

    /**
     * Get the current key
     * @return Current key value
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * Get BudgetValue
     * @return Current BudgetValue
     */
    @Override
    public BudgetValueAtomic getBudget() {
        return budget;
    }

    /**
     * Get priority value
     * @return Current priority value
     */
     @Override
     public float getPriority() {
        return budget.getPriority();
    }

    /**
     * Set priority value
     * @param v Set a new priority value
     */
    @Override
    public void setPriority(float v) {
        budget.setPriority(v);
    }

    /**
     * Increase priority value
     * @param v The amount of increase
     */
    @Override
    public void incPriority(float v) {
        budget.incPriority(v);
    }

    /**
     * Decrease priority value
     * @param v The amount of decrease
     */
    @Override
    public void decPriority(float v) {
        budget.decPriority(v);
    }

    /**
     * Get durability value
     * @return Current durability value
     */
    @Override
    public float getDurability() {
        return budget.getDurability();
    }

    /**
     * Set durability value
     * @param v The new durability value
     */
    @Override
    public void setDurability(float v) {
        budget.setDurability(v);
    }

    /**
     * Increase durability value
     * @param v The amount of increase
     */
    @Override
    public void incDurability(float v) {
        budget.incDurability(v);
    }

    /**
     * Decrease durability value
     * @param v The amount of decrease
     */
    @Override
    public void decDurability(float v) {
        budget.decDurability(v);
    }

    /**
     * Get quality value
     * @return The quality value
     */
    @Override
    public float getQuality() {
        return budget.getQuality();
    }

    /**
     * Set quality value
     * @param v The new quality value
     */
    @Override
    public void setQuality(float v) {
        budget.setQuality(v);
    }

    /**
     * Merge with another Item with identical key
     * @param that The Item to be merged
     */
    public void merge(ItemAtomic that) {
        budget.merge(that.getBudget());
    }

    /**
     * Return a String representation of the Item
     * @return The String representation of the full content
     */
    @Override
    public String toString() {
        return budget + " " + key ;
    }

    /**
     * Return a String representation of the Item after simplification
     * @return A simplified String representation of the content
     */
    public String toStringBrief() {
        return budget.toStringBrief() + " " + key ;
    }
    
    public String toStringLong() {
    	return toString();
    }

}
