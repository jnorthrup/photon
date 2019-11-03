/*
 * Sentence.java
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

import nars.io.Symbols
import nars.language.Term
import java.util.*

/**
 * A Sentence is an abstract class, mainly containing a Term, a TruthValue, and
 * a Stamp.
 *
 *
 * It is used as the premises and conclusions of all inference rules.
 */
class Sentence : Cloneable {
    /**
     * Get the content of the sentence
     *
     * @return The content Term
     */
    /**
     * Set the content Term of the Sentence
     *
     * @param t The new content
     */
    /**
     * The content of a Sentence is a Term
     */
    var content: Term
    /**
     * Get the punctuation of the sentence
     *
     * @return The character '.' or '?'
     */
    /**
     * The punctuation also indicates the type of the Sentence: Judgment,
     * Question, or Goal
     */
    var punctuation: Char
        private set
    /**
     * Get the truth value of the sentence
     *
     * @return Truth value, null for question
     */
    /**
     * The truth value of Judgment
     */
    var truth: TruthValue?
        private set
    /**
     * Get the stamp of the sentence
     *
     * @return The stamp
     */
    /**
     * Partial record of the derivation path
     */
    var stamp: Stamp
        private set
    /**
     * Whether the sentence can be revised
     */
    var revisible: Boolean

    /**
     * Create a Sentence with the given fields
     *
     * @param content     The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth       The truth value of the sentence, null for question
     * @param stamp       The stamp of the sentence indicating its derivation time and
     * base
     */
    constructor(content: Term, punctuation: Char, truth: TruthValue?, stamp: Stamp) {
        this.content = content
        this.content.renameVariables()
        this.punctuation = punctuation
        this.truth = truth
        this.stamp = stamp
        revisible = true
    }

    /**
     * Create a Sentence with the given fields
     *
     * @param content     The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth       The truth value of the sentence, null for question
     * @param stamp       The stamp of the sentence indicating its derivation time and
     * base
     * @param revisible   Whether the sentence can be revised
     */
    constructor(content: Term, punctuation: Char, truth: TruthValue, stamp: Stamp, revisible: Boolean) {
        this.content = content
        this.content.renameVariables()
        this.punctuation = punctuation
        this.truth = truth
        this.stamp = stamp
        this.revisible = revisible
    }

    /**
     * To check whether two sentences are equal
     *
     * @param that The other sentence
     * @return Whether the two sentences have the same content
     */

    override fun equals(that: Any?): Boolean {
        if (that is Sentence) {
            val t = that
            return content == t.content && punctuation == t.punctuation && truth == t.truth && stamp == t.stamp
        }
        return false
    }

    /**
     * To produce the hashcode of a sentence
     *
     * @return A hashcode
     */

    override fun hashCode(): Int {
        var hash = 5
        hash = 67 * hash + Optional.ofNullable(content).map { obj: Term -> obj.hashCode() }.orElse(0)
        hash = 67 * hash + punctuation.toInt()
        hash = 67 * hash + Optional.ofNullable(truth).map { obj: TruthValue -> obj.hashCode() }.orElse(0)
        hash = 67 * hash + Optional.ofNullable(stamp).map { obj: Stamp -> obj.hashCode() }.orElse(0)
        return hash
    }

    /**
     * Check whether the judgment is equivalent to another one
     *
     *
     * The two may have different keys
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    fun equivalentTo(that: Sentence): Boolean {
        assert(content == that.content && punctuation == that.punctuation)
        return truth == that.truth && stamp == that.stamp
    }

    /**
     * Clone the Sentence
     *
     * @return The clone
     */

    public override fun clone(): Any {
        return if (truth == null) {
            Sentence(content.clone() as Term, punctuation, null, stamp.clone() as Stamp)
        } else Sentence(content.clone() as Term, punctuation, TruthValue(truth!!), stamp.clone() as Stamp, revisible)
    }

    /**
     * Clone the content of the sentence
     *
     * @return A clone of the content Term
     */
    fun cloneContent(): Term {
        return content.clone() as Term
    }

    /**
     * Distinguish Judgment from Goal ("instanceof Judgment" doesn't work)
     *
     * @return Whether the object is a Judgment
     */
    val isJudgment: Boolean
        get() = punctuation == Symbols.JUDGMENT_MARK

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     *
     * @return Whether the object is a Question
     */
    val isQuestion: Boolean
        get() = punctuation == Symbols.QUESTION_MARK

    fun containQueryVar(): Boolean {
        return content.name.indexOf(Symbols.VAR_QUERY) >= 0
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */

    override fun toString(): String {
        val s = StringBuilder()
        s.append(content.toString())
        s.append(punctuation).append(" ")
        if (truth != null) {
            s.append(truth.toString())
        }
        s.append(stamp.toString())
        return s.toString()
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    fun toStringBrief(): String {
        return toKey() + stamp.toString()
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    fun toKey(): String {
        val s = StringBuilder()
        s.append(content.toString())
        s.append(punctuation).append(" ")
        if (truth != null) {
            s.append(truth?.toStringBrief())
        }
        return s.toString()
    }
}