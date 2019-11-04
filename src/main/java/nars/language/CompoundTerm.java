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
import nars.entity.TermLinkConstants;
import nars.io.Symbols;
import nars.storage.Memory;

import java.util.*;

/**
 * A CompoundTerm is a Term with internal (syntactic) structure
 * <p>
 * A CompoundTerm consists of a term operator with one or more component Terms.
 * <p>
 * This abstract class contains default methods for all CompoundTerms.
 */
public abstract class CompoundTerm extends CompoundTermState {

    /* ----- abstract methods to be implemented in subclasses ----- */

    /**
     * Constructor called from subclasses constructors to clone the fields
     *
     * @param name       Name
     * @param components Component list
     * @param isConstant Whether the term refers to a concept
     * @param complexity Complexity of the compound term
     */
    protected CompoundTerm(String name ,  List<Term> components, boolean isConstant, short complexity) {
        super(name);
        this.setComponents(components);
        this.setConstant(isConstant);
        this.setComplexity(complexity);
    }

    /**
     * Default constructor
     */
    protected CompoundTerm() {
    }

    /* ----- object builders, called from subclasses ----- */

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param components Component list
     */
    protected CompoundTerm( List<Term> components) {
        this.setComponents(components);
        calcComplexity();
        name = makeName();
        setConstant(!Variable.containVar(name));
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *  @param name       Name of the compound
     * @param components Component list
     */
    protected CompoundTerm(String name, List<Term> components) {
        super(name);
        setConstant(!Variable.containVar(name));
        this.setComponents(components);
        calcComplexity();
    }

    /**
     * Try to make a compound term from an operator and a list of components
     * <p>
     * Called from StringParser
     *
     * @param op     Term operator
     * @param arg    Component list
     * @param memory Reference to the memory
     * @return A compound term or null
     */
    public static Term make(String op ,  List<Term> arg, Memory  memory) {
        if (op.length() == 1) {
            if (op.charAt(0) == Symbols.SET_EXT_OPENER) {
                return SetExt.make(arg, memory);
            }
            if (op.charAt(0) == Symbols.SET_INT_OPENER) {
                return SetInt.make(arg, (Memory) memory);
            }
            if (op.equals(Symbols.INTERSECTION_EXT_OPERATOR)) {
                return IntersectionExt.make(arg, memory);
            }
            if (op.equals(Symbols.INTERSECTION_INT_OPERATOR)) {
                return IntersectionInt.make(arg, memory);
            }
            if (op.equals(Symbols.DIFFERENCE_EXT_OPERATOR)) {
                return DifferenceExt.make(arg, memory);
            }
            if (op.equals(Symbols.DIFFERENCE_INT_OPERATOR)) {
                return DifferenceInt.make(arg, memory);
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
        if (op.length() == 2) {
            if (op.equals(Symbols.NEGATION_OPERATOR)) {
                return Negation.make(arg, memory);
            }
            if (op.equals(Symbols.DISJUNCTION_OPERATOR)) {
                return Disjunction.make(arg, memory);
            }
            if (op.equals(Symbols.CONJUNCTION_OPERATOR)) {
                return Conjunction.make(arg, memory);
            }
        }
        return null;
    }

    /**
     * build a component list from two terms
     *
     * @param t1 the first component
     * @param t2 the second component
     * @return the component list
     */
    protected static ArrayList<Term> argumentsToList(Term t1, Term t2) {
        var list = new ArrayList<Term>(2);
        list.add(t1);
        list.add(t2);
        return list;
    }

    /**
     * default method to make the oldName of a compound term from given fields
     *
     * @param op  the term operator
     * @param arg the list of components
     * @return the oldName of the term
     */
    protected static String makeCompoundName(String op, Iterable<Term> arg) {
        var name = new StringBuilder();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        for (var t : arg) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (t instanceof CompoundTerm) {
                ((CompoundTermState) t).setName(((CompoundTerm) t).makeName());
            }
            name.append(t.getName());
        }
        name.append(Symbols.COMPOUND_TERM_CLOSER);
        return name.toString();
    }

    /* static methods making new compounds, which may return null */

    /**
     * make the oldName of an ExtensionSet or IntensionSet
     *
     * @param opener the set opener
     * @param closer the set closer
     * @param arg    the list of components
     * @return the oldName of the term
     */
    protected static String makeSetName(char opener, Collection<Term> arg, char closer) {
        StringJoiner joiner = new StringJoiner(String.valueOf(Symbols.ARGUMENT_SEPARATOR), String.valueOf(opener), String.valueOf(closer));
        for (Term term : arg) {
            String termName = term.getName();
            joiner.add(termName);
        }
        return joiner.toString();
    }

    /**
     * default method to make the oldName of an image term from given fields
     *
     * @param op            the term operator
     * @param arg           the list of components
     * @param relationIndex the location of the place holder
     * @return the oldName of the term
     */
    protected static String makeImageName(String op, List<Term> arg, int relationIndex) {
        var name = new StringBuilder();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        name.append(Symbols.ARGUMENT_SEPARATOR);
        name.append(arg.get(relationIndex).getName());
        int bound = arg.size();
        for (int i = 0; i < bound; i++) {
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

    /**
     * Deep clone an array list of terms
     *
     * @param original The original component list
     * @return an identical and separate copy of the list
     */
    public static ArrayList<Term> cloneList(Collection<Term> original) {
        if (original == null) {
            return null;
        }
        ArrayList<Term> terms = new ArrayList<>(original.size());
        for (Term term : original) {
            Term clone = (Term) term.clone();
            terms.add(clone);
        }
        return terms;
    }

    /* ----- utilities for oldName ----- */

    /**
     * Try to remove a component from a compound
     *
     * @param t1     The compound
     * @param t2     The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term reduceComponents(CompoundTerm t1, Term t2, Memory memory) {
        boolean success;
        var list = t1.cloneComponents();
        if (t1.getClass() == t2.getClass()) {
            success = list.removeAll(((CompoundTermState) t2).getComponents());
        } else {
            success = list.remove(t2);
        }
        return (success ? Util11. make(t1, list, memory) : null);
    }

    /**
     * Abstract method to get the operator of the compound
     *
     * @return The operator in a String
     */
    public abstract String operator();


    /* ----- utilities for other fields ----- */

    /**
     * The complexity of the term is the sum of those of the components plus 1
     */
    private void calcComplexity() {
        setComplexity((short) 1);
        for (Term t : getComponents()) {
            setComplexity((short) (getComplexity() + t.getComplexity()));
        }
    }

    /**
     * Orders among terms: variable < atomic < compound @p
     * <p>
     * <p>
     * aram that The Term to be compared with the current Term @return The same
     * as compareTo as defined on Strings
     */

    @Override
    public int compareTo(Term that) {
        if (that instanceof CompoundTerm) {
            var t = (CompoundTerm ) that;
            var minSize = Math.min(size(), t.size());
            for (var i = 0; i < minSize; i++) {
                var diff = componentAt(i).compareTo(t.componentAt(i));
                if (diff != 0) {
                    return diff;
                }
            }
            return size() - t.size();
        } else {
            return 1;
        }
    }

    /**
     * default method to make the oldName of the current term from existing
     * fields
     *
     * @return the oldName of the term
     */
    protected String makeName() {
        return makeCompoundName(operator(), getComponents());
    }

    /* ----- extend Collection methods to component list ----- */

    /**
     * Check if the order of the components matters
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    public boolean isCommutative() {
        return false;
    }

    /**
     * get the number of components
     *
     * @return the size of the component list
     */
    public int size() {
        return getComponents().size();
    }

    /**
     * get a component by index
     *
     * @param i index of the component
     * @return the component
     */
    public Term componentAt(int i) {
        return getComponents().get(i);
    }

    /**
     * Clone the component list
     *
     * @return The cloned component list
     */
    public   List<Term> cloneComponents() {
        return cloneList(getComponents());
    }

    /**
     * Check whether the compound contains a certain component
     *
     * @param t The component to be checked
     * @return Whether the component is in the compound
     */
    public boolean containComponent(Term t) {
        return getComponents().contains(t);
    }

    /**
     * Recursively check if a compound contains a term
     *
     * @param target The term to be searched
     * @return Whether the target is in the current term
     */

    @Override
    public boolean containTerm(Term target) {
        for (Term term : getComponents()) {
            if (term.containTerm(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the compound contains all components of another term, or
     * that term as a whole
     *
     * @param t The other term
     * @return Whether the components are all in the compound
     */
    public boolean containAllComponents(Term t) {
        if (getClass() == t.getClass()) {
            return getComponents().containsAll(((CompoundTermState) t).getComponents());
        } else {
            return getComponents().contains(t);
        }
    }

    /* ----- variable-related utilities ----- */

    /**
     * Whether this compound term contains any variable term
     *
     * @return Whether the name contains a variable
     */
    public boolean containVar() {
        return Variable.containVar(name);
    }

    /**
     * Rename the variables in the compound, called from Sentence constructors
     */

    @Override
    public void renameVariables() {
        if (containVar()) {
            renameVariables(new HashMap<>());
        }
        setConstant(true);
        setName(makeName());
    }

    /**
     * Recursively rename the variables in the compound
     *
     * @param map The substitution established so far
     */
    private void renameVariables(HashMap<Variable, Variable> map) {
        if (containVar()) {
            for (var i = 0; i < getComponents().size(); i++) {
                var term = componentAt(i);
                if (term instanceof Variable) {
                    Variable var;
                    if (term.getName().length() == 1) { // anonymous variable from input
                        var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                    } else {
                        var = (Variable) map.get((Variable) term);
                        if (var == null) {
                            var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                        }
                    }
                    if (!term.equals(var)) {
                        getComponents().set(i, var);
                    }
                    map.put((Variable) term, var);
                } else if (term instanceof CompoundTerm) {
                    ((CompoundTerm) term).renameVariables(map);
                    ((CompoundTermState) term).setName(((CompoundTerm) term).makeName());
                }
            }
        }
    }

    /**
     * Recursively apply a substitute to the current CompoundTerm
     *
     * @param subs
     */
    public void applySubstitute(HashMap<Term, Term> subs) {
        Term t1, t2;
        for (var i = 0; i < size(); i++) {
            t1 = componentAt(i);
            t2 = subs.get(t1);
            if (t2 != null) {
                getComponents().set(i, (Term) t2.clone());
            } else if (t1 instanceof CompoundTerm) {
                ((CompoundTerm) t1).applySubstitute(subs);
            }
        }
        if (this.isCommutative()) {         // re-order
            var s = new TreeSet<>(getComponents());
            setComponents(new ArrayList<>(s));
        }
        name = makeName();
    }

    /* ----- link CompoundTerm and its components ----- */

    /**
     * Build TermLink templates to constant components and subcomponents
     * <p>
     * The compound type determines the link type; the component type determines
     * whether to build the link.
     *
     * @return A list of TermLink templates
     */
    public   List<TermLinkConstants> prepareComponentLinks() {
        var componentLinks = new ArrayList<TermLinkConstants>();
        var type = (this instanceof Statement) ? TermLinkConstants.COMPOUND_STATEMENT : TermLinkConstants.COMPOUND;   // default
        prepareComponentLinks(componentLinks, type, this);
        return componentLinks;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     *
     * @param componentLinks The list of TermLink templates built so far
     * @param type           The type of TermLink to be built
     * @param term           The CompoundTerm for which the links are built
     */
    private void prepareComponentLinks(List<TermLinkConstants> componentLinks, short type, CompoundTerm term) {
        Term t1, t2, t3;                    // components at different levels
        for (var i = 0; i < term.size(); i++) {     // first level components
            t1 = term.componentAt(i);
            if (t1.isConstant()) {
                componentLinks.add(new TermLink(t1, type, i));
            }
            if ((t1 instanceof Conjunction) && ((this instanceof Equivalence) || ((this instanceof Implication) && (i == 0)))) {
                ((CompoundTerm) t1).prepareComponentLinks(componentLinks, TermLinkConstants.COMPOUND_CONDITION, (CompoundTerm) t1);
            } else if (t1 instanceof CompoundTerm) {
                for (var j = 0; j < ((CompoundTerm) t1).size(); j++) {  // second level components
                    t2 = ((CompoundTerm) t1).componentAt(j);
                    if (t2.isConstant()) {
                        if ((t1 instanceof Product) || (t1 instanceof ImageExt) || (t1 instanceof ImageInt)) {
                            if (type == TermLinkConstants.COMPOUND_CONDITION) {
                                componentLinks.add(new TermLink(t2, TermLinkConstants.TRANSFORM, 0, i, j));
                            } else {
                                componentLinks.add(new TermLink(t2, TermLinkConstants.TRANSFORM, i, j));
                            }
                        } else {
                            componentLinks.add(new TermLink(t2, type, i, j));
                        }
                    }
                    if ((t2 instanceof Product) || (t2 instanceof ImageExt) || (t2 instanceof ImageInt)) {
                        for (var k = 0; k < ((CompoundTerm) t2).size(); k++) {
                            t3 = ((CompoundTerm) t2).componentAt(k);
                            if (t3.isConstant()) {                           // third level
                                if (type == TermLinkConstants.COMPOUND_CONDITION) {
                                    componentLinks.add(new TermLink(t3, TermLinkConstants.TRANSFORM, 0, i, j, k));
                                } else {
                                    componentLinks.add(new TermLink(t3, TermLinkConstants.TRANSFORM, i, j, k));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
