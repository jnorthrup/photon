/*
 * LocalRules.java
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
import nars.data.TruthHandle;
import nars.storage.Memory;
import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import nars.storage.WorkSpace;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
public class LocalRules {

	/* -------------------- same contents -------------------- */
	/**
	 * The task and belief have the same content
	 * <p>
	 * called in RuleTables.reason
	 * 
	 * @param task
	 *            The task
	 * @param belief
	 *            The belief
	 * @param memory
	 *            Reference to the memory
	 */
	public static void match(Task task, Sentence belief, Memory memory) {
		Sentence sentence = (Sentence) task.getSentence().clone();
		if (sentence.isJudgment()) {
			if (revisible(sentence, belief)) {
				revision(sentence, belief, true, memory);
			}
		} else if (Variable.unify(Symbols.VAR_QUERY, sentence.getContent(),
				(Term) belief.getContent().clone())) {
			// trySolution(sentence, belief, task, memory);
			trySolution(belief, task, memory);
		}
	}

	/**
	 * Check whether two sentences can be used in revision
	 * 
	 * @param s1
	 *            The first sentence
	 * @param s2
	 *            The second sentence
	 * @return If revision is possible between the two sentences
	 */
	public static boolean revisible(Sentence s1, SentenceStruct s2) {
		return (s1.getContent().equals(s2.getContent()) && s1.getRevisible());
	}

	/**
	 * Belief revision
	 * <p>
	 * called from Concept.reviseTable and match
	 * 
	 * @param newBelief
	 *            The new belief in task
	 * @param oldBelief
	 *            The previous belief with the same content
	 * @param feedbackToLinks
	 *            Whether to send feedback to the links
	 * @param memory
	 */
	public static void revision(SentenceStruct newBelief,
			SentenceStruct oldBelief, boolean feedbackToLinks, Memory memory) {
		TruthValue newTruth = (TruthValue) newBelief.getTruth();
		TruthValue oldTruth = (TruthValue) oldBelief.getTruth();
		TruthValue truth = TruthFunctions.revision(newTruth, oldTruth);
		BudgetValue budget = revise(newTruth, oldTruth, truth, feedbackToLinks,
				memory);
		Term content = (Term) newBelief.getContent();
		Memory.doublePremiseTask(memory, content, truth, budget);
	}

	/**
	 * Check if a Sentence provide a better answer to a Question or Goal
	 * 
	 * @param belief
	 *            The proposed answer
	 * @param task
	 *            The task to be processed
	 * @param memory
	 *            Reference to the memory
	 */
	// public static void trySolution(Sentence problem, Sentence belief, Task
	// task, Memory memory) {
	public static void trySolution(Sentence belief, Task task, Memory memory) {
		Sentence problem = task.getSentence();
		Sentence oldBest = task.getBestSolution();
		float newQ = solutionQuality(problem, belief);
		if (oldBest != null) {
			float oldQ = solutionQuality(problem, oldBest);
			if (oldQ >= newQ) {
				return;
			}
		}
		task.setBestSolution(belief);
		if (task.isInput()) { // moved from Sentence
			Memory.report(memory, belief, false);
		}
		BudgetValue budget = solutionEval(problem, belief, task, memory);
		if ((budget != null) && budget.aboveThreshold()) {
			Memory.activatedTask(memory, budget, belief, task.getParentBelief());
		}
	}

	/**
	 * Evaluate the quality of the judgment as a solution to a problem
	 * 
	 * @param problem
	 *            A goal or question
	 * @param solution
	 *            The solution to be evaluated
	 * @return The quality of the judgment as the solution
	 */
	public static float solutionQuality(SentenceStruct problem,
			SentenceStruct solution) {
		TruthValue truth1 = (TruthValue) solution.getTruth();
		if (problem == null) {
			return (truth1).getExpectation();
		}
		TruthValue truth = (TruthValue) truth1;
		if (Sentence.containQueryVar(problem)) { // "yes/no" question
			return truth.getExpectation()
					/ ((Term) solution.getContent()).getComplexity();
		} else { // "what" question or goal
			return truth.getConfidence();
		}
	}

	/*
	 * -------------------- same terms, difference relations
	 * --------------------
	 */
	/**
	 * The task and belief match reversely
	 * 
	 * @param memory
	 *            Reference to the memory
	 */
	public static void matchReverse(Memory memory) {
		Task task = memory.getCurrentTask();
		Sentence belief = memory.getCurrentBelief();
		Sentence sentence = task.getSentence();
		if (sentence.isJudgment()) {
			inferToSym(sentence, belief, memory);
		} else {
			conversion(memory);
		}
	}

	/**
	 * Inheritance/Implication matches Similarity/Equivalence
	 * 
	 * @param asym
	 *            A Inheritance/Implication sentence
	 * @param sym
	 *            A Similarity/Equivalence sentence
	 * @param figure
	 *            location of the shared term
	 * @param memory
	 */
	public static void matchAsymSym(Sentence asym, SentenceStruct sym,
			int figure, Memory memory) {
		if (memory.getCurrentTask().getSentence().isJudgment()) {
			inferToAsym(asym, sym, memory);
		} else {
			convertRelation(memory);
		}
	}

	/* -------------------- two-premise inference rules -------------------- */
	/**
	 * {<S --> P>,
	 * <P -->
	 * S} |- <S <-> p> Produce Similarity/Equivalence from a pair of reversed
	 * Inheritance/Implication
	 * 
	 * @param judgment1
	 *            The first premise
	 * @param judgment2
	 *            The second premise
	 * @param memory
	 *            Reference to the memory
	 */
	private static void inferToSym(Sentence judgment1,
			SentenceStruct judgment2, Memory memory) {
		Statement s1 = (Statement) judgment1.getContent();
		Term t1 = s1.getSubject();
		Term t2 = s1.getPredicate();
		Term content;
		if (s1 instanceof Inheritance) {
			content = Similarity.make(t1, t2, memory);
		} else {
			content = Equivalence.Companion.make(t1, t2, memory);
		}
		TruthValue value1 = judgment1.getTruth();
		TruthHandle value2 = judgment2.getTruth();
		TruthValue truth = TruthFunctions.intersection(value1, value2);
		BudgetValue budget = BudgetFunctions.forward(memory, truth);
		Memory.doublePremiseTask(memory, content, truth, budget);
	}

	/**
	 * {<S <-> P>,
	 * <P -->
	 * S>} |- <S --> P> Produce an Inheritance/Implication from a
	 * Similarity/Equivalence and a reversed Inheritance/Implication
	 * 
	 * @param asym
	 *            The asymmetric premise
	 * @param sym
	 *            The symmetric premise
	 * @param memory
	 *            Reference to the memory
	 */
	private static void inferToAsym(Sentence asym, SentenceStruct sym,
			Memory memory) {
		Statement statement = (Statement) asym.getContent();
		Term sub = statement.getPredicate();
		Term pre = statement.getSubject();
		Statement content = Statement.make(statement, sub, pre, memory);
		TruthValue truth = TruthFunctions.reduceConjunction(sym.getTruth(),
				asym.getTruth());
		BudgetValue budget = BudgetFunctions.forward(memory, truth);
		Memory.doublePremiseTask(memory, content, truth, budget);
	}

	/* -------------------- one-premise inference rules -------------------- */
	/**
	 * {
	 * <P -->
	 * S>} |- <S --> P> Produce an Inheritance/Implication from a reversed
	 * Inheritance/Implication
	 * 
	 * @param memory
	 *            Reference to the memory
	 */
	private static void conversion(Memory memory) {
		TruthValue truth = TruthFunctions.conversion(memory.getCurrentBelief()
				.getTruth());
		BudgetValue budget = BudgetFunctions.forward(memory, truth);
		convertedJudgment(truth, budget, memory);
	}

	/**
	 * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
	 * Inheritance/Implication and Similarity/Equivalence
	 * 
	 * @param memory
	 *            Reference to the memory
	 */
	private static void convertRelation(Memory memory) {
		TruthValue truth = memory.getCurrentBelief().getTruth();
		if (((Statement) memory.getCurrentTask().getContent()).isCommutative()) {
			truth = TruthFunctions.abduction(truth, 1.0f);
		} else {
			truth = TruthFunctions.deduction(truth, 1.0f);
		}
		BudgetValue budget = BudgetFunctions.forward(memory, truth);
		convertedJudgment(truth, budget, memory);
	}

	/**
	 * Convert jusgment into different relation
	 * <p>
	 * called in MatchingRules
	 * 
	 * @param newBudget
	 *            The budget value of the new task
	 * @param newTruth
	 *            The truth value of the new task
	 * @param memory
	 *            Reference to the memory
	 */
	private static void convertedJudgment(TruthValue newTruth,
			BudgetValue newBudget, Memory memory) {
		Statement content = (Statement) memory.getCurrentTask().getContent();
		Statement beliefContent = (Statement) memory.getCurrentBelief()
				.getContent();
		Term subjT = content.getSubject();
		Term predT = content.getPredicate();
		Term subjB = beliefContent.getSubject();
		Term predB = beliefContent.getPredicate();
		Term otherTerm;
		if (Variable.containVarQuery(subjT.getName())) {
			otherTerm = (predT.equals(subjB)) ? predB : subjB;
			content = Statement.make(content, otherTerm, predT, memory);
		}
		if (Variable.containVarQuery(predT.getName())) {
			otherTerm = (subjT.equals(subjB)) ? predB : subjB;
			content = Statement.make(content, subjT, otherTerm, memory);
		}
		Memory.singlePremiseTask(memory, content, Symbols.JUDGMENT_MARK,
				newTruth, newBudget);
	}

	/**
	 * Evaluate the quality of a revision, then de-prioritize the premises
	 * 
	 * @param tTruth
	 *            The truth value of the judgment in the task
	 * @param bTruth
	 *            The truth value of the belief
	 * @param truth
	 *            The truth value of the conclusion of revision
	 * @return The budget for the new task
	 */
	static BudgetValue revise(TruthValue tTruth, TruthValue bTruth,
			TruthValue truth, boolean feedbackToLinks, WorkSpace memory) {
		float difT = truth.isExpDifAbs(tTruth);
		Task task = memory.getCurrentTask();
		Item.decPriority(task.getBudget(), 1 - difT);
		Item.decDurability(task.getBudget(), 1 - difT);
		if (feedbackToLinks) {
			TaskLink tLink = memory.getCurrentTaskLink();
			Item.decPriority(tLink.getBudget(), 1 - difT);
			Item.decDurability(tLink.getBudget(), 1 - difT);
			TermLink bLink = memory.getCurrentBeliefLink();
			float difB = truth.isExpDifAbs(bTruth);
			Item.decPriority(bLink.getBudget(), 1 - difB);
			Item.decDurability(bLink.getBudget(), 1 - difB);
		}
		float dif = truth.getConfidence()
				- Math.max(tTruth.getConfidence(), bTruth.getConfidence());
		float priority = UtilityFunctions.or(dif, task.getPriority());
		float durability = UtilityFunctions.or(dif, task.getDurability());
		float quality = BudgetFunctions.truthToQuality(truth);
		return new BudgetValue(priority, durability, quality);
	}

	/**
	 * Evaluate the quality of a belief as a solution to a problem, then reward
	 * the belief and de-prioritize the problem
	 * 
	 * @param problem
	 *            The problem (question or goal) to be solved
	 * @param solution
	 *            The belief as solution
	 * @param task
	 *            The task to be immediatedly processed, or null for continued
	 *            process
	 * @return The budget for the new task which is the belief activated, if
	 *         necessary
	 */
	static BudgetValue solutionEval(SentenceStruct problem,
			SentenceStruct solution, Task task, WorkSpace memory) {
		BudgetValue budget = null;
		boolean feedbackToLinks = false;
		if (task == null) { // called in continued processing
			task = memory.getCurrentTask();
			feedbackToLinks = true;
		}
		boolean judgmentTask = task.getSentence().isJudgment();
		float quality = solutionQuality(problem, solution);
		if (judgmentTask) {
			Item.incPriority(task.getBudget(), quality);
		} else {
			task.setPriority(Math.min(1 - quality, task.getPriority()));
			budget = new BudgetValue(quality, task.getDurability(),
					BudgetFunctions.truthToQuality(solution.getTruth()));
		}
		if (feedbackToLinks) {
			TaskLink tLink = memory.getCurrentTaskLink();
			tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
			TermLink bLink = memory.getCurrentBeliefLink();
			Item.incPriority(bLink.getBudget(), quality);
		}
		return budget;
	}
}
