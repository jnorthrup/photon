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

/**
 * A Statement about an Equivalence relation.
 */
class Equivalence : Statement {

    /**
     * Constructor with partial values, called by make

     * @param components The component list of the term
     */
    protected constructor(components: List<Term>) : super(components) {
    }

    /**
     * Constructor with full values, called by clone

     * @param n          The name of the term
     * *
     * @param components Component list
     * *
     * @param constant   Whether the statement contains open variable
     * *
     * @param complexity Syntactic complexity of the compound
     */
    protected constructor(n: String, components: List<Term>, constant: Boolean, complexity: Int) : super(n, components,
            constant, complexity) {
    }

    /**
     * Clone an object

     * @return A new object
     */
    override fun clone(): Any {
        return Equivalence(name, CompoundTerm.cloneList(components), isConstant, complexity)
    }

    /**
     * Get the operator of the term.

     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.EQUIVALENCE_RELATION
    }

    /**
     * Check if the compound is commutative.

     * @return true for commutative
     */
    override fun isCommutative(): Boolean {
        return true
    }

    companion object {

        /**
         * Try to make a new compound from two components. Called by the inference rules.

         * @param s   The first component
         * *
         * @param p The second component
         * *
         * @param memory    Reference to the memory
         * *
         * @return A compound generated or null
         */
        fun make(s: Term, p: Term, memory: Memory): Equivalence? {
            // to be extended to check if subject is Conjunction
            var subject = s
            var predicate = p
            var r: Equivalence? =null

            if (subject !is Implication && subject !is Equivalence && predicate !is Implication && predicate !is
                    Equivalence && !Statement.invalidStatement(subject, predicate)) {
                if (0 < subject.compareTo(predicate)) {
                    val interm = subject
                    subject = predicate
                    predicate = interm
                }
                val name = Statement.makeStatementName(subject, Symbols.EQUIVALENCE_RELATION, predicate)
                val t = Memory.nameToListedTerm(memory, name)
                r = if (null == t) Equivalence(CompoundTerm.argumentsToList(subject, predicate)) else t as Equivalence?
            }
            return r
        }
    }
}
