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
package nars.language;

import nars.data.TermStruct;
import nars.io.Symbols;

import java.util.HashMap;
import java.util.Map;

/**
 * A variable term, which does not correspond to a concept
 */
public class Variable extends Term {

    /**
     * Constructor, from a given variable name
     * @param s A String read from input
     */
    public Variable(String s) {
        super(s);
    }

    /**
     * Clone a Variable
     * @return The cloned Variable
     */
    @Override
    public Object clone() {
        return new Variable(name);
    }

    /**
     * Get the type of the variable
     * @return The variable type
     */
    public char getType() {
        return name.charAt(0);
    }

    /**
     * A variable is not constant
     * @return false
     */
    @Override
    public boolean isConstant() {
        return false;
    }

    /**
     * Check whether a string represent a name of a term that contains an independent variable
     * @param n The string name to be checked
     * @return Whether the name contains an independent variable
     */
    public static boolean containVarIndep(String n) {
        return n.indexOf(Symbols.VAR_INDEPENDENT) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a dependent variable
     * @param n The string name to be checked
     * @return Whether the name contains a dependent variable
     */
    public static boolean containVarDep(String n) {
        return n.indexOf(Symbols.VAR_DEPENDENT) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a query variable
     * @param n The string name to be checked
     * @return Whether the name contains a query variable
     */
    public static boolean containVarQuery(String n) {
        return n.indexOf(Symbols.VAR_QUERY) >= 0;
    }

    /**
     * Check whether a string represent a name of a term that contains a variable
     * @param n The string name to be checked
     * @return Whether the name contains a variable
     */
    public static boolean containVar(String n) {
        return containVarIndep(n) || containVarDep(n) || containVarQuery(n);
    }

    /**
     * To unify two terms
     * @param type The type of variable that can be substituted
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether the unification is possible
     */
    public static boolean unify(char type, Term t1, TermStruct t2) {
        return unify(type, t1, t2, t1, t2);
    }

    /**
     * To unify two terms
     * @param type The type of variable that can be substituted
     * @param t1 The first term to be unified
     * @param t2 The second term to be unified
     * @param compound1 The compound contermaining the first term
     * @param compound2 The compound contermaining the second term
     * @return Whether the unification is possible
     */
    public static boolean unify(char type, TermStruct t1, TermStruct t2, Term compound1, TermStruct compound2) {
         Map<Term, Term> map1 = new HashMap<>();
         Map<Term, Term> map2 = new HashMap<>();
        boolean hasSubs = Expr3.INSTANCE.findSubstitute(type, t1, t2, map1, map2); // find substitution
        if (hasSubs) {
            if (!map1.isEmpty()) {
                ((CompoundTerm) compound1).applySubstitute(map1);
            }
            if (!map2.isEmpty()) {
                ((CompoundTerm) compound2).applySubstitute(map2);
            }
        }
        return hasSubs;
    }


}
