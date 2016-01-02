/*
 * StringParser.java
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

import java.util.*;

//import deductions.runtime.swing.TemporaryFrame;

import nars.data.TruthHandle;
import nars.entity.*;
import nars.inference.*;
import nars.language.*;
import nars.storage.Memory;
import nars.storage.Parameters;

/**
 * Parse input String into Task or Term.
 * Abstract class with static methods only.
 */
public abstract class StringParser extends Symbols {

    /**
     * Check Statement relation symbol, called in StringPaser
     *
     * @param s0 The String to be checked
     * @return if the given String is a relation symbol
     */
    public static boolean isRelation(String s0) {
        String s;
        return (s = s0.trim()).length() == 3 && (s.equals(INHERITANCE_RELATION) || s.equals(SIMILARITY_RELATION) || s.equals(INSTANCE_RELATION) || s.equals(PROPERTY_RELATION) || s.equals(INSTANCE_PROPERTY_RELATION) || s.equals(IMPLICATION_RELATION) || s.equals(EQUIVALENCE_RELATION));
    }

    /**
     * Check CompoundTerm operator symbol
     *
     * @param s The String to be checked
     * @return if the given String is an operator symbol
     */
    public static boolean isOperator(String s) {
        return 1 == s.length()
                && (s.equals(INTERSECTION_EXT_OPERATOR) ||
                s.equals(INTERSECTION_INT_OPERATOR) ||
                s.equals(DIFFERENCE_EXT_OPERATOR) ||
                s.equals(DIFFERENCE_INT_OPERATOR) ||
                s.equals(PRODUCT_OPERATOR) ||
                s.equals(IMAGE_EXT_OPERATOR) ||
                s.equals(IMAGE_INT_OPERATOR)) ||
                1 != s.length() && 2 == s.length()
                        && (s.equals(NEGATION_OPERATOR) || s.equals(DISJUNCTION_OPERATOR) || s.equals(CONJUNCTION_OPERATOR));
    }

