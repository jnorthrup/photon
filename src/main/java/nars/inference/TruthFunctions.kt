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
package nars.inference

import nars.entity.TruthValue

/**
 * All truth-value (and desire-value) functions used in inference rules
 */
object TruthFunctions : UtilityFunctions() {/* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A></A>  B>} |- <B></B>  A>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    internal fun conversion(v1: TruthValue): TruthValue {
        val f1 = v1.frequency
        val c1 = v1.confidence
        val w = and(f1, c1)
        val c = w2c(w)
        return TruthValue(1f, c)
    }

    /* ----- Single argument functions, called in StructuralRules ----- */


    /**
     * {A} |- (--A)
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    internal fun negation(v1: TruthValue): TruthValue {
        val f = 1 - v1.frequency
        val c = v1.confidence
        return TruthValue(f, c)
    }

    /**
     * {<A></A>  B>} |- <(--, B) ==> (--, A)>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    internal fun contraposition(v1: TruthValue): TruthValue {
        val f1 = v1.frequency
        val c1 = v1.confidence
        val w = and(1 - f1, c1)
        val c = w2c(w)
        return TruthValue(0f, c)
    }

    /* ----- double argument functions, called in MatchingRules ----- */


    /**
     * {<S></S>  P>, <S></S>  P>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun revision(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val w1 = c2w(c1)
        val w2 = c2w(c2)
        val w = w1 + w2
        val f = (w1 * f1 + w2 * f2) / w
        val c = w2c(w)
        return TruthValue(f, c)
    }

    /* ----- double argument functions, called in SyllogisticRules ----- */


    /**
     * {<S></S>  M>, <M></M>  P>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun deduction(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val f = and(f1, f2)
        val c = and(c1, c2, f)
        return TruthValue(f, c)
    }

    /**
     * {M, <M></M>  P>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    internal fun deduction(v1: TruthValue, reliance: Float): TruthValue {
        val f1 = v1.frequency
        val c1 = v1.confidence
        val c = and(f1, c1, reliance)
        return TruthValue(f1, c, true)
    }

    /**
     * {<S></S>  M>, <M></M> <=> P>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun analogy(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val f = and(f1, f2)
        val c = and(c1, c2, f2)
        return TruthValue(f, c)
    }

    /**
     * {<S></S> <=> M>, <M></M> <=> P>} |- <S></S> <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun resemblance(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val f = and(f1, f2)
        val c = and(c1, c2, or(f1, f2))
        return TruthValue(f, c)
    }

    /**
     * {<S></S>  M>, <P></P>  M>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun abduction(v1: TruthValue, v2: TruthValue): TruthValue {
        if (v1.analytic || v2.analytic) {
            return TruthValue(0.5f, 0f)
        }
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val w = and(f2, c1, c2)
        val c = w2c(w)
        return TruthValue(f1, c)
    }

    /**
     * {M, <P></P>  M>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    internal fun abduction(v1: TruthValue, reliance: Float): TruthValue {
        if (v1.analytic) {
            return TruthValue(0.5f, 0f)
        }
        val f1 = v1.frequency
        val c1 = v1.confidence
        val w = and(c1, reliance)
        val c = w2c(w)
        return TruthValue(f1, c, true)
    }

    /**
     * {<M></M>  S>, <M></M>  P>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun induction(v1: TruthValue, v2: TruthValue): TruthValue {
        return abduction(v2, v1)
    }

    /**
     * {<M></M>  S>, <P></P>  M>} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun exemplification(v1: TruthValue, v2: TruthValue): TruthValue {
        if (v1.analytic || v2.analytic) {
            return TruthValue(0.5f, 0f)
        }
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val w = and(f1, f2, c1, c2)
        val c = w2c(w)
        return TruthValue(1f, c)
    }

    /**
     * {<M></M>  S>, <M></M>  P>} |- <S></S> <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun comparison(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val f0 = or(f1, f2)
        val f: Float = if (f0 == 0f) 0f else and(f1, f2) / f0
        val w = and(f0, c1, c2)
        val c = w2c(w)
        return TruthValue(f, c)
    }

    /* ----- desire-value functions, called in SyllogisticRules ----- */

    /* ----- double argument functions, called in CompositionalRules ----- */


    /**
     * {<M --> S>, <M></M><-> P>} |- <M --> (S|P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
    </M></M> */
    internal fun union(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val f2 = v2.frequency
        val c1 = v1.confidence
        val c2 = v2.confidence
        val f = or(f1, f2)
        val c = and(c1, c2)
        return TruthValue(f, c)
    }

    /**
     * {<M --> S>, <M></M><-> P>} |- <M --> (S&P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
    </M></M> */
    internal fun intersection(v1: TruthValue, v2: TruthValue): TruthValue = TruthValue(and(v1.frequency, v2.frequency), and(v1.confidence, v2.confidence))

    /**
     * {(||, A, B), (--, B)} |- A
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun reduceDisjunction(v1: TruthValue, v2: TruthValue): TruthValue {
        val v0 = intersection(v1, negation(v2))
        return deduction(v0, 1f)
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun reduceConjunction(v1: TruthValue, v2: TruthValue): TruthValue {
        val v0 = intersection(negation(v1), v2)
        return negation(deduction(v0, 1f))
    }

    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun reduceConjunctionNeg(v1: TruthValue, v2: TruthValue): TruthValue {
        return reduceConjunction(v1, negation(v2))
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S></S>  P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    internal fun anonymousAnalogy(v1: TruthValue, v2: TruthValue): TruthValue {
        val f1 = v1.frequency
        val c1 = v1.confidence
        val v0 = TruthValue(f1, w2c(c1))
        return analogy(v2, v0)
    }
}