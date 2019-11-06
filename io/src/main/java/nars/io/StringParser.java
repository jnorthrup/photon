package nars.io;

//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Stream;

import static nars.io.Symbols.*;
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
public class StringParser {
     static int steps;

    public static void main(String[] args) throws FileNotFoundException  {
        BufferedReader input;
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//        var objectWriter = mapper.writerWithDefaultPrettyPrinter();
        if (args.length > 0) {
            input = new BufferedReader(new FileReader(args[0]));
        } else input = new BufferedReader(new InputStreamReader(System.in));

        Memory memory = new Memory();
        Stream<String> lines = input.lines();
        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext()) {

            String text = iterator.next();
            if (!text.isBlank()) {
                char c = text.charAt(0);
                if (c == Symbols.RESET_MARK) {
                    memory = new Memory();
                    memory.getExportStrings().add(text);
                } else if (c != Symbols.COMMENT_MARK) {
                    // read NARS language or an integer : TODO duplicated code
                    try {
                        int i = Integer.parseInt(text);
                        walk(i);
                    } catch (NumberFormatException e) {
                        Task task = StringParser.parseExperience(new StringBuffer(text), memory, System.currentTimeMillis());
                        if (task != null) {
                            memory.inputTask(task);
                        }
                    }
                }
            }
        }
    }


    static void walk(int i) {
        steps += i;
    }


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
        int i = buffer.indexOf(PREFIX_MARK + "");
        if (i > 0) {
            String prefix = buffer.substring(0, i).trim();
            if (!OUTPUT_LINE.equals(prefix)) {
                if (INPUT_LINE.equals(prefix)) {
                    buffer.delete(0, i + 1);
                }
            } else return null;
        }
        char c = buffer.charAt(buffer.length() - 1);
        if (c == STAMP_CLOSER) {
            int j = buffer.lastIndexOf(STAMP_OPENER + "");
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
        StringBuffer buffer = new StringBuffer(s);
        Task task = null;
        try {
            String budgetString = getBudgetString(buffer);
            String truthString = getTruthString(buffer);
            String str = buffer.toString().trim();
            int last = str.length() - 1;
            char punc = str.charAt(last);
            StampHandle stamp = new StampHandle(time);

            TruthValue truth = parseTruth(truthString, punc);
            Term content = parseTerm(str.substring(0, last), memory);
            Sentence sentence = new Sentence(content, punc, truth, stamp);
            if ((content instanceof Conjunction) && Variable.containVarDep(content.getName()))
                sentence.setRevisible(false);
            BudgetValue budget = parseBudget(budgetString, punc, truth);
            task = new Task(sentence, budget);
        } catch (InvalidInputException e) {
            String message = " !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.getMessage();
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
    public static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (s.charAt(0) != BUDGET_VALUE_MARK) {
            return null;
        }
        int i = s.indexOf(BUDGET_VALUE_MARK + "", 1);    // looking for the end
        assert i >= 0 : "missing budget closer";
        String budgetString = s.substring(1, i).trim();
        assert budgetString.length() != 0 : "empty budget";
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
    public static String getTruthString(StringBuffer s) throws InvalidInputException {
        int last = s.length() - 1;
        if (s.charAt(last) != TRUTH_VALUE_MARK) {       // use default
            return null;
        }
        int first = s.indexOf(TRUTH_VALUE_MARK + "");    // looking for the beginning
        // no matching closer
        assert first != last : "missing truth mark";
        String truthString = s.substring(first + 1, last).trim();
        // empty usage
        assert truthString.length() != 0 : "empty truth";
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
    public static TruthValue parseTruth(String s, char type) {
        if (type != QUESTION_MARK) {
            float frequency = 1.0f;
            float confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE;
            if (s != null) {
                int i = s.indexOf(VALUE_SEPARATOR);
                if (i < 0) {
                    frequency = Float.parseFloat(s);
                } else {
                    frequency = Float.parseFloat(s.substring(0, i));
                    confidence = Float.parseFloat(s.substring(i + 1));
                }
            }
            return new TruthValue(frequency, confidence);
        }
        return null;
    }

    /**
     * parse the input String into a BudgetValue
     *
     * @param truth       the TruthValue of the task
     * @param inputString input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     *                                                    be parsed into a BudgetValue
     */
    public static BudgetValue parseBudget(String inputString, char punctuation, TruthValue truth) throws InvalidInputException {
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
        if (inputString != null) { // overrite default
            int i = inputString.indexOf(VALUE_SEPARATOR);
            if (i < 0) {        // default durability
                priority = Float.parseFloat(inputString);
            } else {
                priority = Float.parseFloat(inputString.substring(0, i));
                durability = Float.parseFloat(inputString.substring(i + 1));
            }
        }
        float quality = (truth == null) ? 1 : BudgetFunctions.truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     * <p>
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an ExtensionSet; 3. [A1, ..., An] is an
     * IntensionSet; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
     */
    public static Term parseTerm(String s0, Memory memory) {
        Term result = null;
        String s = s0.replaceAll("\\s+", "");
        try {
            if (s.length() != 0) {
                Term t = memory.nameToListedTerm(s);    // existing constant or operator
                if (t == null) {
                    int index = s.length() - 1;
                    char first = s.charAt(0);
                    char last = s.charAt(index);
                    switch (first) {
                        case COMPOUND_TERM_OPENER:
                            assert last == COMPOUND_TERM_CLOSER : "missing CompoundTerm closer";
                            CompoundTerm compoundTerm = (CompoundTerm) CompoundTerm.parseCompoundTerm(s.substring(1, index), memory);
                            result = compoundTerm;
                            break;
                        case SET_EXT_OPENER:
                            assert last == SET_EXT_CLOSER : "missing ExtensionSet closer";
                            result = (ExtensionSet) new ExtensionSet(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                            break;
                        case SET_INT_OPENER:
                            assert last == SET_INT_CLOSER : "missing IntensionSet closer";
                            result = (IntensionSet) new IntensionSet(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR, memory), memory);
                            break;
                        case STATEMENT_OPENER:
                            assert last == STATEMENT_CLOSER : "missing Statement closer";
                            result = (Statement) parseStatement(s.substring(1, index), memory);
                            break;
                        default:
                            result = parseAtomicTerm(s);
                            break;
                    }
                } else {
                    result = t;
                }                           // existing Term
            } else {
                throw new InvalidInputException("missing content");
            }
        } catch (Throwable e) {
            String message = " !!! INVALID INPUT: parseTerm: " + s + " --- " + e.getMessage();
            System.out.println(message);
//            showWarning(message);
        }
        return result;
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
    public static Term parseAtomicTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        assert s.length() != 0 : "missing term";
        // invalid characters in a name
        assert !s.contains(" ") : "invalid term";
        if (Variable.containVar(s)) {
            return new Variable(s);
        } else {
            return new Term(s);
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
     *                                                    parsed into a Term
     */
    public static Statement parseStatement(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        int i = topRelation(s);
        assert i >= 0 : "invalid statement";
        String relation = s.substring(i, i + 3);
        Term subject = parseTerm(s.substring(0, i), memory);
        Term predicate = parseTerm(s.substring(i + 3), memory);
        Statement t = Statement.make(relation, subject, predicate, memory);
        assert t != null : "invalid statement";
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
    public static ArrayList<Term> parseArguments(String s0, Memory memory) throws InvalidInputException {
        String s = s0.trim();
        ArrayList<Term> list = new ArrayList<>();
        int start = 0;
        int end = 0;
        while (end < s.length() - 1) {
            end = nextSeparator(s, start);
            // recursive call
            Term t = parseTerm(s.substring(start, end), memory);
            list.add(t);
            start = end + 1;
        }
        assert !list.isEmpty() : "null argument";
        return list;
    }

    /**
     * Locate the first top-level separator in a CompoundTerm
     *
     * @param s     The String to be parsed
     * @param first The starting index
     * @return the index of the next seperator in a String
     */
    public static int nextSeparator(String s, int first) {
        int levelCounter = 0;
        int i = first;
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
    public static int topRelation(String s) {
        int result = -1;      // need efficiency improvement
        int levelCounter = 0;
        int i = 0;
        while (i < s.length() - 3) {    // don't need to check the last 3 characters
            if ((levelCounter == 0) && (Statement.isRelation(s.substring(i, i + 3)))) {
                result = i;
                break;
            }
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            }
            i++;
        }
        return result;
    }

    /**
     * Check CompoundTerm opener symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is an opener symbol
     */
    public static boolean isOpener(String s, int i) {
        boolean result = false;
        char c = s.charAt(i);
        boolean b = (c == Symbols.COMPOUND_TERM_OPENER)
                || (c == SET_EXT_OPENER)
                || (c == SET_INT_OPENER)
                || (c == STATEMENT_OPENER);
        if (b) {
            if (i + 3 > s.length() || !Statement.isRelation(s.substring(i, i + 3))) {
                result = true;
            }
        }
        return result;
    }


    /* ---------- recognize symbols ---------- */

    /**
     * Check CompoundTerm closer symbol
     *
     * @param s The String to be checked
     * @param i The starting index
     * @return if the given String is a closer symbol
     */
    public static boolean isCloser(String s, int i) {
        boolean result = false;
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_CLOSER)
                || (c == SET_EXT_CLOSER)
                || (c == SET_INT_CLOSER)
                || (c == STATEMENT_CLOSER);
        if (b) {
            if (i < 2 || !Statement.isRelation(s.substring(i - 2, i + 1))) {
                result = true;
            }
        }
        return result;
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
    public static Statement make(String relation, Term subject, Term predicate, Memory memory) {
        if (Statement.invalidStatement(subject, predicate)) {
            return null;
        }
        if (relation.equals(Symbols.INHERITANCE_RELATION)) {
            return new Inheritance(subject, predicate, memory);
        }
        if (relation.equals(Symbols.SIMILARITY_RELATION)) {
            return new Similarity(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_RELATION)) {
            return new Statement.Instance(subject, predicate, memory);
        }
        if (relation.equals(Symbols.PROPERTY_RELATION)) {
            return new Statement.Property(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_PROPERTY_RELATION)) {
            return new Statement.InstanceProperty(subject, predicate, memory);
        }
        if (relation.equals(Symbols.IMPLICATION_RELATION)) {
            return new Statement.Implication(subject, predicate, memory);
        }
        if (relation.equals(Symbols.EQUIVALENCE_RELATION)) {
            return (Equivalence) new Equivalence(subject, predicate, memory);
        }
        return null;
    }

    /**
     * All kinds of invalid input lines
     */
    public static class InvalidInputException extends Exception {

        /**
         * An invalid input line.
         *
         * @param s type of error
         */
        InvalidInputException(String s) {
            super(s);
        }

    }

    static class StampHandle {

        public StampHandle(long time) {
        }
    }

    static class TruthValueRefier {

    }

    static class Term {

         final String name;

        public Term(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }


    }

    static class Variable extends Term {

        public Variable(String s) {
            super(s);

        }

        public static boolean containVarDep(String name) {
            return false;
        }

        public static boolean containVar(String s) {
            return false;
        }

    }

    static class Conjunction extends Term {

        public Conjunction(String s) {
            super(s);
        }
    }

    static class BudgetValue {

         final float priority;
         final float durability;
         final float quality;

        public BudgetValue(float priority, float durability, float quality) {
            this.priority = priority;
            this.durability = durability;
            this.quality = quality;
        }
    }

    static class TruthValue {
         final float frequency;
         final float confidence;

        public TruthValue(float frequency, float confidence) {
            this.frequency = frequency;
            this.confidence = confidence;
        }
    }

    static class BudgetFunctions {
         static TruthValue truth;

        public static float truthToQuality(TruthValue truth) {
            BudgetFunctions.truth = truth;
            return 0;
        }
    }

    static class Task {

         final Sentence sentence;
         final BudgetValue budget;

        public Task(Sentence sentence, BudgetValue budget) {

            this.sentence = sentence;
            this.budget = budget;
        }

    }

    static class Memory {

         LinkedHashMap<String, Term> terms = new LinkedHashMap<>();
         Collection<String> exportStrings = new LinkedHashSet<>();
         Gson gson;


        public Term nameToListedTerm(String termName) {
//            return terms.computeIfAbsent(termName, (named -> new Term(named)));
            return terms.get(termName);//
        }

        public Collection<String> getExportStrings() {
            return exportStrings;
        }

        public void setExportStrings(Collection<String> exportStrings) {
            this.exportStrings = exportStrings;
        }

        public void init() {
 this.gson=        new GsonBuilder().setPrettyPrinting().create();
        }

        public void workCycle(long clock) {

        }

        public void inputTask(Task task)  {
            var x =gson.toJson(task);
            System.out.println(x);

        }
    }

     static class Inheritance extends Statement {


         final Term subject;
         final Term predicate;
         final Memory memory;


        public Inheritance(Term subject, Term predicate, Memory memory) {
            super(subject, predicate, memory);
            this.subject = subject;
            this.predicate = predicate;
            this.memory = memory;
        }

        @Override
        String operator() {
            return null;
        }
    }

     static class Similarity extends Statement {
         final Term subject;
         final Term predicate;
         final Memory memory;

        public Similarity(Term subject, Term predicate, Memory memory) {
            super("");
            this.subject = subject;
            this.predicate = predicate;
            this.memory = memory;
        }


        @Override
        String operator() {
            return null;
        }
    }

    public static abstract class Statement extends CompoundTerm {
        private  Term subject;
        private  Term predicate;
        private  Memory memory;
        List<Term> components;

        public Statement(Term subject, Term predicate, Memory memory) {
          super("");  this.subject = subject;
            this.predicate = predicate;
            this.memory = memory;
        }

        public Statement(String s) {
            super(s);
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
        public static Statement make(String relation, Term subject, Term predicate, Memory memory) {
            if (invalidStatement(subject, predicate)) {
                return null;
            }
            if (relation.equals(Symbols.INHERITANCE_RELATION)) {
                return (Inheritance) new Inheritance(subject, predicate, memory);
            }
            if (relation.equals(Symbols.SIMILARITY_RELATION)) {
                return (Similarity) new Similarity(subject, predicate, memory);
            }
            if (relation.equals(Symbols.INSTANCE_RELATION)) {
                return (Instance) new Instance(subject, predicate, memory);
            }
            if (relation.equals(Symbols.PROPERTY_RELATION)) {
                return (Property) new Property(subject, predicate, memory);
            }
            if (relation.equals(Symbols.INSTANCE_PROPERTY_RELATION)) {
                return (InstanceProperty) new InstanceProperty(subject, predicate, memory);
            }
            if (relation.equals(Symbols.IMPLICATION_RELATION)) {
                return (Implication) new Implication(subject, predicate, memory);
            }
            if (relation.equals(Symbols.EQUIVALENCE_RELATION)) {
                return (Equivalence) new Equivalence(subject, predicate, memory);
            }
            return null;
        }

        /**
         * Make a Statement from given components, called by the rules
         *
         * @param subj      The first component
         * @param pred      The second component
         * @param statement A sample statement providing the class type
         * @param memory    Reference to the memory
         * @return The Statement built
         */
        public static Statement make(Statement statement, Term subj, Term pred, Memory memory) {
            if (statement instanceof Inheritance) {
                return new Inheritance(subj, pred, memory);
            }
            if (statement instanceof Similarity) {
                return new Similarity(subj, pred, memory);
            }
            if (statement instanceof Implication) {
                return new Implication(subj, pred, memory);
            }
            if (statement instanceof Equivalence) {
                return new Equivalence(subj, pred, memory);
            }
            return null;
        }

        /**
         * Make a symmetric Statement from given components and temporal information, called by the rules
         *
         * @param statement A sample asymmetric statement providing the class type
         * @param subj      The first component
         * @param pred      The second component
         * @param memory    Reference to the memory
         * @return The Statement built
         */
        public static Statement makeSym(Statement statement, Term subj, Term pred, Memory memory) {
            if (statement instanceof Inheritance) {
                return new Similarity(subj, pred, memory);
            }
            if (statement instanceof Implication) {
                return new Equivalence(subj, pred, memory);
            }
            return null;
        }

        /**
         * Default method to make the nameStr of an image term from given fields
         *
         * @param subject   The first component
         * @param predicate The second component
         * @param relation  The relation operator
         * @return The nameStr of the term
         */
        public static String makeStatementName(Term subject, String relation, Term predicate) {
            StringBuilder nameStr = new StringBuilder();
            nameStr.append(Symbols.STATEMENT_OPENER);
            nameStr.append(subject.getName());
            nameStr.append(' ').append(relation).append(' ');
            nameStr.append(predicate.getName());
            nameStr.append(Symbols.STATEMENT_CLOSER);
            return nameStr.toString();
        }

        /**
         * Check the validity of a potential Statement. [To be refined]
         * <p>
         * Minimum requirement: the two terms cannot be the same, or containing each other as component
         *
         * @param subject   The first component
         * @param predicate The second component
         * @return Whether The Statement is invalid
         */
        public static boolean invalidStatement(Term subject, Term predicate) {
            if (subject.equals(predicate)) {
                return true;
            }
            if ((subject instanceof CompoundTerm) && ((CompoundTerm) subject).containComponent(predicate)) {
                return true;
            }
            if ((predicate instanceof CompoundTerm) && ((CompoundTerm) predicate).containComponent(subject)) {
                return true;
            }
            if ((subject instanceof Statement) && (predicate instanceof Statement)) {
                Statement s1 = (Statement) subject;
                Statement s2 = (Statement) predicate;
                Term t11 = s1.getSubject();
                Term t12 = s1.getPredicate();
                Term t21 = s2.getSubject();
                Term t22 = s2.getPredicate();
                if (t11.equals(t22) && t12.equals(t21)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check Statement relation symbol, called in StringPaser
         *
         * @param s0 The String to be checked
         * @return if the given String is a relation symbol
         */
        public static boolean isRelation(String s0) {
            String s = s0.trim();
            return Set.of(INHERITANCE_RELATION,
                    SIMILARITY_RELATION,
                    INSTANCE_RELATION,
                    PROPERTY_RELATION,
                    INSTANCE_PROPERTY_RELATION,
                    IMPLICATION_RELATION,
                    EQUIVALENCE_RELATION).contains(s);


        }

        /**
         * Check the validity of a potential Statement. [To be refined]
         * <p>
         * Minimum requirement: the two terms cannot be the same, or containing each other as component
         *
         * @return Whether The Statement is invalid
         */
        public boolean invalid() {
            return invalidStatement(getSubject(), getPredicate());
        }

        /**
         * Return the first component of the statement
         *
         * @return The first component
         */
        public Term getSubject() {
            return components.get(0);
        }

        /**
         * Return the second component of the statement
         *
         * @return The second component
         */
        public Term getPredicate() {
            return components.get(1);
        }

        /**
         * Override the default in making the nameStr of the current term from existing fields
         *
         * @return the nameStr of the term
         */
        public String makeName() {
            return makeStatementName(getSubject(), operator(), getPredicate());
        }

        abstract String operator();

        static class Instance extends Statement {
             final Term subject;
             final Term predicate;
             final Memory memory;

            public Instance(Term subject, Term predicate, Memory memory) {
                super("");
                this.subject = subject;
                this.predicate = predicate;
                this.memory = memory;
            }

            @Override
            String operator() {
                return null;
            }
        }

         static class Property extends Statement {

             final Term subject;
             final Term predicate;
             final Memory memory;

            public Property(Term subject, Term predicate, Memory memory) {
                super("");
                this.subject = subject;
                this.predicate = predicate;
                this.memory = memory;
            }

            @Override
            String operator() {
                return null;
            }
        }

         static class InstanceProperty extends Statement {
             private final Term subject;
             private final Term predicate;
             private final Memory memory;


            public InstanceProperty(Term subject, Term predicate, Memory memory) {
                super("");
                this.subject = subject;
                this.predicate = predicate;
                this.memory = memory;
            }

            @Override
            String operator() {
                return null;
            }
        }

         static class Implication extends Statement {
            public Implication(String s) {
                super(s);
            }

            public Implication(Term subject, Term predicate, Memory memory) {
                super("");
            }

            @Override
            String operator() {
                return null;
            }
        }

         static class Equivalence extends Statement {


             private final Term subject;
             private final Term predicate;
             private final Memory memory;

             public Equivalence(Term subject, Term predicate, Memory memory) {
                 super("");
                 this.subject = subject;
                 this.predicate = predicate;
                 this.memory = memory;
             }

            @Override
            String operator() {
                return null;
            }
        }
    }

    static class Sentence {

         final Term content;
         final char punc;
         final TruthValue truth;
         final StampHandle stamp;
        boolean revisible;

        public Sentence(Term content, char punc, TruthValue truth, StampHandle stamp) {

            this.content = content;
            this.punc = punc;
            this.truth = truth;
            this.stamp = stamp;
        }

        public boolean getRevisible() {
            return revisible;
        }

        public void setRevisible(boolean revisible) {
            this.revisible = revisible;
        }

    }

     static class CompoundTerm extends Term {

        public static final Set<String> COMP_OPERATORS = Set.of(
                INTERSECTION_EXT_OPERATOR,
                INTERSECTION_INT_OPERATOR,
                DIFFERENCE_EXT_OPERATOR,
                DIFFERENCE_INT_OPERATOR,
                PRODUCT_OPERATOR,
                IMAGE_EXT_OPERATOR,
                IMAGE_INT_OPERATOR,
                NEGATION_OPERATOR,
                DISJUNCTION_OPERATOR,
                CONJUNCTION_OPERATOR
        );
         private  Term subject;
         private  Term predicate;
         private  Memory memory;

         public CompoundTerm(Term subject, Term predicate, Memory memory) {
            super("");

             this.subject = subject;
             this.predicate = predicate;
             this.memory = memory;
         }

         public CompoundTerm(String s) {
             super(s);
         }

         /**
         * Parse a String to create a CompoundTerm.
         *
         * @param s0 The String to be parsed
         * @return the Term generated from the String
         * @throws InvalidInputException the String cannot be
         *                               parsed into a Term
         */
        public static Term parseCompoundTerm(String s0, Memory memory) throws InvalidInputException {
            String s = s0.trim();
            int firstSeparator = s.indexOf(ARGUMENT_SEPARATOR);
            String op = s.substring(0, firstSeparator).trim();
            assert CompoundTerm.isOperator(op) : "unknown operator: " + op;
            ArrayList<Term> arg = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR, memory);
            Term t = CompoundTerm.make(op, arg, memory);
            assert t != null : "invalid compound term";
            return t;
        }

        static Term make(String op, ArrayList<Term> arg, Memory memory) {
            return null;
        }


        /**
         * Check CompoundTerm operator symbol
         *
         * @param s The String to be checked
         * @return if the given String is an operator symbol
         */
        public static boolean isOperator(String s) {
            return COMP_OPERATORS.contains(s);

        }

        protected boolean containComponent(Term predicate) {
            return false;
        }
    }

     static class ExtensionSet extends Term {
         final ArrayList<Term> parseArguments;
         final Memory memory;


        public ExtensionSet(ArrayList<Term> parseArguments, Memory memory) {
            super("");
            this.parseArguments = parseArguments;
            this.memory = memory;
        }
    }

     static class IntensionSet extends Term {

         final ArrayList<Term> parseArguments;
         final Memory memory;

        public IntensionSet(ArrayList<Term> parseArguments, Memory memory) {
            super(" ");
            this.parseArguments = parseArguments;
            this.memory = memory;
        }
    }

     static class Equivalence extends Statement {
         final Term subject;
         final Term predicate;
         final Memory memory;


        public Equivalence(Term subject, Term predicate, Memory memory) {
            super("");
            this.subject = subject;
            this.predicate = predicate;
            this.memory = memory;
        }

        @Override
        String operator() {
            return null;
        }
    }
}

/**
 * Collected system parameters. To be modified before compiling.
 */
class Parameters {

    /* ---------- initial values of run-time adjustable parameters ---------- */
    /**
     * Concept decay rate in ConceptBag, in [1, 99].
     */
    public static final int CONCEPT_FORGETTING_CYCLE = 10;
    /**
     * TaskLink decay rate in TaskLinkBag, in [1, 99].
     */
    public static final int TASK_LINK_FORGETTING_CYCLE = 20;
    /**
     * TermLink decay rate in TermLinkBag, in [1, 99].
     */
    public static final int TERM_LINK_FORGETTING_CYCLE = 50;
    /**
     * Silent threshold for task reporting, in [0, 100].
     */
    public static final int SILENT_LEVEL = 0;

    /* ---------- time management ---------- */
    /**
     * Task decay rate in TaskBuffer, in [1, 99].
     */
    public static final int NEW_TASK_FORGETTING_CYCLE = 1;
    /**
     * Maximum TermLinks checked for novelty for each TaskLink in TermLinkBag
     */
    public static final int MAX_MATCHED_TERM_LINK = 10;
    /**
     * Maximum TermLinks used in reasoning for each Task in Concept
     */
    public static final int MAX_REASONED_TERM_LINK = 3;

    /* ---------- logical parameters ---------- */
    /**
     * Evidential Horizon, the amount of future evidence to be considered.
     */
    public static final int HORIZON = 1;    // or 2, can be float
    /**
     * Reliance factor, the empirical confidence of analytical truth.
     */
    public static final float RELIANCE = (float) 0.9;    // the same as default confidence

    /* ---------- budget thresholds ---------- */
    /**
     * The budget threshold rate for task to be accepted.
     */
    public static final float BUDGET_THRESHOLD = (float) 0.01;

    /* ---------- default input values ---------- */
    /**
     * Default expectation for confirmation.
     */
    public static final float DEFAULT_CONFIRMATION_EXPECTATION = (float) 0.8;
    /**
     * Default expectation for confirmation.
     */
    public static final float DEFAULT_CREATION_EXPECTATION = (float) 0.66;
    /**
     * Default confidence of input judgment.
     */
    public static final float DEFAULT_JUDGMENT_CONFIDENCE = (float) 0.9;
    /**
     * Default priority of input judgment
     */
    public static final float DEFAULT_JUDGMENT_PRIORITY = (float) 0.8;
    /**
     * Default durability of input judgment
     */
    public static final float DEFAULT_JUDGMENT_DURABILITY = (float) 0.8;
    /**
     * Default priority of input question
     */
    public static final float DEFAULT_QUESTION_PRIORITY = (float) 0.9;
    /**
     * Default durability of input question
     */
    public static final float DEFAULT_QUESTION_DURABILITY = (float) 0.9;

    /* ---------- space management ---------- */
    /**
     * Level granularity in Bag, two digits
     */
    public static final int BAG_LEVEL = 100;
    /**
     * Level separation in Bag, one digit, for display (run-time adjustable) and management (fixed)
     */
    public static final int BAG_THRESHOLD = 10;
    /**
     * Hashtable load factor in Bag
     */
    public static final float LOAD_FACTOR = (float) 0.5;
    /**
     * Size of ConceptBag
     */
    public static final int CONCEPT_BAG_SIZE = 1000;
    /**
     * Size of TaskLinkBag
     */
    public static final int TASK_LINK_BAG_SIZE = 20;
    /**
     * Size of TermLinkBag
     */
    public static final int TERM_LINK_BAG_SIZE = 100;
    /**
     * Size of TaskBuffer
     */
    public static final int TASK_BUFFER_SIZE = 10;

    /* ---------- avoiding repeated reasoning ---------- */
    /**
     * Maximum length of Stamp, a power of 2
     */
    public static final int MAXIMUM_STAMP_LENGTH = 8;
    /**
     * Remember recently used TermLink on a Task
     */
    public static final int TERM_LINK_RECORD_LENGTH = 10;
    /**
     * Maximum number of beliefs kept in a Concept
     */
    public static final int MAXIMUM_BELIEF_LENGTH = 7;
    /**
     * Maximum number of goals kept in a Concept
     */
    public static final int MAXIMUM_QUESTIONS_LENGTH = 5;
}


/**
 * The ASCII symbols used in I/O.
 */
class Symbols {

    /* sentence type and delimitors */
    public static final char JUDGMENT_MARK = '.';
    public static final char QUESTION_MARK = '?';

    /* variable type */
    public static final char VAR_INDEPENDENT = '$';
    public static final char VAR_DEPENDENT = '#';
    public static final char VAR_QUERY = '?';

    /* numerical value delimitors, must be different from the Term delimitors */
    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';

    /* CompountTerm delimitors, must use 4 different pairs */
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';
    public static final char STATEMENT_OPENER = '<';
    public static final char STATEMENT_CLOSER = '>';
    public static final char SET_EXT_OPENER = '{';
    public static final char SET_EXT_CLOSER = '}';
    public static final char SET_INT_OPENER = '[';
    public static final char SET_INT_CLOSER = ']';

    /* special characors in argument list */
    public static final char ARGUMENT_SEPARATOR = ',';
    public static final char IMAGE_PLACE_HOLDER = '_';

    /* CompountTerm operators, length = 1 */
    public static final String INTERSECTION_EXT_OPERATOR = "&";
    public static final String INTERSECTION_INT_OPERATOR = "|";
    public static final String DIFFERENCE_EXT_OPERATOR = "-";
    public static final String DIFFERENCE_INT_OPERATOR = "~";
    public static final String PRODUCT_OPERATOR = "*";
    public static final String IMAGE_EXT_OPERATOR = "/";
    public static final String IMAGE_INT_OPERATOR = "\\";

    /* CompoundStatement operators, length = 2 */
    public static final String NEGATION_OPERATOR = "--";
    public static final String DISJUNCTION_OPERATOR = "||";
    public static final String CONJUNCTION_OPERATOR = "&&";

    /* built-in relations, length = 3 */
    public static final String INHERITANCE_RELATION = "-->";
    public static final String SIMILARITY_RELATION = "<->";
    public static final String INSTANCE_RELATION = "{--";
    public static final String PROPERTY_RELATION = "--]";
    public static final String INSTANCE_PROPERTY_RELATION = "{-]";
    public static final String IMPLICATION_RELATION = "==>";
    public static final String EQUIVALENCE_RELATION = "<=>";

    /* experience line prefix */
    public static final String INPUT_LINE = "IN";
    public static final String OUTPUT_LINE = "OUT";
    public static final char PREFIX_MARK = ':';
    public static final char RESET_MARK = '*';
    public static final char COMMENT_MARK = '/';

    /* Stamp, display only */
    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';

    /* TermLink type, display only */
    public static final String TO_COMPONENT_1 = " @(";
    public static final String TO_COMPONENT_2 = ")_ ";
    public static final String TO_COMPOUND_1 = " _@(";
    public static final String TO_COMPOUND_2 = ") ";

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
}







