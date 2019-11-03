/*
 * Bag.java
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
package nars.storage;

import nars.entity.AbstractItem;
import nars.inference.BudgetFunctions;
import nars.main_nogui.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A Bag is a storage with a constant capacity and maintains an internal
 * priority distribution for retrieval.
 * <p>
 * Each entity in a bag must extend AbstractItem, which has a BudgetValue and a key.
 * <p>
 * A name table is used to merge duplicate items that have the same key.
 * <p>
 * The bag space is divided by a threshold, above which is mainly time
 * management, and below, space management. Differences: (1) level selection vs.
 * item selection, (2) decay rate
 *
 * @param <E> The type of the AbstractItem in the Bag
 */
public abstract class Bag<E extends AbstractItem> {

    /**
     * priority levels
     */
    private static final int TOTAL_LEVEL = Parameters.BAG_LEVEL;
    /**
     * firing threshold
     */
    private static final int THRESHOLD = Parameters.BAG_THRESHOLD;
    /**
     * relative threshold, only calculate once
     */
    private static final float RELATIVE_THRESHOLD = (float) THRESHOLD / (float) TOTAL_LEVEL;
    /**
     * hashtable load factor
     */
    private static final float LOAD_FACTOR = Parameters.LOAD_FACTOR;       //
    /**
     * shared DISTRIBUTOR that produce the probability distribution
     */
    private static final Distributor DISTRIBUTOR = new Distributor(TOTAL_LEVEL); //
    /**
     * defined in different bags
     */
    private final int capacity;
    /**
     * reference to memory
     */
    protected Memory memory;
    /**
     * mapping from key to item
     */
    private HashMap<String, E> nameTable;
    /**
     * array of lists of items, for items on different level
     */
    private List<ArrayList<E>> itemTable;
    /**
     * current sum of occupied level
     */
    private int mass;
    /**
     * index to get next level, kept in individual objects
     */
    private int levelIndex;
    /**
     * current take out level
     */
    private int currentLevel;
    /**
     * maximum number of items to be taken out at current level
     */
    private int currentCounter;
    private BagObserver<E> bagObserver = new NullBagObserver<>();
    /**
     * The display level; initialized at lowest
     */
    private int showLevel = Parameters.BAG_THRESHOLD;

    /**
     * constructor, called from subclasses
     *
     * @param memory The reference to memory
     */
    protected Bag(Memory memory) {
        this.memory = memory;
//        showing = false;
        capacity = capacity();
        init();
    }

    public void init() {
        itemTable = new ArrayList<>(TOTAL_LEVEL);
        int bound = TOTAL_LEVEL;
        for (int i = 0; i < bound; i++) {
            itemTable.add(new ArrayList<>());
        }
        nameTable = new HashMap<>((int) (capacity / LOAD_FACTOR), LOAD_FACTOR);
        currentLevel = TOTAL_LEVEL - 1;
        levelIndex = capacity % TOTAL_LEVEL; // so that different bags start at different point
        mass = 0;
        currentCounter = 0;
    }

    /**
     * To get the capacity of the concrete subclass
     *
     * @return Bag capacity, in number of Items allowed
     */
    protected abstract int capacity();

    /**
     * Get the item decay rate, which differs in difference subclass, and can be
     * changed in run time by the user, so not a constant.
     *
     * @return The number of times for a decay factor to be fully applied
     */
    protected abstract int forgetRate();

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    public int size() {
        return nameTable.size();
    }

    /**
     * Get the average priority of Items
     *
     * @return The average priority of Items in the bag
     */
    public float averagePriority() {
        if (size() == 0) {
            return 0.01f;
        }
        var f = (float) mass / (size() * TOTAL_LEVEL);
        if (f > 1) {
            return 1.0f;
        }
        return f;
    }



    /**
     * Get an AbstractItem by key
     *
     * @param key The key of the AbstractItem
     * @return The AbstractItem with the given key
     */
    public E get(String key) {
        return nameTable.get(key);
    }

    /**
     * Add a new AbstractItem into the Bag
     *
     * @param newItem The new AbstractItem
     * @return Whether the new AbstractItem is added into the Bag
     */
    public boolean putIn(E newItem) {
        var newKey = newItem.getKey();
        var oldItem = nameTable.put(newKey, newItem);
        if (oldItem != null) {                  // merge duplications
            outOfBase(oldItem);
            newItem.merge(oldItem);
        }
        var overflowItem = intoBase(newItem);  // put the (new or merged) item into itemTable
        if (overflowItem != null) {             // remove overflow
            var overflowKey = overflowItem.getKey();
            nameTable.remove(overflowKey);
            return (overflowItem != newItem);
        } else {
            return true;
        }
    }

    /**
     * Put an item back into the itemTable
     * <p>
     * The only place where the forgetting rate is applied
     *
     * @param oldItem The AbstractItem to put back
     * @return Whether the new AbstractItem is added into the Bag
     */
    public boolean putBack(E oldItem) {
        BudgetFunctions.forget(oldItem.getBudget(), forgetRate(), RELATIVE_THRESHOLD);
        return putIn(oldItem);
    }

