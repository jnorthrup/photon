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

import nars.data.TermLinkStruct;
import nars.data.TermStruct;
import nars.io.Symbols;
import nars.language.Term;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

/**
 * A link between a compound term and a component term
 * <p>
 * A TermLink links the current Term to a term Term, which is
 * either a component of, or compound made from, the current term.
 * <p>
 * Neither of the two terms contain variable shared with other terms.
 * <p>
 * The index value(s) indicates the location of the component in the compound.
 * <p>
 * This class is mainly used in inference.RuleTable to dispatch premises to inference rules
 */
public class TermLink extends Item implements TermLinkStruct {
    /**
     * At C, point to C; TaskLink only
     */
    public static final short SELF = 0;
    /**
     * At (&&, A, C), point to C
     */
    public static final short COMPONENT = 1;
    /**
     * At C, point to (&&, A, C)
     */
    public static final short COMPOUND = 2;
    /**
     * At <C --> A>, point to C
     */
    public static final short COMPONENT_STATEMENT = 3;
    /**
     * At C, point to <C --> A>
     */
    public static final short COMPOUND_STATEMENT = 4;
    /**
     * At <(&&, C, B) ==> A>, point to C
     */
    public static final short COMPONENT_CONDITION = 5;
    /**
     * At C, point to <(&&, C, B) ==> A>
     */
    public static final short COMPOUND_CONDITION = 6;
    /**
     * At C, point to <(*, C, B) --> A>; TaskLink only
     */
    public static final short TRANSFORM = 8;
    private Term term;
    private int type;
    private List<Integer> index;

    /**
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     *
     * @param term    Target Term
     * @param typ     Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLink(Term term, short typ, IntStream indices) {
        setTerm(term);
        setType(typ);
        assert type % 2 == 0; // template types all point to compound, though the term is component
        setIndex(new CopyOnWriteArrayList<>());
        if (type == COMPOUND_CONDITION) {  // the first index is 0 by default
            index.add(0);

        }
        if (null != indices) indices.forEachOrdered(a -> {
            index.add(a);
        });
    }

    /**
     * called from TaskLink
     *
     * @param s The key of the TaskLink
     * @param v The budget value of the TaskLink
     */
    protected TermLink(String s, BudgetValue v) {
        super(s, v);
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
        super(t.getName(), v);
        setTerm(t);
        setType(template.type);
        if (template.term.equals(t)) {
            setType(type - 1);     // point to component
        }
        setIndex(template.getIndex());
        setKey();
    }

    public TermLink(Term t2, short transform, int... j) {
        this(t2, transform, Arrays.stream(j));
    }

    /**
     * Set the key of the link
     */
    protected void setKey() {
        String at1, at2;
        if (type % 2 == 1) {  // to component
            at1 = Symbols.TO_COMPONENT_1;
            at2 = Symbols.TO_COMPONENT_2;
        } else {                // to compound
            at1 = Symbols.TO_COMPOUND_1;
            at2 = Symbols.TO_COMPOUND_2;
        }
        String in = "T" + type;
        if (index != null) {
            for (int i = 0; i < index.size(); i++) {
                in += "-" + (index.get(i) + 1);
            }
        }
        setKey(at1 + in + at2);
        if (term != null) {
            setKey(getKey() + term);
        }
    }

    /** The linked Term */
    /**
     * Get the term of the link
     *
     * @return The Term pointed by the link
     */
    @Override
    public Term getTerm() {
        return term;
    }

    /**
     * The type of link, one of the above
     */

    public void setTerm(Term term) {
        this.term = term;
    }

    @Override
    public void setTerm(TermStruct $term$) {
        term = (Term) $term$;
    }

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }


    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */

    public Integer getIndex(int i) {
        if (index != null && i < index.size()) {
            return index.get(i);
        } else {
            return -1;
        }
    }

    /**
     * The index of the component in the component list of the compound, may have up to 4 levels
     */
    @Override
    public List<Integer> getIndex() {
        return index;
    }

    @Override
    public void setIndex(List<Integer> index) {
        this.index = index;
    }
}
