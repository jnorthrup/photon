/*
 * CompositionalRules.java
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
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.inference

import nars.entity.Sentence
import nars.entity.Task
import nars.entity.TruthValue
import nars.inference.BudgetFunctions.compoundForward
import nars.inference.BudgetFunctions.forward
import nars.language.*
import nars.storage.Memory
import java.util.*

/**
 * Compound term composition and decomposition rules, with two premises.
 *
 *
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
object CompositionalRules {/* -------------------- intersections and differences -------------------- */

    /**
     * {<S></S>  M>, <P></P>  M>} |- {<(S|P) ==> M>, <(S&P) ==> M>, <(S-P) ==> M>,
     * <(P-S) ==> M>}
     *
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param index        The location of the shared term
     * @param memory       Reference to the memory
     */
    internal fun composeCompound(taskContent: Statement, beliefContent: Statement, index: Int, memory: Memory) {
        if (!memory.currentTask.sentence.isJudgment || taskContent.javaClass != beliefContent.javaClass) {
            return
        }
        val componentT: Term = taskContent.componentAt(1 - index)
        val componentB: Term = beliefContent.componentAt(1 - index)
        val componentCommon: Term = taskContent.componentAt(index)
        if (componentT is CompoundTerm && componentT.containAllComponents(componentB)) {
            decomposeCompound(componentT, componentB, componentCommon, index, true, memory)
            return
        } else if (componentB is CompoundTerm && componentB.containAllComponents(componentT)) {
            decomposeCompound(componentB, componentT, componentCommon, index, false, memory)
            return
        }
        val truthT = memory.currentTask.sentence.truth
        val truthB = memory.currentBelief!!.truth
        val truthOr: TruthValue? = TruthFunctions.union(truthT!!, truthB!!)
        val truthAnd: TruthValue? = TruthFunctions.intersection(truthT, truthB)
        var truthDif: TruthValue? = null
        var termOr: Term? = null
        var termAnd: Term? = null
        var termDif: Term? = null
        if (index == 0) {
            if (taskContent is Inheritance) {
                termOr = IntersectionInt.make(componentT, componentB, memory)
                termAnd = IntersectionExt.make(componentT, componentB, memory)
                if (truthB.isNegative) {
                    if (!truthT.isNegative) {
                        termDif = DifferenceExt.make(componentT, componentB, memory)
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB))
                    }
                } else if (truthT.isNegative) {
                    termDif = DifferenceExt.make(componentB, componentT, memory)
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT))
                }
            } else if (taskContent is Implication) {
                termOr = Disjunction.make(componentT, componentB, memory)
                termAnd = Conjunction.make(componentT, componentB, memory)
            }
            processComposed(taskContent, componentCommon.clone() as Term, termOr, truthOr, memory)
            processComposed(taskContent, componentCommon.clone() as Term, termAnd, truthAnd, memory)
            processComposed(taskContent, componentCommon.clone() as Term, termDif, truthDif, memory)
        } else {    // index == 1

            if (taskContent is Inheritance) {
                termOr = IntersectionExt.make(componentT, componentB, memory)
                termAnd = IntersectionInt.make(componentT, componentB, memory)
                if (truthB.isNegative) {
                    if (!truthT.isNegative) {
                        termDif = DifferenceInt.make(componentT, componentB, memory)
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB))
                    }
                } else if (truthT.isNegative) {
                    termDif = DifferenceInt.make(componentB, componentT, memory)
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT))
                }
            } else if (taskContent is Implication) {
                termOr = Conjunction.make(componentT, componentB, memory)
                termAnd = Disjunction.make(componentT, componentB, memory)
            }
            processComposed(taskContent, termOr, componentCommon.clone() as Term, truthOr, memory)
            processComposed(taskContent, termAnd, componentCommon.clone() as Term, truthAnd, memory)
            processComposed(taskContent, termDif, componentCommon.clone() as Term, truthDif, memory)
        }
        if (taskContent is Inheritance) {
            introVarOuter(taskContent, beliefContent, index, memory)//            introVarImage(taskContent, beliefContent, index, memory);
        }
    }

    /**
     * Finish composing implication term
     *
     * @param premise1  Type of the contentInd
     * @param subject   Subject of contentInd
     * @param predicate Predicate of contentInd
     * @param truth     TruthValue of the contentInd
     * @param memory    Reference to the memory
     */
    private fun processComposed(statement: Statement, subject: Term?, predicate: Term?, truth: TruthValue?, memory: Memory) {
        if (subject == null || predicate == null) {
            return
        }
        val content: Term? = Statement.make(statement, subject, predicate, memory)
        if (content == null || content == statement || content == memory.currentBelief!!.content) {
            return
        }
        val budget = compoundForward(truth, content, memory)
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * {<(S|P) ==> M>, <P></P>  M>} |- <S></S>  M>
     *
     * @param implication     The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param term1           The other term in the contentInd
     * @param index           The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param compoundTask    Whether the implication comes from the task
     * @param memory          Reference to the memory
     */
    @JvmStatic
    private fun decomposeCompound(compound: CompoundTerm, component: Term, term1: Term, index: Int, compoundTask: Boolean, memory: Memory) {
        if (compound is Statement) {
            return
        }
        val term2 = CompoundTerm.reduceComponents(compound, component, memory) ?: return
        val task: Task = memory.currentTask
        val sentence = task.sentence
        val belief = memory.currentBelief
        val oldContent = task.content as Statement
        val v1: TruthValue?
        val v2: TruthValue?
        if (compoundTask) {
            v1 = sentence.truth
            v2 = belief!!.truth
        } else {
            v1 = belief!!.truth
            v2 = sentence.truth
        }
        var truth: TruthValue? = null
        val content: Term ?
        if (index == 0) {
            content = Statement.make(oldContent, term1, term2, memory)
            if (content != null) {
                if (oldContent is Inheritance) {
                    if (compound is IntersectionExt) {
                        truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                    } else if (compound is IntersectionInt) {
                        truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                    } else if (compound is SetInt && component is SetInt) {
                        truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                    } else if (compound is SetExt && component is SetExt) {
                        truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                    } else if (compound is DifferenceExt) {
                        truth = if (compound.componentAt(0) == component) {
                            TruthFunctions.reduceDisjunction(v2!!, v1!!)
                        } else {
                            TruthFunctions.reduceConjunctionNeg(v1!!, v2!!)
                        }
                    }
                } else if (oldContent is Implication) {
                    if (compound is Conjunction) {
                        truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                    } else if (compound is Disjunction) {
                        truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                    }
                }
            } else return
        } else {
            content = Statement.make(oldContent, term2, term1, memory)
            when {
                content == null -> return
                oldContent is Inheritance -> if (compound is IntersectionInt) {
                    truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                } else if (compound is IntersectionExt) {
                    truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                } else if (compound is SetExt && component is SetExt) {
                    truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                } else if (compound is SetInt && component is SetInt) {
                    truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                } else if (compound is DifferenceInt) {
                    truth = if (compound.componentAt(1) == component) {
                        TruthFunctions.reduceDisjunction(v2!!, v1!!)
                    } else {
                        TruthFunctions.reduceConjunctionNeg(v1!!, v2!!)
                    }
                }
                oldContent is Implication -> if (compound is Disjunction) {
                    truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
                } else if (compound is Conjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
                }
            }
        }
        when {
            truth != null -> {
                val budget = compoundForward(truth, content!!, memory)
                memory.doublePremiseTask(content, truth, budget)
            }
        }
    }

    /**
     * {(||, S, P), P} |- S {(&&, S, P), P} |- S
     *
     * @param implication     The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param compoundTask    Whether the implication comes from the task
     * @param memory          Reference to the memory
     */
    internal fun decomposeStatement(compound: CompoundTerm?, component: Term?, compoundTask: Boolean, memory: Memory) {
        val task: Task = memory.currentTask
        val sentence = task.sentence
        if (sentence.isQuestion) {
            return
        }
        val belief = memory.currentBelief
        val content = CompoundTerm.reduceComponents(compound, component, memory) ?: return
        val v1: TruthValue?
        val v2: TruthValue?
        if (compoundTask) {
            v1 = sentence.truth
            v2 = belief!!.truth
        } else {
            v1 = belief!!.truth
            v2 = sentence.truth
        }
        var truth: TruthValue? = null
        if (compound is Conjunction) {
            if (sentence is Sentence) {
                truth = TruthFunctions.reduceConjunction(v1!!, v2!!)
            }
        } else if (compound is Disjunction) {
            if (sentence is Sentence) {
                truth = TruthFunctions.reduceDisjunction(v1!!, v2!!)
            }
        } else {
            return
        }
        val budget = compoundForward(truth, content, memory)
        memory.doublePremiseTask(content, truth, budget)
    }

    /* --------------- rules used for variable introduction --------------- */


    /**
     * Introduce a dependent variable in an outer-layer conjunction
     *
     * @param taskContent   The first premise <M --> S>
     * @param beliefContent The second premise <M --> P>
     * @param index         The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param memory        Reference to the memory
    </M></M> */
    private fun introVarOuter(taskContent: Statement, beliefContent: Statement, index: Int, memory: Memory) {
        val truthT = memory.currentTask.sentence.truth
        val truthB = memory.currentBelief!!.truth
        val varInd = Variable("\$varInd1")
        val varInd2 = Variable("\$varInd2")
        val term11: Term?
        val term12: Term?
        val term21: Term?
        val term22: Term?
        var commonTerm: Term?
        val subs = HashMap<Term, Term>()
        if (index == 0) {
            term11 = varInd
            term21 = varInd
            term12 = taskContent.predicate
            term22 = beliefContent.predicate
            if (term12 is ImageExt && term22 is ImageExt) {
                commonTerm = term12.theOtherComponent
                if (commonTerm == null || !term22.containTerm(commonTerm)) {
                    commonTerm = term22.theOtherComponent
                    if (commonTerm == null || !term12.containTerm(commonTerm)) {
                        commonTerm = null
                    }
                }
                if (commonTerm != null) {
                    subs[commonTerm] = varInd2
                    term12.applySubstitute(subs)
                    term22.applySubstitute(subs)
                }
            }
        } else {
            term11 = taskContent.subject
            term21 = beliefContent.subject
            term12 = varInd
            term22 = varInd
            if (term11 is ImageInt && term21 is ImageInt) {
                commonTerm = term11.theOtherComponent
                if (commonTerm == null || !term21.containTerm(commonTerm)) {
                    commonTerm = term21.theOtherComponent
                    if (commonTerm == null || !(term11 as ImageExt).containTerm(commonTerm)) {
                        commonTerm = null
                    }
                }
                if (commonTerm != null) {
                    subs[commonTerm] = varInd2
                    term11.applySubstitute(subs)
                    term21.applySubstitute(subs)
                }
            }
        }
        var state1: Statement = Inheritance.make(term11, term12, memory)!!
        var state2: Statement = Inheritance.make(term21, term22, memory)!!
        var content: Term = Implication.make(state1, state2, memory)!!
        var truth: TruthValue = TruthFunctions.induction(truthT!!, truthB!!)
        var budget = compoundForward(truth, content!!, memory)
        memory.doublePremiseTask(content, truth, budget)
        content = Implication.make(state2 as Term, state1 as Term, memory as Memory)!!
        truth = TruthFunctions.induction(truthB!!, truthT)
        budget = compoundForward(truth, content, memory)
        memory.doublePremiseTask(content, truth, budget)
        content = Equivalence.make(state1, state2, memory)!!
        truth = TruthFunctions.comparison(truthT, truthB!!)
        budget = compoundForward(truth, content, memory)
        memory.doublePremiseTask(content, truth, budget)
        val varDep = Variable("#varDep")
        if (index == 0) {
            state1 = Inheritance.make(varDep, taskContent.predicate, memory)!!
            state2 = Inheritance.make(varDep, beliefContent.predicate, memory)!!
        } else {
            state1 = Inheritance.make(taskContent.subject, varDep, memory)!!
            state2 = Inheritance.make(beliefContent.subject, varDep, memory)!!
        }
        content = Conjunction.make(state1, state2, memory)!!
        truth = TruthFunctions.intersection(truthT, truthB)
        budget = compoundForward(truth, content, memory)
        memory.doublePremiseTask(content, truth, budget, false)
    }

    /**
     * {<M --> S>, <C></C> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent   The first premise directly used in internal induction,
     * <M --> S>
     * @param beliefContent The componentCommon to be used as a premise in
     * internal induction, <M --> P>
     * @param oldCompound   The whole contentInd of the first premise, Implication
     * or Conjunction
     * @param memory        Reference to the memory
    </M></M></M></M></M></M> */
    internal fun introVarInner(premise1: Statement, premise2: Statement, oldCompound: CompoundTerm, memory: Memory) {
        val task: Task = memory.currentTask
        val taskSentence = task.sentence
        if (!taskSentence.isJudgment || premise1.javaClass != premise2.javaClass || oldCompound.containComponent(premise1)) {
            return
        }
        val subject1: Term = premise1.subject
        val subject2: Term = premise2.subject
        val predicate1: Term = premise1.predicate
        val predicate2: Term = premise2.predicate
        val commonTerm1: Term
        val commonTerm2: Term?
        if (subject1 == subject2) {
            commonTerm1 = subject1
            commonTerm2 = secondCommonTerm(predicate1, predicate2, 0)
        } else if (predicate1 == predicate2) {
            commonTerm1 = predicate1
            commonTerm2 = secondCommonTerm(subject1, subject2, 0)
        } else {
            return
        }
        val belief = memory.currentBelief
        val substitute = HashMap<Term, Term>()
        substitute[commonTerm1] = Variable("#varDep2")
        var content = Conjunction.make(premise1, oldCompound, memory) as CompoundTerm
        content.applySubstitute(substitute)
        var truth: TruthValue? = TruthFunctions.intersection(taskSentence.truth!!, belief!!.truth!!)
        var budget = forward(truth, memory)
        memory.doublePremiseTask(content, truth, budget, false)
        substitute.clear()
        substitute[commonTerm1] = Variable("\$varInd1")
        if (commonTerm2 != null) {
            substitute[commonTerm2] = Variable("\$varInd2")
        }
        content = Implication.make(premise1, oldCompound, memory)!!
        content.applySubstitute(substitute)
        truth = if (premise1 == taskSentence.content) {
            TruthFunctions.induction(belief.truth!!, taskSentence.truth!!)
        } else {
            TruthFunctions.induction(taskSentence.truth!!, belief.truth!!)
        }
        budget = forward(truth, memory)
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * Introduce a second independent variable into two terms with a common
     * component
     *
     * @param term1 The first term
     * @param term2 The second term
     * @param index The index of the terms in their statement
     */
    private fun secondCommonTerm(term1: Term, term2: Term, index: Int): Term? {
        var commonTerm: Term? = null
        if (index == 0) {
            if (term1 is ImageExt && term2 is ImageExt) {
                commonTerm = term1.theOtherComponent
                if (commonTerm == null || !term2.containTerm(commonTerm)) {
                    commonTerm = term2.theOtherComponent
                    if (commonTerm == null || !term1.containTerm(commonTerm)) {
                        commonTerm = null
                    }
                }
            }
        } else {
            if (term1 is ImageInt && term2 is ImageInt) {
                commonTerm = term1.theOtherComponent
                if (commonTerm == null || !term2.containTerm(commonTerm)) {
                    commonTerm = term2.theOtherComponent
                    if (commonTerm == null || !(term1 as ImageExt).containTerm(commonTerm)) {
                        commonTerm = null
                    }
                }
            }
        }
        return commonTerm
    }
}