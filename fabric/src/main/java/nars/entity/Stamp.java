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
package nars.entity;

import nars.data.StampStruct;
import nars.io.Symbols;
import nars.storage.Parameters;
import nars.storage.ReasonerBatch;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;

/**
 * Each Sentence has a time stamp, consisting the following components:
 * (1) The creation time of the sentence,
 * (2) A evidentialBase of serial numbers of sentence, from which the sentence is derived.
 * Each input sentence gets a unique serial number, though the creation time may be not unique.
 * The derived sentences inherits serial numbers from its parents, cut at the baseLength limit.
 */
public class Stamp implements Cloneable, StampStruct {

    private static long currentSerial;
    private final int baseLength  ;
    private List<Long> evidentialBase=new CopyOnWriteArrayList<>();
    private long creationTime;

    /**
     * Generate a new stamp, with a new serial number, for a new Task
     *
     * @param time Creation time of the stamp
     */
    private Stamp(long time) {
        setCurrentSerial(getCurrentSerial() + 1);
        baseLength = 1;

        getEvidentialBase().add(0, getCurrentSerial());
        creationTime = time;
    }

    /**
     * Generate a new stamp identical with a given one
     *
     * @param old The stamp to be cloned
     */
    private Stamp(Stamp old) {
        baseLength = old.getBaseLength();
        setEvidentialBase(old.getBase());
        creationTime = old.getCreationTime();
    }

    /**
     * Generate a new stamp from an existing one, with the same evidentialBase but different creation time
     * <p>
     * For single-premise rules
     *
     * @param old  The stamp of the single premise
     * @param time The current time
     */
    private Stamp(Stamp old, long time) {
        baseLength = old.getBaseLength();
        setEvidentialBase(old.getBase());
        creationTime = time;
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * the first one is no shorter than the second
     *
     * @param first  The first Stamp
     * @param second The second Stamp
     */
    private Stamp(Stamp first, Stamp second, long time) {
        int i1, i2, j;
        i1 = i2 = j = 0;
        baseLength = Math.min(first.getBaseLength() + second.getBaseLength(), Parameters.MAXIMUM_STAMP_LENGTH);
        getEvidentialBase().clear();
        while (i2 < second.getBaseLength() && j < getBaseLength()) {
            getEvidentialBase().add( first.get(i1));
            i1++;
            j++;
            getEvidentialBase().add( second.get(i2));
            i2++;
            j++;
        }
        while (i1 < first.getBaseLength() && j < getBaseLength()) {
            getEvidentialBase().add(first.get(i1));
            i1++;
            j++;
        }
        creationTime = time;
    }

    /**
     * Try to merge two Stamps, return null if have overlap
     * <p>
     * By default, the event time of the first stamp is used in the result
     *
     * @param first  The first Stamp
     * @param second The second Stamp
     * @param time   The new creation time
     * @return The merged Stamp, or null
     */
    public static Stamp make(Stamp first, Stamp second, long time) {
        Stamp r = null;
        e:
        {
            for (int i = 0; i < first.getBaseLength(); i++) {
                for (int j = 0; j < second.getBaseLength(); j++) {
                    if (first.get(i) == second.get(j)) {
                        break e;
                    }
                }
            }

            r = first.getBaseLength() > second.getBaseLength() ? new Stamp(first, second, time) : new Stamp(second, first, time);
        }


        return r;
    }

    /**
     * Initialize the stamp mechanism of the system, called in Reasoner
     */
    public static void init() {
        setCurrentSerial(0);
    }

    public static Stamp createStamp(long time) {
        return new Stamp(time);
    }

    public static Stamp createStamp(Stamp old, long time) {
        return new Stamp(old, time);
    }

    /**
     * serial number, for the whole system
     * TODO : should it really be static?
     * or a Stamp be a field in {@link ReasonerBatch} ?
     */
    public static long getCurrentSerial() {
        return currentSerial;
    }

    public static void setCurrentSerial(long currentSerial) {
        Stamp.currentSerial = currentSerial;
    }

    /**
     * Clone a stamp
     *
     * @return The cloned stamp
     */
    @Override
    public Object clone() {
        return new Stamp(this);
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     *
     * @param i The index
     * @return The number at the index
     */
    long get(int i) {
        return getEvidentialBase().get(i);
    }

    /**
     * Get the evidentialBase, called in this class only
     *
     * @return The evidentialBase of numbers
     */
    private List<Long> getBase() {
        return getEvidentialBase();
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private Collection<Long> toSet() {
        Collection<Long> set = new TreeSet<Long>();
        for (int i = 0; i < getBaseLength(); i++) {
            set.add(getEvidentialBase().get(i));
        }
        return set;
    }

    /**
     * Check if two stamps contains the same content
     *
     * @param that The Stamp to be compared
     * @return Whether the two have contain the same elements
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof StampStruct)) {
            return false;
        }
        Collection<Long> set1 = toSet();
        Collection<Long> set2 = ((Stamp) that).toSet();
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    /**
     * The hash code of Stamp
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * creation time of the stamp
     */ /**
     * Get the creationTime of the truth-value
     *
     * @return The creation time
     */
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(" " + Symbols.STAMP_OPENER + getCreationTime());
        buffer.append(" " + Symbols.STAMP_STARTER + " ");
        for (int i = 0; i < getBaseLength(); i++) {
            buffer.append(Long.toString(getEvidentialBase().get(i)));
            if (i < getBaseLength() - 1) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            } else {
                buffer.append(Symbols.STAMP_CLOSER + " ");
            }
        }
        return buffer.toString();
    }

    /**
     * evidentialBase baseLength
     */
     public int getBaseLength() {
        return baseLength;
    }

    /**
     * serial numbers
     */

    public List<Long> getEvidentialBase() {
        return evidentialBase;
    }

     public void setEvidentialBase(List<Long> evidentialBase) {
        this.evidentialBase = evidentialBase;
    }

    /**
     * temporary
     */
    class LongArrayList extends ArrayList<Long> {

        @Override
        public void ensureCapacity(int minCapacity) {

            clear();
            LongStream.of(0).limit(minCapacity+1).forEach(this::add);

        }

        LongArrayList(int baseLength) {
            super(baseLength);
            ensureCapacity(baseLength);
        }
    }
}
