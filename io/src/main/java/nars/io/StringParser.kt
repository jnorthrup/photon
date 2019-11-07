package nars.io

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import nars.io.StringParser.Statement.Implication
import nars.io.StringParser.Statement.InstanceProperty
import org.graalvm.compiler.nodeinfo.StructuralInput
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.InputStreamReader
import java.util.*
import java.util.Set

//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
/**
 * Parse input String into Task or Term. Abstract class with static methods
 * only.
 */
object StringParser {
    var steps = 0
    @Throws(FileNotFoundException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val input: BufferedReader
        //        ObjectMapper mapper = new ObjectMapper();
//        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//        var objectWriter = mapper.writerWithDefaultPrettyPrinter();
        input = if (args.size > 0) {
            BufferedReader(FileReader(args[0]))
        } else BufferedReader(InputStreamReader(System.`in`))
        var memory = Memory()
        val lines = input.lines()
        val iterator = lines.iterator()
        while (iterator.hasNext()) {
            val text = iterator.next()
            if (!text.isBlank()) {
                val c = text[0]
                if (c == Symbols.RESET_MARK) {
                    memory = Memory()
                    memory.exportStrings.add(text)
                } else if (c != Symbols.COMMENT_MARK) { // read NARS language or an integer : TODO duplicated code
                    try {
                        val i = text.toInt()
                        walk(i)
                    } catch (e: NumberFormatException) {
                        val task = parseExperience(StringBuffer(text), memory, System.currentTimeMillis())
                        if (task != null) {
                            memory.inputTask(task)
                        }
                    }
                }
            }
        }
    }

