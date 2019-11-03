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
package nars.io

import nars.entity.*
import nars.inference.BudgetFunctions.truthToQuality
import nars.language.*
import nars.main_nogui.Parameters
import nars.storage.Memory
import java.util.*

//import deductions.runtime.swing.TemporaryFrame;

/**
 * Parse input String into Task or Term. Abstract class with static methods
 * only.
 */
object StringParser {
    /**
     * Parse a line of input experience
     *
     *
     * called from ExperienceIO.loadLine
     *
     * @param buffer The line to be parsed
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    fun parseExperience(buffer: StringBuffer, memory: Memory, time: Long): Task? {
        val i = buffer.indexOf(Symbols.PREFIX_MARK + "")
        if (i > 0) {
            val prefix = buffer.substring(0, i).trim { it <= ' ' }
            when (prefix) {
                Symbols.OUTPUT_LINE -> return null
                Symbols.INPUT_LINE -> buffer.delete(0, i + 1)
            }
        }
        val c = buffer[buffer.length - 1]
        if (c == Symbols.STAMP_CLOSER) {
            val j = buffer.lastIndexOf(Symbols.STAMP_OPENER + "")
            buffer.delete(j - 1, buffer.length)
        }
        return parseTask(buffer.toString().trim { it <= ' ' }, memory, time)
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
    fun parseTask(s: String, memory: Memory, time: Long): Task? {
        val buffer = StringBuffer(s)
        var task: Task? = null
        try {
            val budgetString = getBudgetString(buffer)
            val truthString = getTruthString(buffer)
            val str = buffer.toString().trim { it <= ' ' }
            val last = str.length - 1
            val punc = str[last]
            val stamp = Stamp(time)
            val truth = parseTruth(truthString, punc)
            val content = parseTerm(str.substring(0, last), memory)
            val sentence = Sentence(content!!, punc, truth, stamp)
            if (content is Conjunction && Variable.containVarDep(content.getName())) {
                sentence.revisible = false
            }
            val budget = parseBudget(budgetString, punc, truth)
            task = Task(sentence, budget)
        } catch (e: InvalidInputException) {
            val message = " !!! INVALID INPUT: parseTask: " + buffer.toString() + " --- " + e.message
            println(message)
//            showWarning(message);

        }
        return task
    }

    /**
     * Return the prefix of a task string that contains a BudgetValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     * parsed into a BudgetValue
     */
    @Throws(InvalidInputException::class)
    private fun getBudgetString(s: StringBuffer): String? {
        if (s[0] != Symbols.BUDGET_VALUE_MARK) {
            return null
        }
        val i = s.indexOf(Symbols.BUDGET_VALUE_MARK + "", 1)    // looking for the end

        assert(i >= 0) { "missing budget closer" }
        val budgetString = s.substring(1, i).trim { it <= ' ' }
        assert(budgetString.length != 0) { "empty budget" }
        s.delete(0, i + 1)
        return budgetString
    }

    /* ---------- parse values ---------- */


