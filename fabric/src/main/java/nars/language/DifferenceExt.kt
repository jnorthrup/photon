/*
 * DifferenceExt.java
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
 * A compound term whose extension is the difference of the extensions of its components
 */
class DifferenceExt : CompoundTerm {

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
     * @return A new object, to be casted into a DifferenceExt
     */
    override fun clone(): Any {
        return DifferenceExt(this.name, CompoundTerm.cloneList(this.components), this.isConstant, this.complexity)
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.DIFFERENCE_EXT_OPERATOR
    }

    companion object {

        /**
         * Try to make a new DifferenceExt. Called by StringParser.
         * @return the Term generated from the arguments
         * *
         * @param argList The list of components
         * *
         * @param memory Reference to the memory
         */
        fun make(argList: List<Term>, memory: Memory): Term? {
            if (1 != argList.size) {
                if (2 == argList.size) {
                    val name = CompoundTerm.makeCompoundName(Symbols.DIFFERENCE_EXT_OPERATOR, argList)
                    val t = Memory.nameToListedTerm(memory, name)
                    return t ?: DifferenceExt(argList)
                }
                return null
            }
            return argList[0]
        }

        /**
         * Try to make a new compound from two components. Called by the inference rules.
         * @param t1 The first compoment
         * *
         * @param t2 The second compoment
         * *
         * @param memory Reference to the memory
         * *
         * @return A compound generated or a term it reduced to
         */
        fun make(t1: Term, t2: Term, memory: Memory): Term? {
            return if (t1 != t2) {
                when {
                    t1 is SetExt && t2 is SetExt -> {
                        val set = TreeSet((t1 as CompoundTerm).cloneComponents())
                        set.removeAll((t2 as CompoundTerm).cloneComponents())           // set difference
                        return SetExt.make(set, memory)
                    }
                }
                make(CompoundTerm.argumentsToList(t1, t2), memory)
            } else null
        }
    }
}
