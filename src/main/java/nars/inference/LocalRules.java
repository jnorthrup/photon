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

import nars.entity.BudgetValue;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.io.Symbols;
import nars.language.*;
import nars.storage.Memory;

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
     * @param task   The task
     * @param belief The belief
     * @param memory Reference to the memory
     */
    public static void match(Task task, Sentence belief, Memory memory) {
        var sentence = (Sentence) task.getSentence().clone();
        if (sentence.isJudgment()) {
            if (revisible(sentence, belief)) {
                revision(sentence, belief, true, memory);
            }
        } else if (Variable.unify(Symbols.VAR_QUERY, sentence.getContent(), (Term) belief.getContent().clone())) {
            trySolution(belief, task, memory);
        }
    }

    /**
     * Check whether two sentences can be used in revision
     *
     * @param s1 The first sentence
     * @param s2 The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisible(Sentence s1, Sentence s2) {
        return (s1.getContent().equals(s2.getContent()) && s1.getRevisible());
    }

    /**
     * Belief revision
     * <p>
     * called from Concept.reviseTable and match
     *
     * @param newBelief       The new belief in task
     * @param oldBelief       The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param memory          Reference to the memory
     */
    public static void revision(Sentence newBelief, Sentence oldBelief, boolean feedbackToLinks, Memory memory) {
        var newTruth = newBelief.getTruth();
        var oldTruth = oldBelief.getTruth();
        var truth = TruthFunctions.revision(newTruth, oldTruth);
        var budget = BudgetFunctions.revise(newTruth, oldTruth, truth, feedbackToLinks, memory);
        var content = newBelief.getContent();
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief The proposed answer
     * @param task   The task to be processed
     * @param memory Reference to the memory
     */
    public static void trySolution(Sentence belief, Task task, Memory memory) {
        var problem = task.getSentence();
        var oldBest = task.getBestSolution();
        var newQ = solutionQuality(problem, belief);
        if (oldBest != null) {
            var oldQ = solutionQuality(problem, oldBest);
            if (oldQ >= newQ) {
                return;
            }
        }
        task.setBestSolution(belief);
        if (task.isInput()) {    // moved from Sentence
            memory.report(belief, false);
        }
        var budget = BudgetFunctions.solutionEval(problem, belief, task, memory);
        if ((budget != null) && budget.aboveThreshold()) {
            memory.activatedTask(budget, belief, task.getParentBelief());
        }
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     *
     * @param problem  A goal or question
     * @param solution The solution to be evaluated
     * @return The quality of the judgment as the solution
     */
    public static float solutionQuality(Sentence problem, Sentence solution) {
        if (problem == null) {
            return solution.getTruth().getExpectation();
        }
        var truth = solution.getTruth();
        if (problem.containQueryVar()) {   // "yes/no" question
            return truth.getExpectation() / solution.getContent().getComplexity();
        } else {                                    // "what" question or goal
            return truth.getConfidence();
        }
    }

    /* -------------------- same terms, difference relations -------------------- */

    /**
     * The task and belief match reversely
     *
     * @param memory Reference to the memory
     */
    public static void matchReverse(Memory memory) {
        var task = memory.currentTask;
        var belief = memory.currentBelief;
        var sentence = task.getSentence();
        if (sentence.isJudgment()) {
            inferToSym((Sentence) sentence, belief, memory);
        } else {
            conversion(memory);
        }
    }

    /**
     * Inheritance/Implication matches Similarity/Equivalence
     *
     * @param asym   A Inheritance/Implication sentence
     * @param sym    A Similarity/Equivalence sentence
     * @param figure location of the shared term
     * @param memory Reference to the memory
     */
    public static void matchAsymSym(Sentence asym, Sentence sym, int figure, Memory memory) {
        if (memory.currentTask.getSentence().isJudgment()) {
            inferToAsym((Sentence) asym, (Sentence) sym, memory);
        } else {
            convertRelation(memory);
        }
    }

    /* -------------------- two-premise inference rules -------------------- */

    /**
     * {<S --> P>, <P --> S} |- <S <-> p> Produce Similarity/Equivalence from a
     * pair of reversed Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param memory    Reference to the memory
     */
    private static void inferToSym(Sentence judgment1, Sentence judgment2, Memory memory) {
        var s1 = (Statement) judgment1.getContent();
        var t1 = s1.getSubject();
        var t2 = s1.getPredicate();
        Term content;
        if (s1 instanceof Inheritance) {
            content = Similarity.make(t1, t2, memory);
        } else {
            content = Equivalence.make(t1, t2, memory);
        }
        var value1 = judgment1.getTruth();
        var value2 = judgment2.getTruth();
        var truth = TruthFunctions.intersection(value1, value2);
        var budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym   The asymmetric premise
     * @param sym    The symmetric premise
     * @param memory Reference to the memory
     */
    private static void inferToAsym(Sentence asym, Sentence sym, Memory memory) {
        var statement = (Statement) asym.getContent();
        var sub = statement.getPredicate();
        var pre = statement.getSubject();
        var content = Statement.make(statement, sub, pre, memory);
        var truth = TruthFunctions.reduceConjunction(sym.getTruth(), asym.getTruth());
        var budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */

    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param memory Reference to the memory
     */
    private static void conversion(Memory memory) {
        var truth = TruthFunctions.conversion(memory.currentBelief.getTruth());
        var budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param memory Reference to the memory
     */
    private static void convertRelation(Memory memory) {
        var truth = memory.currentBelief.getTruth();
        if (((Statement) memory.currentTask.getContent()).isCommutative()) {
            truth = TruthFunctions.abduction(truth, 1.0f);
        } else {
            truth = TruthFunctions.deduction(truth, 1.0f);
        }
        var budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * Convert judgment into different relation
     * <p>
     * called in MatchingRules
     *
     * @param budget The budget value of the new task
     * @param truth  The truth value of the new task
     * @param memory Reference to the memory
     */
    private static void convertedJudgment(TruthValue newTruth, BudgetValue newBudget, Memory memory) {
        var content = (Statement) memory.currentTask.getContent();
        var beliefContent = (Statement) memory.currentBelief.getContent();
        var subjT = content.getSubject();
        var predT = content.getPredicate();
        var subjB = beliefContent.getSubject();
        var predB = beliefContent.getPredicate();
        Term otherTerm;
        if (Variable.containVarQuery(subjT.getName())) {
            otherTerm = (predT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, otherTerm, predT, memory);
        }
        if (Variable.containVarQuery(predT.getName())) {
            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, subjT, otherTerm, memory);
        }
        memory.singlePremiseTask(content, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
