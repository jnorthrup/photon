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

import java.util.ArrayList;
import java.util.List;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A Statement about a Similarity relation.
 */
public class Similarity extends Statement {

    /**
     * Constructor with partial values, called by make
     * @param arg The component list of the term
     */
    private Similarity(List<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * @param cs Component list
     * @param i Syntactic complexity of the compound
     */
    private Similarity(String n, List<Term> cs, boolean con, int i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     * @return A new object, to be casted into a Similarity
     */
    public Object clone() {
        return new Similarity(name, (ArrayList<Term>) cloneList(getComponents()), isConstant(), getComplexity());
    }

    /**
     * Try to make a new compound from two components. Called by the inference rules.
     * @param subject The first compoment
     * @param predicate The second compoment
     * @param memory Reference to the memeory
     * @return A compound generated or null
     */
    public static Similarity make(Term subject, Term predicate, Memory memory) {
        if (!invalidStatement(subject, predicate)) {
            if (subject.compareTo(predicate) <= 0) {
                String name = makeStatementName(subject, Symbols.SIMILARITY_RELATION, predicate);
                Term t = Memory.nameToListedTerm(memory, name);
                if (t == null) {
                    return new Similarity(argumentsToList(subject, predicate));
                }
                return (Similarity) t;
            } else {
                return make(predicate, subject, memory);
            }
        } else {
            return null;
        }
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.SIMILARITY_RELATION;
    }

    /**
     * Check if the compound is communitative.
     * @return true for communitative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
