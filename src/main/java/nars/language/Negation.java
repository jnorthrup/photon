/*
 * Negation.java
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

/**
 * A negation of a statement.
 */
public class Negation extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Negation( List<Term> arg) {
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
    private Negation(String n ,  List<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a Negation of one component. Called by the inference rules.
     *
     * @param t      The component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t, Memory memory) {
        if (t instanceof Negation) {
            return ((CompoundTerm) t).cloneComponents().get(0);
        }         // (--,(--,P)) = P
        var argument = new ArrayList<Term>();
        argument.add(t);
        return make(argument, memory);
    }

    /**
     * Try to make a new Negation. Called by StringParser.
     *
     * @param argument The list of components
     * @param memory   Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make( List<Term> argument, Memory memory) {
        if (argument.size() != 1) {
            return null;
        }
        var name = makeCompoundName(Symbols.NEGATION_OPERATOR, argument);
        var t = memory.nameToListedTerm(name);
        return (t != null) ? t : new Negation(argument);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */

    public Object clone() {
        return new Negation(name, ( List<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    public String operator() {
        return Symbols.NEGATION_OPERATOR;
    }
}
