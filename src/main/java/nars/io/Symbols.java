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
package nars.io;

/**
 * The ASCII symbols used in I/O.
 */
public interface Symbols {

    /* sentence type and delimitors */
    char JUDGMENT_MARK = '.';
    char QUESTION_MARK = '?';

    /* variable type */
    char VAR_INDEPENDENT = '$';
    char VAR_DEPENDENT = '#';
    char VAR_QUERY = '?';

    /* numerical value delimitors, must be different from the Term delimitors */
    char BUDGET_VALUE_MARK = '$';
    char TRUTH_VALUE_MARK = '%';
    char VALUE_SEPARATOR = ';';

    /* CompountTerm delimitors, must use 4 different pairs */
    char COMPOUND_TERM_OPENER = '(';
    char COMPOUND_TERM_CLOSER = ')';
    char STATEMENT_OPENER = '<';
    char STATEMENT_CLOSER = '>';
    char SET_EXT_OPENER = '{';
    char SET_EXT_CLOSER = '}';
    char SET_INT_OPENER = '[';
    char SET_INT_CLOSER = ']';

    /* special characors in argument list */
    char ARGUMENT_SEPARATOR = ',';
    char IMAGE_PLACE_HOLDER = '_';

    /* CompountTerm operators, length = 1 */
    String INTERSECTION_EXT_OPERATOR = "&";
    String INTERSECTION_INT_OPERATOR = "|";
    String DIFFERENCE_EXT_OPERATOR = "-";
    String DIFFERENCE_INT_OPERATOR = "~";
    String PRODUCT_OPERATOR = "*";
    String IMAGE_EXT_OPERATOR = "/";
    String IMAGE_INT_OPERATOR = "\\";

    /* CompoundStatement operators, length = 2 */
    String NEGATION_OPERATOR = "--";
    String DISJUNCTION_OPERATOR = "||";
    String CONJUNCTION_OPERATOR = "&&";

    /* built-in relations, length = 3 */
    String INHERITANCE_RELATION = "-->";
    String SIMILARITY_RELATION = "<->";
    String INSTANCE_RELATION = "{--";
    String PROPERTY_RELATION = "--]";
    String INSTANCE_PROPERTY_RELATION = "{-]";
    String IMPLICATION_RELATION = "==>";
    String EQUIVALENCE_RELATION = "<=>";

    /* experience line prefix */
    String INPUT_LINE = "IN";
    String OUTPUT_LINE = "OUT";
    char PREFIX_MARK = ':';
    char RESET_MARK = '*';
    char COMMENT_MARK = '/';

    /* Stamp, display only */
    char STAMP_OPENER = '{';
    char STAMP_CLOSER = '}';
    char STAMP_SEPARATOR = ';';
    char STAMP_STARTER = ':';

    /* TermLink type, display only */
    String TO_COMPONENT_1 = " @(";
    String TO_COMPONENT_2 = ")_ ";
    String TO_COMPOUND_1 = " _@(";
    String TO_COMPOUND_2 = ") ";
}
