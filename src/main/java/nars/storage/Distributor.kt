/*
 * Distributor.java
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

/**
 * A pseudo-random number generator, used in Bag.
 */
internal class Distributor( var rank: Int) {
    /**
     * Shuffled sequence of index numbers
     */
    private val order: IntArray
    /**
     * Capacity of the array
     */
    private val capacity: Int

    /**
     * Get the next number according to the given index
     *
     * @param index The current index
     * @return the random value
     */
    fun pick(index: Int): Int {
        return order[index]
    }

    /**
     * Advance the index
     *
     * @param index The current index
     * @return the next index
     */
    fun next(index: Int): Int {
        return (index + 1) % capacity
    }

    /**
     * For any number N < range, there is N+1 copies of it in the array, distributed as evenly as possible
     *
     * @param range Range of valid numbers
     */

    init {
        var index: Int
        var time: Int
        capacity = rank * (rank + 1) / 2
        order = IntArray(capacity)
        index = 0
        while (index < capacity) {
            order[index] = -1
            index++
        }
        while (rank > 0) {
            time = 0
            while (time < rank) {
                index = (capacity / rank + index) % capacity
                while (order[index] >= 0) {
                    index = (index + 1) % capacity
                }
                order[index] = rank - 1
                time++
            }
            rank--
        }
    }
}