    /**
     * Choose an AbstractItem according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected AbstractItem
     */
    public E takeOut() {
        if (nameTable.isEmpty()) { // empty bag
            return null;
        }
        if (emptyLevel(currentLevel) || (currentCounter == 0)) { // done with the current level
            currentLevel = DISTRIBUTOR.pick(levelIndex);
            levelIndex = DISTRIBUTOR.next(levelIndex);
            while (emptyLevel(currentLevel)) {          // look for a non-empty level
                currentLevel = DISTRIBUTOR.pick(levelIndex);
                levelIndex = DISTRIBUTOR.next(levelIndex);
            }
            if (currentLevel < THRESHOLD) { // for dormant levels, take one item
                currentCounter = 1;
            } else {                  // for active levels, take all current items
                currentCounter = itemTable.get(currentLevel).size();
            }
        }
        var selected = takeOutFirst(currentLevel); // take out the first item in the level
        currentCounter--;
        nameTable.remove(selected.getKey());
        refresh();
        return selected;
    }

    /**
     * Pick an item by key, then remove it from the bag
     *
     * @param key The given key
     * @return The AbstractItem with the key
     */
    public E pickOut(String key) {
        var picked = nameTable.get(key);
        if (picked != null) {
            outOfBase(picked);
            nameTable.remove(key);
        }
        return picked;
    }

    /**
     * Check whether a level is empty
     *
     * @param n The level index
     * @return Whether that level is empty
     */
    protected boolean emptyLevel(int n) {
        return ((itemTable.get(n) == null) || itemTable.get(n).isEmpty());
    }

    /**
     * Decide the put-in level according to priority
     *
     * @param item The AbstractItem to put in
     * @return The put-in level
     */
    private int getLevel(E item) {
        var fl = item.getPriority() * TOTAL_LEVEL;
        var level = (int) Math.ceil(fl) - 1;
        return (level < 0) ? 0 : level;     // cannot be -1
    }

    /**
     * Insert an item into the itemTable, and return the overflow
     *
     * @param newItem The AbstractItem to put in
     * @return The overflow AbstractItem
     */
    private E intoBase(E newItem) {
        E oldItem = null;
        var inLevel = getLevel(newItem);
        if (size() > capacity) {      // the bag is full
            var outLevel = 0;
            while (emptyLevel(outLevel)) {
                outLevel++;
            }
            if (outLevel > inLevel) {           // ignore the item and exit
                return newItem;
            } else {                            // remove an old item in the lowest non-empty level
                oldItem = takeOutFirst(outLevel);
            }
        }
        itemTable.get(inLevel).add(newItem);        // FIFO
        mass += (inLevel + 1);                  // increase total mass
        refresh();                              // refresh the window
        return oldItem;        // TODO return null is a bad smell
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @param level The current level
     * @return The first AbstractItem
     */
    private E takeOutFirst(int level) {
        var selected = itemTable.get(level).get(0);
        itemTable.get(level).remove(0);
        mass -= (level + 1);
        refresh();
        return selected;
    }

    /**
     * Remove an item from itemTable, then adjust mass
     *
     * @param oldItem The AbstractItem to be removed
     */
    protected void outOfBase(E oldItem) {
        var level = getLevel(oldItem);
        itemTable.get(level).remove(oldItem);
        mass -= (level + 1);
        refresh();
    }

    /**
     * To start displaying the Bag in a BagWindow;
     * {@link nars.gui.BagWindow} implements interface {@link BagObserver};
     *
     * @param bagObserver BagObserver to set
     * @param title       The title of the window
     */
    public void addBagObserver(BagObserver<E> bagObserver, String title) {
        this.bagObserver = bagObserver;
        bagObserver.post(toString());
        bagObserver.setTitle();
        bagObserver.setBag(this);
    }

    /**
     * Refresh display
     */
    public void refresh() {
        bagObserver.refresh(toString());
    }

    /**
     * Collect Bag content into a String for display
     */
    @Override
    public String toString() {
        var buf = new StringBuffer(" ");
        for (var i = TOTAL_LEVEL; i >= showLevel; i--) {
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- Level ").append(i).append(":\n ");
                for (var j = 0; j < itemTable.get(i - 1).size(); j++) {
                    buf = buf.append(itemTable.get(i - 1).get(j).toStringBrief()).append("\n ");
                }
            }
        }
        return buf.toString();
    }

    /**
     * TODO refactor : paste from preceding method
     */
    public String toStringLong() {
        var buf = new StringBuffer(" BAG " + getClass().getSimpleName());
        buf.append(" ").append(showSizes());
        for (var i = TOTAL_LEVEL; i >= showLevel; i--) {
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- LEVEL ").append(i).append(":\n ");
                for (var j = 0; j < itemTable.get(i - 1).size(); j++) {
                    buf = buf.append(itemTable.get(i - 1).get(j).toStringLong()).append("\n ");
                }
            }
        }
        buf.append(">>>> end of Bag").append(getClass().getSimpleName());
        return buf.toString();
    }

    /**
     * show item Table Sizes
     */
    String showSizes() {
        var buf = new StringBuilder(" ");
        var levels = 0;
        for (Collection<E> items : itemTable) {
            if ((items != null) && !items.isEmpty()) {
                levels++;
                buf.append(items.size()).append(" ");
            }
        }
        return "Levels: " + levels + ", sizes: " + buf;
    }

}
