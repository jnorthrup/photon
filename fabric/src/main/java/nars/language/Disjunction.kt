/*
 * Disjunction.java
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
 * A disjunction of Statements.
 */
class Disjunction : CompoundTerm {

    /**
     * Constructor with partial values, called by make
     * @param arg The component list of the term
     */
    private constructor(arg: List<Term>) : super(arg) {
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * *
     * @param cs Component list
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
        return Disjunction(name, CompoundTerm.cloneList(components), isConstant, complexity)
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.DISJUNCTION_OPERATOR
    }

    /**
     * Disjunction is communitative.
     * @return true for communitative
     */
    override fun isCommutative(): Boolean {
        return true
    }

    companion object {

        /**
         * Try to make a new Disjunction from two components. Called by the inference rules.
         * @param term1 The first compoment
         * *
         * @param term2 The first compoment
         * *
         * @param memory Reference to the memory
         * *
         * @return A Disjunction generated or a Term it reduced to
         */
        fun make(term1: Term, term2: Term, memory: Memory): Term {
            val set: TreeSet<Term>
            if (term1 is Disjunction) {
                set = TreeSet((term1 as CompoundTerm).cloneComponents())
                if (term2 is Disjunction) {
                    set.addAll((term2 as CompoundTerm).cloneComponents())
                } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
                else {
                    set.add(term2.clone() as Term)
                }                          // (&,(&,P,Q),R) = (&,P,Q,R)
            } else if (term2 is Disjunction) {
                set = TreeSet((term2 as CompoundTerm).cloneComponents())
                set.add(term1.clone() as Term)                              // (&,R,(&,P,Q)) = (&,P,Q,R)
            } else {
                set = TreeSet<Term>()
                set.add(term1.clone() as Term)
                set.add(term2.clone() as Term)
            }
            return make(set, memory)
        }

        /**
         * Try to make a new IntersectionExt. Called by StringParser.
         * @param argList a list of Term as compoments
         * *
         * @param memory Reference to the memory
         * *
         * @return the Term generated from the arguments
         */
        fun make(argList: List<Term>, memory: Memory): Term {
            val set = TreeSet(argList) // sort/merge arguments
            return make(set, memory)
        }

        /**
         * Try to make a new Disjunction from a set of components. Called by the public make methods.
         * @param set a set of Term as compoments
         * *
         * @param memory Reference to the memory
         * *
         * @return the Term generated from the arguments
         */
        fun make(set: TreeSet<Term>, memory: Memory): Term {
            if (set.size == 1) {
                return set.first()
            }                         // special case: single component
            val argument = ArrayList(set)
            val name = CompoundTerm.makeCompoundName(Symbols.DISJUNCTION_OPERATOR, argument)
            val t = Memory.nameToListedTerm(memory, name)
            return t ?: Disjunction(argument)
        }
    }
}
