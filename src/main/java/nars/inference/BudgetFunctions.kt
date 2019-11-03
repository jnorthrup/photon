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
package nars.inference

import nars.entity.*
import nars.inference.UtilityFunctions.Companion.and
import nars.inference.UtilityFunctions.Companion.aveAri
import nars.inference.UtilityFunctions.Companion.or
import nars.inference.UtilityFunctions.Companion.w2c
import nars.language.Term
import nars.storage.Memory

/**
 * Budget functions for resources allocation
 */
object BudgetFunctions   {/* ----------------------- Belief evaluation ----------------------- */

    /**
     * Determine the quality of a judgment by its truth value alone
     *
     *
     * Mainly decided by confidence, though binary judgment is also preferred
     *
     * @param t The truth value of a judgment
     * @return The quality of the judgment, according to truth value only
     */
    @JvmStatic
    fun truthToQuality(t: TruthValue?): Float {
        val exp = t!!.expectation
        return Math.max(exp.toDouble(), (1 - exp) * 0.75).toFloat()
    }

    /**
     * Determine the rank of a judgment by its quality and originality (stamp
     * length), called from Concept
     *
     * @param judg The judgment to be ranked
     * @return The rank of the judgment, according to truth value only
     */
    @JvmStatic
    fun rankBelief(judg: Sentence): Float {
        val confidence = judg.truth!!.getConfidence()
        val originality = 1.0f / (judg.stamp.length() + 1)
        return or(confidence, originality)
    }

    /* ----- Functions used both in direct and indirect processing of tasks ----- */


    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param problem  The problem (question or goal) to be solved
     * @param solution The belief as solution
     * @param task     The task to be immediately processed, or null for continued
     * process
     * @return The budget for the new task which is the belief activated, if
     * necessary
     */
 @JvmStatic     fun solutionEval(problem: Sentence?, solution: Sentence, task: Task?, memory: Memory): BudgetValue? {
        var task1 = task
        var budget: BudgetValue? = null
        var feedbackToLinks = false
        if (task1 == null) {                   // called in continued processing
            task1 = memory.currentTask
            feedbackToLinks = true
        }
        val judgmentTask = task1!!.sentence.isJudgment
        val quality = LocalRules.solutionQuality(problem, solution)
        if (judgmentTask) {
            task1.incPriority(quality)
        } else {
            val taskPriority = task1.priority
            budget = BudgetValue(or(taskPriority, quality), task1.durability, truthToQuality(solution.truth))
            task1.priority = Math.min(1 - quality, taskPriority)
        }
        if (feedbackToLinks) {
            val tLink: TaskLink = memory.currentTaskLink
            tLink.priority = Math.min(1 - quality, tLink.priority)
            val bLink = memory.currentBeliefLink
            bLink!!.incPriority(quality)
        }
        return budget
    }

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     *
     * @param tTruth The truth value of the judgment in the task
     * @param bTruth The truth value of the belief
     * @param truth  The truth value of the conclusion of revision
     * @return The budget for the new task
     */
     @JvmStatic   fun revise(tTruth: TruthValue, bTruth: TruthValue, truth: TruthValue, feedbackToLinks: Boolean, memory: Memory): BudgetValue {
        val difT = truth.getExpDifAbs(tTruth)
        val task: Task = memory.currentTask
        task.decPriority(1 - difT)
        task.decDurability(1 - difT)
        if (feedbackToLinks) {
            val tLink: TaskLink = memory.currentTaskLink
            tLink.decPriority(1 - difT)
            tLink.decDurability(1 - difT)
            val bLink = memory.currentBeliefLink
            val difB = truth.getExpDifAbs(bTruth)
            bLink!!.decPriority(1 - difB)
            bLink.decDurability(1 - difB)
        }
        val dif = truth.getConfidence() - Math.max(tTruth.getConfidence(), bTruth.getConfidence())
        val priority = or(dif, task.priority)
        val durability = aveAri(dif, task.durability)
        val quality = truthToQuality(truth)
        return BudgetValue(priority, durability, quality)
    }

    /* ----------------------- Links ----------------------- */


    /**
     * Distribute the budget of a task among the links to it
     *
     * @param b The original budget
     * @param n Number of links
     * @return Budget value for each link
     */
    @JvmStatic
    fun distributeAmongLinks(b: BudgetValue, n: Int): BudgetValue {
        val priority = (b.priority / Math.sqrt(n.toDouble())).toFloat()
        return BudgetValue(priority, b.durability, b.quality)
    }

    /* ----------------------- Concept ----------------------- */


