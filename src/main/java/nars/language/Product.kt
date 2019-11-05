/*
 * Product.java
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
import nars.io.compound_oper_arity1.PRODUCT_OPERATOR
import nars.storage.BackingStore

/**
 * A Product is a sequence of terms.
 */
class Product : CompoundTerm {
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
     * @param n          The name of the term
     * @param cs         Component list
     * @param open       Open variable list
     * @param complexity Syntactic complexity of the compound
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, complexity:  Int) : super(n, cs, con, complexity)

    /**
     * Clone a Product
     *
     * @return A new object, to be casted into an ImageExt
     */
    override fun clone(): Term {
        return Product(name, Util2.cloneList(components) as List<Term>, constant, complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return PRODUCT_OPERATOR.sym
    }

    companion object {
        /**
         * Try to make a new compound. Called by StringParser.
         *
         * @param argument The list of components
         * @param memory   Reference to the memeory
         * @return the Term generated from the arguments
         */
        @JvmStatic   fun make(argument: List<Term>, memory: BackingStore): Term {
            val name  = Util2.makeCompoundName(PRODUCT_OPERATOR.sym, argument)
            val t = memory.nameToListedTerm(name)
            return t ?: Product(argument)
        }

        /**
         * Try to make a Product from an ImageExt/ImageInt and a component. Called by the inference rules.
         *
         * @param image     The existing Image
         * @param component The component to be added into the component list
         * @param index     The index of the place-holder in the new Image -- optional parameter
         * @param memory    Reference to the memeory
         * @return A compound generated or a term it reduced to
         */
        @JvmStatic     fun make(image: CompoundTerm, component: Term, index: Int, memory: BackingStore): Term {
            val argument: MutableList<Term> = image.cloneComponents()
            argument[index] = component
            return make(argument, memory)
        }
    }
}