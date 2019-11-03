/*
 * SetExt.java
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
 * An extensionally defined set, which contains one or more instances.
 */
class SetExt : CompoundTerm {
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
     * Clone a SetExt
     *
     * @return A new object, to be casted into a SetExt
     */
    override fun clone(): Any {
        return SetExt(name, cloneList(components) as List<Term>, isConstant(), complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return "" + Symbols.SET_EXT_OPENER
    }

    /**
     * Check if the compound is communitative.
     *
     * @return true for communitative
     */

    override fun isCommutative(): Boolean {
        return true
    }

    /**
     * Make a String representation of the set, override the default.
     *
     * @return true for communitative
     */

    public override fun makeName(): String {
        return makeSetName(Symbols.SET_EXT_OPENER, components, Symbols.SET_EXT_CLOSER)
    }

    companion object {
        /**
         * Try to make a new set from one component. Called by the inference rules.
         *
         * @param t      The compoment
         * @param memory Reference to the memeory
         * @return A compound generated or a term it reduced to
         */
    @JvmStatic       fun make(t: Term, memory: Memory): Term? {
            val set = TreeSet<Term>()
            set.add(t)
            return make(set, memory)
        }

        /**
         * Try to make a new SetExt. Called by StringParser.
         *
         * @param argList The list of components
         * @param memory  Reference to the memeory
         * @return the Term generated from the arguments
         */
     @JvmStatic      fun make(argList: List<Term>?, memory: Memory): Term? {
            val set = TreeSet(argList) // sort/merge arguments

            return make(set, memory)
        }

        /**
         * Try to make a new compound from a set of components. Called by the public make methods.
         *
         * @param set    a set of Term as compoments
         * @param memory Reference to the memeory
         * @return the Term generated from the arguments
         */
    @JvmStatic    fun make(set: TreeSet<Term>, memory: Memory): Term? {
            if (set.isEmpty()) {
                return null
            }
            val argument = ArrayList(set)
            val name: String? = makeSetName(Symbols.SET_EXT_OPENER, argument, Symbols.SET_EXT_CLOSER)
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: SetExt(argument)
        }
    }
}