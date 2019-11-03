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

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.language.*;
import nars.main_nogui.Parameters;
import nars.storage.Memory;

import java.util.ArrayList;

//import deductions.runtime.swing.TemporaryFrame;

/**
 * Parse input String into Task or Term. Abstract class with static methods
 * only.
 */
public abstract class StringParser extends Symbols {

    /**
     * Parse a line of input experience
     * <p>
     * called from ExperienceIO.loadLine
     *
     * @param buffer The line to be parsed
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    public static Task parseExperience(StringBuffer buffer, Memory memory, long time) {
        var i = buffer.indexOf(PREFIX_MARK + "");
        if (i > 0) {
            var prefix = buffer.substring(0, i).trim();
            switch (prefix) {
                case OUTPUT_LINE:
                    return null;
                case INPUT_LINE:
                    buffer.delete(0, i + 1);
                    break;
            }
        }
        var c = buffer.charAt(buffer.length() - 1);
        if (c == STAMP_CLOSER) {
            var j = buffer.lastIndexOf(STAMP_OPENER + "");
            buffer.delete(j - 1, buffer.length());
        }
        return parseTask(buffer.toString().trim(), memory, time);
    }

    /**
     * Enter a new Task in String into the memory, called from InputWindow or
     * locally.
     *
     * @param s      the single-line input String
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    public static Task parseTask(String s, Memory memory, long time) {
        var buffer = new StringBuffer(s);
        Task task = null;
        try {
            var budgetString = getBudgetString(buffer);
            var truthString = getTruthString(buffer);
            var str = buffer.toString().trim();
            var last = str.length() - 1;
            var punc = str.charAt(last);
            var stamp = new Stamp(time);
            var truth = parseTruth(truthString, punc);
            var content = parseTerm(str.substring(0, last), memory);
            var sentence = new Sentence(content, punc, truth, stamp);
            if ((content instanceof Conjunction) && Variable.containVarDep(content.getName())) {
                sentence.setRevisible(false);
            }
            var budget = parseBudget(budgetString, punc, truth);
            task = new Task(sentence, budget);
        } catch (InvalidInputException e) {
            var message = " !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.getMessage();
            System.out.println(message);
//            showWarning(message);
        }
        return task;
    }

    /**
     * Return the prefix of a task string that contains a BudgetValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     *                                                    parsed into a BudgetValue
     */
    private static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (s.charAt(0) != BUDGET_VALUE_MARK) {
            return null;
        }
        var i = s.indexOf(BUDGET_VALUE_MARK + "", 1);    // looking for the end
        if (i < 0) {
            throw new InvalidInputException("missing budget closer");
        }
        var budgetString = s.substring(1, i).trim();
        if (budgetString.length() == 0) {
            throw new InvalidInputException("empty budget");
        }
        s.delete(0, i + 1);
        return budgetString;
    }

    /* ---------- parse values ---------- */

    /**
     * Return the postfix of a task string that contains a TruthValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a TruthValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     *                                                    parsed into a TruthValue
     */
    private static String getTruthString(StringBuffer s) throws InvalidInputException {
        var last = s.length() - 1;
        if (s.charAt(last) != TRUTH_VALUE_MARK) {       // use default
            return null;
        }
        var first = s.indexOf(TRUTH_VALUE_MARK + "");    // looking for the beginning
        if (first == last) { // no matching closer
            throw new InvalidInputException("missing truth mark");
        }
        var truthString = s.substring(first + 1, last).trim();
        if (truthString.length() == 0) {                // empty usage
            throw new InvalidInputException("empty truth");
        }
        s.delete(first, last + 1);                 // remaining input to be processed outside
        s.trimToSize();
        return truthString;
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     *
     * @param s    input String
     * @param type Task type
     * @return the input TruthValue
     */
    private static TruthValue parseTruth(String s, char type) {
        if (type == QUESTION_MARK) {
            return null;
        }
        var frequency = 1.0f;
        var confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE;
        if (s != null) {
            var i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {
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
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     *                                                    be parsed into a BudgetValue
     */
    private static BudgetValue parseBudget(String s, char punctuation, TruthValue truth) throws InvalidInputException {
        float priority, durability;
        switch (punctuation) {
            case JUDGMENT_MARK:
                priority = Parameters.DEFAULT_JUDGMENT_PRIORITY;
                durability = Parameters.DEFAULT_JUDGMENT_DURABILITY;
                break;
            case QUESTION_MARK:
                priority = Parameters.DEFAULT_QUESTION_PRIORITY;
                durability = Parameters.DEFAULT_QUESTION_DURABILITY;
                break;
            default:
                throw new InvalidInputException("unknown punctuation: '" + punctuation + "'");
        }
        if (s != null) { // overrite default
            var i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {        // default durability
                priority = Float.parseFloat(s);
            } else {
                priority = Float.parseFloat(s.substring(0, i));
                durability = Float.parseFloat(s.substring(i + 1));
            }
        }
        var quality = (truth == null) ? 1 : BudgetFunctions.truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     * <p>
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an SetExt; 3. [A1, ..., An] is an
     * SetInt; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
     */
    public static Term parseTerm(String s0, Memory memory) {
        var s = s0.trim();
        try {
            if (s.length() == 0) {
                throw new InvalidInputException("missing content");
            }
            var t = memory.nameToListedTerm(s);    // existing constant or operator
            if (t != null) {
                return t;
            }                           // existing Term
            var index = s.length() - 1;
            var first = s.charAt(0);
            var last = s.charAt(index);
            switch (first) {
                case COMPOUND_TERM_OPENER:
                    if (last == COMPOUND_TERM_CLOSER) {
                        return parseCompoundTerm(s.substring(1, index), memory);
                    } else {
                        throw new InvalidInputException("missing CompoundTerm closer");
                    }
                case SET_EXT_OPENER:
                    if (last == SET_EXT_CLOSER) {
                        return SetExt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                    } else {
                        throw new InvalidInputException("missing ExtensionSet closer");
                    }
                case SET_INT_OPENER:
                    if (last == SET_INT_CLOSER) {
                        return SetInt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                    } else {
                        throw new InvalidInputException("missing IntensionSet closer");
                    }
                case STATEMENT_OPENER:
                    if (last == STATEMENT_CLOSER) {
                        return parseStatement(s.substring(1, index), memory);
                    } else {
                        throw new InvalidInputException("missing Statement closer");
                    }
                default:
                    return parseAtomicTerm(s);
            }
        } catch (InvalidInputException e) {
            var message = " !!! INVALID INPUT: parseTerm: " + s + " --- " + e.getMessage();
            System.out.println(message);
//            showWarning(message);
        }
        return null;
    }

    /* ---------- parse String into term ---------- */

    /**
     * Parse a Term that has no internal structure.
     * <p>
     * The Term can be a constant or a variable.
     *
     * @param s0 the String to be parsed
     * @return the Term generated from the String
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     */
    private static Term parseAtomicTerm(String s0) throws InvalidInputException {
        var s = s0.trim();
        if (s.length() == 0) {
            throw new InvalidInputException("missing term");
        }
        if (s.contains(" ")) // invalid characters in a name
        {
            throw new InvalidInputException("invalid term");
        }
        if (Variable.containVar(s)) {
            return new Variable(s);
        } else {
            return new Term(s);
        }
    }

//    private static void showWarning(String message) {
//		new TemporaryFrame( message + "\n( the faulty line has been kept in the input window )",
//				40000, TemporaryFrame.WARNING );
//    }

    /**
     * Parse a String to create a Statement.
     *
     * @param s0 The input String to be parsed
     * @return the Statement generated from the String
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     */
    private static Statement parseStatement(String s0, Memory memory) throws InvalidInputException {
        var s = s0.trim();
        var i = topRelation(s);
        if (i < 0) {
            throw new InvalidInputException("invalid statement");
        }
        var relation = s.substring(i, i + 3);
        var subject = parseTerm(s.substring(0, i), memory);
        var predicate = parseTerm(s.substring(i + 3), memory);
        var t = Statement.make(relation, subject, predicate, memory);
        if (t == null) {
            throw new InvalidInputException("invalid statement");
        }
        return t;
    }

    /**
     * Parse a String to create a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the Term generated from the String
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     */
    private static Term parseCompoundTerm(String s0, Memory memory) throws InvalidInputException {
        var s = s0.trim();
        var firstSeparator = s.indexOf(ARGUMENT_SEPARATOR);
        var op = s.substring(0, firstSeparator).trim();
        if (!CompoundTerm.isOperator(op)) {
            throw new InvalidInputException("unknown operator: " + op);
        }
        var arg = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR, memory);
        var t = CompoundTerm.make(op, arg, memory);
        if (t == null) {
            throw new InvalidInputException("invalid compound term");
        }
        return t;
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the arguments in an ArrayList
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into an argument get
     */
    private static ArrayList<Term> parseArguments(String s0, Memory memory) throws InvalidInputException {
        var s = s0.trim();
        var list = new ArrayList<Term>();
        var start = 0;
        var end = 0;
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
        var levelCounter = 0;
        var i = first;
        while (i < s.length() - 1) {
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            } else if (s.charAt(i) == ARGUMENT_SEPARATOR) {
                if (levelCounter == 0) {
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
        var levelCounter = 0;
        var i = 0;
        while (i < s.length() - 3) {    // don't need to check the last 3 characters
            if ((levelCounter == 0) && (Statement.isRelation(s.substring(i, i + 3)))) {
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
        var c = s.charAt(i);
        var b = (c == COMPOUND_TERM_OPENER)
                || (c == SET_EXT_OPENER)
                || (c == SET_INT_OPENER)
                || (c == STATEMENT_OPENER);
        if (!b) {
            return false;
        }
        if (i + 3 <= s.length() && Statement.isRelation(s.substring(i, i + 3))) {
            return false;
        }
        return true;
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
        var c = s.charAt(i);
        var b = (c == COMPOUND_TERM_CLOSER)
                || (c == SET_EXT_CLOSER)
                || (c == SET_INT_CLOSER)
                || (c == STATEMENT_CLOSER);
        if (!b) {
            return false;
        }
        if (i >= 2 && Statement.isRelation(s.substring(i - 2, i + 1))) {
            return false;
        }
        return true;
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
