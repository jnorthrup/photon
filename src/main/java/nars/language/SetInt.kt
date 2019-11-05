/*
 * SetInt.java
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

//import nars.io.Symbols
import nars.io.compound_delim.SET_INT_CLOSER
import nars.io.compound_delim.SET_INT_OPENER
import nars.storage.BackingStore
import java.util.*

/**
 * An intensionally defined set, which contains one or more instances defining the Term.
 */
class SetInt : CompoundTerm {
    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private constructor(arg: List<Term>) : super(arg)

    /**
     * constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, i:  Int) : super(n, cs, con, i)

    /**
     * Clone a SetInt
     *
     * @return A new object, to be casted into a SetInt
     */
    override fun clone(): Term {
        return SetInt(name, Util2.cloneList(components) as List<Term>, constant, complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return "" + SET_INT_OPENER.sym
    }

    /**
     * Check if the compound is communitative.
     *
     * @return true for communitative
     */

    override var commutative =true

    /**
     * Make a String representation of the set, override the default.
     *
     * @return true for communitative
     */

    override fun makeName(): String {
        return Util2.makeSetName(SET_INT_OPENER.sym, this.components!!, SET_INT_CLOSER.sym)
    }

    companion object {
        /**
         * Try to make a new set from one component. Called by the inference rules.
         *
         * @param t      The compoment
         * @param memory Reference to the memeory
         * @return A compound generated or a term it reduced to
         */
        @JvmStatic    fun make(t: Term, memory: BackingStore): Term? {
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
        @JvmStatic  fun make(argList: List<Term>?, memory: BackingStore): Term? {
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
        @JvmStatic    fun make(set: SortedSet <Term>, memory: BackingStore): Term? {
            if (!set.isEmpty()) {
                val argument = ArrayList(set)
                val name  = Util2.makeSetName(SET_INT_OPENER.sym, argument, SET_INT_CLOSER.sym)
                val t = memory.nameToListedTerm(name)
                return t ?: SetInt(argument)
            }
            return null
        }
    }
}