    /**
     * Enter a new Task in String into the memory, called from InputWindow or locally.
     *
     * @param s      the single-line input String
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    public static Task parseTask(String s, Memory memory, long time) {
        StringBuffer buffer = new StringBuffer(s);
        Task task = null;
        try {
            String budgetString = getBudgetString(buffer);
            String truthString = getTruthString(buffer);
            String str = buffer.toString().trim();
            int last = str.length() - 1;
            char punc = str.charAt(last);
            Stamp stamp = Stamp.createStamp(time);
            TruthValue truth = parseTruth(truthString, punc);
            Term content = parseTerm(str.substring(0, last), memory);
            Sentence sentence = new Sentence(content, punc, truth, stamp);
            if (content instanceof Conjunction && Variable.containVarDep(content.getName()))
                sentence.setRevisible(false);
            BudgetValue budget = parseBudget(budgetString, punc, truth);
            task = new Task(sentence, budget);
        } catch (InvalidInputException e) {
            String message = " !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.getMessage();
            System.out.println(message);
            showWarning(message);
        }
        return task;
    }

    /**
     * Return the prefex of a task string that contains a BudgetValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws InvalidInputException if the input cannot be parsed into a BudgetValue
     */
    private static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (Symbols.BUDGET_VALUE_MARK == s.charAt(0)) {
            int i = s.indexOf(Symbols.BUDGET_VALUE_MARK + "", 1);    // looking for the end
            assert 0 <= i : "missing budget closer";
            String budgetString = s.substring(1, i).trim();
            assert !budgetString.isEmpty() : "empty budget";
            s.delete(0, i + 1);
            return budgetString;
        }
        return null;
    }

    /* ---------- parse values ---------- */

    /**
     * Return the postfix of a task string that contains a TruthValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a TruthValue
     * @throws InvalidInputException if the input cannot be parsed into a TruthValue
     */
    private static String getTruthString(StringBuffer s) throws InvalidInputException {
        int last = s.length() - 1;
        if (Symbols.TRUTH_VALUE_MARK == s.charAt(last)) {
            int first = s.indexOf(Symbols.TRUTH_VALUE_MARK + "");    // looking for the beginning
            assert first != last : "missing truth mark";
            String truthString = s.substring(first + 1, last).trim();
            assert !truthString.isEmpty() : "empty truth";
            s.delete(first, last + 1);                 // remaining input to be processed outside
            s.trimToSize();
            return truthString;
        }
        return null;
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     *
     * @param s    input String
     * @param type Task type
     * @return the input TruthValue
     */
    private static TruthValue parseTruth(String s, char type) {
        if (Symbols.QUESTION_MARK == type) {
            return null;
        }
        float frequency = 1.0f;
        float confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE;
        if (null != s) {
            int i = s.indexOf(Symbols.VALUE_SEPARATOR);
            if (0 > i) {
                frequency = Float.parseFloat(s);
            } else {
                frequency = Float.parseFloat(s.substring(0, i));
                confidence = Float.parseFloat(s.substring(i + 1));
            }
        }
        return new TruthValue(frequency, confidence);
    }

    /**
     * parse the input String into a BudgetValue
     *
     * @param truth       the TruthValue of the task
     * @param s           input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws InvalidInputException If the String cannot be parsed into a BudgetValue
     */
    private static BudgetValue parseBudget(String s, char punctuation, TruthHandle truth) throws InvalidInputException {
        float priority, durability;
        switch (punctuation) {
            case Symbols.JUDGMENT_MARK:
                priority = Parameters.DEFAULT_JUDGMENT_PRIORITY;
                durability = Parameters.DEFAULT_JUDGMENT_DURABILITY;
                break;
            case Symbols.QUESTION_MARK:
                priority = Parameters.DEFAULT_QUESTION_PRIORITY;
                durability = Parameters.DEFAULT_QUESTION_DURABILITY;
                break;
            default:
                throw new InvalidInputException("unknown punctuation: '" + punctuation + "'");
        }
        if (null != s) { // overrite default
            int i = s.indexOf(Symbols.VALUE_SEPARATOR);
            if (0 > i) {        // default durability
                priority = Float.parseFloat(s);
            } else {
                priority = Float.parseFloat(s.substring(0, i));
                durability = Float.parseFloat(s.substring(i + 1));
            }
        }
        float quality = null == truth ? 1 : BudgetFunctions.truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * Top-level method that parse a Term in general, which may recursively call itself.
     * <p>
     * There are 5 valid cases:
     * 1. (Op, A1, ..., An) is a CompoundTerm if Op is a built-in operator
     * 2. {A1, ..., An} is an SetExt;
     * 3. [A1, ..., An] is an SetInt;
     * 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
     */
    public static Term parseTerm(String s0, Memory memory) {
        String s = s0.trim();
        try {
            assert !s.isEmpty() : "missing content";
            Term t = Memory.nameToListedTerm(memory, s);    // existing constant or operator
            if (null == t) {
                int index = s.length() - 1;
                char first = s.charAt(0);
                char last = s.charAt(index);
                switch (first) {
                    case Symbols.COMPOUND_TERM_OPENER:
                        assert Symbols.COMPOUND_TERM_CLOSER == last : "missing CompoundTerm closer";
                        return parseCompoundTerm(s.substring(1, index), memory);
                    case Symbols.SET_EXT_OPENER:
                        assert Symbols.SET_EXT_CLOSER == last : "missing ExtensionSet closer";
                        return SetExt.make(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory);
                    case Symbols.SET_INT_OPENER:
                        assert Symbols.SET_INT_CLOSER == last : "missing IntensionSet closer";
                        return SetInt.make(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory);
                    case Symbols.STATEMENT_OPENER:
                        assert Symbols.STATEMENT_CLOSER == last : "missing Statement closer";
                        return parseStatement(s.substring(1, index), memory);
                    default:
                        return parseAtomicTerm(s);
                }
            }
            return t;
        } catch (InvalidInputException e) {
            String message = " !!! INVALID INPUT: parseTerm: " + s + " --- " + e.getMessage();
            System.out.println(message);
            showWarning(message);
        }
        return null;
    }

    /* ---------- parse String into term ---------- */

    private static void showWarning(String message) {
//		new TemporaryFrame( message + "\n( the faulty line has been kept in the input window )",
//				40000, TemporaryFrame.WARNING );
    }

    /**
     * Parse a Term that has no internal structure.
     * <p>
     * The Term can be a constant or a variable.
     *
     * @param s0 the String to be parsed
     * @return the Term generated from the String
     * @throws InvalidInputException the String cannot be parsed into a Term
     */
    private static Term parseAtomicTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        if (s.isEmpty()) {
            throw new InvalidInputException("missing term");
        }
        assert !s.contains(" ") : "invalid term";
        return Variable.containVar(s) ? new Variable(s) : new Term(s);
    }

    /**
     * Parse a String to create a Statement.
     *
     * @param s0 The input String to be parsed
     * @return the Statement generated from the String
     * @throws InvalidInputException the String cannot be parsed into a Term
     */
    private static Statement parseStatement(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        int i = topRelation(s);
        if (0 > i) {
            throw new InvalidInputException("invalid statement");
        }
        String relation = s.substring(i, i + 3);
        Term subject = parseTerm(s.substring(0, i), memory);
        Term predicate = parseTerm(s.substring(i + 3), memory);
        Statement t = Statement.make(relation, subject, predicate, memory);
        assert null != t : "invalid statement";
        return t;
    }

    /**
     * Parse a String to create a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the Term generated from the String
     * @throws InvalidInputException the String cannot be parsed into a Term
     */
    private static Term parseCompoundTerm(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        int firstSeparator = s.indexOf(Symbols.ARGUMENT_SEPARATOR);
        String op = s.substring(0, firstSeparator).trim();
        if (!isOperator(op)) {
            throw new InvalidInputException("unknown operator: " + op);
        }
        List<Term> arg = parseArguments(s.substring(firstSeparator + 1) + Symbols.ARGUMENT_SEPARATOR, memory);
        Term t = CompoundTerm.make(op, arg, memory);
        if (null == t) {
            throw new InvalidInputException("invalid compound term");
        }
        return t;
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the arguments in an ArrayList
     * @throws InvalidInputException the String cannot be parsed into an argument get
     */
    private static List<Term> parseArguments(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        List<Term> list = new ArrayList<Term>();
        int start = 0;
        int end = 0;
        Term t;
        while (end < s.length() - 1) {
            end = nextSeparator(s, start);
            t = parseTerm(s.substring(start, end), memory);     // recursive call
            list.add(t);
            start = end + 1;
        }
        if (list.isEmpty()) {
            throw new InvalidInputException("null argument");
        }
        return list;
    }

    /**
     * Locate the first top-level separator in a CompoundTerm
     *
     * @param s     The String to be parsed
     * @param first The starting index
     * @return the index of the next seperator in a String
     */
    private static int nextSeparator(String s, int first) {
        int levelCounter = 0;
        int i = first;
        while (i < s.length() - 1) {
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            } else if (Symbols.ARGUMENT_SEPARATOR == s.charAt(i)) {
                if (0 == levelCounter) {
                    break;
                }
            }
            i++;
        }
        return i;
    }

    /* ---------- locate top-level substring ---------- */

    /**
     * locate the top-level relation in a statement
     *
     * @param s The String to be parsed
     * @return the index of the top-level relation
     */
    private static int topRelation(String s) {      // need efficiency improvement
        int levelCounter = 0;
        int i = 0;
        while (i < s.length() - 3) {    // don't need to check the last 3 characters
            if (0 == levelCounter && isRelation(s.substring(i, i + 3))) {
                return i;
            }
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            }
            i++;
        }
        return -1;
    }

    /**
     * Check CompoundTerm opener symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is an opener symbol
     */
    private static boolean isOpener(String s, int i) {
        char c = s.charAt(i);
        return (Symbols.COMPOUND_TERM_OPENER == c ||
                Symbols.SET_EXT_OPENER == c ||
                Symbols.SET_INT_OPENER == c ||
                Symbols.STATEMENT_OPENER == c) && !(i + 3 <= s.length() && isRelation(s.substring(i, i + 3)));
    }

    /* ---------- recognize symbols ---------- */

    /**
     * Check CompoundTerm closer symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is a closer symbol
     */
    private static boolean isCloser(String s, int i) {
        char c = s.charAt(i);
        return (Symbols.COMPOUND_TERM_CLOSER == c ||
                Symbols.SET_EXT_CLOSER == c ||
                Symbols.SET_INT_CLOSER == c ||
                Symbols.STATEMENT_CLOSER == c) && !(2 <= i && isRelation(s.substring(i - 2, i + 1)));
    }

    /**
     * All kinds of invalid input lines
     */
    private static class InvalidInputException extends Exception {

        /**
         * An invalid input line.
         *
         * @param s type of error
         */
        InvalidInputException(String s) {
            super(s);
        }
    }
}
