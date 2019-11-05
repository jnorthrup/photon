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

//import nars.io.Symbols
import nars.io.builtin_relation_arity3.*
import nars.io.compound_delim
import nars.storage.BackingStore

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
    protected constructor(arg: List<Term?>?) : super(arg)


    /**
     * Constructor with full values, called by clone
     *
     * @param n   The nameStr of the term
     * @param cs  Component list
     * @param con Constant indicator
     * @param i   Syntactic complexity of the compound
     */
    protected constructor(n: String?, cs: List<Term>?, con: Boolean, i: Int) : super(n, cs, con, i)

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
        get() = components!![0]

    /**
     * Return the second component of the statement
     *
     * @return The second component
     */
    val predicate: Term
        get() = components!![1]

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
        @JvmStatic
        fun make(relation: String, subject: Term, predicate: Term, memory: BackingStore): Statement? {
            if (!invalidStatement(subject, predicate)) {
                return when (relation) {
                    INHERITANCE_RELATION.sym -> Inheritance.make(subject, predicate, memory)
                    SIMILARITY_RELATION.sym -> Similarity.make(subject, predicate, memory)
                    INSTANCE_RELATION.sym -> Instance.make(subject, predicate, memory)
                    PROPERTY_RELATION.sym -> Property.make(subject, predicate, memory)
                    INSTANCE_PROPERTY_RELATION.sym -> InstanceProperty.make(subject, predicate, memory)
                    IMPLICATION_RELATION.sym -> Implication.make(subject, predicate, memory)
                    else -> if (relation == EQUIVALENCE_RELATION.sym) {
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
        @JvmStatic
        fun make(statement: Statement, subj: Term, pred: Term, memory: BackingStore) =
                when (statement) {
                    is Inheritance -> {
                        Inheritance.make(subj, pred, memory)
                    }
                    is Similarity -> {
                        Similarity.make(subj, pred, memory)
                    }
                    is Implication -> {
                        Implication.make(subj, pred, memory)
                    }
                    else -> if (statement is Equivalence) {
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
        @JvmStatic
        fun makeSym(statement: Statement, subj: Term, pred: Term, memory: BackingStore): Statement? {
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
        @JvmStatic
        fun isRelation(s0: String): Boolean {
            val s = s0.trim { it <= ' ' }
            return if (s.length != 3) {
                false
            } else listOf(INHERITANCE_RELATION.sym, SIMILARITY_RELATION.sym, INSTANCE_RELATION.sym, PROPERTY_RELATION.sym, INSTANCE_PROPERTY_RELATION.sym, IMPLICATION_RELATION.sym, EQUIVALENCE_RELATION.sym).contains(s)
        }

        /**
         * Default method to make the nameStr of an image term from given fields
         *
         * @param subject   The first component
         * @param predicate The second component
         * @param relation  The relation operator
         * @return The nameStr of the term
         */
        @JvmStatic
        protected fun makeStatementName(subject: Term, relation: Any, predicate: Term): String {
            val nameStr = StringBuilder()
            nameStr.append(compound_delim.STATEMENT_OPENER.sym)
            nameStr.append(subject.name)
            nameStr.append(' ').append(relation).append(' ')
            nameStr.append(predicate.name)
            nameStr.append(compound_delim.STATEMENT_CLOSER.sym)
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
        @JvmStatic
        fun invalidStatement(subject: Term, predicate: Term): Boolean {
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
                val t11 = subject.subject
                val t12 = subject.predicate
                val t21 = predicate.subject
                val t22 = predicate.predicate
                if (t11 == t22 && t12 == t21) {
                    return true
                }
            }
            return false
        }
    }
}