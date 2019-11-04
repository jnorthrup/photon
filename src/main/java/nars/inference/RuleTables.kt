/*
 * RuleTables.java
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
package nars.inference

import nars.entity.*
import nars.io.Symbols
import nars.language.*
import nars.storage.BackingStore

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirective processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
object RuleTables {
    /**
     * Entry point of the inference engine
     *
     * @param tLink  The selected TaskLink, which will provide a task
     * @param bLink  The selected TermLink, which may provide a belief
     * @param memory Reference to the memory
     */
    @JvmStatic
    fun reason(tLink: TaskLink, bLink: TermLink, memory: BackingStore) {
        val task: Task = memory.currentTask!!
        val taskSentence = task.sentence
        val taskTerm = taskSentence.content.clone() as Term         // cloning for substitution

        val beliefTerm = bLink.target.clone() as Term       // cloning for substitution

        val beliefConcept: Concept? = memory.termToConcept(beliefTerm)
        var belief: Sentence? = null
        if (beliefConcept != null) {
            belief = beliefConcept.getBelief(task)
        }
        memory.currentBelief = belief
        if (belief != null) {
            LocalRules.match(task, belief, memory)
        }
        if (!memory.noResult() && task.sentence.isJudgment) {
            return
        }
        val tIndex = tLink.getIndex(0)
        var bIndex = bLink.getIndex(0)
        when (tLink.type) {
            TermLink.SELF -> when (bLink.type) {
                TermLink.COMPONENT -> compoundAndSelf(taskTerm as CompoundTerm, beliefTerm, true, memory)
                TermLink.COMPOUND -> compoundAndSelf(beliefTerm as CompoundTerm, taskTerm, false, memory)
                TermLink.COMPONENT_STATEMENT -> if (belief != null) {
                    SyllogisticRules.detachment(task.sentence, belief, bIndex.toInt(), memory)
                }
                TermLink.COMPOUND_STATEMENT -> if (belief != null) {
                    SyllogisticRules.detachment(belief, task.sentence, bIndex.toInt(), memory)
                }
                TermLink.COMPONENT_CONDITION -> if (belief != null) {
                    bIndex = bLink.getIndex(1)
                    SyllogisticRules.conditionalDedInd(taskTerm as Implication, bIndex, beliefTerm, tIndex.toInt(), memory)
                }
                TermLink.COMPOUND_CONDITION -> if (belief != null) {
                    bIndex = bLink.getIndex(1)
                    SyllogisticRules.conditionalDedInd(beliefTerm as Implication, bIndex, taskTerm, tIndex.toInt(), memory)
                }
            }
            TermLink.COMPOUND -> when (bLink.type) {
                TermLink.COMPOUND -> compoundAndCompound(taskTerm as CompoundTerm, beliefTerm as CompoundTerm, memory)
                TermLink.COMPOUND_STATEMENT -> compoundAndStatement(taskTerm as CompoundTerm, tIndex, beliefTerm as Statement, bIndex, beliefTerm, memory)
                TermLink.COMPOUND_CONDITION -> if (belief != null) {
                    if (beliefTerm is Implication) {
                        SyllogisticRules.conditionalDedInd(beliefTerm, bIndex, taskTerm, -1, memory)
                    } else if (beliefTerm is Equivalence) {
                        SyllogisticRules.conditionalAna(beliefTerm, bIndex, taskTerm, -1, memory)
                    }
                }
            }
            TermLink.COMPOUND_STATEMENT -> when (bLink.type) {
                TermLink.COMPONENT -> componentAndStatement(memory.currentTerm as CompoundTerm, bIndex, taskTerm as Statement, tIndex, memory)
                TermLink.COMPOUND -> compoundAndStatement(beliefTerm as CompoundTerm, bIndex, taskTerm as Statement, tIndex, beliefTerm, memory)
                TermLink.COMPOUND_STATEMENT -> if (belief != null) {
                    syllogisms(tLink, bLink, taskTerm, beliefTerm, memory)
                }
                TermLink.COMPOUND_CONDITION -> if (belief != null) {
                    bIndex = bLink.getIndex(1)
                    if (beliefTerm is Implication) {
                        conditionalDedIndWithVar(beliefTerm, bIndex, taskTerm as Statement, tIndex, memory)
                    }
                }
            }
            TermLink.COMPOUND_CONDITION -> when (bLink.type) {
                TermLink.COMPOUND_STATEMENT -> if (belief != null) {
                    if (taskTerm is Implication)// TODO maybe put instanceof test within conditionalDedIndWithVar()
                    {
                        conditionalDedIndWithVar(taskTerm, tIndex, beliefTerm as Statement, bIndex, memory)
                    }
                }
            }
        }
    }

    /* ----- syllogistic inferences ----- */


    /**
     * Meta-table of syllogistic rules, indexed by the content classes of the
     * taskSentence and the belief
     *
     * @param tLink      The link to task
     * @param bLink      The link to belief
     * @param taskTerm   The content of task
     * @param beliefTerm The content of belief
     * @param memory     Reference to the memory
     */
    private fun syllogisms(tLink: TaskLink, bLink: TermLink, taskTerm: Term, beliefTerm: Term, memory: BackingStore) {
        val taskSentence = memory.currentTask!!.sentence
        val belief = memory.currentBelief
        val figure: Int
        if (taskTerm is Inheritance) {
            if (beliefTerm is Inheritance) {
                figure = indexToFigure(tLink, bLink)
                asymmetricAsymmetric(taskSentence, belief, figure, memory)
            } else if (beliefTerm is Similarity) {
                figure = indexToFigure(tLink, bLink)
                asymmetricSymmetric(taskSentence, belief, figure, memory)
            } else {
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0).toInt(), memory)
            }
        } else if (taskTerm is Similarity) {
            if (beliefTerm is Inheritance) {
                figure = indexToFigure(bLink, tLink)
                asymmetricSymmetric(belief, taskSentence, figure, memory)
            } else if (beliefTerm is Similarity) {
                figure = indexToFigure(bLink, tLink)
                symmetricSymmetric(belief, taskSentence, figure, memory)
            }
        } else if (taskTerm is Implication) {
            if (beliefTerm is Implication) {
                figure = indexToFigure(tLink, bLink)
                asymmetricAsymmetric(taskSentence, belief, figure, memory)
            } else if (beliefTerm is Equivalence) {
                figure = indexToFigure(tLink, bLink)
                asymmetricSymmetric(taskSentence, belief, figure, memory)
            } else if (beliefTerm is Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0).toInt(), memory)
            }
        } else if (taskTerm is Equivalence) {
            if (beliefTerm is Implication) {
                figure = indexToFigure(bLink, tLink)
                asymmetricSymmetric(belief, taskSentence, figure, memory)
            } else if (beliefTerm is Equivalence) {
                figure = indexToFigure(bLink, tLink)
                symmetricSymmetric(belief, taskSentence, figure, memory)
            } else if (beliefTerm is Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0).toInt(), memory)
            }
        }
    }

    /**
     * Decide the figure of syllogism according to the locations of the common
     * term in the premises
     *
     * @param link1 The link to the first premise
     * @param link2 The link to the second premise
     * @return The figure of the syllogism, one of the four: 11, 12, 21, or 22
     */
    private fun indexToFigure(link1: TermLink, link2: TermLink): Int {
        return (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1)
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param sentence The taskSentence in the task
     * @param belief   The judgment in the belief
     * @param figure   The location of the shared term
     * @param memory   Reference to the memory
     */
    private fun asymmetricAsymmetric(sentence: Sentence, belief: Sentence?, figure: Int, memory: BackingStore) {
        val s1 = sentence.cloneContent() as Statement
        val s2 = belief!!.cloneContent() as Statement
        val t1: Term?
        val t2: Term?
        when (figure) {
            11 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.subject, s2.subject, s1, s2)) {
                if (s1 == s2) {
                    return
                }
                t1 = s2.predicate
                t2 = s1.predicate
                CompositionalRules.composeCompound(s1, s2, 0, memory)
                SyllogisticRules.abdIndCom(t1, t2, sentence, belief, memory)
            }
            12 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.subject, s2.predicate, s1, s2)) {
                if (s1 == s2) {
                    return
                }
                t1 = s2.subject
                t2 = s1.predicate
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                    LocalRules.matchReverse(memory)
                } else {
                    SyllogisticRules.dedExe(t1, t2, sentence, belief, memory)
                }
            }
            21 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.predicate, s2.subject, s1, s2)) {
                if (s1 == s2) {
                    return
                }
                t1 = s1.subject
                t2 = s2.predicate
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                    LocalRules.matchReverse(memory)
                } else {
                    SyllogisticRules.dedExe(t1, t2, sentence, belief, memory)
                }
            }
            22 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.predicate, s2.predicate, s1, s2)) {
                if (s1 == s2) {
                    return
                }
                t1 = s1.subject
                t2 = s2.subject
                if (!SyllogisticRules.conditionalAbd(t1, t2, s1, s2, memory)) {         // if conditional abduction, skip the following

                    CompositionalRules.composeCompound(s1, s2, 1, memory)
                    SyllogisticRules.abdIndCom(t1, t2, sentence, belief, memory)
                }
            }
            else -> {
            }
        }
    }

    /**
     * Syllogistic rules whose first premise is on an asymmetric relation, and
     * the second on a symmetric relation
     *
     * @param asym   The asymmetric premise
     * @param sym    The symmetric premise
     * @param figure The location of the shared term
     * @param memory Reference to the memory
     */
    private fun asymmetricSymmetric(asym: Sentence?, sym: Sentence?, figure: Int, memory: BackingStore) {
        val asymSt = asym!!.cloneContent() as Statement
        val symSt = sym!!.cloneContent() as Statement
        val t1: Term?
        val t2: Term?
        when (figure) {
            11 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.subject, symSt.subject, asymSt, symSt)) {
                t1 = asymSt.predicate
                t2 = symSt.predicate
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                    LocalRules.matchAsymSym(asym, sym, figure, memory)
                } else {
                    SyllogisticRules.analogy(t2, t1, asym, sym, memory)
                }
            }
            12 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.subject, symSt.predicate, asymSt, symSt)) {
                t1 = asymSt.predicate
                t2 = symSt.subject
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                    LocalRules.matchAsymSym(asym, sym, figure, memory)
                } else {
                    SyllogisticRules.analogy(t2, t1, asym, sym, memory)
                }
            }
            21 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.predicate, symSt.subject, asymSt, symSt)) {
                t1 = asymSt.subject
                t2 = symSt.predicate
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                    LocalRules.matchAsymSym(asym, sym, figure, memory)
                } else {
                    SyllogisticRules.analogy(t1, t2, asym, sym, memory)
                }
            }
            22 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.predicate, symSt.predicate, asymSt, symSt)) {
                t1 = asymSt.subject
                t2 = symSt.subject
                if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                    LocalRules.matchAsymSym(asym, sym, figure, memory)
                } else {
                    SyllogisticRules.analogy(t1, t2, asym, sym, memory)
                }
            }
        }
    }

    /**
     * Syllogistic rules whose both premises are on the same symmetric relation
     *
     * @param belief       The premise that comes from a belief
     * @param taskSentence The premise that comes from a task
     * @param figure       The location of the shared term
     * @param memory       Reference to the memory
     */
    private fun symmetricSymmetric(belief: Sentence?, taskSentence: Sentence, figure: Int, memory: BackingStore) {
        val s1 = belief!!.cloneContent() as Statement
        val s2 = taskSentence.cloneContent() as Statement
        when (figure) {
            11 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.subject, s2.subject, s1, s2)) {
                SyllogisticRules.resemblance(s1.predicate, s2.predicate, belief, taskSentence, memory)
            }
            12 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.subject, s2.predicate, s1, s2)) {
                SyllogisticRules.resemblance(s1.predicate, s2.subject, belief, taskSentence, memory)
            }
            21 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.predicate, s2.subject, s1, s2)) {
                SyllogisticRules.resemblance(s1.subject, s2.predicate, belief, taskSentence, memory)
            }
            22 -> if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.predicate, s2.predicate, s1, s2)) {
                SyllogisticRules.resemblance(s1.subject, s2.subject, belief, taskSentence, memory)
            }
        }
    }

    /* ----- conditional inferences ----- */


    /**
     * The detachment rule, with variable unification
     *
     * @param originalMainSentence The premise that is an Implication or
     * Equivalence
     * @param subSentence          The premise that is the subject or predicate of the
     * first one
     * @param index                The location of the second premise in the first
     * @param memory               Reference to the memory
     */
    private fun detachmentWithVar(originalMainSentence: Sentence?, subSentence: Sentence?, index: Int, memory: BackingStore) {
        val mainSentence = originalMainSentence!!.clone() as Sentence   // for substitution

        val statement = mainSentence.content as Statement
        val component: Term = statement.componentAt(index)
        val content = subSentence!!.content
        if (component is Inheritance && memory.currentBelief != null) {
            if (component.isConstant()) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory)
            } else if (Variable.unify(Symbols.VAR_INDEPENDENT, component, content, statement, content)) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory)
            } else if (statement is Implication && statement.predicate is Statement && memory.currentTask!!.sentence.isJudgment) {
                val s2 = statement.predicate as Statement
                if (s2.subject == (content as Statement).subject) {
                    CompositionalRules.introVarInner(content, s2, statement, memory)
                }
            }
        }
    }

    /**
     * Conditional deduction or induction, with variable unification
     *
     * @param conditional The premise that is an Implication with a Conjunction
     * as condition
     * @param index       The location of the shared term in the condition
     * @param statement   The second premise that is a statement
     * @param side        The location of the shared term in the statement
     * @param memory      Reference to the memory
     */
    private fun conditionalDedIndWithVar(conditional: Implication, index: Short, statement: Statement, side: Short, memory: BackingStore) {
        var side1 = side
        val condition = conditional.subject as CompoundTerm
        val component: Term  = condition.componentAt(index.toInt())
        var component2: Term? = null
        if (statement is Inheritance) {
            component2 = statement
            side1 = -1
        } else if (statement is Implication) {
            component2 = statement.componentAt(side1.toInt())
        }
        if (component2 != null && Variable.unify(Symbols.VAR_INDEPENDENT, component, component2, conditional, statement)) {
            SyllogisticRules.conditionalDedInd(conditional, index, statement, side1.toInt(), memory)
        }
    }

    /* ----- structural inferences ----- */


    /**
     * Inference between a compound term and a component of it
     *
     * @param compound     The compound term
     * @param component    The component term
     * @param compoundTask Whether the compound comes from the task
     * @param memory       Reference to the memory
     */
    private fun compoundAndSelf(compound: CompoundTerm, component: Term, compoundTask: Boolean, memory: BackingStore) {
        if (compound is Conjunction || compound is Disjunction) {
            if (memory.currentBelief != null) {
                CompositionalRules.decomposeStatement(compound, component, compoundTask, memory)
            }
//        } else if ((compound instanceof Negation) && !memory.currentTask.isStructural()) {
            else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(compound, component, compoundTask, memory)
            }
        } else if (compound is Negation) {
            if (compoundTask) {
                StructuralRules.transformNegation(compound.componentAt(0), memory)
            } else {
                StructuralRules.transformNegation(compound, memory)
            }
        }
    }

    /**
     * Inference between two compound terms
     *
     * @param taskTerm   The compound from the task
     * @param beliefTerm The compound from the belief
     * @param memory     Reference to the memory
     */
    private fun compoundAndCompound(taskTerm: CompoundTerm, beliefTerm: CompoundTerm, memory: BackingStore) {
        if (taskTerm.javaClass == beliefTerm.javaClass) {
            if (taskTerm.size() > beliefTerm.size()) {
                compoundAndSelf(taskTerm, beliefTerm, true, memory)
            } else if (taskTerm.size() < beliefTerm.size()) {
                compoundAndSelf(beliefTerm, taskTerm, false, memory)
            }
        }
    }

    /**
     * Inference between a compound term and a statement
     *
     * @param compound   The compound term
     * @param index      The location of the current term in the compound
     * @param statement  The statement
     * @param side       The location of the current term in the statement
     * @param beliefTerm The content of the belief
     * @param memory     Reference to the memory
     */
    private fun compoundAndStatement(compound: CompoundTerm, index: Short, statement: Statement, side: Short, beliefTerm: Term, memory: BackingStore) {
        val component: Term = compound.componentAt(index.toInt())
        val task: Task = memory.currentTask!!
        if (component.javaClass == statement.javaClass) {
            if (compound is Conjunction && memory.currentBelief != null) {
                if (Variable.unify(Symbols.VAR_DEPENDENT, component, statement, compound, statement)) {
                    SyllogisticRules.elimiVarDep(compound, component, statement == beliefTerm, memory)
                } else if (task.sentence.isJudgment) { // && !compound.containComponent(component)) {

                    CompositionalRules.introVarInner(statement, component as Statement, compound, memory)
                }
            }
        } else {
//            if (!task.isStructural() && task.getSentence().isJudgment()) {

            if (task.sentence.isJudgment) {
                if (statement is Inheritance) {
                    StructuralRules.structuralCompose1(compound, index, statement, memory)
//                    if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {


                    if (!(compound is SetExt || compound is SetInt || compound is Negation)) {
                        StructuralRules.structuralCompose2(compound, index, statement, side, memory)
                    }    // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                } else if (statement is Similarity && compound !is Conjunction) {
                    StructuralRules.structuralCompose2(compound, index, statement, side, memory)
                }       // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
            }
        }
    }

    /**
     * Inference between a component term (of the current term) and a statement
     *
     * @param compound  The compound term
     * @param index     The location of the current term in the compound
     * @param statement The statement
     * @param side      The location of the current term in the statement
     * @param memory    Reference to the memory
     */
    private fun componentAndStatement(compound: CompoundTerm, index: Short, statement: Statement, side: Short, memory: BackingStore) {
//        if (!memory.currentTask.isStructural()) {

        if (statement is Inheritance) {
            StructuralRules.structuralDecompose1(compound, index, statement, memory)
            if (compound !is SetExt && compound !is SetInt) {
                StructuralRules.structuralDecompose2(statement, index.toInt(), memory)    // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            } else {
                StructuralRules.transformSetRelation(compound, statement, side, memory)
            }
        }
//        }
        else if (statement is Similarity) {
            StructuralRules.structuralDecompose2(statement, index.toInt(), memory)        // {(C-B) --> (C-A), A @ (C-A)} |- A --> B

            if (compound is SetExt || compound is SetInt) {
                StructuralRules.transformSetRelation(compound, statement, side, memory)
            }
        } else if (statement is Implication && compound is Negation) {
            StructuralRules.contraposition(statement, memory)
        }
    }

    /* ----- inference with one TaskLink only ----- */


    /**
     * The TaskLink is of type TRANSFORM, and the conclusion is an equivalent
     * transformation
     *
     * @param tLink  The task link
     * @param memory Reference to the memory
     */
    @JvmStatic
    fun transformTask(tLink: TaskLink, memory: BackingStore) {
        val content = memory.currentTask!!.content!!.clone() as CompoundTerm
        val indices: ShortArray = tLink.indices
        var inh: Term? = null
        if (indices.size == 2 || content is Inheritance) {          // <(*, term, #) --> #>
            inh = content
        } else if (indices.size == 3) {   // <<(*, term, #) --> #> ==> #>
            inh = content.componentAt(indices[0].toInt())
        } else if (indices.size == 4) {   // <(&&, <(*, term, #) --> #>, #) ==> #>

            val component: Term? = content.componentAt(indices[0].toInt())
            if (component is Conjunction && (content is Implication && indices[0] == 0.toShort() || content is Equivalence)) {
                inh = (component as CompoundTerm).componentAt(indices[1].toInt())
            } else {
                return
            }
        }
        if (inh is Inheritance) {
            StructuralRules.transformProductImage(inh, content, indices, memory)
        }
    }
}