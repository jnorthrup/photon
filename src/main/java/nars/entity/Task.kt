/*
 * Task.java
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
package nars.entity

import nars.language.Term

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
class Task
/**
 * Constructor for input task
 *
 * @param s The sentence
 * @param b The budget
 */(
        /**
         * The sentence of the Task
         */
        val sentence: Sentence, b: BudgetValue) : ItemIdentity(b) {
    /**
     * Get the sentence
     *
     * @return The sentence
     */
    /**
     * Get the parent task of a task
     *
     * @return The task from which the task is derived
     */
    /**
     * Task from which the Task is derived, or null if input
     */
      var parentTask: Task?=null
        private set
    /**
     * Get the parent belief of a task
     *
     * @return The belief from which the task is derived
     */
    /**
     * Belief from which the Task is derived, or null if derived from a theorem
     */
    var parentBelief: Sentence?=null
        private set
    /**
     * Get the best-so-far solution for a Question or Goal
     *
     * @return The stored Sentence or null
     */
    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     *
     * @param judg The solution to be remembered
     */
    /**
     * For Question and Goal: best solution found so far
     */
    var bestSolution: Sentence? = null

    /**
     * Constructor for a derived task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    constructor(s: Sentence, b: BudgetValue, parentTask: Task?, parentBelief: Sentence?) : this(s, b) {
        this.parentTask = parentTask
        this.parentBelief = parentBelief
    }

    /**
     * Constructor for an activated task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     * @param solution     The belief to be used in future inference
     */
    constructor(s: Sentence, b: BudgetValue, parentTask: Task, parentBelief: Sentence, solution: Sentence?) : this(s, b, parentTask, parentBelief) {
        bestSolution = solution
    }

    /**
     * Directly get the content of the sentence
     *
     * @return The content of the sentence
     */
    val content: Term?
        get() = sentence.content

    /**
     * Directly get the creation time of the sentence
     *
     * @return The creation time of the sentence
     */
    private val creationTime: Long
        get() = sentence.stamp.creationTime

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    val isInput: Boolean
        get() = parentTask == null

    /**
     * Check if a Task is derived by a StructuralRule
     *
     * @return Whether the Task is derived by a StructuralRule
     */
//    public boolean isStructural() {
//        return (parentBelief == null) && (parentTask != null);
//    }


    /**
     * Merge one Task into another
     *
     * @param that The other Task
     */

    fun merge(that: Task) {
        if (creationTime >= that.creationTime) {
            super.merge(that)
        } else {
            that.merge(this)
        }
    }

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */

    override fun toString(): String {
        val s = StringBuilder()
        s.append(super.toString()).append(" ")
        s.append(sentence.stamp)
          (parentTask  )?.run {
            s.append("  \n from task: ").append(toStringBrief())
              (this@Task.parentBelief)?.run {
                s.append("  \n from belief: ").append(toStringBrief())
            }
        }
         (bestSolution )?.run {
            s.append("  \n solution: ").append(toStringBrief())
        }
        return s.toString()
    }

    /**
     *
     */
    override val key: String
        get() = sentence.toKey()

}