    /**
     * Return the postfix of a task string that contains a TruthValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a TruthValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     * parsed into a TruthValue
     */
    @Throws(InvalidInputException::class)
    private fun getTruthString(s: StringBuffer): String? {
        val last = s.length - 1
        if (s[last] != Symbols.TRUTH_VALUE_MARK) {       // use default

            return null
        }
        val first = s.indexOf(Symbols.TRUTH_VALUE_MARK + "")    // looking for the beginning

        // no matching closer

        assert(first != last) { "missing truth mark" }
        val truthString = s.substring(first + 1, last).trim { it <= ' ' }
        // empty usage

        assert(truthString.length != 0) { "empty truth" }
        s.delete(first, last + 1)                 // remaining input to be processed outside

        s.trimToSize()
        return truthString
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     *
     * @param s    input String
     * @param type Task type
     * @return the input TruthValue
     */
    private fun parseTruth(s: String?, type: Char): TruthValue? {
        if (type == Symbols.QUESTION_MARK) {
            return null
        }
        var frequency = 1.0f
        var confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE
        if (s != null) {
            val i = s.indexOf(Symbols.VALUE_SEPARATOR)
            if (i < 0) {
                frequency    =  (s).toFloat()
            } else {
                frequency   =  (s.substring(0, i)).toFloat()
                confidence   =  (s.substring(i + 1)).toFloat()
            }
        }
        return TruthValue(frequency, confidence)
    }

    /**
     * parse the input String into a BudgetValue
     *
     * @param truth       the TruthValue of the task
     * @param s           input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     * be parsed into a BudgetValue
     */
    @Throws(InvalidInputException::class)
    private fun parseBudget(s: String?, punctuation: Char, truth: TruthValue?): BudgetValue {
        var priority: Float
        var durability: Float
        when (punctuation) {
            Symbols.JUDGMENT_MARK -> {
                priority = Parameters.DEFAULT_JUDGMENT_PRIORITY
                durability = Parameters.DEFAULT_JUDGMENT_DURABILITY
            }
            Symbols.QUESTION_MARK -> {
                priority = Parameters.DEFAULT_QUESTION_PRIORITY
                durability = Parameters.DEFAULT_QUESTION_DURABILITY
            }
            else -> throw InvalidInputException("unknown punctuation: '$punctuation'")
        }
        if (s != null) { // overrite default
            val i = s.indexOf(Symbols.VALUE_SEPARATOR)
            if (i < 0) {        // default durability
                priority =         s.toFloat()
            } else {
                priority =        s.substring(0, i).toFloat()
                durability =      s.substring(i + 1).toFloat()
            }
        }
        val quality: Float = if (truth == null) 1f else truthToQuality(truth)
        return BudgetValue(priority, durability, quality)
    }

    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     *
     *
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an SetExt; 3. [A1, ..., An] is an
     * SetInt; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
    </T1> */
    fun parseTerm(s0: String, memory: Memory): Term? {
        var result: Term? = null
        val s = s0.trim { it <= ' ' }
        try {
            assert(s.length != 0) { "missing content" }
            val t: Term? = memory.nameToListedTerm(s)    // existing constant or operator

            result = if (t == null) {
                val index = s.length - 1
                val first = s[0]
                val last = s[index]
                if (first == Symbols.COMPOUND_TERM_OPENER) {
                    if (last == Symbols.COMPOUND_TERM_CLOSER) {
                        parseCompoundTerm(s.substring(1, index), memory)
                    } else {
                        throw InvalidInputException("missing CompoundTerm closer")
                    }
                } else if (first == Symbols.SET_EXT_OPENER) {
                    if (last == Symbols.SET_EXT_CLOSER) {
                        SetExt.make(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory)
                    } else {
                        throw InvalidInputException("missing ExtensionSet closer")
                    }
                } else if (first == Symbols.SET_INT_OPENER) {
                    if (last == Symbols.SET_INT_CLOSER) {
                        SetInt.make(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory)
                    } else {
                        throw InvalidInputException("missing IntensionSet closer")
                    }
                } else if (first == Symbols.STATEMENT_OPENER) {
                    if (last == Symbols.STATEMENT_CLOSER) {
                        parseStatement(s.substring(1, index), memory)
                    } else {
                        throw InvalidInputException("missing Statement closer")
                    }
                } else {
                    parseAtomicTerm(s)
                }
            } else {
                t
            }                           // existing Term
        } catch (e: InvalidInputException) {
            val message = " !!! INVALID INPUT: parseTerm: " + s + " --- " + e.message
            println(message)
//            showWarning(message);

        }
        return result
    }

    /* ---------- parse String into term ---------- */


    /**
     * Parse a Term that has no internal structure.
     *
     *
     * The Term can be a constant or a variable.
     *
     * @param s0 the String to be parsed
     * @return the Term generated from the String
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into a Term
     */
    @Throws(InvalidInputException::class)
    private fun parseAtomicTerm(s0: String): Term {
        val s = s0.trim { it <= ' ' }
        assert(s.length != 0) { "missing term" }
        assert(!s.contains(" ")) { "invalid term" }
        return if (Variable.containVar(s)) {
            Variable(s)
        } else {
            Term(s)
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
     * parsed into a Term
     */
    @Throws(InvalidInputException::class)
    private fun parseStatement(s0: String, memory: Memory): Statement? {
        val s = s0.trim { it <= ' ' }
        val i = topRelation(s)
        assert(i >= 0) { "invalid statement" }
        val relation = s.substring(i, i + 3)
        val subject = parseTerm(s.substring(0, i), memory)
        val predicate = parseTerm(s.substring(i + 3), memory)
        return Statement.make(relation, subject, predicate, memory) ?: error("invalid statement")
    }

    /**
     * Parse a String to create a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the Term generated from the String
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into a Term
     */
    @Throws(InvalidInputException::class)
    private fun parseCompoundTerm(s0: String, memory: Memory): Term? {
        val s = s0.trim { it <= ' ' }
        val firstSeparator = s.indexOf(Symbols.ARGUMENT_SEPARATOR)
        val op = s.substring(0, firstSeparator).trim { it <= ' ' }
        assert(CompoundTerm.isOperator(op)) { "unknown operator: $op" }
        val arg = parseArguments(s.substring(firstSeparator + 1) + Symbols.ARGUMENT_SEPARATOR, memory)
        return CompoundTerm.make(op, arg, memory) ?: error("invalid compound term")
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     *
     * @param s0 The String to be parsed
     * @return the arguments in an ArrayList
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     * parsed into an argument get
     */
    @Throws(InvalidInputException::class)
    private fun parseArguments(s0: String, memory: Memory): ArrayList<Term?> {
        val s = s0.trim { it <= ' ' }
        val list = ArrayList<Term?>()
        var start = 0
        var end = 0
        var t: Term?
        while (end < s.length - 1) {
            end = nextSeparator(s, start)
            t = parseTerm(s.substring(start, end), memory)
            list.add(t)
            start = end + 1
        }
        assert(!list.isEmpty()) { "null argument" }
        return list
    }

    /**
     * Locate the first top-level separator in a CompoundTerm
     *
     * @param s     The String to be parsed
     * @param first The starting index
     * @return the index of the next seperator in a String
     */
    private fun nextSeparator(s: String, first: Int): Int {
        var levelCounter = 0
        var i = first
        while (i < s.length - 1) {
            if (isOpener(s, i)) {
                levelCounter++
            } else if (isCloser(s, i)) {
                levelCounter--
            } else if (s[i] == Symbols.ARGUMENT_SEPARATOR) {
                if (levelCounter == 0) {
                    break
                }
            }
            i++
        }
        return i
    }

    /* ---------- locate top-level substring ---------- */


    /**
     * locate the top-level relation in a statement
     *
     * @param s The String to be parsed
     * @return the index of the top-level relation
     */
    private fun topRelation(s: String): Int {      // need efficiency improvement

        var levelCounter = 0
        var i = 0
        while (i < s.length - 3) {    // don't need to check the last 3 characters

            if (levelCounter == 0 && Statement.isRelation(s.substring(i, i + 3))) {
                return i
            }
            if (isOpener(s, i)) {
                levelCounter++
            } else if (isCloser(s, i)) {
                levelCounter--
            }
            i++
        }
        return -1
    }

    /**
     * Check CompoundTerm opener symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is an opener symbol
     */
    private fun isOpener(s: String, i: Int): Boolean {
        val c = s[i]
        val b = (c == Symbols.COMPOUND_TERM_OPENER
                || c == Symbols.SET_EXT_OPENER
                || c == Symbols.SET_INT_OPENER
                || c == Symbols.STATEMENT_OPENER)
        if (!b) {
            return false
        }
        return if (i + 3 <= s.length && Statement.isRelation(s.substring(i, i + 3))) {
            false
        } else true
    }

    /* ---------- recognize symbols ---------- */


    /**
     * Check CompoundTerm closer symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is a closer symbol
     */
    private fun isCloser(s: String, i: Int): Boolean {
        val c = s[i]
        val b = (c == Symbols.COMPOUND_TERM_CLOSER
                || c == Symbols.SET_EXT_CLOSER
                || c == Symbols.SET_INT_CLOSER
                || c == Symbols.STATEMENT_CLOSER)
        if (!b) {
            return false
        }
        return if (i >= 2 && Statement.isRelation(s.substring(i - 2, i + 1))) {
            false
        } else true
    }

    /**
     * All kinds of invalid input lines
     */
    private class InvalidInputException
    /**
     * An invalid input line.
     *
     * @param s type of error
     */
    internal constructor(s: String) : Exception(s)
}