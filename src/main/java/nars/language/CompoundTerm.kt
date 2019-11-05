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
package nars.language

import nars.entity.TermLink
import nars.entity.TermLinkType
import nars.entity.TermLinkType.*
import nars.language.Util2.cloneList
import nars.language.Util2.makeCompoundName
import nars.language.Variable.Companion.containVar
import java.util.*

/**
 * A CompoundTerm is a Term with internal (syntactic) structure
 *
 *
 * A CompoundTerm consists of a term operator with one or more component Terms.
 *
 *
 * This abstract class contains default methods for all CompoundTerms.
 */
abstract class CompoundTerm : CompoundTermState {
    /* ----- abstract methods to be implemented in subclasses ----- */
    /**
     * Constructor called from subclasses constructors to clone the fields
     *
     * @param name       Name
     * @param components Component list
     * @param isConstant Whether the term refers to a concept
     * @param complexity Complexity of the compound term
     */
    internal constructor(name: String?, components: List<Term>?, isConstant: Boolean, complexity:  Int) : super(name!!) {
        this.components = components
        this.constant = (isConstant)
        this.complexity = (complexity)
    }

    /* ----- object builders, called from subclasses ----- */
    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param components Component list
     */
    internal constructor(components: List<Term?>?) : super("failwhale") {
        this.components = components as List<Term>?
        calcComplexity()
        constant=(!containVar(name))
        name=makeName()
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     * @param name       Name of the compound
     * @param components Component list
     */
    internal constructor(name: String?, components: List<Term?>?) : super(name!!) {
        constant=(!containVar(name))
        this.components = components as List<Term>?
        calcComplexity()
    }
    /* static methods making new compounds, which may return null */ /* ----- utilities for oldName ----- */
    /**
     * Abstract method to get the operator of the compound
     *
     * @return The operator in a String
     */
    abstract fun operator(): String
    /* ----- utilities for other fields ----- */
    /**
     * The complexity of the term is the sum of those of the components plus 1
     */
    fun calcComplexity() {
        complexity=(1.toInt())
        for (t in components!!) {
            complexity=((complexity + t.complexity).toInt())
        }
    }

    /**
     * Orders among terms: variable < atomic < compound @p
     *
     *
     *
     *
     * aram that The Term to be compared with the current Term @return The same
     * as compareTo as defined on Strings
     */
    override fun compareTo(that: Term): Int {
        var result = 1
        var finished = false
        if (that is CompoundTerm) {
            val t = that
            val minSize = Math.min(size(), t.size())
            for (i in 0 until minSize) {
                val diff = componentAt(i).compareTo(t.componentAt(i))
                if (diff != 0) {
                    result = diff
                    finished = true
                    break
                }
            }
            if (!finished) {
                result = size() - t.size()
            }
        }
        return result
    }

    /**
     * default method to make the oldName of the current term from existing
     * fields
     *
     * @return the oldName of the term
     */
    open fun makeName(): String {
        return makeCompoundName(operator()!!, components!!)
    }
    /* ----- extend Collection methods to component list ----- */
    /**
     * Check if the order of the components matters
     *
     *
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    open val commutative: Boolean
        get() = false

    /**
     * get the number of components
     *
     * @return the size of the component list
     */
    fun size(): Int {
        return components!!.size
    }

    /**
     * get a component by index
     *
     * @param i index of the component
     * @return the component
     */
    fun componentAt(i: Int): Term {
        return components!![i]
    }

    /**
     * Clone the component list
     *
     * @return The cloned component list
     */
    fun cloneComponents()=cloneList(components)

    /**
     * Check whether the compound contains a certain component
     *
     * @param t The component to be checked
     * @return Whether the component is in the compound
     */
    fun containComponent(t: Term?): Boolean {
        return components!!.contains(t!!)
    }

    /**
     * Recursively check if a compound contains a term
     *
     * @param target The term to be searched
     * @return Whether the target is in the current term
     */
    open fun containTerm(target: Term): Boolean = components!!.any {
        (it as? CompoundTerm)?.containTerm(target) ?: false
    }


    /**
     * Check whether the compound contains all components of another term, or
     * that term as a whole
     *
     * @param t The other term
     * @return Whether the components are all in the compound
     */
    fun containAllComponents(t: Term): Boolean {
        val result: Boolean
        result = if (javaClass == t.javaClass) {
            components!!.containsAll((t as CompoundTermState).components!!)
        } else {
            components!!.contains(t)
        }
        return result
    }
    /* ----- variable-related utilities ----- */
    /**
     * Whether this compound term contains any variable term
     *
     * @return Whether the name contains a variable
     */
    fun containVar(): Boolean {
        return containVar(name)
    }

    /**
     * Rename the variables in the compound, called from Sentence constructors
     */
    fun renameVariables() {
        if (containVar()) {
            renameVariables(HashMap())
        }
        constant = (true)
        name = makeName()
    }

    /**
     * Recursively rename the variables in the compound
     *
     * @param map The substitution established so far
     */
    fun renameVariables(map: MutableMap<Variable?, Variable?>) {
        if (containVar()) {
            for (i in components!!.indices) {
                val term = componentAt(i)
                if (term is Variable) {
                    var `var`: Variable?
                    if (term.name.length == 1) { // anonymous variable from input
                        `var` = Variable(term.name[0].toString() + "" + (map.size + 1))
                    } else {
                        `var` = map[term]
                        if (`var` == null) {
                            `var` = Variable(term.name[0].toString() + "" + (map.size + 1))
                        }
                    }
                    if (term != `var`) {
                        (components as MutableList)[i] = `var`
                    }
                    map[term] = `var`
                } else if (term is CompoundTerm) {
                    term.renameVariables(map)
                    (term as CompoundTermState).name = term.makeName()
                }
            }
        }
    }

    /**
     * Recursively apply a substitute to the current CompoundTerm
     *
     * @param subs
     */
    fun applySubstitute(subs: Map<Term , Term >) {
        var t1: Term
        var t2: Term?
        for (i in 0 until size()) {
            t1 = componentAt(i)
            t2 = subs[t1]
            if (t2 != null) {
                (components as MutableList)[i] = t2.clone()
            } else if (t1 is CompoundTerm) {
                t1.applySubstitute(subs)
            }
        }
        if (commutative) { // re-order
            components =  TreeSet(components).toList()
        }
        name = makeName()
    }
    /* ----- link CompoundTerm and its components ----- */
    /**
     * Build TermLink templates to constant components and subcomponents
     *
     *
     * The compound type determines the link type; the component type determines
     * whether to build the link.
     *
     * @return A list of TermLink templates
     */
    fun prepareComponentLinks():  List<TermLink > {
        val componentLinks = ArrayList<TermLink >()
        val type = if (this is Statement) COMPOUND_STATEMENT else COMPOUND // default
        prepareComponentLinks(componentLinks, type, this)
        return componentLinks
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     *
     *
     *
     * @param componentLinks The list of TermLink templates built so far
     * @param type           The type of TermLink to be built
     * @param term           The CompoundTerm for which the links are built
     */
    fun prepareComponentLinks(componentLinks: MutableList<TermLink >, type: TermLinkType, term: CompoundTerm) {
        var t1: Term
        var t2: Term
        var t3: Term // components at different levels
        for (i in 0 until term.size()) { // first level components
            t1 = term.componentAt(i)
            if (t1.constant) {
                componentLinks.add(TermLink(t1, type, i))
            }
            if (t1 is Conjunction && (this is Equivalence || this is Implication && i == 0)) {
                (t1 as CompoundTerm).prepareComponentLinks(componentLinks, COMPOUND_CONDITION, t1 as CompoundTerm)
            } else if (t1 is CompoundTerm) {
                for (j in 0 until t1.size()) { // second level components
                    t2 = t1.componentAt(j)
                    if (t2.constant) {
                        if (t1 is Product || t1 is ImageExt || t1 is ImageInt) {
                            if (type == COMPOUND_CONDITION) {
                                componentLinks.add(TermLink(t2, TRANSFORM, 0, i, j))
                            } else {
                                componentLinks.add(TermLink(t2, TRANSFORM, i, j))
                            }
                        } else {
                            componentLinks.add(TermLink(t2, type, i, j))
                        }
                    }
                    if (t2 is Product || t2 is ImageExt || t2 is ImageInt) {
                        for (k in 0 until (t2 as CompoundTerm).size()) {
                            t3 = t2.componentAt(k)
                            if (t3.constant) { // third level
                                if (type == COMPOUND_CONDITION) {
                                    componentLinks.add(TermLink(t3, TRANSFORM, 0, i, j, k))
                                } else {
                                    componentLinks.add(TermLink(t3, TRANSFORM, i, j, k))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}