    /**
     * Activate a concept by an incoming TaskLink
     *
     * @param concept The concept
     * @param budget  The budget for the new item
     */
    @JvmStatic
    fun activate(concept: Concept, budget: BudgetValue) {
        val oldPri = concept.priority
        val priority = or(oldPri, budget.priority)
        val durability = aveAri(concept.durability, budget.durability)
        val quality = concept.quality
        concept.priority = priority
        concept.durability = durability
        concept.quality = quality
    }

    /* ---------------- Bag functions, on all Items ------------------- */


    /**
     * Decrease Priority after an item is used, called in Bag
     *
     *
     * After a constant time, p should become d*p. Since in this period, the
     * item is accessed c*p times, each time p-q should multiple d^(1/(c*p)).
     * The intuitive meaning of the parameter "forgetRate" is: after this number
     * of times of access, priority 1 will become d, it is a system parameter
     * adjustable in run time.
     *
     * @param budget            The previous budget value
     * @param forgetRate        The budget for the new item
     * @param relativeThreshold The relative threshold of the bag
     */
    @JvmStatic
    fun forget(budget: BudgetValue, forgetRate: Float, relativeThreshold: Float) {
        var quality = (budget.quality * relativeThreshold).toDouble()      // re-scaled quality

        val p = budget.priority - quality                     // priority above quality

        if (p > 0) {
            quality += p * Math.pow(budget.durability.toDouble(), 1.0 / (forgetRate * p))
        }    // priority Durability

        budget.priority = quality.toFloat()
    }

    /**
     * Merge an item into another one in a bag, when the two are identical
     * except in budget values
     * @param baseValue   The budget value to be modified
     * @param adjustValue The budget doing the adjusting
     */
    @JvmStatic
    fun merge(baseValue: BudgetValue, adjustValue: BudgetTriple) {
        baseValue.priority = Math.max(baseValue.priority, adjustValue.priority)
        baseValue.durability = Math.max(baseValue.durability, adjustValue.durability)
        baseValue.quality = Math.max(baseValue.quality, adjustValue.quality)
    }

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */


    /**
     * Forward inference result and adjustment
     *
     * @param truth The truth value of the conclusion
     * @return The budget value of the conclusion
     */

    @JvmStatic      fun forward(truth: TruthValue?, memory: Memory): BudgetValue {
        return budgetInference(truthToQuality(truth), 1, memory)
    }

    /**
     * Backward inference result and adjustment, stronger case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    @JvmStatic
    fun backward(truth: TruthValue?, memory: Memory): BudgetValue {
        return budgetInference(truthToQuality(truth), 1, memory)
    }

    /**
     * Backward inference result and adjustment, weaker case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    @JvmStatic
    fun backwardWeak(truth: TruthValue?, memory: Memory): BudgetValue {
        return budgetInference(w2c(1f) * truthToQuality(truth), 1, memory)
    }

    /* ----- Task derivation in CompositionalRules and StructuralRules ----- */


    /**
     * Forward inference with CompoundTerm conclusion
     *
     * @param truth   The truth value of the conclusion
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    @JvmStatic
    fun compoundForward(truth: TruthValue?, content: Term, memory: Memory): BudgetValue {
        return budgetInference(truthToQuality(truth), content.complexity, memory)
    }

    /**
     * Backward inference with CompoundTerm conclusion, stronger case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    @JvmStatic
    fun compoundBackward(content: Term, memory: Memory): BudgetValue {
        return budgetInference(1f, content.complexity, memory)
    }

    /**
     * Backward inference with CompoundTerm conclusion, weaker case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    @JvmStatic
    fun compoundBackwardWeak(content: Term, memory: Memory): BudgetValue {
        return budgetInference(w2c(1f), content.complexity, memory)
    }

    /**
     * Common processing for all inference step
     *
     * @param qual       Quality of the inference
     * @param complexity Syntactic complexity of the conclusion
     * @param memory     Reference to the memory
     * @return Budget of the conclusion task
     */
    @JvmStatic    fun budgetInference(qual: Float, complexity: Int, memory: Memory): BudgetValue {
        var t: BudgetTriple? = memory.currentTaskLink
        if (t == null) {
            t = memory.currentTask
        }
        var priority = t!!.priority
        var durability = t.durability / complexity
        val quality = qual / complexity
        val bLink = memory.currentBeliefLink
        if (bLink != null) {
            priority = or(priority, bLink.priority)
            durability = and(durability, bLink.durability)
            val targetActivation = memory.getConceptActivation(bLink.target)
            bLink.incPriority(or(quality, targetActivation))
            bLink.incDurability(quality)
        }
        return BudgetValue(priority, durability, quality)
    }
}