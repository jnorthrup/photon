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
package nars.entity;

import nars.io.Symbols;
import nars.language.Term;

/**
 * A Sentence is an abstract class, mainly containing a Term, a TruthValue, and
 * a Stamp.
 * <p>
 * It is used as the premises and conclusions of all inference rules.
 */
public class SentenceHandle implements Cloneable, Sentence {

    /**
     * The content of a Sentence is a Term
     */
    public Term content;
    /**
     * The punctuation also indicates the type of the Sentence: Judgment,
     * Question, or Goal
     */
    public char punctuation;
    /**
     * The truth value of Judgment
     */
    public TruthValueRefier truth;
    /**
     * Partial record of the derivation path
     */
    public StampHandle stamp;
    /**
     * Whether the sentence can be revised
     */
    public boolean revisible;

    /**
     * Create a Sentence with the given fields
     *
     * @param content The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth The truth value of the sentence, null for question
     * @param stamp The stamp of the sentence indicating its derivation time and
     * base
     */
    public SentenceHandle(Term content, char punctuation, TruthValueRefier truth, StampHandle stamp) {
        this.content = content;
        this.content.renameVariables();
        this.punctuation = punctuation;
        this.truth = truth;
        this.stamp = stamp;
        this.revisible = true;
    }

    /**
     * Create a Sentence with the given fields
     *
     * @param content The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth The truth value of the sentence, null for question
     * @param stamp The stamp of the sentence indicating its derivation time and
     * base
     * @param revisible Whether the sentence can be revised
     */
    public SentenceHandle(Term content, char punctuation, TruthValueRefier truth, StampHandle stamp, boolean revisible) {
        this.content = content;
        this.content.renameVariables();
        this.punctuation = punctuation;
        this.truth = truth;
        this.stamp = stamp;
        this.revisible = revisible;
    }

    /**
     * To check whether two sentences are equal
     *
     * @param that The other sentence
     * @return Whether the two sentences have the same content
     */
    @Override
    public boolean equals(Object that) {
        if (that instanceof SentenceHandle) {
            Sentence t = (Sentence) that;
            return content.equals(t.getContent()) && punctuation == t.getPunctuation() && truth.equals(t.getTruth()) && stamp.equals(t.getStamp());
        }
        return false;
    }

    /**
     * To produce the hashcode of a sentence
     *
     * @return A hashcode
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.content != null ? this.content.hashCode() : 0);
        hash = 67 * hash + this.punctuation;
        hash = 67 * hash + (this.truth != null ? this.truth.hashCode() : 0);
        hash = 67 * hash + (this.stamp != null ? this.stamp.hashCode() : 0);
        return hash;
    }

    /**
     * Check whether the judgment is equivalent to another one
     * <p>
     * The two may have different keys
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    public boolean equivalentTo(Sentence that) {
        assert content.equals(that.getContent()) && punctuation == that.getPunctuation();
        return (truth.equals(that.getTruth()) && stamp.equals(that.getStamp()));
    }

    /**
     * Clone the Sentence
     *
     * @return The clone
     */
    @Override
    public Object clone() {
        if (truth == null) {
            return new SentenceHandle((Term) content.clone(), punctuation, null, (StampHandle) stamp.clone());
        }
        return new SentenceHandle((Term) content.clone(), punctuation, new TruthValueRefier(truth), (StampHandle) stamp.clone(), revisible);
    }

    /**
     * Get the content of the sentence
     *
     * @return The content Term
     */
    @Override
    public Term getContent() {
        return content;
    }

    /**
     * Get the punctuation of the sentence
     *
     * @return The character '.' or '?'
     */
    @Override
    public char getPunctuation() {
        return punctuation;
    }

    /**
     * Clone the content of the sentence
     *
     * @return A clone of the content Term
     */
    public Term cloneContent() {
        return (Term) content.clone();
    }

    /**
     * Set the content Term of the Sentence
     *
     * @param t The new content
     */
    @Override
    public void setContent(Term t) {
        content = t;
    }

    /**
     * Get the truth value of the sentence
     *
     * @return Truth value, null for question
     */
    @Override
    public TruthValueRefier getTruth() {
        return truth;
    }

    /**
     * Get the stamp of the sentence
     *
     * @return The stamp
     */
    @Override
    public StampHandle getStamp() {
        return stamp;
    }

    /**
     * Distinguish Judgment from Goal ("instanceof Judgment" doesn't work)
     *
     * @return Whether the object is a Judgment
     */
    @Override
    public boolean isJudgment() {
        return (punctuation == Symbols.JUDGMENT_MARK);
    }

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     *
     * @return Whether the object is a Question
     */
    @Override
    public boolean isQuestion() {
        return (punctuation == Symbols.QUESTION_MARK);
    }

    public boolean containQueryVar() {
        return (content.getName().indexOf(Symbols.VAR_QUERY) >= 0);
    }

    @Override
    public boolean getRevisible() {
        return revisible;
    }

    @Override
    public void setRevisible(boolean b) {
        revisible = b;
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(content.toString());
        s.append(punctuation).append(" ");
        if (truth != null) {
            s.append(truth.toString());
        }
        s.append(stamp.toString());
        return s.toString();
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    public String toStringBrief() {
        return toKey() + stamp.toString();
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    public String toKey() {
        StringBuilder s = new StringBuilder();
        s.append(content.toString());
        s.append(punctuation).append(" ");
        if (truth != null) {
            s.append(truth.toStringBrief());
        }
        return s.toString();
    }
}
