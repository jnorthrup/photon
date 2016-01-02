/*
 * CompoundTerm.java
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

import nars.entity.TermLink;
import nars.io.Symbols;
import nars.storage.Memory;

import java.util.*;
import java.util.concurrent.*;

/**
 * A CompoundTerm is a Term with internal (syntactic) structure
 * <p>
 * A CompoundTerm consists of a term operator with one or more component Terms.
 * <p>
 * This abstract class contains default methods for all CompoundTerms.
 */
public abstract class CompoundTerm extends Term {

    private List<Term> components;
    private int complexity;
    private boolean isConstant = true;

    /* ----- abstract methods to be implemented in subclasses ----- */
    /**
     * Abstract method to get the operator of the compound
     * @return The operator in a String
     */
    public abstract String operator();

    /**
     * Abstract clone method
     * @return A clone of the compound term
     */
    @Override
    public abstract Object clone();

    /* ----- object builders, called from subclasses ----- */

    /**
     * Constructor called from subclasses constructors to clone the fields
     * @param name Name
     * @param components Component list
     * @param isConstant Whether the term refers to a concept
     * @param complexity Complexity of the compound term
     */
    protected CompoundTerm(String name, List<Term> components, boolean isConstant, int complexity) {
        super(name);
        this.components = components;
        this.isConstant = isConstant;
        this.complexity = complexity;
    }

