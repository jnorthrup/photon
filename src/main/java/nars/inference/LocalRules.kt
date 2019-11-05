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
package nars.inference

import nars.entity.BudgetValue
import nars.entity.Sentence
import nars.entity.Task
import nars.entity.TruthValue
import nars.inference.BudgetFunctions.forward
import nars.inference.BudgetFunctions.revise
import nars.inference.BudgetFunctions.solutionEval
import nars.io.sentence_type
import nars.io.var_type
//import nars.io.Symbols
import nars.language.*
import nars.storage.BackingStore

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 *
 *
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
object LocalRules {/* -------------------- same contents -------------------- */

    /**
     * The task and belief have the same content
     *
     *
     * called in RuleTables.reason
     *
     * @param task   The task
     * @param belief The belief
     * @param memory Reference to the memory
     */
    @JvmStatic
    fun match(task: Task, belief: Sentence, memory: BackingStore) {
        val sentence = task.sentence.clone() as Sentence
        if (sentence.isJudgment) {
            if (revisible(sentence, belief)) {
                revision(sentence, belief, true, memory)
            }
        } else if (Variable.unify(var_type.VAR_QUERY.sym, sentence.content, belief.content.clone() as Term)) {
            trySolution(belief, task, memory)
        }
    }

    /**
     * Check whether two sentences can be used in revision
     *
     * @param s1 The first sentence
     * @param s2 The second sentence
     * @return If revision is possible between the two sentences
     */
    fun revisible(s1: Sentence, s2: Sentence): Boolean {
        return s1.content == s2.content && s1.revisible
    }

