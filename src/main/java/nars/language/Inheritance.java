/*
 * Inheritance.java
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
 * A Statement about an Inheritance relation.
 */
public class Inheritance extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Inheritance( List<Term> arg) {
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
    private Inheritance(String n ,  List<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a new compound from two components. Called by the inference rules.
     *
     * @param subject   The first compoment
     * @param predicate The second compoment
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Inheritance make(Term subject, Term predicate, Memory memory) {
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        var name = makeStatementName(subject, Symbols.INHERITANCE_RELATION, predicate);
        var t = memory.nameToListedTerm(name);
        if (t != null) {
            return (Inheritance) t;
        }
        var argument = argumentsToList(subject, predicate);
        return new Inheritance(argument);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a SetExt
     */
    @Override
    public Object clone() {
        return new Inheritance(name, ( List<Term>) cloneList(components), isConstant, complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.INHERITANCE_RELATION;
    }

}

