/*
 * Conjunction.java
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
 * Conjunction of statements
 */
class Conjunction : CompoundTerm {
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected constructor(arg: List<Term>) : super(arg) {}

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether the term is a constant
     * @param i   Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, i: Short) : super(n, cs, con, i) {}

    /**
     * Clone an object
     *
     * @return A new object
     */

    override fun clone(): Any {
        return Conjunction(name, cloneList(components) as List<Term>, isConstant(), complexity as Short)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    override fun operator(): String {
        return Symbols.CONJUNCTION_OPERATOR
    }

    // overload this method by term type?


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
         * Try to make a new compound from a list of components. Called by
         * StringParser.
         *
         * @param argList the list of arguments
         * @param memory  Reference to the memory
         * @return the Term generated from the arguments
         */
        @JvmStatic     fun make(argList: List<Term>?, memory: Memory): Term? {
            val set = TreeSet(argList) // sort/merge arguments

            return make(set, memory)
        }

        /**
         * Try to make a new Disjunction from a set of components. Called by the
         * public make methods.
         *
         * @param set    a set of Term as components
         * @param memory Reference to the memory
         * @return the Term generated from the arguments
         */
        @JvmStatic      private fun make(set: TreeSet<Term>, memory: Memory): Term? {
            if (set.isEmpty()) {
                return null
            }                         // special case: single component

            if (set.size == 1) {
                return set.first()
            }                         // special case: single component

            val argument = ArrayList(set)
            val name: String? = makeCompoundName(Symbols.CONJUNCTION_OPERATOR, argument)
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: Conjunction(argument)
        }

        /**
         * Try to make a new compound from two components. Called by the inference
         * rules.
         *
         * @param term1  The first component
         * @param term2  The second component
         * @param memory Reference to the memory
         * @return A compound generated or a term it reduced to
         */
 @JvmStatic  fun make(term1: Term, term2: Term, memory: Memory): Term? {
            val set: TreeSet<Term>
            if (term1 is Conjunction) {
                set = TreeSet((term1 as CompoundTerm).cloneComponents())
                if (term2 is Conjunction) {
                    set.addAll((term2 as CompoundTerm).cloneComponents())
                } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
                else {
                    set.add(term2.clone() as Term)
                }                          // (&,(&,P,Q),R) = (&,P,Q,R)
            } else if (term2 is Conjunction) {
                set = TreeSet((term2 as CompoundTerm).cloneComponents())
                set.add(term1.clone() as Term)                              // (&,R,(&,P,Q)) = (&,P,Q,R)
            } else {
                set = TreeSet()
                set.add(term1.clone() as Term)
                set.add(term2.clone() as Term)
            }
            return make(set, memory)
        }
    }
}