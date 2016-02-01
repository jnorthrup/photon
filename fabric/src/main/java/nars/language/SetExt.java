/*
 * SetExt.java
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * An extensionally defined set, which contains one or more instances.
 */
public class SetExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
      * @param arg The component list of the term
     */
    private SetExt(List<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * @param cs Component list
     * @param i Syntactic complexity of the compound
     */
    private SetExt(String n, List<Term> cs, boolean con, int i) {
        super(n, cs, con, i);
    }

    /**
     * Clone a SetExt
     * @return A new object, to be casted into a SetExt
     */
    @Override
    public Object clone() {
        return new SetExt(this.name, cloneList(this.getComponents()), this.isConstant(), this.getComplexity());
    }

    /**
     * Try to make a new set from one component. Called by the inference rules.
     * @param t The compoment
     * @param memory Reference to the memeory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t, Memory memory) {
        Collection<Term> set = new TreeSet<Term>();
        set.add(t);
        return make(set, memory);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory Reference to the memeory
     */
    public static Term make(List<Term> argList, Memory memory) {
        Collection<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return make(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public make methods.
     * @param set a set of Term as compoments
     * @param memory Reference to the memeory
     * @return the Term generated from the arguments
     */
    public static Term make(Collection<Term> set, Memory memory) {
        Term t = null;
        return !set.isEmpty() && null == (t = Memory.nameToListedTerm(memory, makeSetName(Symbols.SET_EXT_OPENER, argumentsToList(set.toArray(set.toArray(new Term[set.size()]))), Symbols.SET_EXT_CLOSER))) ? new SetExt(argumentsToList(set.toArray(set.toArray(new Term[set.size()])))) : t;
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return "" + Symbols.SET_EXT_OPENER;
    }

    /**
     * Check if the compound is communitative.
     * @return true for communitative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }

    /**
     * Make a String representation of the set, override the default.
     * @return true for communitative
     */
    @Override
    public String makeName() {
        return makeSetName(Symbols.SET_EXT_OPENER, this.getComponents(), Symbols.SET_EXT_CLOSER);
    }
}

