/*
 * SyllogisticRules.java
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

import nars.entity.BudgetValue
import nars.entity.Sentence
import nars.entity.Task
import nars.entity.TruthValue
import nars.inference.BudgetFunctions.backward
import nars.inference.BudgetFunctions.backwardWeak
import nars.inference.BudgetFunctions.compoundForward
import nars.inference.BudgetFunctions.forward
import nars.io.Symbols
import nars.language.*
import nars.storage.Memory

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
object SyllogisticRules {/* --------------- rules used in both first-tense inference and higher-tense inference --------------- */

    /**
     * <pre>
     * {<S></S> M>, <M></M> P>} |- {<S></S> P>, <P></P> S>}
    </pre> *
     *
     * @param term1    Subject of the first new task
     * @param term2    Predicate of the first new task
     * @param sentence The first premise
     * @param belief   The second premise
     * @param memory   Reference to the memory
     */
    internal fun dedExe(term1: Term , term2: Term , sentence: Sentence, belief: Sentence, memory: Memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return
        }
        val value1 = sentence.truth!!
        val value2 = belief.truth!!
        var truth1: TruthValue? = null
        var truth2: TruthValue? = null
        val budget1: BudgetValue
        val budget2: BudgetValue
        if (sentence.isQuestion) {
            budget1 = backwardWeak(value2, memory)
            budget2 = backwardWeak(value2, memory)
        } else {
            truth1 = TruthFunctions.deduction(value1, value2)
            truth2 = TruthFunctions.exemplification(value1, value2)
            budget1 = forward(truth1, memory)
            budget2 = forward(truth2, memory)
        }
        val content = sentence.content as Statement
        val content1: Statement? = Statement.make(content, term1, term2, memory)
        val content2: Statement? = Statement.make(content, term2, term1, memory)
        memory.doublePremiseTask(content1, truth1, budget1)
        memory.doublePremiseTask(content2, truth2, budget2)
    }

    /**
     * {<M></M>  S>, <M></M>  P>} |- {<S></S>  P>, <P></P>  S>, <S></S> <=> P>}
     * @param term1        Subject of the first new task
     * @param term2        Predicate of the first new task
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param memory       Reference to the memory
     */
    internal fun abdIndCom(term1: Term , term2: Term , taskSentence: Sentence, belief: Sentence, memory: Memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return
        }
        val taskContent = taskSentence.content as Statement
        var truth1: TruthValue? = null
        var truth2: TruthValue? = null
        var truth3: TruthValue? = null
        val budget1: BudgetValue
        val budget2: BudgetValue
        val budget3: BudgetValue
        val value1 = taskSentence.truth
        val value2 = belief.truth
        if (taskSentence.isQuestion) {
            budget1 = backward(value2, memory)
            budget2 = backwardWeak(value2, memory)
            budget3 = backward(value2, memory)
        } else {
            truth1 = TruthFunctions.abduction(value1!!, value2!!)
            truth2 = TruthFunctions.abduction(value2!!, value1!!)
            truth3 = TruthFunctions.comparison(value1!!, value2!!)
            budget1 = forward(truth1, memory)
            budget2 = forward(truth2, memory)
            budget3 = forward(truth3, memory)
        }
        val statement1: Statement? = Statement.make(taskContent, term1, term2, memory)
        val statement2: Statement? = Statement.make(taskContent, term2, term1, memory)
        val statement3: Statement? = Statement.makeSym(taskContent, term1, term2, memory)
        memory.doublePremiseTask(statement1, truth1, budget1)
        memory.doublePremiseTask(statement2, truth2, budget2)
        memory.doublePremiseTask(statement3, truth3, budget3)
    }

    /**
     * {<S></S>  P>, <M></M> <=> P>} |- <S></S>  P>
     * @param term1  Subject of the new task
     * @param term2  Predicate of the new task
     * @param asym   The asymmetric premise
     * @param sym    The symmetric premise
     * @param memory Reference to the memory
     */
    internal fun analogy(term1: Term, term2: Term, asym: Sentence, sym: Sentence, memory: Memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return
        }
        val st = asym.content as Statement
        var truth: TruthValue? = null
        val budget: BudgetValue
        val sentence = memory.currentTask.sentence
        val taskTerm = sentence.content as CompoundTerm
        if (sentence.isQuestion) {
            budget = if (taskTerm.isCommutative) {
                backwardWeak(asym.truth, memory)
            } else {
                backward(sym.truth, memory)
            }
        } else {
            truth = TruthFunctions.analogy(asym.truth!!, sym.truth!!)
            budget = forward(truth, memory)
        }
        val content: Term? = Statement.make(st, term1, term2, memory)
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * {<S></S> <=> M>, <M></M> <=> P>} |- <S></S> <=> P>
     * @param term1    Subject of the new task
     * @param term2    Predicate of the new task
     * @param belief   The first premise
     * @param sentence The second premise
     * @param memory   Reference to the memory
     */
    internal fun resemblance(term1: Term, term2: Term, belief: Sentence, sentence: Sentence, memory: Memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return
        }
        val st = belief.content as Statement
        var truth: TruthValue? = null
        val budget: BudgetValue
        if (sentence.isQuestion) {
            budget = backward(belief.truth, memory)
        } else {
            truth = TruthFunctions.resemblance(belief.truth!!, sentence.truth!!)
            budget = forward(truth, memory)
        }
        val statement: Term? = Statement.make(st, term1, term2, memory)
        memory.doublePremiseTask(statement, truth, budget)
    }

    /* --------------- rules used only in conditional inference --------------- */


    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P> {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S> {<<M --> S> <=> <M --> P>>, <M --> S>}
     * |- <M --> P> {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence  The premise on part of s1
     * @param side         The location of s2 in s1
     * @param memory       Reference to the memory
    </M></M></M></M></M></M></M></M></M></M></M></M></M></M></M></M> */
    internal fun detachment(mainSentence: Sentence, subSentence: Sentence, side: Int, memory: Memory) {
        val statement = mainSentence.content as Statement
        if (statement !is Implication && statement !is Equivalence) {
            return
        }
        val subject: Term = statement.subject
        val predicate: Term = statement.predicate
        val content: Term
        val term = subSentence.content
        content = if (side == 0 && term == subject) {
            predicate
        } else if (side == 1 && term == predicate) {
            subject
        } else {
            return
        }
        if (content is Statement && content.invalid()) {
            return
        }
        val taskSentence = memory.currentTask.sentence
        val beliefSentence = memory.currentBelief
        val beliefTruth = beliefSentence!!.truth
        val truth1 = mainSentence.truth
        val truth2 = subSentence.truth
        var truth: TruthValue? = null
        val budget: BudgetValue
        if (taskSentence.isQuestion) {
            budget = if (statement is Equivalence) {
                backward(beliefTruth, memory)
            } else if (side == 0) {
                backwardWeak(beliefTruth, memory)
            } else {
                backward(beliefTruth, memory)
            }
        } else {
            truth = if (statement is Equivalence) {
                TruthFunctions.analogy(truth2!!, truth1!!)
            } else if (side == 0) {
                TruthFunctions.deduction(truth1!!, truth2!!)
            } else {
                TruthFunctions.abduction(truth2!!, truth1!!)
            }
            budget = forward(truth, memory)
        }
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P> {<(&&, S2, S3) ==>
     * P>, <S1></S1>  S2>} |- <(&&, S1, S3) ==> P> {<(&&, S1, S3) ==> P>, <S1></S1>
     * S2>} |- <(&&, S2, S3) ==> P>
     *
     * @param premise1 The conditional premise
     * @param index    The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the
     * condition of premise1
     * @param side     The location of the shared term in premise2: 0 for subject, 1
     * for predicate, -1 for the whole term
     * @param memory   Reference to the memory
     */
    internal fun conditionalDedInd(premise1: Implication, index: Short, premise2: Term, side: Int, memory: Memory) {
        var index1 = index
        val task: Task = memory.currentTask
        val taskSentence = task.sentence
        val belief = memory.currentBelief
        val deduction = side != 0
        val conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief!!.content)
        val commonComponent: Term
        var newComponent: Term? = null
        if (side == 0) {
            commonComponent = (premise2 as Statement).subject
            newComponent = premise2.predicate
        } else if (side == 1) {
            commonComponent = (premise2 as Statement).predicate
            newComponent = premise2.subject
        } else {
            commonComponent = premise2
        }
        val oldCondition = premise1.subject as Conjunction
        val index2 = oldCondition.components.indexOf(commonComponent)
        if (index2 >= 0) {
            index1 = index2.toShort()
        } else {
            var match = Variable.unify(Symbols.VAR_INDEPENDENT, oldCondition.componentAt(index1.toInt()), commonComponent, premise1, premise2)
            if (!match && commonComponent.javaClass == oldCondition.javaClass) {
                match = Variable.unify(Symbols.VAR_INDEPENDENT, oldCondition.componentAt(index1.toInt()), (commonComponent as CompoundTerm).componentAt(index1.toInt()), premise1, premise2)
            }
            if (!match) {
                return
            }
        }
        val newCondition: Term?
        newCondition = if (oldCondition == commonComponent) {
            null
        } else {
            CompoundTerm.setComponent(oldCondition, index1.toInt(), newComponent, memory)
        }
        val content: Term?
        content = if (newCondition != null) {
            Statement.make(premise1, newCondition, premise1.predicate, memory)
        } else {
            premise1.predicate
        }
        if (content == null) {
            return
        }
        val truth1 = taskSentence.truth!!
        val truth2 = belief.truth!!
        var truth: TruthValue? = null
        val budget: BudgetValue
        if (taskSentence.isQuestion) {
            budget = backwardWeak(truth2, memory)
        } else {
            truth = if (deduction) {
                TruthFunctions.deduction(truth1, truth2)
            } else if (conditionalTask) {
                TruthFunctions.induction(truth2, truth1)
            } else {
                TruthFunctions.induction(truth1, truth2)
            }
            budget = forward(truth, memory)
        }
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     *
     * @param premise1 The equivalence premise
     * @param index    The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the
     * condition of premise1
     * @param side     The location of the shared term in premise2: 0 for subject, 1
     * for predicate, -1 for the whole term
     * @param memory   Reference to the memory
     */
    internal fun conditionalAna(premise1: Equivalence, index: Short, premise2: Term, side: Int, memory: Memory) {
        val task: Task = memory.currentTask
        val taskSentence = task.sentence
        val belief = memory.currentBelief
        val conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief!!.content)
        val commonComponent: Term
        var newComponent: Term? = null
        if (side == 0) {
            commonComponent = (premise2 as Statement).subject
            newComponent = premise2.predicate
        } else if (side == 1) {
            commonComponent = (premise2 as Statement).predicate
            newComponent = premise2.subject
        } else {
            commonComponent = premise2
        }
        val oldCondition = premise1.subject as Conjunction
        var match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index.toInt()), commonComponent, premise1, premise2)
        if (!match && commonComponent.javaClass == oldCondition.javaClass) {
            match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index.toInt()), (commonComponent as CompoundTerm).componentAt(index.toInt()), premise1, premise2)
        }
        if (!match) {
            return
        }
        val newCondition: Term?
        newCondition = if (oldCondition == commonComponent) {
            null
        } else {
            CompoundTerm.setComponent(oldCondition, index.toInt(), newComponent, memory)
        }
        val content: Term?
        content = if (newCondition != null) {
            Statement.make(premise1, newCondition, premise1.predicate, memory)
        } else {
            premise1.predicate
        }
        if (content == null) {
            return
        }
        val truth1 = taskSentence.truth!!
        val truth2 = belief.truth!!
        var truth: TruthValue? = null
        val budget: BudgetValue
        if (taskSentence.isQuestion) {
            budget = backwardWeak(truth2, memory)
        } else {
            truth = if (conditionalTask) {
                TruthFunctions.comparison(truth1, truth2)
            } else {
                TruthFunctions.analogy(truth1, truth2)
            }
            budget = forward(truth, memory)
        }
        memory.doublePremiseTask(content, truth, budget)
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1></S1>  S2>
     *
     * @param cond1       The condition of the first premise
     * @param cond2       The condition of the second premise
     * @param taskContent The first premise
     * @param st2         The second premise
     * @param memory      Reference to the memory
     * @return Whether there are derived tasks
     */
    internal fun conditionalAbd(cond1: Term, cond2: Term, st1: Statement, st2: Statement, memory: Memory): Boolean {
        if (st1 !is Implication || st2 !is Implication) {
            return false
        }
        if (cond1 !is Conjunction && cond2 !is Conjunction) {
            return false
        }
        var term1: Term? = null
        var term2: Term? = null
//        if ((cond1 instanceof Conjunction) && !Variable.containVarDep(cond1.getName())) {


        if (cond1 is Conjunction) {
            term1 = CompoundTerm.reduceComponents(cond1, cond2, memory)
        }
//        if ((cond2 instanceof Conjunction) && !Variable.containVarDep(cond2.getName())) {


        if (cond2 is Conjunction) {
            term2 = CompoundTerm.reduceComponents(cond2, cond1, memory)
        }
        if (term1 == null && term2 == null) {
            return false
        }
        val task: Task = memory.currentTask
        val sentence = task.sentence
        val belief = memory.currentBelief
        val value1 = sentence.truth!!
        val value2 = belief!!.truth!!
        var content: Term
        var truth: TruthValue? = null
        var budget: BudgetValue
        if (term1 != null) {
            content = when {
                term2 != null -> Statement.make(st2!!, term2!!, term1!!, memory!!)!!
                else -> term1
            }
            if (sentence.isQuestion) {
                budget = backwardWeak(value2, memory)
            } else {
                truth = TruthFunctions.abduction(value2, value1)
                budget = forward(truth, memory)
            }
            memory.doublePremiseTask(content, truth, budget)
        }
        if (term2 != null) {
            content = if (term1 != null) {
                Statement.make(st1!!, term1!!, term2!!, memory!!)!!
            } else {
                term2
            }
            if (sentence.isQuestion) {
                budget = backwardWeak(value2, memory)
            } else {
                truth = TruthFunctions.abduction(value1, value2)
                budget = forward(truth, memory)
            }
            memory.doublePremiseTask(content, truth, budget)
        }
        return true
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>>, <M --> P>} |- <M --> S>
     *
     * @param compound     The compound term to be decomposed
     * @param component    The part of the compound to be removed
     * @param compoundTask Whether the compound comes from the task
     * @param memory       Reference to the memory
    </M></M> */
    internal fun elimiVarDep(compound: CompoundTerm?, component: Term?, compoundTask: Boolean, memory: Memory) {
        val content: Term? = CompoundTerm.reduceComponents(compound, component, memory)
        val task: Task = memory.currentTask
        val sentence = task.sentence
        val belief = memory.currentBelief
        val v1 = sentence.truth!!
        val v2 = belief!!.truth!!
        var truth: TruthValue? = null
        val budget: BudgetValue
        if (sentence.isQuestion) {
            budget = if (compoundTask) backward(v2, memory) else backwardWeak(v2, memory)
        } else {
            truth = if (compoundTask) TruthFunctions.anonymousAnalogy(v1, v2) else TruthFunctions.anonymousAnalogy(v2, v1)
            budget = compoundForward(truth, content!!, memory)
        }
        memory.doublePremiseTask(content, truth, budget)
    }
}