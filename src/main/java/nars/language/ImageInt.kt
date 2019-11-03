/*
 * ImageInt.java
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
 * An intension image.
 *
 *
 * (\,P,A,_)) --> B iff P --> (*,A,B)
 *
 *
 * Internally, it is actually (\,A,P)_1, with an index.
 */
class ImageInt : CompoundTerm {
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
     * constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    private constructor(n: String, arg: List<Term>, index: Short) : super(n, arg) {
        relationIndex = index
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n          The name of the term
     * @param cs         Component list
     * @param open       Open variable list
     * @param complexity Syntactic complexity of the compound
     * @param index      The index of relation in the component list
     */
    private constructor(n: String, cs: List<Term>, con: Boolean, complexity: Short, index: Short) : super(n, cs, con, complexity) {
        relationIndex = index
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageInt
     */
    override fun clone(): Any {
        return ImageInt(name, cloneList(components) as List<Term>, isConstant, complexity, relationIndex)
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
     * Override the default in making the name of the current term from existing fields
     *
     * @return the name of the term
     */

    public override fun makeName(): String {
        return makeImageName(Symbols.IMAGE_INT_OPERATOR, components, relationIndex.toInt())
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.IMAGE_INT_OPERATOR
    }

    companion object {
        /**
         * Try to make a new ImageExt. Called by StringParser.
         *
         * @param argList The list of components
         * @param memory  Reference to the memory
         * @return the Term generated from the arguments
         */
   @JvmStatic     fun make(argList: List<Term>, memory: Memory): Term? {
            if (argList.size < 2) {
                return null
            }
            val relation = argList[0]
            val argument = ArrayList<Term>()
            var index = 0
            for (j in 1 until argList.size) {
                if (argList[j].getName()[0] == Symbols.IMAGE_PLACE_HOLDER) {
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
         * @param memory   Reference to the memory
         * @return A compound generated or a term it reduced to
         */
    @JvmStatic    fun make(product: Product, relation: Term, index: Short, memory: Memory): Term {
            if (relation is Product) {
                if (product.size() == 2 && relation.size() == 2) {
                    if (index.toInt() == 0 && product.componentAt(1) == relation.componentAt(1)) {// (\,_,(*,a,b),b) is reduced to a

                        return relation.componentAt(0)
                    }
                    if (index.toInt() == 1 && product.componentAt(0) == relation.componentAt(0)) {// (\,(*,a,b),a,_) is reduced to b

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
         * @param memory    Reference to the memory
         * @return A compound generated or a term it reduced to
         */
     @JvmStatic   fun make(oldImage: ImageInt, component: Term, index: Short, memory: Memory): Term {
            val argList: MutableList<Term> = oldImage.cloneComponents()
            val oldIndex = oldImage.relationIndex.toInt()
            val relation = argList[oldIndex]
            argList[oldIndex] = component
            argList[index.toInt()] = relation
            return make(argList, index, memory)
        }

        /**
         * Try to make a new compound from a set of components. Called by the public make methods.
         *
         * @param argument The argument list
         * @param index    The index of the place-holder in the new Image
         * @param memory   Reference to the memory
         * @return the Term generated from the arguments
         */
  @JvmStatic      fun make(argument: List<Term>, index: Short, memory: Memory): Term {
            val name: String = makeImageName(Symbols.IMAGE_INT_OPERATOR, argument, index.toInt())
            val t: Term? = memory.nameToListedTerm(name)
            return t ?: ImageInt(name, argument, index)
        }
    }
}