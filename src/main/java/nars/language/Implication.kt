/*
 * Implication.java
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
 * A Statement about an Inheritance relation.
 */
class Implication : Statement {
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected constructor(arg: List<Term>) : super(arg) {}

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether it is a constant term
     * @param i   Syntactic complexity of the compound
     */
    protected constructor(n: String, cs: List<Term>, con: Boolean, i: Short) : super(n, cs, con, i) {}

    /**
     * Clone an object
     *
     * @return A new object
     */
    override fun clone(): Any {
        return Implication(name, cloneList(components) as List<Term>, isConstant(), complexity)
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    override fun operator(): String {
        return Symbols.IMPLICATION_RELATION
    }

    companion object {
        /**
         * Try to make a new compound from two components. Called by the inference rules.
         *
         * @param subject   The first component
         * @param predicate The second component
         * @param memory    Reference to the memory
         * @return A compound generated or a term it reduced to
         */
        fun make(subject: Term?, predicate: Term?, memory: Memory): Implication? {
            var result: Implication? = null
            if (subject != null && predicate != null) {
                if (subject != null && predicate != null) {
                    if (subject !is Implication && subject !is Equivalence && predicate !is Equivalence) {
                        if (!invalidStatement(subject, predicate)) {
                            val name = makeStatementName(subject, Symbols.IMPLICATION_RELATION, predicate)
                            val t: Term? = memory.nameToListedTerm(name)
                            if (t != null) {
                                result = t as Implication
                            } else if (predicate is Implication) {
                                val oldCondition = predicate.subject
                                if (oldCondition !is Conjunction || !oldCondition.containComponent(subject)) {
                                    val newCondition: Term? = Conjunction.make(subject, oldCondition, memory)
                                    result = make(newCondition, predicate.predicate, memory)
                                }
                            } else {
                                val argument: ArrayList<Term> = argumentsToList(subject, predicate)
                                result = Implication(argument)
                            }
                        }
                    }
                }
            }
            return result
        }
    }
}