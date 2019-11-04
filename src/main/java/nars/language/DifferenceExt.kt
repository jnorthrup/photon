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

//import nars.io.Symbols
import nars.io.compound_oper_arity1
import nars.io.compound_oper_arity1.*
import nars.storage.BackingStore
import java.util.*

/**
 * A compound term whose extension is the difference of the extensions of its components
 */
class DifferenceExt : CompoundTerm {
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
    private constructor(n: String, cs: List<Term>, con: Boolean, i: Int) : super(n, cs, con, i.toShort())

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a DifferenceExt
     */
    override fun clone(): Any {
        return DifferenceExt(name, Util2.cloneList(components) as List<Term>, isConstant, complexity as Int)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return DIFFERENCE_EXT_OPERATOR.sym
    }

    companion object {
        /**
         * Try to make a new DifferenceExt. Called by StringParser.
         *
         * @param argList The list of components
         * @param memory  Reference to the memory
         * @return the Term generated from the arguments
         */
   @JvmStatic         fun make(argList: List<Term>, memory: BackingStore): Term? {
            if (argList.size == 1) { // special case from CompoundTerm.reduceComponent

                return argList[0]
            }
            if (argList.size != 2) {
                return null
            }
            val name: String? = Util2.makeCompoundName(DIFFERENCE_EXT_OPERATOR.sym, argList)
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: DifferenceExt(argList)
        }

        /**
         * Try to make a new compound from two components. Called by the inference rules.
         *
         * @param t1     The first compoment
         * @param t2     The second compoment
         * @param memory Reference to the memory
         * @return A compound generated or a term it reduced to
         */
  @JvmStatic      fun make(t1: Term, t2: Term, memory: BackingStore): Term? {
            if (t1 == t2) {
                return null
            }
            if (t1 is SetExt && t2 is SetExt) {
                val set = TreeSet((t1 as CompoundTerm).cloneComponents())
                set.removeAll((t2 as CompoundTerm).cloneComponents())           // set difference

                return SetExt.make(set, memory)
            }
            val list: ArrayList<Term> = Util2.argumentsToList(t1, t2)
            return make(list, memory)
        }
    }
}