    fun walk(i: Int) {
        steps += i
    }

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
        val i = buffer.indexOf(Symbols.PREFIX_MARK.toString() + "")
        if (i > 0) {
            val prefix = buffer.substring(0, i).trim { it <= ' ' }
            if (Symbols.OUTPUT_LINE != prefix) {
                if (Symbols.INPUT_LINE == prefix) {
                    buffer.delete(0, i + 1)
                }
            } else return null
        }
        val c = buffer[buffer.length - 1]
        if (c == Symbols.STAMP_CLOSER) {
            val j = buffer.lastIndexOf(Symbols.STAMP_OPENER.toString() + "")
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
    fun parseTask(s: String?, memory: Memory, time: Long): Task? {
        val buffer = StringBuffer(s)
        var task: Task? = null
        try {
            val budgetString = getBudgetString(buffer)
            val truthString = getTruthString(buffer)
            val str = buffer.toString().trim { it <= ' ' }
            val last = str.length - 1
            val punc = str[last]
            val stamp = StampHandle(time)
            val truth = parseTruth(truthString, punc)
            val content = parseTerm(str.substring(0, last), memory)
            val sentence = Sentence(content, punc, truth, stamp)
            if (content is Conjunction && Variable.containVarDep(content.name)) sentence.revisible = false
            val budget = parseBudget(budgetString, punc, truth)
            task = Task(sentence, budget)
        } catch (e: InvalidInputException) {
            val message = " !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.message
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
    fun getBudgetString(s: StringBuffer): String? {
        if (s[0] != Symbols.BUDGET_VALUE_MARK) {
            return null
        }
        val i = s.indexOf(Symbols.BUDGET_VALUE_MARK.toString() + "", 1) // looking for the end
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
    fun getTruthString(s: StringBuffer): String? {
        val last = s.length - 1
        if (s[last] != Symbols.TRUTH_VALUE_MARK) { // use default
            return null
        }
        val first = s.indexOf(Symbols.TRUTH_VALUE_MARK.toString() + "") // looking for the beginning
        assert(first != last) { "missing truth mark" }
        val truthString = s.substring(first + 1, last).trim { it <= ' ' }
        assert(truthString.length != 0) { "empty truth" }
        s.delete(first, last + 1) // remaining input to be processed outside
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
    fun parseTruth(s: String?, type: Char): TruthValue? {
        if (type != Symbols.QUESTION_MARK) {
            var frequency = 1.0f
            var confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE
            if (s != null) {
                val i = s.indexOf(Symbols.VALUE_SEPARATOR)
                if (i < 0) {
                    frequency = s.toFloat()
                } else {
                    frequency = s.substring(0, i).toFloat()
                    confidence = s.substring(i + 1).toFloat()
                }
            }
            return TruthValue(frequency, confidence)
        }
        return null
    }

    /**
     * parse the input String into a BudgetValue
     *
     * @param truth       the TruthValue of the task
     * @param inputString input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     * be parsed into a BudgetValue
     */
    @Throws(InvalidInputException::class)
    fun parseBudget(inputString: String?, punctuation: Char, truth: TruthValue?): BudgetValue {
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
        if (inputString != null) { // overrite default
            val i = inputString.indexOf(Symbols.VALUE_SEPARATOR)
            if (i < 0) { // default durability
                priority = inputString.toFloat()
            } else {
                priority = inputString.substring(0, i).toFloat()
                durability = inputString.substring(i + 1).toFloat()
            }
        }
        val quality: Float = if (truth == null) 1 else BudgetFunctions.truthToQuality(truth)
        return BudgetValue(priority, durability, quality)
    }
    fun parseTerm2(s0: String, memory: StructuralInput.Memory): Term? {}
    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     *
     *
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an ExtensionSet; 3. [A1, ..., An] is an
     * IntensionSet; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
    </T1> */
    fun parseTerm(s0: String, memory: Memory): Term? {
        var result: Term? = null
        val s = s0.replace("\\s+".toRegex(), "")
        try {
            result = if (s.length != 0) {
                val t = memory.nameToListedTerm(s) // existing constant or operator
                if (t == null) {
                    val index = s.length - 1
                    val first = s[0]
                    val last = s[index]
                    when (first) {
                        Symbols.COMPOUND_TERM_OPENER -> {
                            assert(last == Symbols.COMPOUND_TERM_CLOSER) { "missing CompoundTerm closer" }
                            val compoundTerm = CompoundTerm.parseCompoundTerm(s.substring(1, index), memory) as CompoundTerm?
                            compoundTerm
                        }
                        Symbols.SET_EXT_OPENER -> {
                            assert(last == Symbols.SET_EXT_CLOSER) { "missing ExtensionSet closer" }
                            ExtensionSet(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory)
                        }
                        Symbols.SET_INT_OPENER -> {
                            assert(last == Symbols.SET_INT_CLOSER) { "missing IntensionSet closer" }
                            IntensionSet(parseArguments(s.substring(1, index) + Symbols.ARGUMENT_SEPARATOR, memory), memory)
                        }
                        Symbols.STATEMENT_OPENER -> {
                            assert(last == Symbols.STATEMENT_CLOSER) { "missing Statement closer" }
                            parseStatement(s.substring(1, index), memory)
                        }
                        else -> parseAtomicTerm(s)
                    }
                } else {
                    t
                } // existing Term
            } else {
                throw InvalidInputException("missing content")
            }
        } catch (e: Throwable) {
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
    fun parseAtomicTerm(s0: String): Term {
        val s = s0.trim { it <= ' ' }
        assert(s.length != 0) { "missing term" }
        assert(!s.contains(" ")) { "invalid term" }
        return if (Variable.containVar(s)) {
            Variable(s)
        } else {
            Term(s)
        }
    }
    //    public static void showWarning(String message) {
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
    fun parseStatement(s0: String, memory: Memory): Statement? {
        val s = s0.trim { it <= ' ' }
        val i = topRelation(s)
        assert(i >= 0) { "invalid statement" }
        val relation = s.substring(i, i + 3)
        val subject = parseTerm(s.substring(0, i), memory)
        val predicate = parseTerm(s.substring(i + 3), memory)
        return Statement.make(relation, subject, predicate, memory) ?: error("invalid statement")
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
    fun parseArguments(s0: String, memory: Memory): ArrayList<Term?> {
        val s = s0.trim { it <= ' ' }
        val list = ArrayList<Term?>()
        var start = 0
        var end = 0
        while (end < s.length - 1) {
            end = nextSeparator(s, start)
            // recursive call
            val t = parseTerm(s.substring(start, end), memory)
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
    fun nextSeparator(s: String, first: Int): Int {
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
    fun topRelation(s: String): Int {
        var result = -1 // need efficiency improvement
        var levelCounter = 0
        var i = 0
        while (i < s.length - 3) { // don't need to check the last 3 characters
            if (levelCounter == 0 && Statement.isRelation(s.substring(i, i + 3))) {
                result = i
                break
            }
            if (isOpener(s, i)) {
                levelCounter++
            } else if (isCloser(s, i)) {
                levelCounter--
            }
            i++
        }
        return result
    }

    /**
     * Check CompoundTerm opener symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is an opener symbol
     */
    fun isOpener(s: String, i: Int): Boolean {
        var result = false
        val c = s[i]
        val b = (c == Symbols.COMPOUND_TERM_OPENER
                || c == Symbols.SET_EXT_OPENER
                || c == Symbols.SET_INT_OPENER
                || c == Symbols.STATEMENT_OPENER)
        if (b) {
            if (i + 3 > s.length || !Statement.isRelation(s.substring(i, i + 3))) {
                result = true
            }
        }
        return result
    }
    /* ---------- recognize symbols ---------- */
    /**
     * Check CompoundTerm closer symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is a closer symbol
     */
    fun isCloser(s: String, i: Int): Boolean {
        var result = false
        val c = s[i]
        val b = (c == Symbols.COMPOUND_TERM_CLOSER
                || c == Symbols.SET_EXT_CLOSER
                || c == Symbols.SET_INT_CLOSER
                || c == Symbols.STATEMENT_CLOSER)
        if (b) {
            if (i < 2 || !Statement.isRelation(s.substring(i - 2, i + 1))) {
                result = true
            }
        }
        return result
    }

    /**
     * Make a Statement from String, called by StringParser
     *
     * @param relation  The relation String
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return The Statement built
     */
    fun make(relation: String, subject: Term?, predicate: Term?, memory: Memory?): Statement? {
        if (Statement.invalidStatement(subject, predicate)) {
            return null
        }
        if (relation == Symbols.INHERITANCE_RELATION) {
            return Inheritance(subject, predicate, memory)
        }
        if (relation == Symbols.SIMILARITY_RELATION) {
            return Similarity(subject, predicate, memory)
        }
        if (relation == Symbols.INSTANCE_RELATION) {
            return Statement.Instance(subject, predicate, memory)
        }
        if (relation == Symbols.PROPERTY_RELATION) {
            return Statement.Property(subject, predicate, memory)
        }
        if (relation == Symbols.INSTANCE_PROPERTY_RELATION) {
            return InstanceProperty(subject, predicate, memory)
        }
        if (relation == Symbols.IMPLICATION_RELATION) {
            return Implication(subject, predicate, memory)
        }
        return if (relation == Symbols.EQUIVALENCE_RELATION) {
            Equivalence(subject, predicate, memory)
        } else null
    }

    /**
     * All kinds of invalid input lines
     */
    class InvalidInputException
    /**
     * An invalid input line.
     *
     * @param s type of error
     */
    internal constructor(s: String?) : Exception(s)

    internal class StampHandle(time: Long)
    internal class TruthValueRefier
    open class Term(val name: String)

    internal class Variable(s: String) : Term(s) {
        companion object {
            fun containVarDep(name: String?): Boolean {
                return false
            }

            fun containVar(s: String?): Boolean {
                return false
            }
        }
    }

    internal class Conjunction(s: String) : Term(s)
    class BudgetValue(val priority: Float, val durability: Float, val quality: Float)

    class TruthValue(val frequency: Float, val confidence: Float)

    internal object BudgetFunctions {
        var truth: TruthValue? = null
        fun truthToQuality(truth: TruthValue?): Float {
            BudgetFunctions.truth = truth
            return 0
        }
    }

    class Task(val sentence: Sentence, val budget: BudgetValue)

    class Memory {
        var terms = LinkedHashMap<String, Term>()
        var exportStrings: MutableCollection<String> = LinkedHashSet()
        var gson: Gson? = null
        fun nameToListedTerm(termName: String): Term? { //            return terms.computeIfAbsent(termName, (named -> new Term(named)));
            return terms[termName] //
        }

        fun init() {
            gson = GsonBuilder().setPrettyPrinting().create()
        }

        fun workCycle(clock: Long) {}
        fun inputTask(task: Task?) {
            val x = gson!!.toJson(task)
            println(x)
        }
    }

    internal class Inheritance(val subject: Term?, val predicate: Term?, val memory: Memory?) : Statement(subject, predicate, memory) {
        override fun operator(): String? {
            return null
        }

    }

    internal class Similarity(val subject: Term?, val predicate: Term?, val memory: Memory?) : Statement("") {
        override fun operator(): String? {
            return null
        }

    }

/*
    abstract class Statement : CompoundTerm {
        private var subject: Term? = null
        private var predicate: Term? = null
        private var memory: Memory? = null
        var components: List<Term>? = null

        constructor(subject: Term?, predicate: Term?, memory: Memory?) : super("") {
            this.subject = subject
            this.predicate = predicate
            this.memory = memory
        }

        constructor(s: String) : super(s) {}

        */
/**
         * Check the validity of a potential Statement. [To be refined]
         *
         *
         * Minimum requirement: the two terms cannot be the same, or containing each other as component
         *
         * @return Whether The Statement is invalid
         *//*

        fun invalid(): Boolean {
            return invalidStatement(getSubject(), getPredicate())
        }

        */
/**
         * Return the first component of the statement
         *
         * @return The first component
         *//*

        fun getSubject(): Term {
            return components!![0]
        }

        */
/**
         * Return the second component of the statement
         *
         * @return The second component
         *//*

        fun getPredicate(): Term {
            return components!![1]
        }

        */
/**
         * Override the default in making the nameStr of the current term from existing fields
         *
         * @return the nameStr of the term
         *//*

        fun makeName(): String {
            return makeStatementName(getSubject(), operator(), getPredicate())
        }

        abstract fun operator(): String?
        internal class Instance(val subject: Term?, val predicate: Term?, val memory: Memory?) : Statement("") {
            override fun operator(): String? {
                return null
            }

        }

        internal class Property(val subject: Term?, val predicate: Term?, val memory: Memory?) : Statement("") {
            override fun operator(): String? {
                return null
            }

        }

        internal class InstanceProperty(private val subject: Term?, private val predicate: Term?, private val memory: Memory?) : Statement("") {
            override fun operator(): String? {
                return null
            }

        }

        internal class Implication : Statement {
            constructor(s: String) : super(s) {}
            constructor(subject: Term?, predicate: Term?, memory: Memory?) : super("") {}

            override fun operator(): String? {
                return null
            }
        }

        internal class Equivalence(private val subject: Term?, private val predicate: Term?, private val memory: Memory) : Statement("") {
            override fun operator(): String? {
                return null
            }

        }

        companion object {
            */
/**
             * Make a Statement from String, called by StringParser
             *
             * @param relation  The relation String
             * @param subject   The first component
             * @param predicate The second component
             * @param memory    Reference to the memory
             * @return The Statement built
             *//*

            fun make(relation: String, subject: Term?, predicate: Term?, memory: Memory): Statement? {
                if (invalidStatement(subject, predicate)) {
                    return null
                }
                if (relation == Symbols.INHERITANCE_RELATION) {
                    return Inheritance(subject, predicate, memory)
                }
                if (relation == Symbols.SIMILARITY_RELATION) {
                    return Similarity(subject, predicate, memory)
                }
                if (relation == Symbols.INSTANCE_RELATION) {
                    return Instance(subject, predicate, memory)
                }
                if (relation == Symbols.PROPERTY_RELATION) {
                    return Property(subject, predicate, memory)
                }
                if (relation == Symbols.INSTANCE_PROPERTY_RELATION) {
                    return InstanceProperty(subject, predicate, memory)
                }
                if (relation == Symbols.IMPLICATION_RELATION) {
                    return Implication(subject, predicate, memory)
                }
                return if (relation == Symbols.EQUIVALENCE_RELATION) {
                    Equivalence(subject, predicate, memory)
                } else null
            }

            */
/**
             * Make a Statement from given components, called by the rules
             *
             * @param subj      The first component
             * @param pred      The second component
             * @param statement A sample statement providing the class type
             * @param memory    Reference to the memory
             * @return The Statement built
             *//*

            fun make(statement: Statement?, subj: Term?, pred: Term?, memory: Memory): Statement? {
                if (statement is Inheritance) {
                    return Inheritance(subj, pred, memory)
                }
                if (statement is Similarity) {
                    return Similarity(subj, pred, memory)
                }
                if (statement is Implication) {
                    return Implication(subj, pred, memory)
                }
                return if (statement is Equivalence) {
                    Equivalence(subj, pred, memory)
                } else null
            }

            */
/**
             * Make a symmetric Statement from given components and temporal information, called by the rules
             *
             * @param statement A sample asymmetric statement providing the class type
             * @param subj      The first component
             * @param pred      The second component
             * @param memory    Reference to the memory
             * @return The Statement built
             *//*

            fun makeSym(statement: Statement?, subj: Term?, pred: Term?, memory: Memory): Statement? {
                if (statement is Inheritance) {
                    return Similarity(subj, pred, memory)
                }
                return if (statement is Implication) {
                    Equivalence(subj, pred, memory)
                } else null
            }

            */
/**
             * Default method to make the nameStr of an image term from given fields
             *
             * @param subject   The first component
             * @param predicate The second component
             * @param relation  The relation operator
             * @return The nameStr of the term
             *//*

            fun makeStatementName(subject: Term, relation: String?, predicate: Term): String {
                val nameStr = StringBuilder()
                nameStr.append(Symbols.STATEMENT_OPENER)
                nameStr.append(subject.name)
                nameStr.append(' ').append(relation).append(' ')
                nameStr.append(predicate.name)
                nameStr.append(Symbols.STATEMENT_CLOSER)
                return nameStr.toString()
            }

            */
/**
             * Check the validity of a potential Statement. [To be refined]
             *
             *
             * Minimum requirement: the two terms cannot be the same, or containing each other as component
             *
             * @param subject   The first component
             * @param predicate The second component
             * @return Whether The Statement is invalid
             *//*

            fun invalidStatement(subject: Term?, predicate: Term?): Boolean {
                if (subject == predicate) {
                    return true
                }
                if (subject is CompoundTerm && subject.containComponent(predicate)) {
                    return true
                }
                if (predicate is CompoundTerm && predicate.containComponent(subject)) {
                    return true
                }
                if (subject is Statement && predicate is Statement) {
                    val s1 = subject
                    val s2 = predicate
                    val t11 = s1.getSubject()
                    val t12 = s1.getPredicate()
                    val t21 = s2.getSubject()
                    val t22 = s2.getPredicate()
                    if (t11 == t22 && t12 == t21) {
                        return true
                    }
                }
                return false
            }

            */
/**
             * Check Statement relation symbol, called in StringPaser
             *
             * @param s0 The String to be checked
             * @return if the given String is a relation symbol
             *//*

            fun isRelation(s0: String): Boolean {
                val s = s0.trim { it <= ' ' }
                return Set.of(Symbols.INHERITANCE_RELATION,
                        Symbols.SIMILARITY_RELATION,
                        Symbols.INSTANCE_RELATION,
                        Symbols.PROPERTY_RELATION,
                        Symbols.INSTANCE_PROPERTY_RELATION,
                        Symbols.IMPLICATION_RELATION,
                        Symbols.EQUIVALENCE_RELATION).contains(s)
            }
        }
    }
*/

    internal class Sentence(val content: Term?, val punc: Char, val truth: TruthValue?, val stamp: StampHandle) {
        var revisible = false

    }

    open class CompoundTerm : Term {
        private var subject: Term? = null
        private var predicate: Term? = null
        private var memory: Memory? = null

        constructor(subject: Term?, predicate: Term?, memory: Memory?) : super("") {
            this.subject = subject
            this.predicate = predicate
            this.memory = memory
        }

        constructor(s: String) : super(s) {}

        fun containComponent(predicate: Term?): Boolean {
            return false
        }

        companion object {
            val COMP_OPERATORS = Set.of(
                    Symbols.INTERSECTION_EXT_OPERATOR,
                    Symbols.INTERSECTION_INT_OPERATOR,
                    Symbols.DIFFERENCE_EXT_OPERATOR,
                    Symbols.DIFFERENCE_INT_OPERATOR,
                    Symbols.PRODUCT_OPERATOR,
                    Symbols.IMAGE_EXT_OPERATOR,
                    Symbols.IMAGE_INT_OPERATOR,
                    Symbols.NEGATION_OPERATOR,
                    Symbols.DISJUNCTION_OPERATOR,
                    Symbols.CONJUNCTION_OPERATOR
            )

            /**
             * Parse a String to create a CompoundTerm.
             *
             * @param s0 The String to be parsed
             * @return the Term generated from the String
             * @throws InvalidInputException the String cannot be
             * parsed into a Term
             */
            @Throws(InvalidInputException::class)
            fun parseCompoundTerm(s0: String ): Term? {
                val s = s0.trim { it <= ' ' }
                val firstSeparator = s.indexOf(Symbols.ARGUMENT_SEPARATOR)
                val op = s.substring(0, firstSeparator).trim { it <= ' ' }
                assert(isOperator(op)) { "unknown operator: $op" }
                val arg = parseArguments(s.substring(firstSeparator + 1) + Symbols.ARGUMENT_SEPARATOR, memory)
                return make(op, arg, memory) ?: error("invalid compound term")
            }

            fun make(op: String?, arg: ArrayList<Term?>?, memory: Memory?): Term? {
                return null
            }

            /**
             * Check CompoundTerm operator symbol
             *
             * @param s The String to be checked
             * @return if the given String is an operator symbol
             */
            fun isOperator(s: String): Boolean {
                return COMP_OPERATORS.contains(s)
            }
        }
    }

    internal class ExtensionSet(val parseArguments: ArrayList<Term?>, val memory: Memory) : Term("")

    internal class IntensionSet(val parseArguments: ArrayList<Term?>, val memory: Memory) : Term(" ")

    internal class Equivalence(val subject: Term?, val predicate: Term?, val memory: Memory?) : Statement("") {
        override fun operator(): String? {
            return null
        }

    }
}