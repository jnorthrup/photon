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
    private int type;
    @Nullable
    private int [] index;
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
    public TermLink(Term t, int p, int... indices) {
        setTarget(t);
        setType(p);
        assert (getType() % 2 == 0); // template types all point to compound, though the target is component
        if (getType() == TermLinkConstants.COMPOUND_CONDITION) {  // the first index is 0 by default
            setIndex(new int[indices.length + 1]);
            getIndex()[0] = 0;
            for (int i = 0; i < indices.length; i++) {
                getIndex()[i + 1] = (short) indices[i];
            }
        } else {
            setIndex(new int[indices.length]);
            int bound = getIndex().length;
            for (int i = 0; i < bound; i++) {
                getIndex()[i] = (short) indices[i];
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
        setKey(s);
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
        setTarget(t);
        setType(template.getType());
        if (template.getTarget().equals(t)) {
            setType(getType() - 1);     // point to component
        }
        setIndex(template.getIndices());
//        setKey();
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * The index of the component in the component list of the compound, may have up to 4 levels
     */
    @Nullable
    public int[] getIndex() {
        return index;
    }

    public void setIndex(@Nullable int[] index) {
        this.index = index;
    }

    public void setTarget(Term target) {
        this.target = target;
    }

    enum   termlink_type     {
        TO_COMPONENT_1(" @("),
                TO_COMPONENT_2(")_ "),
                TO_COMPOUND_1(" _@("),
                TO_COMPOUND_2(") "),
        ;

        private final String sym;

        termlink_type(String sym) {

            this.sym = sym;
        }
    }
    /**
     * Set the key of the link
     */
    public   String getKey() {
        if (this.key == null) {
            String key, at1, at2;
            if ((getType() % 2) == 1) {  // to component
                at1 = termlink_type.TO_COMPONENT_1.sym;
                at2 = termlink_type.TO_COMPONENT_2.sym;
            } else {                // to compound
                at1 = termlink_type.TO_COMPOUND_1.sym;
                at2 = termlink_type.TO_COMPOUND_2.sym;
            }
            var in = "T" + getType();
            if (getIndex() != null) {
                for (int value : getIndex()) {
                    in += "-" + (value + 1);
                }
            }
            key = at1 + in + at2;
            if (getTarget() != null) {
                key += getTarget();
            }
            return key;
        } else {
            return key;
        }
    }

    /**
     * The linked Term
     */ /**
     * Get the target of the link
     *
     * @return The Term pointed by the link
     */
    public Term getTarget() {
        return target;
    }

    /**
     * The type of link, one of the above
     */ /**
     * Get the link type
     *
     * @return Type of the link
     */
    public int getType() {
        return type;
    }

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public int[] getIndices() {
        return getIndex();
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public  int getIndex(int i) {
        if ((getIndex() != null) && (i < getIndex().length)) {
            return getIndex()[i];
        } else {
            return -1;
        }
    }

}
