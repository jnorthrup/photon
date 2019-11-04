/*
 * Stamp.java
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

//import nars.io.Symbols
import nars.io.stamp_display
import nars.io.stamp_display.STAMP_OPENER
import nars.io.stamp_display.STAMP_STARTER
import nars.main_nogui.Parameters
import nars.main_nogui.ReasonerBatch
import kotlin.math.min

/**
 * Each Sentence has a time stamp, consisting the following components:
 * (1) The creation time of the sentence,
 * (2) A evidentialBase of serial numbers of sentence, from which the sentence is derived.
 * Each input sentence gets a unique serial number, though the creation time may be not unique.
 * The derived sentences inherits serial numbers from its parents, cut at the baseLength limit.
 */
class Stamp : Cloneable {
    /**
     * Get the evidentialBase, called in this class only
     *
     * @return The evidentialBase of numbers
     */
    /**
     * serial numbers
     */
    private var base: LongArray
    /**
     * evidentialBase baseLength
     */
    private var baseLength: Int
    /**
     * Get the creationTime of the truth-value
     *
     * @return The creation time
     */
    /**
     * creation time of the stamp
     */
    var creationTime: Long
        private set

    /**
     * Generate a new stamp, with a new serial number, for a new Task
     *
     * @param time Creation time of the stamp
     */
    constructor(time: Long) {
        currentSerial++
        baseLength = 1
        base = LongArray(baseLength)
        base[0] = currentSerial
        creationTime = time
    }

    /**
     * Generate a new stamp identical with a given one
     *
     * @param old The stamp to be cloned
     */
    private constructor(old: Stamp) {
        baseLength = old.length()
        base = old.base
        creationTime = old.creationTime
    }

    /**
     * Generate a new stamp from an existing one, with the same evidentialBase but different creation time
     *
     *
     * For single-premise rules
     *
     * @param old  The stamp of the single premise
     * @param time The current time
     */
    constructor(old: Stamp, time: Long) {
        baseLength = old.length()
        base = old.base
        creationTime = time
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * the first one is no shorter than the second
     *
     * @param first  The first Stamp
     * @param second The second Stamp
     */
    private constructor(first: Stamp, second: Stamp, time: Long) {
        var i1: Int=0
        var i2: Int=0
        var j: Int=0
        baseLength = min(first.length() + second.length(), Parameters.MAXIMUM_STAMP_LENGTH)
        base = LongArray(baseLength)
        while (i2 < second.length() && j < baseLength) {
            base[j] = first[i1]
            i1++
            j++
            base[j] = second[i2]
            i2++
            j++
        }
        while (i1 < first.length() && j < baseLength) {
            base[j] = first[i1]
            i1++
            j++
        }
        creationTime = time
    }

    /**
     * Clone a stamp
     *
     * @return The cloned stamp
     */

    public override fun clone(): Any {
        return Stamp(this)
    }

    /**
     * Return the baseLength of the evidentialBase
     *
     * @return Length of the Stamp
     */
    fun length(): Int {
        return baseLength
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     *
     * @param i The index
     * @return The number at the index
     */
    internal operator fun get(i: Int): Long {
        return base[i]
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private fun toSet()  = base.mapTo(sortedSetOf(), { it })
    /**
     * Check if two stamps contains the same content
     *
     * @param that The Stamp to be compared
     * @return Whether the two have contain the same elements
     */

    override fun equals(that: Any?): Boolean {
        var result = false
        if (that is Stamp) {
            val set1 = toSet()
            val set2 = that.toSet()
            result = set1.containsAll(set2) && set2.containsAll(set1)
        }
        return result
    }

    /**
     * The hash code of Stamp
     *
     * @return The hash code
     */

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */

    override fun toString(): String {
        val buffer = StringBuilder(" " + STAMP_OPENER.sym + creationTime)
        buffer.append(" ").append(STAMP_STARTER.sym).append(" ")
        val bound = baseLength
        (0 until bound).forEach { i ->
            buffer.append(base[i])
            if (i < baseLength - 1) {
                buffer.append(stamp_display.STAMP_SEPARATOR.sym)
            } else {
                buffer.append(stamp_display.STAMP_CLOSER.sym).append(" ")
            }
        }
        return buffer.toString()
    }

    companion object {
        /**
         * serial number, for the whole system
         * TODO : should it really be static?
         * or a Stamp be a field in [ReasonerBatch] ?
         */
        private var currentSerial: Long = 0

        /**
         * Try to merge two Stamps, return null if have overlap
         *
         *
         * By default, the event time of the first stamp is used in the result
         *
         * @param first  The first Stamp
         * @param second The second Stamp
         * @param time   The new creation time
         * @return The merged Stamp, or null
         */
        fun make(first: Stamp, second: Stamp, time: Long): Stamp? {
            for (i in 0 until first.length()) {
                for (j in 0 until second.length()) {
                    if (first[i] == second[j]) {
                        return null
                    }
                }
            }
            return if (first.length() > second.length()) {
                Stamp(first, second, time)
            } else {
                Stamp(second, first, time)
            }
        }

        /**
         * Initialize the stamp mechanism of the system, called in Reasoner
         */
        @JvmStatic
        fun init() {
            currentSerial = 0
        }
    }
}