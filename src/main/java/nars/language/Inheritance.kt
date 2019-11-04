/*
 * Inheritance.java
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
import nars.io.builtin_relation_arity3.INHERITANCE_RELATION
import nars.storage.BackingStore
import java.util.*

/**
 * A Statement about an Inheritance relation.
 */
class Inheritance : Statement {
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
     * @return A new object, to be casted into a SetExt
     */
    override fun clone(): Any {
        return Inheritance(name, Util2.cloneList(components) as List<Term>, isConstant, complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return INHERITANCE_RELATION.sym
    }

    companion object {
        /**
         * Try to make a new compound from two components. Called by the inference rules.
         *
         * @param subject   The first compoment
         * @param predicate The second compoment
         * @param memory    Reference to the memory
         * @return A compound generated or null
         */
        fun make(subject: Term?, predicate: Term?, memory: BackingStore): Inheritance? {
            if (invalidStatement(subject!!, predicate!!)) {
                return null
            }
            val name = makeStatementName(subject, INHERITANCE_RELATION.sym, predicate)
            val t: Term? = memory.nameToListedTerm(name)
            if (t != null) {
                return t as Inheritance
            }
            val argument: ArrayList<Term> = Util2.argumentsToList(subject, predicate)
            return Inheritance(argument)
        }
    }
}