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

import nars.data.SentenceStruct;
import nars.data.StampStruct;
import nars.data.TermStruct;
import nars.data.TruthHandle;
import nars.io.Symbols;
import nars.language.Term;
import org.jetbrains.annotations.NotNull;

/**
 * A Sentence is an abstract class, mainly containing a Term, a TruthValue, and a Stamp.
 *<p>
 * It is used as the premises and conclusions of all inference rules.
 */
public class Sentence implements Cloneable, SentenceStruct {

    private Term content;
    private char punctuation;
    private TruthValue truth;
    private Stamp stamp;
    private boolean revisible;

    /**
     * Create a Sentence with the given fields
     * @param content The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth The truth value of the sentence, null for question
     * @param stamp The stamp of the sentence indicating its derivation time and base
     */
    public Sentence(TermStruct content, int punctuation, TruthValue truth, Stamp stamp) {
        setContent(content);
        getContent().renameVariables();
        setPunctuation(punctuation);
        setTruth(truth);
        setStamp(stamp);
        setRevisible(true);
    }

    /**
     * Create a Sentence with the given fields
     * @param content The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth The truth value of the sentence, null for question
     * @param stamp The stamp of the sentence indicating its derivation time and base
     * @param revisible Whether the sentence can be revised
     */
    public Sentence(TermStruct content, int punctuation, TruthValue truth, Stamp stamp, boolean revisible) {
        setContent(content);
        getContent().renameVariables();
        setPunctuation(punctuation);
        setTruth(truth);
        setStamp(stamp);
        setRevisible(revisible);
    }

    /**
     * To check whether two sentences are equal
     * @param that The other sentence
     * @return Whether the two sentences have the same content
     */
    @Override
    public boolean equals(Object that) {
        if (that instanceof Sentence) {
            SentenceStruct t = (Sentence) that;
            return getContent().equals(t.getContent()) && getPunctuation() == t.getPunctuation() && getTruth().equals(t.getTruth()) && getStamp().equals(t.getStamp());
        }
        return false;
    }

    /**
     * To produce the hashcode of a sentence
     * @return A hashcode
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (getContent() != null ? getContent().hashCode() : 0);
        hash = 67 * hash + getPunctuation();
        hash = 67 * hash + (getTruth() != null ? getTruth().hashCode() : 0);
        hash = 67 * hash + (getStamp() != null ? getStamp().hashCode() : 0);
        return hash;
    }

    /**
     * Check whether the judgment is equivalent to another one
     * <p>
     * The two may have different keys
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    public boolean equivalentTo(SentenceStruct that) {
        assert getContent().equals(that.getContent()) && getPunctuation() == that.getPunctuation();
        return getTruth().equals(that.getTruth()) && getStamp().equals(that.getStamp());
    }

    /**
     * Clone the Sentence
     * @return The clone
     */
    @Override
    public Object clone() {
        if (getTruth() == null) {
            return new Sentence((Term) getContent().clone(), getPunctuation(), null, (Stamp) getStamp().clone());
        }
        return new Sentence((Term) getContent().clone(), getPunctuation(), new TruthValue(getTruth()), (Stamp) getStamp().clone(), isRevisible());
    }

    /** The content of a Sentence is a Term */ /**
     * Get the content of the sentence
     * @return The content Term
     */
    @Override
    public Term getContent() {
        return content;
    }

    /** The punctuation also indicates the type of the Sentence: Judgment, Question, or Goal */ /**
     * Get the punctuation of the sentence
     * @return The character '.' or '?'
     */
    @Override
    public int getPunctuation() {
        return punctuation;
    }

    @Override
    public void setPunctuation(int $punctuation$) {
        punctuation = (char) ((char) $punctuation$&0xffff);
    }

    /**
     * Clone the content of the sentence
     * @return A clone of the content Term
     */
    public Term cloneContent() {
        return (Term) getContent().clone();
    }

    /**
     * Set the content Term of the Sentence
     * @param t The new content
     */
    @Override
    public void setContent(TermStruct t) {
        content = (Term) t;
    }

    /** The truth value of Judgment */ /**
     * Get the truth value of the sentence
     * @return Truth value, null for question
     */
    @Override
    public TruthValue getTruth() {
        return truth;
    }

    @Override
    public void setTruth(TruthHandle $truth$) {
        truth = (TruthValue) $truth$;

    }


    /** Partial record of the derivation path */ /**
     * Get the stamp of the sentence
     * @return The stamp
     */
    @Override
    public Stamp getStamp() {
        return stamp;
    }

    @Override
    public void setStamp(StampStruct $stamp$) {

    }

    /**
     * Distinguish Judgment from Goal ("instanceof Judgment" doesn't work)
     * @return Whether the object is a Judgment
     */

    public boolean isJudgment() {
        return getPunctuation() == Symbols.JUDGMENT_MARK;
    }

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     * @return Whether the object is a Question
     */

    public boolean isQuestion() {
        return getPunctuation() == Symbols.QUESTION_MARK;
    }

    public static boolean containQueryVar(SentenceStruct sentence) {
        return 0 <= sentence.getContent().getName().indexOf(Symbols.VAR_QUERY);
    }


    @Override
    public void setQuestion(boolean $question$) {

    }

     public boolean getRevisible() {
        return isRevisible();
    }

    @Override
    public void setRevisible(boolean b) {
        revisible = b;
    }

    /**
     * Get a String representation of the sentence
     * @return The String
     */
    @Override
    public String toString() {
        return asString(this);
    }

    @NotNull
    private static String asString(Sentence sentence) {
        StringBuffer s = new StringBuffer();
        s.append(sentence.getContent());
        s.append(sentence.getPunctuation() + " ");
        if (sentence.getTruth() != null) {
            s.append(sentence.getTruth());
        }
        s.append(sentence.getStamp());
        return s.toString();
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     * @return The String
     * @param sentence
     */
    public static String toStringBrief(Sentence sentence) {
        return Sentence.toKey(sentence) + sentence.getStamp();
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     * @return The String
     * @param sentence
     */
    public static String toKey(Sentence sentence) {
        StringBuffer s = new StringBuffer();
        s.append(sentence.getContent());
        s.append(sentence.getPunctuation() + " ");
        if (sentence.getTruth() != null) {
            s.append(sentence.getTruth().toStringBrief());
        }
        return s.toString();
    }


    public void setPunctuation(char punctuation) {
        this.punctuation = punctuation;
    }


    public void setTruth(TruthValue truth) {
        this.truth = truth;
    }


    public void setStamp(Stamp stamp) {
        this.stamp = stamp;
    }

    /** Whether the sentence can be revised */

    public boolean isRevisible() {
        return revisible;
    }
}