    /**
     * Default constructor
     */
    protected CompoundTerm() {
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     * @param components Component list
     */
    protected CompoundTerm(List<Term> components) {
        this.components = components;
        calcComplexity(this);
        name = makeName();
        isConstant = !Variable.containVar(name);
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     * @param name Name of the compound
     * @param components Component list
     */
    protected CompoundTerm(String name, List<Term> components) {
        super(name);
        isConstant = !Variable.containVar(name);
        this.components = components;
        calcComplexity(this);
    }

    /**
     * Change the oldName of a CompoundTerm, called after variable substitution
     * @param s The new oldName
     */
    public void setName(String s) {
        name = s;
    }

    /**
     * The complexity of the term is the sum of those of the components plus 1
     * @param compoundTerm
     */
     static void calcComplexity(CompoundTerm compoundTerm) {
        compoundTerm.complexity = 1;
        for (Term t : compoundTerm.components) {
            compoundTerm.complexity += t.getComplexity();
        }
    }

    /* static methods making new compounds, which may return null */
    /**
     * Try to make a compound term from a template and a list of components
     * @param compound The template
     * @param components The components
     * @param memory Reference to the memory
     * @return A compound term or null
     */
    public static Term make(CompoundTerm compound, List<Term> components, Memory memory) {
        if (compound instanceof ImageExt) {
            return ImageExt.make(components, ((ImageExt) compound).getRelationIndex(), memory);
        } else if (compound instanceof ImageInt) {
            return ImageInt.make(components, ((ImageInt) compound).getRelationIndex(), memory);
        } else {
            return make(compound.operator(), components, memory);
        }
    }

    /**
     * Try to make a compound term from an operator and a list of components
     * <p>
     * Called from StringParser
     * @param op Term operator
     * @param arg Component list
     * @param memory Reference to the memory
     * @return A compound term or null
     */
    public static Term make(String op, List<Term> arg, Memory memory) {
        if (1 == op.length()) {
            if (Symbols.SET_EXT_OPENER == op.charAt(0)) {
                return SetExt.make(arg, memory);
            }
            if (Symbols.SET_INT_OPENER == op.charAt(0)) {
                return SetInt.make(arg, memory);
            }
            if (op.equals(Symbols.INTERSECTION_EXT_OPERATOR)) {
                return IntersectionExt.make(arg, memory);
            }
            if (op.equals(Symbols.INTERSECTION_INT_OPERATOR)) {
                return IntersectionInt.make(arg, memory);
            }
            if (op.equals(Symbols.DIFFERENCE_EXT_OPERATOR)) {
                return DifferenceExt.Companion.make(arg, memory);
            }
            if (op.equals(Symbols.DIFFERENCE_INT_OPERATOR)) {
                return DifferenceInt.Companion.make(arg, memory);
            }
            if (op.equals(Symbols.PRODUCT_OPERATOR)) {
                return Product.make(arg, memory);
            }
            if (op.equals(Symbols.IMAGE_EXT_OPERATOR)) {
                return ImageExt.make(arg, memory);
            }
            if (op.equals(Symbols.IMAGE_INT_OPERATOR)) {
                return ImageInt.make(arg, memory);
            }
        }
        if (2 == op.length()) {
            if (op.equals(Symbols.NEGATION_OPERATOR)) {
                return Negation.make(arg, memory);
            }
            if (op.equals(Symbols.DISJUNCTION_OPERATOR)) {
                return Disjunction.Companion.make(arg, memory);
            }
            if (op.equals(Symbols.CONJUNCTION_OPERATOR)) {
                return Conjunction.Companion.make(arg, memory);
            }
        }
        return null;
    }

    /**
     * build a component list from two terms
     * @param t1 the first component
     * @param t2 the second component
     * @return the component list
     */
    protected static List<Term> argumentsToList(Term ...t2) {
        return new CopyOnWriteArrayList<>(t2);

     }

    /* ----- utilities for oldName ----- */
    /**
     * default method to make the oldName of the current term from existing fields
     * @return the oldName of the term
     */
    protected String makeName() {
        return makeCompoundName(operator(), components);
    }

    /**
     * default method to make the oldName of a compound term from given fields
     * @param op the term operator
     * @param arg the list of components
     * @return the oldName of the term
     */
    protected static String makeCompoundName(String op, Iterable<Term> arg) {
        StringBuffer name = new StringBuffer();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        for (Term t : arg) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (t instanceof CompoundTerm) {
                ((CompoundTerm) t).setName(((CompoundTerm) t).makeName());
            }
            name.append(t.getName());
        }
        name.append(Symbols.COMPOUND_TERM_CLOSER);
        return name.toString();
    }

    /**
     * make the oldName of an ExtensionSet or IntensionSet
     * @param opener the set opener
     * @param closer the set closer
     * @param arg the list of components
     * @return the oldName of the term
     */
    protected static String makeSetName(char opener, List<Term> arg, char closer) {
        StringBuffer name = new StringBuffer();
        name.append(opener);
        name.append(arg.get(0).getName());
        for (int i = 1; i < arg.size(); i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            name.append(arg.get(i).getName());
        }
        name.append(closer);
        return name.toString();
    }

    /**
     * default method to make the oldName of an image term from given fields
     * @param op the term operator
     * @param arg the list of components
     * @param relationIndex the location of the place holder
     * @return the oldName of the term
     */
    protected static String makeImageName(String op, List<Term> arg, int relationIndex) {
        StringBuffer name = new StringBuffer();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        name.append(Symbols.ARGUMENT_SEPARATOR);
        name.append(arg.get(relationIndex).getName());
        for (int i = 0; i < arg.size(); i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (i == relationIndex) {
                name.append(Symbols.IMAGE_PLACE_HOLDER);
            } else {
                name.append(arg.get(i).getName());
            }
        }
        name.append(Symbols.COMPOUND_TERM_CLOSER);
        return name.toString();
    }

    /* ----- utilities for other fields ----- */
    /** syntactic complexity of the compound, the sum of those of its components plus 1 */ /**
     * report the term's syntactic complexity
     * @return the complexity value
     */
    @Override
    public int getComplexity() {
        return complexity;
    }

    /** Whether the term names a concept */ /**
     * check if the term contains free variable
     * @return if the term is a constant
     */
    @Override
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * Set the constant status
     * @param isConstant
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * Check if the order of the components matters
     * <p>
     * commutative CompoundTerms: Sets, Intersections
     * Commutative Statements: Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one with a temporal order)
     * @return The default value is false
     */
    public boolean isCommutative() {
        return false;
    }

