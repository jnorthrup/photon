/*
 * IntersectionInt.java
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
 * A compound term whose intension is the intersection of the extensions of its components
 */
class IntersectionInt : CompoundTerm {
    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private constructor(arg: List<Term>) : super(arg) {}

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, i: Short) : super(n, cs, con, i) {}

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Conjunction
     */
    override fun clone(): Any {
        return IntersectionInt(name, cloneList(components) as List<Term>, isConstant(), complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.INTERSECTION_INT_OPERATOR
    }

    /**
     * Check if the compound is communitative.
     *
     * @return true for communitative
     */

    override fun isCommutative(): Boolean {
        return true
    }

    companion object {
        /**
         * Try to make a new compound from two components. Called by the inference rules.
         *
         * @param term1  The first compoment
         * @param term2  The first compoment
         * @param memory Reference to the memory
         * @return A compound generated or a term it reduced to
         */
   @JvmStatic     fun make(term1: Term, term2: Term, memory: Memory): Term {
            val set: TreeSet<Term>
            if (term1 is SetExt && term2 is SetExt) {
                set = TreeSet((term1 as CompoundTerm).cloneComponents())
                set.addAll((term2 as CompoundTerm).cloneComponents())           // set union

                return SetExt.make(set, memory)
            }
            if (term1 is SetInt && term2 is SetInt) {
                set = TreeSet((term1 as CompoundTerm).cloneComponents())
                set.retainAll((term2 as CompoundTerm).cloneComponents())        // set intersection

                return SetInt.make(set, memory)
            }
            if (term1 is IntersectionInt) {
                set = TreeSet((term1 as CompoundTerm).cloneComponents())
                if (term2 is IntersectionInt) {
                    set.addAll((term2 as CompoundTerm).cloneComponents())
                } // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
                else {
                    set.add(term2.clone() as Term)
                }                          // (|,(|,P,Q),R) = (|,P,Q,R)
            } else if (term2 is IntersectionInt) {
                set = TreeSet((term2 as CompoundTerm).cloneComponents())
                set.add(term1.clone() as Term)   // (|,R,(|,P,Q)) = (|,P,Q,R)
            } else {
                set = TreeSet()
                set.add(term1.clone() as Term)
                set.add(term2.clone() as Term)
            }
            return make(set, memory)
        }

        /**
         * Try to make a new IntersectionExt. Called by StringParser.
         *
         * @param argList The list of components
         * @param memory  Reference to the memory
         * @return the Term generated from the arguments
         */
    @JvmStatic    fun make(argList: List<Term>?, memory: Memory): Term {
            val set = TreeSet(argList) // sort/merge arguments

            return make(set, memory)
        }

        /**
         * Try to make a new compound from a set of components. Called by the public make methods.
         *
         * @param set    a set of Term as compoments
         * @param memory Reference to the memory
         * @return the Term generated from the arguments
         */
   @JvmStatic     fun make(set: TreeSet<Term>, memory: Memory): Term {
            if (set.size == 1) {
                return set.first()
            }                         // special case: single component

            val argument = ArrayList(set)
            val name: String? = makeCompoundName(Symbols.INTERSECTION_INT_OPERATOR, argument)
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: IntersectionInt(argument)
        }
    }
}