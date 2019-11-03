/*
 * SetInt.java
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

import nars.io.Symbols;
import nars.storage.Memory;

import java.util.*;
import java.util.List;
import java.util.TreeSet;

/**
 * An intensionally defined set, which contains one or more instances defining the Term.
 */
public class SetInt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private SetInt( List<Term> arg) {
        super(arg);
    }

    /**
     * constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private SetInt(String n ,  List<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a new set from one component. Called by the inference rules.
     *
     * @param t      The compoment
     * @param memory Reference to the memeory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t, Memory memory) {
        var set = new TreeSet<Term>();
        set.add(t);
        return make(set, memory);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     *
     * @param argList The list of components
     * @param memory  Reference to the memeory
     * @return the Term generated from the arguments
     */
    public static Term make(List<Term> argList, Memory memory) {
        var set = new TreeSet<>(argList); // sort/merge arguments
        return make(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public make methods.
     *
     * @param set    a set of Term as compoments
     * @param memory Reference to the memeory
     * @return the Term generated from the arguments
     */
    public static Term make(TreeSet<Term> set, Memory memory) {
        if (set.isEmpty()) {
            return null;
        }
        var argument = new ArrayList<>(set);
        var name = makeSetName(Symbols.SET_INT_OPENER, argument, Symbols.SET_INT_CLOSER);
        var t = memory.nameToListedTerm(name);
        return (t != null) ? t : new SetInt(argument);
    }

    /**
     * Clone a SetInt
     *
     * @return A new object, to be casted into a SetInt
     */
    public Object clone() {
        return new SetInt(name, ( List<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return "" + Symbols.SET_INT_OPENER;
    }

    /**
     * Check if the compound is communitative.
     *
     * @return true for communitative
     */

    public boolean isCommutative() {
        return true;
    }

    /**
     * Make a String representation of the set, override the default.
     *
     * @return true for communitative
     */

    public String makeName() {
        return makeSetName(Symbols.SET_INT_OPENER, components, Symbols.SET_INT_CLOSER);
    }
}

