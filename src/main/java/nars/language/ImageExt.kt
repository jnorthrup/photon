/*
 * ImageExt.java
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
import nars.io.compound_oper_arity1.IMAGE_EXT_OPERATOR
import nars.io.special_operator.IMAGE_PLACE_HOLDER
import nars.storage.BackingStore
import java.util.*

/**
 * An extension image.
 *
 *
 * B --> (/,P,A,_)) iff (*,A,B) --> P
 *
 *
 * Internally, it is actually (/,A,P)_1, with an index.
 */
class ImageExt : CompoundTerm {
    /**
     * get the index of the relation in the component list
     *
     * @return the index of relation
     */
    /**
     * The index of relation in the component list
     */
    var relationIndex: Short
        private set

    /**
     * Constructor with partial values, called by make
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    private constructor(n: String, arg: List<Term>, index: Short) : super(n, arg) {
        relationIndex = index
    }

    /**
     * Constructor with full values, called by clone
     * @param open       Open variable list
     * @param n          The name of the term
     * @param cs         Component list
     * @param complexity Syntactic complexity of the compound
     * @param index      The index of relation in the component list
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, complexity: Short, index: Int) : super(n, cs, con, complexity) {
        relationIndex = index.toShort()
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageExt
     */
    override fun clone(): Any {
        return ImageExt(name, Util2.cloneList(components) as List<Term>, isConstant, complexity, relationIndex.toInt())
    }

    /**
     * Get the relation term in the Image
     *
     * @return The term representing a relation
     */
    val relation: Term?
        get() = components[relationIndex.toInt()]

    /**
     * Get the other term in the Image
     *
     * @return The term related
     */
    val theOtherComponent: Term?
        get() {
            if (components.size != 2) {
                return null
            }
            return if (relationIndex.toInt() == 0) components[1] else components[0]
        }

    /**
     * override the default in making the name of the current term from existing fields
     *
     * @return the name of the term
     */

    public override fun makeName(): String {
        return Util2.makeImageName(IMAGE_EXT_OPERATOR.sym, components, relationIndex.toInt())
    }

    /**
     * get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return IMAGE_EXT_OPERATOR.sym
    }

    companion object {
        /**
         * Try to make a new ImageExt. Called by StringParser.
         *
         * @param argList The list of components
         * @param memory  Reference to the memory
         * @return the Term generated from the arguments
         */
    @JvmStatic    fun make(argList: List<Term>, memory: BackingStore): Term? {
            if (argList.size < 2) {
                return null
            }
            val relation = argList[0]
            val argument = ArrayList<Term>()
            var index = 0
            for (j in 1 until argList.size) {
                if (argList[j].getName()[0] == IMAGE_PLACE_HOLDER.sym) {
                    index = j - 1
                    argument.add(relation)
                } else {
                    argument.add(argList[j])
                }
            }
            return make(argument, index.toShort(), memory)
        }

        /**
         * Try to make an Image from a Product and a relation. Called by the inference rules.
         *
         * @param product  The product
         * @param relation The relation
         * @param index    The index of the place-holder
         * @return A compound generated or a term it reduced to
         */
   @JvmStatic     fun make(product: Product, relation: Term, index: Short, memory: BackingStore): Term {
            if (relation is Product) {
                if (product.size() == 2 && relation.size() == 2) {
                    if (index.toInt() == 0 && product.componentAt(1) == relation.componentAt(1)) { // (/,_,(*,a,b),b) is reduced to a

                        return relation.componentAt(0)
                    }
                    if (index.toInt() == 1 && product.componentAt(0) == relation.componentAt(0)) { // (/,(*,a,b),a,_) is reduced to b

                        return relation.componentAt(1)
                    }
                }
            }
            val argument: MutableList<Term> = product.cloneComponents()
            argument[index.toInt()] = relation
            return make(argument, index, memory)
        }

        /**
         * Try to make an Image from an existing Image and a component. Called by the inference rules.
         *
         * @param oldImage  The existing Image
         * @param component The component to be added into the component list
         * @param index     The index of the place-holder in the new Image
         * @return A compound generated or a term it reduced to
         */
    @JvmStatic    fun make(oldImage: ImageExt, component: Term, index: Int, memory: BackingStore): Term {
            val argList: MutableList<Term> = oldImage.cloneComponents()
            val oldIndex = oldImage.relationIndex.toInt()
            val relation = argList[oldIndex]
            argList[oldIndex] = component
            argList[index] = relation
            return make(argList, index.toShort(), memory)
        }

        /**
         * Try to make a new compound from a set of components. Called by the public make methods.
         *
         * @param argument The argument list
         * @param index    The index of the place-holder in the new Image
         * @return the Term generated from the arguments
         */
   @JvmStatic     fun make(argument: List<Term>, index: Short, memory: BackingStore): Term {
            val name: String = Util2.makeImageName(IMAGE_EXT_OPERATOR.sym, argument, index.toInt())
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: ImageExt(name, argument, index)
        }
    }
}