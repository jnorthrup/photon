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
package nars.storage

import nars.entity.ItemIdentity
import nars.inference.BudgetFunctions.forget
import nars.main_nogui.Parameters
import java.util.*
import kotlin.math.ceil

/**
 * A Bag is a storage with a constant capacity and maintains an internal
 * priority distribution for retrieval.
 *
 *
 * Each entity in a bag must extend ImmutableItemIdentity, which has a BudgetValue and a key.
 *
 *
 * A name table is used to merge duplicate items that have the same key.
 *
 *
 * The bag space is divided by a threshold, above which is mainly time
 * management, and below, space management. Differences: (1) level selection vs.
 * item selection, (2) decay rate
 *
 * @param <E> The type of the ImmutableItemIdentity in the Bag
</E> */
abstract class Bag<E : ItemIdentity?> protected constructor(
        /**
         * reference to memory
         */
        var memory: BackingStore) {
    /**
     * defined in different bags
     */
    private val capacity: Int
    /**
     * mapping from key to item
     */
    private var nameTable: HashMap<String, E>? = null
    /**
     * array of lists of items, for items on different level
     */
    private var itemTable: MutableList<ArrayList<E>>? = null
    /**
     * current sum of occupied level
     */
    private var mass = 0
    /**
     * index to get next level, kept in individual objects
     */
    private var levelIndex = 0
    /**
     * current take out level
     */
    private var currentLevel = 0
    /**
     * maximum number of items to be taken out at current level
     */
    private var currentCounter = 0
    private var bagObserver: BagObserver<E> = NullBagObserver()
    /**
     * The display level; initialized at lowest
     */
    private val showLevel = Parameters.BAG_THRESHOLD

    /**
     *
     */
    fun init() {
        itemTable = ArrayList(TOTAL_LEVEL)
        val bound = TOTAL_LEVEL
        for (i in 0 until bound) {
            itemTable!!.add(ArrayList())
        }
        nameTable = HashMap((capacity / LOAD_FACTOR).toInt(), LOAD_FACTOR)
        currentLevel = TOTAL_LEVEL - 1
        levelIndex = capacity % TOTAL_LEVEL
        mass = 0
        currentCounter = 0
    }

    /**
     * To get the capacity of the concrete subclass
     *
     * @return Bag capacity, in number of Items allowed
     */
    protected abstract fun capacity(): Int

    /**
     * Get the item decay rate, which differs in difference subclass, and can be
     * changed in run time by the user, so not a constant.
     *
     * @return The number of times for a decay factor to be fully applied
     */
    protected abstract fun forgetRate(): Int

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    fun size(): Int {
        return nameTable!!.size
    }

    /**
     * Get the average priority of Items
     *
     * @return The average priority of Items in the bag
     */
    fun averagePriority(): Float {
        if (size() == 0) {
            return 0.01f
        }
        val f = mass.toFloat() / (size() * TOTAL_LEVEL)
        return if (f > 1) {
            1.0f
        } else f
    }

    /**
     * Get an ImmutableItemIdentity by key
     *
     * @param key The key of the ImmutableItemIdentity
     * @return The ImmutableItemIdentity with the given key
     */
    operator fun get(key: String) = nameTable?.get(key)


    /**
     * Add a new ImmutableItemIdentity into the Bag
     *
     * @param newItem The new ImmutableItemIdentity
     * @return Whether the new ImmutableItemIdentity is added into the Bag
     */
    fun putIn(newItem: E): Boolean {
        val newKey = newItem!!.key
        val oldItem = nameTable?.put(newKey, newItem)
        if (oldItem != null) {                  // merge duplications

            outOfBase(oldItem)
            newItem.merge(oldItem)
        }
        val overflowItem = intoBase(newItem)  // put the (new or merged) item into itemTable

        return if (overflowItem != null) {             // remove overflow

            val overflowKey = overflowItem.key
            nameTable!!.remove(overflowKey)
            overflowItem !== newItem
        } else {
            true
        }
    }

    /**
     * Put an item back into the itemTable
     *
     *
     * The only place where the forgetting rate is applied
     *
     * @param oldItem The ImmutableItemIdentity to put back
     * @return Whether the new ImmutableItemIdentity is added into the Bag
     */
    fun putBack(oldItem: E): Boolean {
        forget(oldItem!!.budget, forgetRate().toFloat(), RELATIVE_THRESHOLD)
        return putIn(oldItem)
    }

    /**
     * Choose an ImmutableItemIdentity according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected ImmutableItemIdentity
     */
    fun takeOut(): E? {
        if (nameTable!!.isEmpty()) { // empty bag

            return null
        }
        if (emptyLevel(currentLevel) || currentCounter == 0) { // done with the current level
            currentLevel = DISTRIBUTOR.pick(levelIndex)
            levelIndex = DISTRIBUTOR.next(levelIndex)
            while (emptyLevel(currentLevel)) {          // look for a non-empty level
                currentLevel = DISTRIBUTOR.pick(levelIndex)
                levelIndex = DISTRIBUTOR.next(levelIndex)
            }
            currentCounter = if (currentLevel < THRESHOLD) { // for dormant levels, take one item
                1
            } else {                  // for active levels, take all current items
                itemTable!![currentLevel].size
            }
        }
        val selected = takeOutFirst(currentLevel) // take out the first item in the level

        currentCounter--
        nameTable!!.remove(selected!!.key)
        refresh()
        return selected
    }

    /**
     * Pick an item by key, then remove it from the bag
     *
     * @param key The given key
     * @return The ImmutableItemIdentity with the key
     */
    fun pickOut(key: String): E? {
        val picked = nameTable!![key]
        if (picked != null) {
            outOfBase(picked)
            nameTable!!.remove(key)
        }
        return picked
    }

    /**
     * Check whether a level is empty
     *
     * @param n The level index
     * @return Whether that level is empty
     */
    private fun emptyLevel(n: Int): Boolean {
        return itemTable!![n] == null || itemTable!![n].isEmpty()
    }

    /**
     * Decide the put-in level according to priority
     *
     * @param item The ImmutableItemIdentity to put in
     * @return The put-in level
     */
    private fun getLevel(item: E): Int {
        val fl = item!!.priority * TOTAL_LEVEL
        val level = ceil(fl.toDouble()).toInt() - 1
        return if (level < 0) 0 else level     // cannot be -1
    }

    /**
     * Insert an item into the itemTable, and return the overflow
     *
     * @param newItem The ImmutableItemIdentity to put in
     * @return The overflow ImmutableItemIdentity
     */
    private fun intoBase(newItem: E): E? {
        var oldItem: E? = null
        val inLevel = getLevel(newItem)
        if (size() > capacity) {      // the bag is full

            var outLevel = 0
            while (emptyLevel(outLevel)) {
                outLevel++
            }
            // ignore the item and exit
            if (outLevel <= inLevel) {                            // remove an old item in the lowest non-empty level
                oldItem = takeOutFirst(outLevel)
            } else return newItem
        }
        itemTable!![inLevel].add(newItem)        // FIFO

        mass += inLevel + 1
        refresh()                              // refresh the window

        return oldItem        // TODO return null is a bad smell
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @param level The current level
     * @return The first ImmutableItemIdentity
     */
    private fun takeOutFirst(level: Int): E {
        val selected = itemTable!![level][0]
        itemTable!![level].removeAt(0)
        mass -= level + 1
        refresh()
        return selected
    }

    /**
     * Remove an item from itemTable, then adjust mass
     *
     * @param oldItem The ImmutableItemIdentity to be removed
     */
    private fun outOfBase(oldItem: E) {
        val level = getLevel(oldItem)
        itemTable!![level].remove(oldItem)
        mass -= level + 1
        refresh()
    }

    /**
     * Refresh display
     */
    fun refresh() {
        bagObserver.refresh(toString())
    }

    /**
     * Collect Bag content into a String for display
     */

    override fun toString(): String {
        var buf = StringBuffer(" ")
        (TOTAL_LEVEL downTo showLevel).forEach { i ->
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- Level ").append(i).append(":\n ")
                itemTable!![i - 1].indices.forEach { j ->
                    buf = buf.append(itemTable!![i - 1][j]!!.toStringBrief()).append("\n ")
                }
            }
        }
        return buf.toString()
    }

    /**
     * TODO refactor : paste from preceding method
     */
    fun toStringLong(): String {
        var buf = StringBuffer(" BAG " + javaClass.simpleName)
        buf.append(" ").append(showSizes())
        for (i in TOTAL_LEVEL downTo showLevel) {
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- LEVEL ").append(i).append(":\n ")
                for (j in itemTable!![i - 1].indices) {
                    buf = buf.append(itemTable!![i - 1][j]!!.toStringLong()).append("\n ")
                }
            }
        }
        buf.append(">>>> end of Bag").append(javaClass.simpleName)
        return buf.toString()
    }

    /**
     * show item Table Sizes
     */
    private fun showSizes(): String {
        val buf = StringBuilder(" ")
        var levels = 0
        for (items in itemTable!!) {
            if (items != null && items.isNotEmpty()) {
                levels++
                buf.append(items.size).append(" ")
            }
        }
        return "Levels: $levels, sizes: $buf"
    }

    companion object {
        /**
         * priority levels
         */
        private const val TOTAL_LEVEL = Parameters.BAG_LEVEL
        /**
         * firing threshold
         */
        private const val THRESHOLD = Parameters.BAG_THRESHOLD
        /**
         * relative threshold, only calculate once
         */
        private const val RELATIVE_THRESHOLD = THRESHOLD.toFloat() / TOTAL_LEVEL.toFloat()
        /**
         * hashtable load factor
         */
        private const val LOAD_FACTOR = Parameters.LOAD_FACTOR       //

        /**
         * shared DISTRIBUTOR that produce the probability distribution
         */
        private val DISTRIBUTOR = Distributor(TOTAL_LEVEL) //
    }

    /**
     * constructor, called from subclasses
     *
     * @param memory The reference to memory
     */

    init {
        capacity = capacity()
        init()
    }
}