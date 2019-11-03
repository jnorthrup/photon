/*
 * Statement.java
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

/**
 * A statement is a compound term, consisting of a subject, a predicate,
 * and a relation symbol in between. It can be of either first-order or higher-order.
 */
abstract class Statement : CompoundTerm {
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected constructor(arg: List<Term?>?) : super(arg) {}

    /**
     * Default constructor
     */
    protected constructor() {}

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The nameStr of the term
     * @param cs  Component list
     * @param con Constant indicator
     * @param i   Syntactic complexity of the compound
     */
    protected constructor(n: String?, cs: List<Term?>?, con: Boolean, i: Short) : super(n, cs, con, i) {}

    /**
     * Override the default in making the nameStr of the current term from existing fields
     *
     * @return the nameStr of the term
     */

    override fun makeName(): String {
        return makeStatementName(subject, operator(), predicate)
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     *
     *
     * Minimum requirement: the two terms cannot be the same, or containing each other as component
     *
     * @return Whether The Statement is invalid
     */
    fun invalid(): Boolean {
        return invalidStatement(subject, predicate)
    }

    /**
     * Return the first component of the statement
     *
     * @return The first component
     */
    val subject: Term
        get() = components[0]

    /**
     * Return the second component of the statement
     *
     * @return The second component
     */
    val predicate: Term
        get() = components[1]

    companion object {
        /**
         * Make a Statement from String, called by StringParser
         *
         * @param relation  The relation String
         * @param subject   The first component
         * @param predicate The second component
         * @param memory    Reference to the memory
         * @return The Statement built
         */
      @JvmStatic  fun make(relation: String, subject: Term, predicate: Term, memory: Memory ): Statement? {
            if (!invalidStatement(subject, predicate)) {
                when (relation) {
                    Symbols.INHERITANCE_RELATION -> return Inheritance.make(subject, predicate, memory)
                    Symbols.SIMILARITY_RELATION -> return Similarity.make(subject, predicate, memory)
                    Symbols.INSTANCE_RELATION -> return Instance.make(subject, predicate, memory)
                    Symbols.PROPERTY_RELATION -> return Property.make(subject, predicate, memory)
                    Symbols.INSTANCE_PROPERTY_RELATION -> return InstanceProperty.make(subject, predicate, memory)
                    Symbols.IMPLICATION_RELATION -> return Implication.make(subject, predicate, memory)
                    else -> return if (relation == Symbols.EQUIVALENCE_RELATION) {
                        Equivalence.make(subject, predicate, memory)
                    } else null
                }
            }
            return null
        }

        /**
         * Make a Statement from given components, called by the rules
         *
         * @param subj      The first component
         * @param pred      The second component
         * @param statement A sample statement providing the class type
         * @param memory    Reference to the memory
         * @return The Statement built
         */
      @JvmStatic      fun make(statement: Statement , subj: Term , pred: Term, memory: Memory): Statement? {
            if (statement is Inheritance) {
                return Inheritance.make(subj, pred, memory)
            }
            if (statement is Similarity) {
                return Similarity.make(subj, pred, memory)
            }
            if (statement is Implication) {
                return Implication.make(subj, pred, memory)
            }
            return if (statement is Equivalence) {
                Equivalence.make(subj, pred, memory)
            } else null
        }

        /**
         * Make a symmetric Statement from given components and temporal information, called by the rules
         *
         * @param statement A sample asymmetric statement providing the class type
         * @param subj      The first component
         * @param pred      The second component
         * @param memory    Reference to the memory
         * @return The Statement built
         */
     @JvmStatic       fun makeSym(statement: Statement, subj: Term, pred: Term, memory: Memory): Statement? {
            if (statement is Inheritance) {
                return Similarity.make(subj, pred, memory)
            }
            return if (statement is Implication) {
                Equivalence.make(subj, pred, memory)
            } else null
        }

        /**
         * Check Statement relation symbol, called in StringPaser
         *
         * @param s0 The String to be checked
         * @return if the given String is a relation symbol
         */
     @JvmStatic       fun isRelation(s0: String): Boolean {
            val s = s0.trim { it <= ' ' }
            return if (s.length != 3) {
                false
            } else listOf(Symbols.INHERITANCE_RELATION, Symbols.SIMILARITY_RELATION, Symbols.INSTANCE_RELATION, Symbols.PROPERTY_RELATION, Symbols.INSTANCE_PROPERTY_RELATION, Symbols.IMPLICATION_RELATION, Symbols.EQUIVALENCE_RELATION).contains(s)
        }

        /**
         * Default method to make the nameStr of an image term from given fields
         *
         * @param subject   The first component
         * @param predicate The second component
         * @param relation  The relation operator
         * @return The nameStr of the term
         */
     @JvmStatic       protected fun makeStatementName(subject: Term, relation: String?, predicate: Term): String {
            val nameStr = StringBuilder()
            nameStr.append(Symbols.STATEMENT_OPENER)
            nameStr.append(subject.getName())
            nameStr.append(' ').append(relation).append(' ')
            nameStr.append(predicate.getName())
            nameStr.append(Symbols.STATEMENT_CLOSER)
            return nameStr.toString()
        }

        /**
         * Check the validity of a potential Statement. [To be refined]
         *
         *
         * Minimum requirement: the two terms cannot be the same, or containing each other as component
         *
         * @param subject   The first component
         * @param predicate The second component
         * @return Whether The Statement is invalid
         */
    @JvmStatic        fun invalidStatement(subject: Term, predicate: Term): Boolean {
            if (subject == predicate) {
                return true
            }
            if (subject is CompoundTerm && subject.containComponent(predicate)) {
                return true
            }
            if (predicate is CompoundTerm && predicate.containComponent(subject)) {
                return true
            }
            if (subject is Statement && predicate is Statement) {
                val s1 = subject
                val s2 = predicate
                val t11 = s1.subject
                val t12 = s1.predicate
                val t21 = s2.subject
                val t22 = s2.predicate
                if (t11 == t22 && t12 == t21) {
                    return true
                }
            }
            return false
        }
    }
}