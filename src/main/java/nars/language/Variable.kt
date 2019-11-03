/*
 * Variable.java
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
import java.util.*

/**
 * A variable term, which does not correspond to a concept
 */
class Variable
/**
 * Constructor, from a given variable name
 *
 * @param s A String read from input
 */(s: String) : Term(s) {
    /**
     * Clone a Variable
     *
     * @return The cloned Variable
     */

    override fun clone(): Any {
        return Variable(name)
    }

    /**
     * Get the type of the variable
     *
     * @return The variable type
     */
    val type: Char
        get() = name[0]

    /**
     * A variable is not constant
     *
     * @return false
     */

    override fun isConstant(): Boolean {
        return false
    }

    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */

    override fun getComplexity() =0 as Short

    /**
     * variable terms are listed first alphabetically
     *
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     */

    override fun compareTo(that: Term): Int {
        return if (that is Variable) name.compareTo(that.getName()) else -1
    }

    companion object {
        /**
         * Check whether a string represent a name of a term that contains an
         * independent variable
         *
         * @param n The string name to be checked
         * @return Whether the name contains an independent variable
         */
  @JvmStatic      fun containVarIndep(n: String): Boolean {
            return n.indexOf(Symbols.VAR_INDEPENDENT) >= 0
        }

        /**
         * Check whether a string represent a name of a term that contains a
         * dependent variable
         *
         * @param n The string name to be checked
         * @return Whether the name contains a dependent variable
         */
     @JvmStatic        fun containVarDep(n: String): Boolean {
            return n.indexOf(Symbols.VAR_DEPENDENT) >= 0
        }

        /**
         * Check whether a string represent a name of a term that contains a query
         * variable
         *
         * @param n The string name to be checked
         * @return Whether the name contains a query variable
         */
    @JvmStatic         fun containVarQuery(n: String): Boolean {
            return n.indexOf(Symbols.VAR_QUERY) >= 0
        }

        /**
         * Check whether a string represent a name of a term that contains a
         * variable
         *
         * @param n The string name to be checked
         * @return Whether the name contains a variable
         */
    @JvmStatic         fun containVar(n: String): Boolean {
            return containVarIndep(n) || containVarDep(n) || containVarQuery(n)
        }

        /**
         * To unify two terms
         *
         * @param type The type of variable that can be substituted
         * @param t1   The first term
         * @param t2   The second term
         * @return Whether the unification is possible
         */
     @JvmStatic        fun unify(type: Char, t1: Term, t2: Term): Boolean {
            return unify(type, t1, t2, t1, t2)
        }

        /**
         * To unify two terms
         *
         * @param type      The type of variable that can be substituted
         * @param t1        The first term to be unified
         * @param t2        The second term to be unified
         * @param compound1 The compound containing the first term
         * @param compound2 The compound containing the second term
         * @return Whether the unification is possible
         */
     @JvmStatic        fun unify(type: Char, t1: Term, t2: Term, compound1: Term, compound2: Term): Boolean {
            val map1 = HashMap<Term, Term>()
            val map2 = HashMap<Term, Term>()
            val hasSubs = findSubstitute(type, t1, t2, map1, map2) // find substitution

            if (hasSubs) {
                renameVar(map1, compound1, "-1")
                renameVar(map2, compound2, "-2")
                if (!map1.isEmpty()) {
                    (compound1 as CompoundTerm).applySubstitute(map1)
                }
                if (!map2.isEmpty()) {
                    (compound2 as CompoundTerm).applySubstitute(map2)
                }
            }
            return hasSubs
        }

        /**
         * To recursively find a substitution that can unify two Terms without
         * changing them
         *
         * @param type  The type of Variable to be substituted
         * @param term1 The first Term to be unified
         * @param term2 The second Term to be unified
         * @param map1  The substitution for term1 formed so far
         * @param map2  The substitution for term2 formed so far
         * @return Whether there is a substitution that unifies the two Terms
         */
    @JvmStatic           fun findSubstitute(type: Char, term1: Term, term2: Term,
                                   map1: HashMap<Term, Term>, map2: HashMap<Term, Term>): Boolean {
            val t: Term?
            if (term1 is Variable) {
                val var1 = term1
                t = map1[var1]
                return if (t != null) {    // already mapped

                    findSubstitute(type, t, term2, map1, map2)
                } else {            // not mapped yet

                    if (var1.type == type) {
                        if (term2 is Variable && term2.type == type) {
                            val `var` = Variable(var1.getName() + term2.getName())
                            map1[var1] = `var`  // unify

                            map2[term2] = `var`  // unify
                        } else {
                            map1[var1] = term2  // elimination
                        }
                    } else {    // different type

                        map1[var1] = Variable(var1.getName() + "-1")  // rename
                    }
                    true
                }
            }
            if (term2 is Variable) {
                val var2 = term2
                t = map2[var2]
                return if (t != null) {    // already mapped

                    findSubstitute(type, term1, t, map1, map2)
                } else {            // not mapped yet

                    if (var2.type == type) {
                        map2[var2] = term2  // unify
                    } else {
                        map2[var2] = Variable(var2.getName() + "-2")  // rename
                    }
                    true
                }
            }
            if (term1 is CompoundTerm && term1.javaClass == term2.javaClass) {
                val cTerm1 = term1
                val cTerm2 = term2 as CompoundTerm
                if (cTerm1.size() != cTerm2.size()) {
                    return false
                }
                for (i in 0 until cTerm1.size()) {   // assuming matching order, to be refined in the future

                    val t1: Term = cTerm1.componentAt(i)
                    val t2: Term = cTerm2.componentAt(i)
                    val haveSub = findSubstitute(type, t1, t2, map1, map2)
                    if (!haveSub) {
                        return false
                    }
                }
                return true
            }
            return term1 == term2 // for atomic constant terms
        }

        /**
         * Check if two terms can be unified
         *
         * @param type  The type of variable that can be substituted
         * @param term1 The first term to be unified
         * @param term2 The second term to be unified
         * @return Whether there is a substitution
         */
      @JvmStatic        fun hasSubstitute(type: Char, term1: Term, term2: Term): Boolean {
            return findSubstitute(type, term1, term2, HashMap(), HashMap())
        }

        /**
         * Rename the variables to prepare for unification of two terms
         *
         * @param map    The substitution so far
         * @param term   The term to be processed
         * @param suffix The suffix that distinguish the variables in one premise from those from the other
         */
       @JvmStatic         fun renameVar(map: HashMap<Term, Term>, term: Term, suffix: String) {
            if (term is Variable) {
                val t = map[term]
                if (t == null) {    // new mapped yet

                    map[term] = Variable(term.getName() + suffix)  // rename
                }
            } else if (term is CompoundTerm) {
                // assuming matching order, to be refined in the future

                for (t in term.components) {
                    renameVar(map, t, suffix)
                }
            }
        }
    }
}