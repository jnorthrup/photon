/*
 * SyllogisticRules.java
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

import nars.data.SentenceStruct;
import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
public final class SyllogisticRules {

	/*
	 * --------------- rules used in both first-tense inference and higher-tense
	 * inference ---------------
	 */

	/**
	 * {<S ==> P>, <M <=> P>} |- <S ==> P>
	 * 
	 * @param term1
	 *            Subject of the new task
	 * @param term2
	 *            Predicate of the new task
	 * @param asym
	 *            The asymmetric premise
	 * @param sym
	 *            The symmetric premise
	 * @param figure
	 *            Locations of the shared term in premises
	 * @param memory
	 */
	static void analogy(Term term1, Term term2, Sentence asym,
			SentenceStruct sym, int figure, Memory memory) {
		if (Statement.invalidStatement(term1, term2)) {
			return;
		}
		Statement asymSt = (Statement) asym.getContent();
		// Statement symSt = (Statement) sym.getContent();
		TruthValue truth = null;
		BudgetValue budget;
		Sentence sentence = memory.getCurrentTask().getSentence();
		CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
		if (sentence.isQuestion()) {
			if (taskTerm.isCommutative()) {
				budget = BudgetFunctions.backwardWeak(memory, asym.getTruth());
			} else {
				budget = BudgetFunctions.backward(memory, sym.getTruth());
			}
		} else {
			truth = TruthFunctions.analogy(asym.getTruth(), sym.getTruth());
			budget = BudgetFunctions.forward(memory, truth);
		}
		Term content = Statement.make(asymSt, term1, term2, memory);
		Memory.doublePremiseTask(memory, content, truth, budget);
	}

	/**
	 * {<S <=> M>, <M <=> P>} |- <S <=> P>
	 * 
	 * @param term1
	 *            Subject of the new task
	 * @param term2
	 *            Predicate of the new task
	 * @param belief
	 *            The first premise
	 * @param sentence
	 *            The second premise
	 * @param figure
	 *            Locations of the shared term in premises
	 * @param memory
	 */
	static void resemblance(Term term1, Term term2, Sentence belief,
			SentenceStruct sentence, int figure, Memory memory) {
		if (Statement.invalidStatement(term1, term2)) {
			return;
		}
		Statement st1 = (Statement) belief.getContent();
		// Statement st2 = (Statement) sentence.getContent();
		TruthValue truth = null;
		BudgetValue budget;
		if (sentence.isQuestion()) {
			budget = BudgetFunctions.backward(memory, belief.getTruth());
		} else {
			truth = TruthFunctions.resemblance(belief.getTruth(),
					sentence.getTruth());
			budget = BudgetFunctions.forward(memory, truth);
		}
		Term statement = Statement.make(st1, term1, term2, memory);
		Memory.doublePremiseTask(memory, statement, truth, budget);
	}

	/* --------------- rules used only in conditional inference --------------- */

}
