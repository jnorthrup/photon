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
     * @param arg The component list of the term
     */
    protected constructor(arg: List<Term>) : super(arg) {
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * *
     * @param cs Component list
     * *
     * @param con Whether the term is a constant
     * *
     * @param i Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, i: Int) : super(n, cs, con, i) {
    }

    /**
     * Clone an object
     * @return A new object
     */
    override fun clone(): Any {
        return Conjunction(name, CompoundTerm.cloneList(components) as ArrayList<Term>, isConstant, complexity)
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.CONJUNCTION_OPERATOR
    }

    /**
     * Check if the compound is communitative.
     * @return true for communitative
     */
    override fun isCommutative(): Boolean {
        return true
    }

    companion object {

        /**
         * Try to make a new compound from a list of components. Called by StringParser.
         * @return the Term generated from the arguments
         * *
         * @param argList the list of arguments
         * *
         * @param memory Reference to the memory
         */
        fun make(argList: List<Term>, memory: Memory): Term {
            val set = TreeSet(argList) // sort/merge arguments
            return make(set, memory) as Term
        }

        /**
         * Try to make a new Disjunction from a set of components. Called by the public make methods.
         * @param set a set of Term as compoments
         * *
         * @param memory Reference to the memory
         * *
         * @return the Term generated from the arguments
         */
        private fun make(set: TreeSet<Term>, memory: Memory): Term? {
            if (set.size == 0) {
                return null
            }                         // special case: single component
            if (set.size == 1) {
                return set.first()
            }                         // special case: single component
            val argument = ArrayList(set)
            val name = CompoundTerm.makeCompoundName(Symbols.CONJUNCTION_OPERATOR, argument)
            val t = Memory.nameToListedTerm(memory, name)
            return t ?: Conjunction(argument)
        }

        // overload this method by term type?
        /**
         * Try to make a new compound from two components. Called by the inference rules.
         * @param term1 The first compoment
         * *
         * @param term2 The second compoment
         * *
         * @param memory Reference to the memory
         * *
         * @return A compound generated or a term it reduced to
         */
        fun make(term1: Term, term2: Term, memory: Memory): Term {
            val set: TreeSet<Term>
            when {
                term1 is Conjunction -> {
                    set = TreeSet((term1 as CompoundTerm).cloneComponents())
                    when (term2) {
                        is Conjunction -> {
                            set.addAll((term2 as CompoundTerm).cloneComponents())
                        }
                        else -> {
                            set.add(term2.clone() as Term)
                        }
                    }                          // (&,(&,P,Q),R) = (&,P,Q,R)
                }
                term2 is Conjunction -> {
                    set = TreeSet((term2 as CompoundTerm).cloneComponents())
                    set.add(term1.clone() as Term)                              // (&,R,(&,P,Q)) = (&,P,Q,R)
                }
                else -> {
                    set = TreeSet<Term>()
                    set.add(term1.clone() as Term)
                    set.add(term2.clone() as Term)
                }
            }
            return make(set, memory) as Term
        }
    }
}