    /* ----- extend Collection methods to component list ----- */
    /**
     * get the number of components
     * @return the size of the component list
     */
    public int size() {
        return components.size();
    }

    /**
     * get a component by index
     * @param i index of the component
     * @return the component
     */
    public Term componentAt(int i) {
        return components.get(i);
    }

    /** list of (direct) components */ /**
     * Get the component list
     * @return The component list
     */
    public List<Term> getComponents() {
        return components;
    }

    /**
     * Clone the component list
     * @return The cloned component list
     */
    public  List<Term> cloneComponents() {
        return cloneList(components);
    }

    /**
     * Deep clone an array list of terms
     * @param original The original component list
     * @return an identical and separate copy of the list
     */
    public static  List<Term> cloneList(List<Term> original) {
        if (null == original) {
            return null;
        }
        List<Term> arr = new ArrayList<Term>(original.size());
        for (int i = 0; i < original.size(); i++) {
            arr.add((Term) ((Term) original.get(i)).clone());
        }
        return arr;
    }

    /**
     * Check whether the compound contains a certain component
     * @param t The component to be checked
     * @return Whether the component is in the compound
     */
    public boolean containComponent(Term t) {
        return components.contains(t);
    }

    /**
     * Check whether the compound contains all components of another term, or that term as a whole
     * @param t The other term
     * @return Whether the components are all in the compound
     */
    public boolean containAllComponents(Term t) {
        if (getClass() == t.getClass()) {
            return components.containsAll(((CompoundTerm) t).getComponents());
        } else {
            return components.contains(t);
        }
    }

    /**
     * Try to add a component into a compound
     * @param t1 The compound
     * @param t2 The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term addComponents(CompoundTerm t1, Term t2, Memory memory) {
        if (null == t2) {
            return t1;
        }
        boolean success;
        List<Term> list = t1.cloneComponents();
        if (t1.getClass() == t2.getClass()) {
            success = list.addAll(((CompoundTerm) t2).getComponents());
        } else {
            success = list.add(t2);
        }
        return (success ? make(t1, list, memory) : null);
    }

    /**
     * Try to remove a component from a compound
     * @param t1 The compound
     * @param t2 The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term reduceComponents(CompoundTerm t1, Term t2, Memory memory) {
        boolean success;
        List<Term> list = t1.cloneComponents();
        if (t1.getClass() == t2.getClass()) {
            success = list.removeAll(((CompoundTerm) t2).getComponents());
        } else {
            success = list.remove(t2);
        }
        return (success ? make(t1, list, memory) : null);
    }

    /**
     * Try to replace a component in a compound at a given index by another one
     * @param compound The compound
     * @param index The location of replacement
     * @param t The new component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term setComponent(CompoundTerm compound, int index, Term t, Memory memory) {
        List<Term> list = compound.cloneComponents();
        list.remove(index);
        if (null != t) {
            if (compound.getClass() != t.getClass()) {
                list.add(index, t);
            } else {
                List<Term> list2 = ((CompoundTerm) t).cloneComponents();
                for (int i = 0; i < list2.size(); i++) {
                    list.add(index + i, list2.get(i));
                }
            }
        }
        return make(compound, list, memory);
    }

    /* ----- variable-related utilities ----- */
    /**
     * Whether this compound term contains any variable term
     * @return Whether the name contains a variable
     */
    public boolean containVar() {
        return Variable.containVar(name);
    }

    /**
     * Rename the variables in the compound
     */
    @Override
    public void renameVariables() {
        if (containVar()) {
            renameVariables(new HashMap<Variable, Variable>());
        }
        setConstant(true);
        setName(makeName());
    }

    /**
     * Rename the variables in the compound
     * @param map The substitution established so far
     */
    public void renameVariables(HashMap<Variable, Variable> map) {
        if (containVar()) {
            for (int i = 0; i < components.size(); i++) {
                Term term = componentAt(i);
                if (term instanceof Variable) {
                    Variable var;
                    if (1 == term.getName().length()) { // anonymous variable from input
                        var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                    } else {
                        var = map.get(term);
                        if (null == var) {
                            var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                        }
                    }
                    if (!term.equals(var)) {
                        components.set(i, var);
                    }
                    map.put((Variable) term, var);
                } else if (term instanceof CompoundTerm) {
                    ((CompoundTerm) term).renameVariables(map);
                    term.setName(((CompoundTerm) term).makeName());
                }
            }
        }
    }

