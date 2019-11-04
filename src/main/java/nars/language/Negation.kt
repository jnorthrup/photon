/*
 * Negation.java
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
import nars.storage.BackingStore
import java.util.*

/**
 * A negation of a statement.
 */
class Negation : CompoundTerm {
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
    private constructor(n: String, cs: List<Term?>?, con: Boolean, i: Short) : super(n, cs, con, i)

    /**
     * Clone an object
     *
     * @return A new object
     */

    override fun clone(): Any {
        return Negation(name, cloneList(components), isConstant, complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    override fun operator(): String {
        return Symbols.NEGATION_OPERATOR
    }

    companion object {
        /**
         * Try to make a Negation of one component. Called by the inference rules.
         *
         * @param t      The component
         * @param memory Reference to the memory
         * @return A compound generated or a term it reduced to
         */
        @JvmStatic   fun make(t: Term, memory: BackingStore): Term? {
            if (t is Negation) {
                return (t as CompoundTerm).cloneComponents()[0]
            }         // (--,(--,P)) = P

            val argument = ArrayList<Term>()
            argument.add(t)
            return make(argument, memory)
        }

        /**
         * Try to make a new Negation. Called by StringParser.
         *
         * @param argument The list of components
         * @param memory   Reference to the memory
         * @return the Term generated from the arguments
         */
        @JvmStatic      fun make(argument: List<Term>, memory: BackingStore): Term? {
            var result: Term? = null
            if (argument.size == 1) {
                val name: String? = makeCompoundName(Symbols.NEGATION_OPERATOR, argument)
                val t: Term? = memory.nameToListedTerm(name)
                result = t ?: Negation(argument)
            }
            return result
        }
    }
}