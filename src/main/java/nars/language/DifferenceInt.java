/*
 * DifferenceInt.java
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
import java.util.TreeSet;

/**
 * A compound term whose extension is the difference of the intensions of its components
 */
public class DifferenceInt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private DifferenceInt( List<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private DifferenceInt(String n ,  List<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     *
     * @param argList The list of components
     * @param memory  Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make( List<Term> argList, Memory memory) {
        if (argList.size() == 1) { // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        }
        if (argList.size() != 2) {
            return null;
        }
        var name = makeCompoundName(Symbols.DIFFERENCE_INT_OPERATOR, argList);
        var t = memory.nameToListedTerm(name);
        return (t != null) ? t : new DifferenceInt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference rules.
     *
     * @param t1     The first compoment
     * @param t2     The second compoment
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t1, Term t2, Memory memory) {
        if (t1.equals(t2)) {
            return null;
        }
        if ((t1 instanceof SetInt) && (t2 instanceof SetInt)) {
            var set = new TreeSet<>(((CompoundTerm) t1).cloneComponents());
            set.removeAll(((CompoundTerm) t2).cloneComponents());           // set difference
            return SetInt.make(set, memory);
        }
        var list = argumentsToList(t1, t2);
        return make(list, memory);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a DifferenceInt
     */
    @Override
    public Object clone() {
        return new DifferenceInt(name, ( List<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.DIFFERENCE_INT_OPERATOR;
    }
}


