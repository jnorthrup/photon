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

import nars.data.ItemStruct;

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 * <p>
 * It has a key and a budget. Cannot be cloned
 */
public abstract class Item implements ItemStruct{

    private String key;
    private BudgetValue budget;

    /**
     * The default constructor
     */
    protected Item() {}

    /**
     * Constructor with default budget
     * @param key The key value
     */
    protected Item(String key) {
        this.setKey(key);
        this.setBudget(new BudgetValue());
     }

    /**
     * Constructor with initial budget
     * @param key The key value
     * @param budget The initial budget
     */
    protected Item(String key, BudgetValue budget) {
        this.setKey(key);
        this.setBudget(new BudgetValue(budget));  // clone, not assignment
    }

    /**
     * Constructor with initial budget
     * @param budget The initial budget
     */
    protected void setBudget(BudgetValue budget) {
        this.budget = budget;
    }

    /** The key of the Item, unique in a Bag */ /**
     * Get the current key
     * @return Current key value
     */
    public String getKey() {
        return key;
    }

    /** The budget of the Item, consisting of 3 numbers */ /**
     * Get BudgetValue
     * @return Current BudgetValue
     */
    public BudgetValue getBudget() {
        return budget;
    }

    /**
     * Get priority value
     * @return Current priority value
     */
     public float getPriority() {
        return getBudget().getPriority();
    }

    /**
     * Set priority value
     * @param v Set a new priority value
     */
    public void setPriority(float v) {
        getBudget().setPriority(v);
    }

    /**
     * Increase priority value
     * @param budget
     * @param v The amount of increase
     */
    public static void incPriority(BudgetValue budget, float v) {
        budget.incPriority(v);
    }

    /**
     * Decrease priority value
     * @param budget
     * @param v The amount of decrease
     */
    public static void decPriority(BudgetValue budget, float v) {
        budget.decPriority(v);
    }

    /**
     * Get durability value
     * @return Current durability value
     */
    public float getDurability() {
        return getBudget().getDurability();
    }

    /**
     * Set durability value
     * @param v The new durability value
     */
    public void setDurability(float v) {
        getBudget().setDurability(v);
    }

    /**
     * Increase durability value
     * @param budget
     * @param v The amount of increase
     */
    public static void incDurability(BudgetValue budget, float v) {
        budget.incDurability(v);
    }

    /**
     * Decrease durability value
     * @param budget
     * @param v The amount of decrease
     */
    public static void decDurability(BudgetValue budget, float v) {
        budget.decDurability(v);
    }

    /**
     * Get quality value
     * @return The quality value
     */
    public float getQuality() {
        return getBudget().getQuality();
    }

    /**
     * Set quality value
     * @param v The new quality value
     */
    public void setQuality(float v) {
        getBudget().setQuality(v);
    }

    /**
     * Merge with another Item with identical key
     * @param budget
     * @param that The Item to be merged
     */
    public static void merge(BudgetValue budget, Item that) {
        budget.merge(that.getBudget());
    }

    /**
     * Return a String representation of the Item
     * @return The String representation of the full content
     */
    @Override
    public String toString() {
        return getBudget() + " " + getKey();
    }

    /**
     * Return a String representation of the Item after simplification
     * @return A simplified String representation of the content
     * @param budget
     * @param key
     */
    public static String toStringBrief(BudgetValue budget, String key) {
        return budget.toStringBrief() + " " + key;
    }
    
    public static String toStringLong(Item item) {
    	return item.toString();
    }

    public void setKey(String key) {
        this.key = key;
    }
}
