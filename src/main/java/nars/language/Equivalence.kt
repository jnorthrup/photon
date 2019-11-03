/*
 * Equivalence.java
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
package nars.language

import nars.io.Symbols
import nars.storage.Memory
import java.util.*

/**
 * A Statement about an Equivalence relation.
 */
class Equivalence : Statement {
    /**
     * Constructor with partial values, called by make
     *
     * @param components The component list of the term
     */
    protected constructor(components: List<Term>) : super(components) {}

    /**
     * Constructor with full values, called by clone
     *
     * @param n          The name of the term
     * @param components Component list
     * @param constant   Whether the statement contains open variable
     * @param complexity Syntactic complexity of the compound
     */
    protected constructor(n: String, components: List<Term>, constant: Boolean, complexity: Short) : super(n, components, constant, complexity) {}

    /**
     * Clone an object
     *
     * @return A new object
     */

    override fun clone(): Any {
        return Equivalence(name, cloneList(components) as List<Term>, isConstant(), complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    override fun operator(): String {
        return Symbols.EQUIVALENCE_RELATION
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */

    override fun isCommutative(): Boolean {
        return true
    }

    companion object {
        /**
         * Try to make a new compound from two components. Called by the inference rules.
         *
         * @param subject   The first component
         * @param predicate The second component
         * @param memory    Reference to the memory
         * @return A compound generated or null
         */
        fun make(subject: Term, predicate: Term, memory: Memory): Equivalence? {  // to be extended to check if subject is Conjunction

            var subject1 = subject
            var predicate1 = predicate
            if (subject1 !is Implication && subject1 !is Equivalence) {
                if (predicate1 !is Implication && predicate1 !is Equivalence && !invalidStatement(subject1, predicate1)) {
                    if (subject1.compareTo(predicate1) > 0) {
                        val interm = subject1
                        subject1 = predicate1
                        predicate1 = interm
                    }
                    val name = makeStatementName(subject1, Symbols.EQUIVALENCE_RELATION, predicate1)
                    val t: Term? = memory.nameToListedTerm(name)
                    if (t != null) {
                        return t as Equivalence
                    }
                    val argument: ArrayList<Term> = argumentsToList(subject1, predicate1)
                    return Equivalence(argument)
                }
            }
            return null
        }
    }
}