/*
 * Term.java
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
package nars.language

/**
 * Term is the basic component of Narsese, and the object of processing in NARS.
 *
 *
 * A Term may have an associated Concept containing relations with other Terms.
 * It is not linked in the Term, because a Concept may be forgot while the Term
 * exists. Multiple objects may represent the same Term.
 */
open class Term(
        /**
         * The same as getName by default, used in display only.
         *
         * @return The name of the term as a String
         */
        public open var name: String,


        /**
         * The syntactic complexity, for constant atomic Term, is 1.
         *
         * @return The complexity of the term, an integer
         */


        open var complexity: Int = 1,
        /**
         * Check whether the current Term can name a Concept.
         *
         * @return A Term is constant by default
         */
        open var constant: Boolean = true) : Cloneable, Comparable<Term> {
    override fun compareTo(that: Term): Int {
        var result: Int
        if (that is CompoundTerm) {
            result = -1
        } else if (that is Variable) {
            result = 1
        } else {
            result = name.compareTo(that.name)
        }
        return result

    }

    @Override
    public override fun clone(): Term {
        return Term(this)
    }

    /**copy ctor */
    constructor(orig: Term) : this(orig.name, orig.complexity, false)
//     * A Term is identified uniquely by its name, a sequence of characters in a
//     * given alphabet (ASCII or Unicode)
//     */
//    protected String name;
//
//    /**
//     * Default constructor that build an internal Term
//     */
//    protected Term() {
//    }
//
//    /**
//     * Constructor with a given name
//     *
//     * @param name A String as the name of the Term
//     */
//    public Term(String name) { this.name = name; }
//
//    public Term(Term term) { this(term.getName()); }
//
//    /**
//     * Reporting the name of the current Term.
//     *
//     * @return The name of the term as a String
//     */
//    public String getName() {
//        return name;
//    }
//
//    /**
//     * Make a new Term with the same name.
//     *
//     * @return The new Term
//     */
//

//
//    /**
//     * Equal terms have identical name, though not necessarily the same
//     * reference.
//     *
//     * @param that The Term to be compared with the current Term
//     * @return Whether the two Terms are equal
//     */
//
//    public boolean equals(Object that) {
//        return (that instanceof Term) && name.equals(((Term) that).getName());
//    }
//
//    /**
//     * Produce a hash code for the term
//     *
//     * @return An integer hash code
//     */
//
//    public int hashCode() {
//        return (Optional.ofNullable(name).map(String::hashCode).orElse(7));
//    }
//
//    public boolean isConstant() {
//        return true;
//    }
//
//    public void renameVariables() {
//    }
//
//
//
//
//
//
//    public short getComplexity() {
//        return (short) 1  ;
//    }
//

//    /**
//     * Recursively check if a compound contains a term
//     *
//     * @param target The term to be searched
//     * @return Whether the two have the same content
//     */
//    public boolean containTerm(Term target) {
//        return equals(target);
//    }
//
//
override fun toString():String {
        return name;
    }
}

