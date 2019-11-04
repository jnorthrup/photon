/*
 * TermLink.java
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
package nars.entity;

import nars.io.Symbols;
import nars.language.Term;
import org.jetbrains.annotations.Nullable;

/**
 * A link between a compound term and a component term
 * <p>
 * A TermLink links the current Term to a target Term, which is
 * either a component of, or compound made from, the current term.
 * <p>
 * Neither of the two terms contain variable shared with other terms.
 * <p>
 * The index value(s) indicates the location of the component in the compound.
 * <p>
 * This class is mainly used in inference.RuleTable to dispatch premises to inference rules
 */
public class TermLink extends   ItemIdentity implements TermLinkConstants {
    private String key = null;
    /**
     * The type of link, one of the above
     */
    protected short type;
    /**
     * The index of the component in the component list of the compound, may have up to 4 levels
     */
    @Nullable
    protected short[] index;
    /**
     * The linked Term
     */
    private Term target;

    /**
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     *
     * @param t       Target Term
     * @param p       Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLink(Term t, short p, int... indices) {
        target = t;
        type = p;
        assert (type % 2 == 0); // template types all point to compound, though the target is component
        if (type == TermLinkConstants.COMPOUND_CONDITION) {  // the first index is 0 by default
            index = new short[indices.length + 1];
            index[0] = 0;
            for (int i = 0; i < indices.length; i++) {
                index[i + 1] = (short) indices[i];
            }
        } else {
            index = new short[indices.length];
            int bound = index.length;
            for (int i = 0; i < bound; i++) {
                index[i] = (short) indices[i];
            }
        }
    }

    /**
     * called from TaskLink
     *
     * @param s The key of the TaskLink
     * @param v The budget value of the TaskLink
     */
    protected TermLink(String s, BudgetValue v) {
        super(  v);
        key=s ;
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     *
     * @param t        Target Term
     * @param template TermLink template previously prepared
     * @param v        Budget value of the link
     */
    public TermLink(Term t, TermLink template, BudgetValue v) {
        super(  v);
        target = t;
        type = template.getType();
        if (template.getTarget().equals(t)) {
            type--;     // point to component
        }
        index = template.getIndices();
//        setKey();
    }

    /**
     * Set the key of the link
     */
    public   String getKey() {
        if(this.key!=null)return key;
        String key,at1, at2;
        if ((type % 2) == 1) {  // to component
            at1 = Symbols.TO_COMPONENT_1;
            at2 = Symbols.TO_COMPONENT_2;
        } else {                // to compound
            at1 = Symbols.TO_COMPOUND_1;
            at2 = Symbols.TO_COMPOUND_2;
        }
        var in = "T" + type;
        if (index != null) {
            for (short value : index) {
                in += "-" + (value + 1);
            }
        }
        key = at1 + in + at2;
        if (target != null) {
            key += target;
        }
        return key;
    }

    /**
     * Get the target of the link
     *
     * @return The Term pointed by the link
     */
    public Term getTarget() {
        return target;
    }

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    public short getType() {
        return type;
    }

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public short[] getIndices() {
        return index;
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public short getIndex(int i) {
        if ((index != null) && (i < index.length)) {
            return index[i];
        } else {
            return -1;
        }
    }
}
