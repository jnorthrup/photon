/*
 * TruthFunctions.java
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
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.inference;

import nars.entity.TruthValue;

/**
 * All truth-value (and desire-value) functions used in inference rules
 */
public final class TruthFunctions extends UtilityFunctions {

    /* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue conversion(TruthValue v1) {
        var f1 = v1.getFrequency();
        var c1 = v1.getConfidence();
        var w = and(f1, c1);
        var c = w2c(w);
        return new TruthValue(1, c);
    }

    /* ----- Single argument functions, called in StructuralRules ----- */

    /**
     * {A} |- (--A)
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue negation(TruthValue v1) {
        var f = 1 - v1.getFrequency();
        var c = v1.getConfidence();
        return new TruthValue(f, c);
    }

    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue contraposition(TruthValue v1) {
        var f1 = v1.getFrequency();
        var c1 = v1.getConfidence();
        var w = and(1 - f1, c1);
        var c = w2c(w);
        return new TruthValue(0, c);
    }

    /* ----- double argument functions, called in MatchingRules ----- */

    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue revision(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var w1 = c2w(c1);
        var w2 = c2w(c2);
        var w = w1 + w2;
        var f = (w1 * f1 + w2 * f2) / w;
        var c = w2c(w);
        return new TruthValue(f, c);
    }

    /* ----- double argument functions, called in SyllogisticRules ----- */

    /**
     * {<S ==> M>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue deduction(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2, f);
        return new TruthValue(f, c);
    }

    /**
     * {M, <M ==> P>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    static TruthValue deduction(TruthValue v1, float reliance) {
        var f1 = v1.getFrequency();
        var c1 = v1.getConfidence();
        var c = and(f1, c1, reliance);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue analogy(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue resemblance(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2, or(f1, f2));
        return new TruthValue(f, c);
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue abduction(TruthValue v1, TruthValue v2) {
        if (v1.getAnalytic() || v2.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var w = and(f2, c1, c2);
        var c = w2c(w);
        return new TruthValue(f1, c);
    }

    /**
     * {M, <P ==> M>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    static TruthValue abduction(TruthValue v1, float reliance) {
        if (v1.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        var f1 = v1.getFrequency();
        var c1 = v1.getConfidence();
        var w = and(c1, reliance);
        var c = w2c(w);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue induction(TruthValue v1, TruthValue v2) {
        return abduction(v2, v1);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue exemplification(TruthValue v1, TruthValue v2) {
        if (v1.getAnalytic() || v2.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var w = and(f1, f2, c1, c2);
        var c = w2c(w);
        return new TruthValue(1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue comparison(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f0 = or(f1, f2);
        var f = (f0 == 0) ? 0 : (and(f1, f2) / f0);
        var w = and(f0, c1, c2);
        var c = w2c(w);
        return new TruthValue(f, c);
    }

    /* ----- desire-value functions, called in SyllogisticRules ----- */

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireStrong(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireWeak(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2, f2, w2c(1.0f));
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireDed(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireInd(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var w = and(f2, c1, c2);
        var c = w2c(w);
        return new TruthValue(f1, c);
    }

    /* ----- double argument functions, called in CompositionalRules ----- */

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue union(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = or(f1, f2);
        var c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue intersection(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var f2 = v2.getFrequency();
        var c1 = v1.getConfidence();
        var c2 = v2.getConfidence();
        var f = and(f1, f2);
        var c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {(||, A, B), (--, B)} |- A
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceDisjunction(TruthValue v1, TruthValue v2) {
        var v0 = intersection(v1, negation(v2));
        return deduction(v0, 1f);
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceConjunction(TruthValue v1, TruthValue v2) {
        var v0 = intersection(negation(v1), v2);
        return negation(deduction(v0, 1f));
    }

    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceConjunctionNeg(TruthValue v1, TruthValue v2) {
        return reduceConjunction(v1, negation(v2));
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue anonymousAnalogy(TruthValue v1, TruthValue v2) {
        var f1 = v1.getFrequency();
        var c1 = v1.getConfidence();
        var v0 = new TruthValue(f1, w2c(c1));
        return analogy(v2, v0);
    }
}
