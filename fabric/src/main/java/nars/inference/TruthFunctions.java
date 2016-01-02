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

import nars.data.TruthHandle;
import nars.entity.*;
import nars.storage.Parameters;

/**
 * All truth-value (and desire-value) functions used in inference rules
 */
public final class TruthFunctions extends UtilityFunctions {

	/* ----- Single argument functions, called in MatchingRules ----- */
	/**
	 * {<A ==> B>} |- <B ==> A>
	 * 
	 * @param v1
	 *            Truth value of the premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue conversion(TruthHandle v1) {
		float f1 = v1.getFrequency();
		float c1 = v1.getConfidence();
		float w = and(f1, c1);
		float c = w2c(w);
		return new TruthValue(1, c);
	}

	/* ----- Single argument functions, called in StructuralRules ----- */
	/**
	 * {A} |- (--A)
	 * 
	 * @param v1
	 *            Truth value of the premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue negation(TruthHandle v1) {
		float f = 1 - v1.getFrequency();
		float c = v1.getConfidence();
		return new TruthValue(f, c);
	}

	/* ----- double argument functions, called in MatchingRules ----- */
	/**
	 * {<S ==> P>, <S ==> P>} |- <S ==> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue revision(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float w1 = c2w(c1);
		float w2 = c2w(c2);
		float w = w1 + w2;
		float f = (w1 * f1 + w2 * f2) / w;
		float c = w2c(w);
		return new TruthValue(f, c);
	}

	/* ----- double argument functions, called in SyllogisticRules ----- */
	/**
	 * {<S ==> M>, <M ==> P>} |- <S ==> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue deduction(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2, f);
		return new TruthValue(f, c);
	}

	/**
	 * {M, <M ==> P>} |- P
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param reliance
	 *            Confidence of the second (analytical) premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue deduction(TruthHandle v1, float reliance) {
		float f1 = v1.getFrequency();
		float c1 = v1.getConfidence();
		float c = and(f1, c1, reliance);
		return new TruthValue(f1, c);
	}

	/**
	 * {<S ==> M>, <M <=> P>} |- <S ==> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue analogy(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2, f2);
		return new TruthValue(f, c);
	}

	/**
	 * {<S <=> M>, <M <=> P>} |- <S <=> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue resemblance(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2, or(f1, f2));
		return new TruthValue(f, c);
	}

	/**
	 * {<S ==> M>,
	 * <P ==>
	 * M>} |- <S ==> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue abduction(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float w = and(f2, c1, c2);
		float c = w2c(w);
		return new TruthValue(f1, c);
	}

	/**
	 * {M,
	 * <P ==>
	 * M>} |- P
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param reliance
	 *            Confidence of the second (analytical) premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue abduction(TruthHandle v1, float reliance) {
		float f1 = v1.getFrequency();
		float c1 = v1.getConfidence();
		float w = and(c1, reliance);
		float c = w2c(w);
		return new TruthValue(f1, c);
	}

	/**
	 * {<M ==> S>, <M ==> P>} |- <S ==> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue induction(TruthHandle v1, TruthHandle v2) {
		return abduction(v2, v1);
	}

	/**
	 * {<M ==> S>, <M ==> P>} |- <S <=> P>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue comparison(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f0 = or(f1, f2);
		float f = (f0 == 0) ? 0 : (and(f1, f2) / f0);
		float w = and(f0, c1, c2);
		float c = w2c(w);
		return new TruthValue(f, c);
	}

	/* ----- desire-value functions, called in SyllogisticRules ----- */
	/**
	 * A function specially designed for desire value [To be refined]
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue desireStrong(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2, f2);
		return new TruthValue(f, c);
	}

	/**
	 * A function specially designed for desire value [To be refined]
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue desireWeak(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2, f2, w2c(1.0f));
		return new TruthValue(f, c);
	}

	/**
	 * A function specially designed for desire value [To be refined]
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue desireDed(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2);
		return new TruthValue(f, c);
	}

	/**
	 * A function specially designed for desire value [To be refined]
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue desireInd(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float w = and(f2, c1, c2);
		float c = w2c(w);
		return new TruthValue(f1, c);
	}

	/* ----- double argument functions, called in CompositionalRules ----- */
	/**
	 * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue union(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = or(f1, f2);
		// float c = or(and(f1, c1), and(f2, c2)) + and(1 - f1, 1 - f2, c1, c2);
		float c = and(c1, c2);
		return new TruthValue(f, c);
	}

	/**
	 * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue intersection(TruthHandle v1, TruthHandle v2) {
		float f1 = v1.getFrequency();
		float f2 = v2.getFrequency();
		float c1 = v1.getConfidence();
		float c2 = v2.getConfidence();
		float f = and(f1, f2);
		float c = and(c1, c2);
		return new TruthValue(f, c);
	}

	/**
	 * {(--, (&&, A, B)), B} |- (--, A)
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue reduceConjunction(TruthHandle v1, TruthHandle v2) {
		TruthValue v0 = intersection(negation(v1), v2);
		return negation(deduction(v0, 1f));
	}

	/**
	 * A function to convert confidence to weight
	 * 
	 * @param c
	 *            confidence, in [0, 1)
	 * @return The corresponding weight of evidence, a non-negative real number
	 */
	public static float c2w(float c) {
		return Parameters.HORIZON * c / (1 - c);
	}
}
