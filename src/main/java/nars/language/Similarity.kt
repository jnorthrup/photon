/*
 * Similarity.java
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
 * A Statement about a Similarity relation.
 */
class Similarity : Statement {
    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private constructor(arg: List<Term>) : super(arg)

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, i: Short) : super(n, cs, con, i)

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Similarity
     */

    override fun clone(): Any {
        return Similarity(name, cloneList(components) as List<Term>, isConstant, complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    override fun operator(): String {
        return Symbols.SIMILARITY_RELATION
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
        fun make(subject: Term, predicate: Term, memory: Memory): Similarity? {
            if (!invalidStatement(subject, predicate)) {
                if (subject > predicate) {
                    return make(predicate, subject, memory)
                }
                val name = makeStatementName(subject, Symbols.SIMILARITY_RELATION, predicate)
                val t: Term? = memory.nameToListedTerm(name)
                if (t == null) {
                    val argument: ArrayList<Term> = argumentsToList(subject, predicate)
                    return Similarity(argument)
                }
                return t as Similarity
            }
            return null
        }
    }
}