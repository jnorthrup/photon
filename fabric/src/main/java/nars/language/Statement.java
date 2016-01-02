/*
 * Statement.java
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
package nars.language;

import java.util.ArrayList;
import java.util.List;

import nars.data.TermStruct;
import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A statement is a compound term, consisting of a subject, a predicate,
 * and a relation symbol in between. It can be of either first-order or higher-order.
 */
public abstract class Statement extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     * @param arg The component list of the term
     */
    protected Statement(List<Term> arg) {
        super(arg);
    }

    /**
     * Default constructor
     */
    protected Statement() {
    }

    /**
     * Constructor with full values, called by clone
     * @param n The nameStr of the term
     * @param cs Component list
     * @param con Constant indicator
     * @param i Syntactic complexity of the compound
     */
    protected Statement(String n, List<Term> cs, boolean con, int i) {
        super(n, cs, con, i);
    }

    /**
     * Make a Statement from String, called by StringParser
     * @param relation The relation String
     * @param subject The first component
     * @param predicate The second component
     * @param memory Reference to the memeory
     * @return The Statement built
     */
    public static Statement make(String relation, Term subject, Term predicate, Memory memory) {
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        if (relation.equals(Symbols.INHERITANCE_RELATION)) {
            return Inheritance.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.SIMILARITY_RELATION)) {
            return Similarity.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_RELATION)) {
            return Instance.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.PROPERTY_RELATION)) {
            return Property.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.INSTANCE_PROPERTY_RELATION)) {
            return InstanceProperty.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.IMPLICATION_RELATION)) {
            return Implication.Companion.make(subject, predicate, memory);
        }
        if (relation.equals(Symbols.EQUIVALENCE_RELATION)) {
            return Equivalence.Companion.make(subject, predicate, memory);
        }
        return null;
    }

    /**
     * Make a Statement from given components, called by the rules
     * @return The Statement built
     * @param subj The first component
     * @param pred The second component
     * @param statement A sample statement providing the class type
     * @param memory Reference to the memeory
     */
    public static Statement make(Statement statement, Term subj, Term pred, Memory memory) {
        if (statement instanceof Inheritance) {
            return Inheritance.make(subj, pred, memory);
        }
        if (statement instanceof Similarity) {
            return Similarity.make(subj, pred, memory);
        }
        if (statement instanceof Implication) {
            return Implication.Companion.make(subj, pred, memory);
        }
        if (statement instanceof Equivalence) {
            return Equivalence.Companion.make(subj, pred, memory);
        }
        return null;
    }

    /**
     * Override the default in making the nameStr of the current term from existing fields
     * @return the nameStr of the term
     */
    @Override
    protected String makeName() {
        return makeStatementName(getSubject(), operator(), getPredicate());
    }

    /**
     * Default method to make the nameStr of an image term from given fields
     * @param subject The first component
     * @param predicate The second component
     * @param relation The relation operator
     * @return The nameStr of the term
     */
    protected static String makeStatementName(TermStruct subject, String relation, TermStruct predicate) {
        StringBuffer nameStr = new StringBuffer();
        nameStr.append(Symbols.STATEMENT_OPENER);
        nameStr.append(subject.getName());
        nameStr.append(' ' + relation + ' ');
        nameStr.append(predicate.getName());
        nameStr.append(Symbols.STATEMENT_CLOSER);
        return nameStr.toString();
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * Minimum requirement: the two terms cannot be the same, or containing each other as component
     * @param subject The first component
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
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * Minimum requirement: the two terms cannot be the same, or containing each other as component
     * @return Whether The Statement is invalid
     */
    public boolean invalid() {
        return invalidStatement(getSubject(), getPredicate());
    }

    /**
     * Return the first component of the statement
     * @return The first component
     */
    public Term getSubject() {
        return getComponents().get(0);
    }

    /**
     * Return the second component of the statement
     * @return The second component
     */
    public Term getPredicate() {
        return getComponents().get(1);
    }
}
