/*
 * Similarity.java
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
 * A Statement about a Similarity relation.
 */
public class Similarity extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Similarity( List<Term> arg) {
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
    private Similarity(String n ,  List<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Try to make a new compound from two components. Called by the inference rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Similarity make(Term subject, Term predicate, Memory memory) {
        if (!invalidStatement(subject, predicate)) {
            if (subject.compareTo(predicate) > 0) {
                return make(predicate, subject, memory);
            }
            var name = makeStatementName(subject, Symbols.SIMILARITY_RELATION, predicate);
            var t = memory.nameToListedTerm(name);
            if (t == null) {
                var argument = argumentsToList(subject, predicate);
                return new Similarity(argument);
            }
            return (Similarity) t;
        }
        return null;
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Similarity
     */

    @Override
    public Object clone() {
        return new Similarity(name, ( List<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */

    @Override
    public String operator() {
        return Symbols.SIMILARITY_RELATION;
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */

    @Override
    public boolean isCommutative() {
        return true;
    }
}
