/*
 * Symbols.java
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
package nars.io

/**
 * The ASCII symbols used in I/O.
 */
interface Symbols {
    companion object {
        /* sentence type and delimitors */
        const val JUDGMENT_MARK = '.'
        const val QUESTION_MARK = '?'
        /* variable type */
        const val VAR_INDEPENDENT = '$'
        const val VAR_DEPENDENT = '#'
        const val VAR_QUERY = '?'
        /* numerical value delimitors, must be different from the Term delimitors */
        const val BUDGET_VALUE_MARK = '$'
        const val TRUTH_VALUE_MARK = '%'
        const val VALUE_SEPARATOR = ';'
        /* CompountTerm delimitors, must use 4 different pairs */
        const val COMPOUND_TERM_OPENER = '('
        const val COMPOUND_TERM_CLOSER = ')'
        const val STATEMENT_OPENER = '<'
        const val STATEMENT_CLOSER = '>'
        const val SET_EXT_OPENER = '{'
        const val SET_EXT_CLOSER = '}'
        const val SET_INT_OPENER = '['
        const val SET_INT_CLOSER = ']'
        /* special characors in argument list */
        const val ARGUMENT_SEPARATOR = ','
        const val IMAGE_PLACE_HOLDER = '_'
        /* CompountTerm operators, length = 1 */
        const val INTERSECTION_EXT_OPERATOR = "&"
        const val INTERSECTION_INT_OPERATOR = "|"
        const val DIFFERENCE_EXT_OPERATOR = "-"
        const val DIFFERENCE_INT_OPERATOR = "~"
        const val PRODUCT_OPERATOR = "*"
        const val IMAGE_EXT_OPERATOR = "/"
        const val IMAGE_INT_OPERATOR = "\\"
        /* CompoundStatement operators, length = 2 */
        const val NEGATION_OPERATOR = "--"
        const val DISJUNCTION_OPERATOR = "||"
        const val CONJUNCTION_OPERATOR = "&&"
        /* built-in relations, length = 3 */
        const val INHERITANCE_RELATION = "-->"
        const val SIMILARITY_RELATION = "<->"
        const val INSTANCE_RELATION = "{--"
        const val PROPERTY_RELATION = "--]"
        const val INSTANCE_PROPERTY_RELATION = "{-]"
        const val IMPLICATION_RELATION = "==>"
        const val EQUIVALENCE_RELATION = "<=>"
        /* experience line prefix */
        const val INPUT_LINE = "IN"
        const val OUTPUT_LINE = "OUT"
        const val PREFIX_MARK = ':'
        const val RESET_MARK = '*'
        const val COMMENT_MARK = '/'
        /* Stamp, display only */
        const val STAMP_OPENER = '{'
        const val STAMP_CLOSER = '}'
        const val STAMP_SEPARATOR = ';'
        const val STAMP_STARTER = ':'
        /* TermLink type, display only */
        const val TO_COMPONENT_1 = " @("
        const val TO_COMPONENT_2 = ")_ "
        const val TO_COMPOUND_1 = " _@("
        const val TO_COMPOUND_2 = ") "
    }
}