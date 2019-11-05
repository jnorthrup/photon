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
        open var name: String,


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
    override fun compareTo(that: Term) = (that as? CompoundTerm)
            ?.let { -1 }
            ?: (that as? Variable).let { 1 }
            ?: name.compareTo(that.name)

    @Override
    public override fun clone(): Term {
        return Term(this)
    }

    /**copy ctor */
    constructor(orig: Term) : this(orig.name, orig.complexity, false)

    override fun toString(): String {
        return name
    }
}

