/*
 * Parameters.java
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
package nars.main_nogui

/**
 * Collected system parameters. To be modified before compiling.
 */
object Parameters {/* ---------- initial values of run-time adjustable parameters ---------- */
    /**
     * Concept decay rate in ConceptBag, in [1, 99].
     */
    const val CONCEPT_FORGETTING_CYCLE = 10
    /**
     * TaskLink decay rate in TaskLinkBag, in [1, 99].
     */
    const val TASK_LINK_FORGETTING_CYCLE = 20
    /**
     * TermLink decay rate in TermLinkBag, in [1, 99].
     */
    const val TERM_LINK_FORGETTING_CYCLE = 50
    /**
     * Silent threshold for task reporting, in [0, 100].
     */
    const val SILENT_LEVEL = 0

    /* ---------- time management ---------- */

    /**
     * Task decay rate in TaskBuffer, in [1, 99].
     */
    const val NEW_TASK_FORGETTING_CYCLE = 1
    /**
     * Maximum TermLinks checked for novelty for each TaskLink in TermLinkBag
     */
    const val MAX_MATCHED_TERM_LINK = 10
    /**
     * Maximum TermLinks used in reasoning for each Task in Concept
     */
    const val MAX_REASONED_TERM_LINK = 3

    /* ---------- logical parameters ---------- */

    /**
     * Evidential Horizon, the amount of future evidence to be considered.
     */
    const val HORIZON = 1    // or 2, can be float

    /**
     * Reliance factor, the empirical confidence of analytical truth.
     */
    const val RELIANCE = 0.9.toFloat()    // the same as default confidence


    /* ---------- budget thresholds ---------- */

    /**
     * The budget threshold rate for task to be accepted.
     */
    const val BUDGET_THRESHOLD = 0.01.toFloat()

    /* ---------- default input values ---------- */

    /**
     * Default expectation for confirmation.
     */
    const val DEFAULT_CREATION_EXPECTATION = 0.66.toFloat()
    /**
     * Default confidence of input judgment.
     */
    const val DEFAULT_JUDGMENT_CONFIDENCE = 0.9.toFloat()
    /**
     * Default priority of input judgment
     */
    const val DEFAULT_JUDGMENT_PRIORITY = 0.8.toFloat()
    /**
     * Default durability of input judgment
     */
    const val DEFAULT_JUDGMENT_DURABILITY = 0.8.toFloat()
    /**
     * Default priority of input question
     */
    const val DEFAULT_QUESTION_PRIORITY = 0.9.toFloat()
    /**
     * Default durability of input question
     */
    const val DEFAULT_QUESTION_DURABILITY = 0.9.toFloat()

    /* ---------- space management ---------- */

    /**
     * Level granularity in Bag, two digits
     */
    const val BAG_LEVEL = 100
    /**
     * Level separation in Bag, one digit, for display (run-time adjustable) and management (fixed)
     */
    const val BAG_THRESHOLD = 10
    /**
     * Hashtable load factor in Bag
     */
    const val LOAD_FACTOR = 0.5.toFloat()
    /**
     * Size of ConceptBag
     */
    const val CONCEPT_BAG_SIZE = 1000
    /**
     * Size of TaskLinkBag
     */
    const val TASK_LINK_BAG_SIZE = 20
    /**
     * Size of TermLinkBag
     */
    const val TERM_LINK_BAG_SIZE = 100
    /**
     * Size of TaskBuffer
     */
    const val TASK_BUFFER_SIZE = 10

    /* ---------- avoiding repeated reasoning ---------- */

    /**
     * Maximum length of Stamp, a power of 2
     */
    const val MAXIMUM_STAMP_LENGTH = 8
    /**
     * Remember recently used TermLink on a Task
     */
    const val TERM_LINK_RECORD_LENGTH = 10
    /**
     * Maximum number of beliefs kept in a Concept
     */
    const val MAXIMUM_BELIEF_LENGTH = 7
    /**
     * Maximum number of goals kept in a Concept
     */
    const val MAXIMUM_QUESTIONS_LENGTH = 5
}