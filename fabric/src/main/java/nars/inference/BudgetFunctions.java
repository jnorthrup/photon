/*
 * BudgetFunctions.java
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
package nars.inference;

import nars.data.BudgetStruct;
import nars.data.TruthHandle;
import nars.entity.*;
import nars.language.*;
import nars.storage.WorkSpace;

/**
 * Budget functions for resources allocation
 */
public final class BudgetFunctions extends UtilityFunctions {

	/* ----------------------- Belief evaluation ----------------------- */
	/**
	 * Determine the quality of a judgment by its truth value alone
	 * <p>
	 * Mainly decided by confidence, though binary judgment is also preferred
	 * 
	 * @param t
	 *            The truth value of a judgment
	 * @return The quality of the judgment, according to truth value only
	 */
	public static float truthToQuality(TruthHandle t) {
		float freq = t.getFrequency();
		float conf = t.getConfidence();
		return aveGeo(conf, Math.abs(freq - 0.5f) + freq * 0.5f);
	}

	/*
	 * ----- Functions used both in direct and indirect processing of tasks
	 * -----
	 */

	/**
	 * Update a belief
	 * 
	 * @param task
	 *            The task containing new belief
	 * @param bTruth
	 *            Truth value of the previous belief
	 * @return Budget value of the updating task
	 */
	static BudgetValue update(Task task, TruthValue bTruth) {
		TruthValue tTruth = task.getSentence().getTruth();
		float dif = tTruth.isExpDifAbs(bTruth);
		float priority = or(dif, task.getPriority());
		float durability = or(dif, task.getDurability());
		float quality = truthToQuality(bTruth);
		return new BudgetValue(priority, durability, quality);
	}

	/* ----------------------- Links ----------------------- */

	/* ----------------------- Concept ----------------------- */
	/**
	 * Activate a concept by an incoming TaskLink
	 * 
	 * @param concept
	 *            The concept
	 * @param budget
	 *            The budget for the new item
	 */
	public static void activate(Concept concept, BudgetStruct budget) {
		float oldPri = concept.getPriority();
		float priority = or(oldPri, budget.getPriority());
		float durability = aveAri(concept.getDurability(),
				budget.getDurability(), oldPri / priority);
		float quality = concept.getQuality();
		concept.setPriority(priority);
		concept.setDurability(durability);
		concept.setQuality(quality);
	}

	/* ---------------- Bag functions, on all Items ------------------- */
	/**
	 * Decrease Priority after an item is used, called in Bag
	 * <p>
	 * After a constant time, p should become d*p. Since in this period, the
	 * item is accessed c*p times, each time p-q should multiple d^(1/(c*p)).
	 * The intuitive meaning of the parameter "forgetRate" is: after this number
	 * of times of access, priority 1 will become d, it is a system parameter
	 * adjustable in run time.
	 * 
	 * @param budget
	 *            The previous budget value
	 * @param forgetRate
	 *            The budget for the new item
	 * @param relativeThreshold
	 *            The relative threshold of the bag
	 */
	public static void forget(BudgetStruct budget, float forgetRate,
			float relativeThreshold) {
		double quality = budget.getQuality() * relativeThreshold; // re-scaled
																	// quality
		double p = budget.getPriority() - quality; // priority above quality
		if (p > 0) {
			quality += p
					* Math.pow(budget.getDurability(), 1.0 / (forgetRate * p));
		} // priority Durability
		budget.setPriority((float) quality);
	}

	/**
	 * Merge an item into another one in a bag, when the two are identical
	 * except in budget values
	 * 
	 * @param baseValue
	 *            The budget value to be modified
	 * @param adjustValue
	 *            The budget doing the adjusting
	 */
	public static void merge(BudgetValue baseValue, BudgetStruct adjustValue) {
		baseValue.incPriority(adjustValue.getPriority());
		baseValue.setDurability(Math.max(baseValue.getDurability(),
				adjustValue.getDurability()));
		baseValue.setQuality(Math.max(baseValue.getQuality(),
				adjustValue.getQuality()));
	}