    /**
     * Recursively apply a substitute to the current CompoundTerm
     * @param subs
     */
    public void applySubstitute(Map<Term, Term> subs) {
        Term t1, t2;
        for (int i = 0; i < size(); i++) {
            t1 = componentAt(i);
//            if (t1 instanceof Variable) {
            t2 = subs.get(t1);
            if (null != t2) {
                components.set(i, (Term) t2.clone());
//                }
            } else if (t1 instanceof CompoundTerm) {
                ((CompoundTerm) t1).applySubstitute(subs);
            }
        }
        if (this.isCommutative()) {         // re-order
            TreeSet<Term> s = new TreeSet<Term>(components);
            components = new ArrayList<>(s);
        }
        name = makeName();
    }

    /* ----- link CompoundTerm and its components ----- */
    /**
     * Build TermLink templates to constant components and subcomponents
     * <p>
     * The compound type determines the link type; the component type determines whether to build the link.
     * @return A list of TermLink templates
     */
    public List<TermLink> prepareComponentLinks() {
        ArrayList<TermLink> componentLinks = new ArrayList<TermLink>();
        short type = (this instanceof Statement) ? TermLink.COMPOUND_STATEMENT : TermLink.COMPOUND;   // default
        prepareComponentLinks(componentLinks, type, this);
        return componentLinks;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in special cases
     * <p>
     * @param componentLinks The list of TermLink templates built so far
     * @param type The type of TermLink to be built
     * @param term The CompoundTerm for which the links are built
     */
    private void prepareComponentLinks(ArrayList<TermLink> componentLinks, short type, CompoundTerm term) {
        Term t1, t2, t3;                    // components at different levels
        for (int i = 0; i < term.size(); i++) {     // first level components
            t1 = term.componentAt(i);
            if (t1.isConstant()) {
                componentLinks.add(new TermLink(t1, type, i));
            }
            if ((t1 instanceof Conjunction) && ((this instanceof Equivalence) || ((this instanceof Implication) && (0 == i)))) {
                ((CompoundTerm) t1).prepareComponentLinks(componentLinks, TermLink.COMPOUND_CONDITION, (CompoundTerm) t1);
            } else if (t1 instanceof CompoundTerm) {
                for (int j = 0; j < ((CompoundTerm) t1).size(); j++) {  // second level components
                    t2 = ((CompoundTerm) t1).componentAt(j);
                    if (t2.isConstant()) {
                        if ((t1 instanceof Product) || (t1 instanceof ImageExt) || (t1 instanceof ImageInt)) {
                            if (TermLink.COMPOUND_CONDITION == type) {
                                componentLinks.add(new TermLink(t2, TermLink.TRANSFORM, 0, i, j));
                            } else {
                                componentLinks.add(new TermLink(t2, TermLink.TRANSFORM, i, j));
                            }
                        } else {
                            componentLinks.add(new TermLink(t2, type, i, j));
                        }
                    }
                    if ((t2 instanceof Product) || (t2 instanceof ImageExt) || (t2 instanceof ImageInt)) {
                        for (int k = 0; k < ((CompoundTerm) t2).size(); k++) {
                            t3 = ((CompoundTerm) t2).componentAt(k);
                            if (t3.isConstant()) {                           // third level
                                if (TermLink.COMPOUND_CONDITION == type) {
                                    componentLinks.add(new TermLink(t3, TermLink.TRANSFORM, 0, i, j, k));
                                } else {
                                    componentLinks.add(new TermLink(t3, TermLink.TRANSFORM, i, j, k));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void setComponents(List<Term> components) {
        this.components = components;
    }

    public void setComplexity(short complexity) {
        this.complexity = complexity;
    }
}