    /**
     * Belief revision
     *
     *
     * called from Concept.reviseTable and match
     *
     * @param newBelief       The new belief in task
     * @param oldBelief       The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param memory          Reference to the memory
     */
    @JvmStatic
    fun revision(newBelief: Sentence, oldBelief: Sentence, feedbackToLinks: Boolean, memory: BackingStore) {
        val newTruth = newBelief.truth
        val oldTruth = oldBelief.truth
        val truth: TruthValue? = TruthFunctions.revision(newTruth!!, oldTruth!!)
        val budget = memory.revise(newTruth, oldTruth, truth!!, feedbackToLinks)
        val content = newBelief.content
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief The proposed answer
     * @param task   The task to be processed
     * @param memory Reference to the memory
     */
    @JvmStatic
    fun trySolution(belief: Sentence, task: Task, memory: BackingStore) {
        val problem = task.sentence
        val oldBest = task.bestSolution
        val newQ = solutionQuality(problem, belief)
        if (oldBest != null) {
            val oldQ = solutionQuality(problem, oldBest)
            if (oldQ >= newQ) {
                return
            }
        }
        task.bestSolution = belief
        if (task.isInput) {    // moved from Sentence

            memory.report(belief, false)
        }
        val budget = memory.solutionEval(problem, belief, task)
        if (budget != null && budget.aboveThreshold()) {
            memory.activatedTask(budget, belief, task.parentBelief!!)
        }
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     *
     * @param problem  A goal or question
     * @param solution The solution to be evaluated
     * @return The quality of the judgment as the solution
     */
    fun solutionQuality(problem: Sentence?, solution: Sentence): Float {
        if (problem == null) {
            return solution.truth!!.expectation
        }
        val truth = solution.truth
        return if (problem.containQueryVar()) {   // "yes/no" question

            truth!!.expectation / solution.content.complexity
        } else {                                    // "what" question or goal

            truth!!.confidence
        }
    }

    /* -------------------- same terms, difference relations -------------------- */


    /**
     * The task and belief match reversely
     *
     * @param memory Reference to the memory
     */
    @JvmStatic
    fun matchReverse(memory: BackingStore) {
        val task: Task = memory.currentTask!!
        val belief = memory.currentBelief
        val sentence = task.sentence
        if (sentence.isJudgment) {
            inferToSym(sentence, belief, memory)
        } else {
            conversion(memory)
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
    @JvmStatic
    fun matchAsymSym(asym: Sentence, sym: Sentence, figure: Int, memory: BackingStore) {
        if (memory.currentTask!!.sentence.isJudgment) {
            inferToAsym(asym, sym, memory)
        } else {
            convertRelation(memory)
        }
    }

    /* -------------------- two-premise inference rules -------------------- */


    /**
     * {<S --> P>, <P --> S} |- <S></S><-> p> Produce Similarity/Equivalence from a
     * pair of reversed Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param memory    Reference to the memory
    </P></S> */
    private fun inferToSym(judgment1: Sentence, judgment2: Sentence?, memory: BackingStore) {
        val s1 = judgment1.content as Statement
        val t1: Term = s1.subject
        val t2: Term = s1.predicate
        val content: Term?
        content = if (s1 is Inheritance) {
            Similarity.make(t1, t2, memory)
        } else {
            Equivalence.make(t1, t2, memory)
        }
        val value1 = judgment1.truth
        val value2 = judgment2!!.truth
        val truth: TruthValue  = TruthFunctions.intersection(value1!!, value2!!)
        val budget = memory.forward(truth)
        memory.doublePremiseTask(content!!, truth, budget)
    }

    /**
     * {<S></S> <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym   The asymmetric premise
     * @param sym    The symmetric premise
     * @param memory Reference to the memory
    </S></P> */
    private fun inferToAsym(asym: Sentence, sym: Sentence, memory: BackingStore) {
        val statement = asym.content as Statement
        val sub: Term  = statement.predicate
        val pre: Term  = statement.subject
        val content: Statement = Statement.make(statement, sub, pre, memory)!!
        val truth: TruthValue  = TruthFunctions.reduceConjunction(sym.truth!!, asym.truth!!)
        val budget = memory.forward(truth)
        memory.doublePremiseTask(content, truth, budget)
    }

    /* -------------------- one-premise inference rules -------------------- */


    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param memory Reference to the memory
    </S></P> */
    private fun conversion(memory: BackingStore) {
        val truth: TruthValue? = TruthFunctions.conversion(memory.currentBelief!!.truth!!)
        val budget = memory.forward(truth)
        convertedJudgment(truth, budget, memory)
    }

    /**
     * {<S --> P>} |- <S></S><-> P> {<S></S><-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param memory Reference to the memory
    </S></S> */
    private fun convertRelation(memory: BackingStore) {
        var truth = memory.currentBelief!!.truth
        truth = if ((memory.currentTask!!.content as Statement?)!!.commutative) {
            TruthFunctions.abduction(truth!!, 1.0f)
        } else {
            TruthFunctions.deduction(truth!!, 1.0f)
        }
        val budget = memory.forward(truth)
        convertedJudgment(truth, budget, memory)
    }

    /**
     * Convert judgment into different relation
     *
     *
     * called in MatchingRules
     *
     * @param budget The budget value of the new task
     * @param truth  The truth value of the new task
     * @param memory Reference to the memory
     */
    private fun convertedJudgment(newTruth: TruthValue?, newBudget: BudgetValue, memory: BackingStore) {
        var content = memory.currentTask!!.content as Statement
        val beliefContent = memory.currentBelief!!.content as Statement
        val subjT: Term = content.subject
        val predT: Term = content.predicate
        val subjB: Term = beliefContent.subject
        val predB: Term = beliefContent.predicate
        var otherTerm: Term
        if (Variable.containVarQuery(subjT.name)) {
            otherTerm = if (predT == subjB) predB else subjB
            content = Statement.make(content, otherTerm, predT, memory)!!
        }
        if (Variable.containVarQuery(predT.name)) {
            otherTerm = if (subjT == subjB) predB else subjB
            content = Statement.make(content, subjT, otherTerm, memory)!!
        }
        memory.singlePremiseTask(content, sentence_type.JUDGMENT_MARK.sym, newTruth!!, newBudget)
    }
}