	/* ----- Task derivation in LocalRules and SyllogisticRules ----- */
	/**
	 * Forward inference result and adjustment
	 * 
	 * @param truth
	 *            The truth value of the conclusion
	 * @return The budget value of the conclusion
	 */
	static BudgetValue forward(WorkSpace memory, TruthHandle truth) {
		return budgetInference(truthToQuality(truth), 1, memory);
	}

	/**
	 * Backward inference result and adjustment, stronger case
	 * 
	 * @param memory
	 *            Reference to the memory
	 * @param truth
	 *            The truth value of the belief deriving the conclusion
	 * @return The budget value of the conclusion
	 */
	public static BudgetValue backward(WorkSpace memory, TruthHandle truth) {
		return budgetInference(truthToQuality(truth), 1, memory);
	}

	/**
	 * Backward inference result and adjustment, weaker case
	 * 
	 * @param memory
	 *            Reference to the memory
	 * @param truth
	 *            The truth value of the belief deriving the conclusion
	 * @return The budget value of the conclusion
	 */
	public static BudgetValue backwardWeak(WorkSpace memory, TruthHandle truth) {
		return budgetInference(w2c(1) * truthToQuality(truth), 1, memory);
	}

	/* ----- Task derivation in CompositionalRules and StructuralRules ----- */
	/**
	 * Forward inference with CompoundTerm conclusion
	 * 
	 * @param memory
	 *            Reference to the memory
	 * @param truth
	 *            The truth value of the conclusion
	 * @param content
	 *            The content of the conclusion
	 * @return The budget of the conclusion
	 */
	public static BudgetValue compoundForward(WorkSpace memory, TruthHandle truth, Term content) {
		return budgetInference(truthToQuality(truth), content.getComplexity(),
				memory);
	}

	/**
	 * Backward inference with CompoundTerm conclusion, stronger case
	 * 
	 * @param content
	 *            The content of the conclusion
	 * @param memory
	 *            Reference to the memory
	 * @return The budget of the conclusion
	 */
	public static BudgetValue compoundBackward(Term content, WorkSpace memory) {
		return budgetInference(1, content.getComplexity(), memory);
	}

	/**
	 * Backward inference with CompoundTerm conclusion, weaker case
	 * 
	 * @param content
	 *            The content of the conclusion
	 * @param memory
	 *            Reference to the memory
	 * @return The budget of the conclusion
	 */
	public static BudgetValue compoundBackwardWeak(Term content, WorkSpace memory) {
		return budgetInference(w2c(1), content.getComplexity(), memory);
	}

	/**
	 * Common processing for all inference step
	 * 
	 * @param qual
	 *            Quality of the inference
	 * @param complexity
	 *            Syntactic complexity of the conclusion
	 * @param memory
	 *            Reference to the memory
	 * @return Budget of the conclusion task
	 */
	private static BudgetValue budgetInference(float qual, int complexity,
			WorkSpace memory) {
		Item t = memory.getCurrentTaskLink();
		if (t == null)
			t = memory.getCurrentTask();
		float priority = t.getPriority();
		float durability = t.getDurability();
		float quality = (float) (qual / Math.sqrt(complexity));
		TermLink bLink = memory.getCurrentBeliefLink();
		if (bLink != null) {
			priority = aveAri(priority, bLink.getPriority());
			durability = aveAri(durability, bLink.getDurability());
			Item.incPriority(bLink.getBudget(), quality);
		}
		return new BudgetValue(and(priority, quality),
				and(durability, quality), quality);
	}

	/**
     * A function where the output is the arithmetic average the inputs
     * @param arr The inputs, each in [0, 1]
     * @return The arithmetic average the inputs
     */
    public static float aveAri(float... arr) {
        float sum = 0;
        for (float f : arr) {
            sum += f;
        }
        return sum / arr.length;
    }
}
