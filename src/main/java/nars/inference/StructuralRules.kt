/*
 * StructuralRules.java
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
import nars.entity.Task
import nars.entity.TruthValue
import nars.inference.BudgetFunctions.compoundBackward
import nars.inference.BudgetFunctions.compoundBackwardWeak
import nars.inference.BudgetFunctions.compoundForward
import nars.inference.BudgetFunctions.forward
import nars.language.*
import nars.main_nogui.Parameters
import nars.storage.BackingStore

/**
 * Single-premise inference rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
object StructuralRules {
    private const val RELIANCE = Parameters.RELIANCE

    /* -------------------- transform between compounds and components -------------------- */


    /**
     * {<S --> P>, S@(S&T)} |- <(S&T) --> (P&T)> {<S --> P>, S@(M-S)} |- <(M-P)
     * --> (M-S)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param memory    Reference to the memory
    </S></S> */
    internal fun structuralCompose2(compound: CompoundTerm, index: Int, statement: Statement, side:  Int, memory: BackingStore) {
        if (compound == statement.componentAt(side.toInt())) {
            return
        }
        var sub: Term = statement.subject
        var pred: Term = statement.predicate
        val components: MutableList<Term> = compound.cloneComponents()!!
        if (side.toInt() == 0 && components.contains(pred) || side.toInt() == 1 && components.contains(sub)) {
            return
        }
        if (side.toInt() == 0) {
            if (components.contains(sub)) {
                if (pred is CompoundTerm) {
                    return
                }
                sub = compound
                components[index.toInt()] = pred
                pred = Util11.make(compound, components, memory)!!
            }
        } else {
            if (components.contains(pred)) {
                if (sub is CompoundTerm) {
                    return
                }
                components[index.toInt()] = sub
                sub = Util11.make(compound, components, memory)!!
                pred = compound
            }
        }
        if (sub == null || pred == null) {
            return
        }
        val content: Term?
        content = if (switchOrder(compound, index)) {
            Statement.make(statement, pred, sub, memory)
        } else {
            Statement.make(statement, sub, pred, memory)
        }
        if (content == null) {
            return
        }
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        var truth = sentence.truth
        val budget: BudgetValue
        if (sentence.isQuestion) {
            budget = memory.compoundBackwardWeak(content)
        } else {
            if (compound.size() > 1) {
                if (sentence.isJudgment) {
                    truth = TruthFunctions.deduction(truth!!, RELIANCE)
                } else {
                    return
                }
            }
            budget = memory.compoundForward(truth, content)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
    </S> */
    internal fun structuralDecompose2(statement: Statement, index: Int, memory: BackingStore) {
        val subj: Term = statement.subject
        val pred: Term = statement.predicate
        if (subj.javaClass != pred.javaClass) {
            return
        }
        val sub = subj as CompoundTerm
        val pre = pred as CompoundTerm
        if (sub.size() != pre.size() || sub.size() <= index) {
            return
        }
        val t1: Term = sub.componentAt(index)
        val t2: Term = pre.componentAt(index)
        val content: Term?
        content = if (switchOrder(sub, index)) {
            Statement.make(statement, t2, t1, memory)
        } else {
            Statement.make(statement, t1, t2, memory)
        }
        if (content == null) {
            return
        }
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        val truth = sentence.truth
        val budget: BudgetValue
        budget = if (sentence.isQuestion) {
            memory.compoundBackward(content)
        } else {
            if (sub !is Product && sub.size() > 1 && sentence.isJudgment) {
                return
            }
            memory.compoundForward(truth, content)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /**
     * List the cases where the direction of inheritance is revised in
     * conclusion
     *
     * @param compound The compound term
     * @param index    The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private fun switchOrder(compound: CompoundTerm, index: Int): Boolean {
        return ((compound is DifferenceExt || compound is DifferenceInt) && index.toInt() == 1
                || compound is ImageExt && index != compound.relationIndex
                || compound is ImageInt && index != compound.relationIndex)
    }

    /**
     * {<S --> P>, P@(P&Q)} |- <S --> (P&Q)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param memory    Reference to the memory
    </S></S> */
    internal fun structuralCompose1(compound: CompoundTerm, index: Int, statement: Statement, memory: BackingStore) {
        if (!memory.currentTask!!.sentence.isJudgment) {
            return
        }
        val component: Term = compound.componentAt(index.toInt())
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        val truth = sentence.truth
        val truthDed: TruthValue = TruthFunctions.deduction(truth!!, RELIANCE)
        val truthNDed: TruthValue = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE))
        val subj: Term = statement.subject
        val pred: Term = statement.predicate
        if (component == subj) {
            if (compound is IntersectionExt) {
                structuralStatement(compound, pred, truthDed, memory)
            } else if (compound is IntersectionInt) {
            } else if (compound is DifferenceExt && index.toInt() == 0) {
                structuralStatement(compound, pred, truthDed, memory)
            } else if (compound is DifferenceInt) {
                if (index.toInt() == 0) {
                } else {
                    structuralStatement(compound, pred, truthNDed, memory)
                }
            }
        } else if (component == pred) {
            if (compound is IntersectionExt) {
            } else if (compound is IntersectionInt) {
                structuralStatement(subj, compound, truthDed, memory)
            } else if (compound is DifferenceExt) {
                if (index.toInt() == 0) {
                } else {
                    structuralStatement(subj, compound, truthNDed, memory)
                }
            } else if (compound is DifferenceInt && index.toInt() == 0) {
                structuralStatement(subj, compound, truthDed, memory)
            }
        }
    }

    /**
     * {<(S&T) --> P>, S@(S&T)} |- <S --> P>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param memory    Reference to the memory
    </S> */
    internal fun structuralDecompose1(compound: CompoundTerm, index: Int, statement: Statement, memory: BackingStore) {
        if (!memory.currentTask!!.sentence.isJudgment) {
            return
        }
        val component: Term = compound.componentAt(index.toInt())
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        val truth = sentence.truth
        val truthDed: TruthValue = TruthFunctions.deduction(truth!!, RELIANCE)
        val truthNDed: TruthValue = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE))
        val subj: Term = statement.subject
        val pred: Term = statement.predicate
        if (compound == subj) {
            if (compound is IntersectionExt) {
            } else if (compound is IntersectionInt) {
                structuralStatement(component, pred, truthDed, memory)
            } else if (compound is DifferenceExt && index.toInt() == 0) {
            } else if (compound is DifferenceInt) {
                if (index.toInt() == 0) {
                    structuralStatement(component, pred, truthDed, memory)
                } else {
                    structuralStatement(component, pred, truthNDed, memory)
                }
            }
        } else if (compound == pred) {
            if (compound is IntersectionExt) {
                structuralStatement(subj, component, truthDed, memory)
            } else if (compound is IntersectionInt) {
            } else if (compound is DifferenceExt) {
                if (index.toInt() == 0) {
                    structuralStatement(subj, component, truthDed, memory)
                }
//            } else if ((compound instanceof DifferenceInt) && (index == 0)) {
                else {
                    structuralStatement(subj, component, truthNDed, memory)
                }
            }
        }
    }

    /**
     * Common final operations of the above two methods
     *
     * @param subject   The subject of the new task
     * @param predicate The predicate of the new task
     * @param truth     The truth value of the new task
     * @param memory    Reference to the memory
     */
    private fun structuralStatement(subject: Term, predicate: Term, truth: TruthValue, memory: BackingStore) {
        val task: Task = memory.currentTask!!
        val oldContent = task.content
        if (oldContent is Statement) {
            val content: Term? = Statement.make(oldContent, subject, predicate, memory)
            if (content != null) {
                val budget = memory.compoundForward(truth, content)
                memory.singlePremiseTask(content, truth, budget)
            }
        }
    }

    /* -------------------- set transform -------------------- */


    /**
     * {<S --> {P}>} |- <S></S><-> {P}>
     *
     * @param compound  The set compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param memory    Reference to the memory
    </S> */
    internal fun transformSetRelation(compound: CompoundTerm, statement: Statement, side:  Int, memory: BackingStore) {
        if (compound.size() > 1) {
            return
        }
        if (statement is Inheritance) {
            if (compound is SetExt && side.toInt() == 0 || compound is SetInt && side.toInt() == 1) {
                return
            }
        }
        val sub: Term = statement.subject
        val pre: Term = statement.predicate
        val content: Term?
        content = if (statement is Inheritance) {
            Similarity.make(sub, pre, memory)
        } else {
            if (compound is SetExt && side.toInt() == 0 || compound is SetInt && side.toInt() == 1) {
                Inheritance.make(pre, sub, memory)
            } else {
                Inheritance.make(sub, pre, memory)
            }
        }
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        val truth = sentence.truth
        val budget: BudgetValue
        budget = if (sentence.isQuestion) {
            memory.compoundBackward(content!!)
        } else {
            memory.compoundForward(truth, content!!)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /* -------------------- products and images transform -------------------- */


    /**
     * Equivalent transformation between products and images {<(*, S, M) --> P>,
     * S@(*, S, M)} |- <S --> (/, P, _, M)> {<S --> (/, P, _, M)>, P@(/, P, _,
     * M)} |- <(*, S, M) --> P> {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M -->
     * (/, P, S, _)>
     *
     * @param inh        An Inheritance statement
     * @param oldContent The whole content
     * @param indices    The indices of the TaskLink
     * @param task       The task
     * @param memory     Reference to the memory
    </M></S></S></S> */
    internal fun transformProductImage(inh: Inheritance, oldContent: CompoundTerm, indices:  IntArray, memory: BackingStore) {
        var subject: Term = inh.subject
        var predicate: Term = inh.predicate
        if (inh == oldContent) {
            if (subject is CompoundTerm) {
                transformSubjectPI(subject, predicate, memory)
            }
            if (predicate is CompoundTerm) {
                transformPredicatePI(subject, predicate, memory)
            }
            return
        }
        val index = indices[indices.size - 1].toInt()
        val side = indices[indices.size - 2].toInt()
        val comp = inh.componentAt(side.toInt()) as CompoundTerm
        when {
            comp is Product -> {
                if (side.toInt() == 0) {
                    subject = comp.componentAt(index.toInt())
                    predicate = ImageExt.make(comp as Product, inh.predicate, index, memory)
                } else {
                    subject = ImageInt.makeProduct((comp as Product ), inh.subject, index.toInt(), memory)
                    predicate = comp.componentAt(index.toInt())
                }
            }
            comp is ImageExt && side.toInt() == 1 -> {
                if (index == comp.relationIndex) {
                    subject = Product.make(comp as ImageExt, inh.subject, index.toInt(), memory)
                    predicate = comp.componentAt(index.toInt())
                } else {
                    subject = comp.componentAt(index.toInt())
                    predicate = ImageExt.make(comp, inh.subject, index.toInt(), memory)
                }
            }
            comp is ImageInt && side.toInt() == 0 -> {
                if (index == comp.relationIndex) {
                    subject = comp.componentAt(index.toInt())
                    predicate = Product.make(comp, inh.predicate, index.toInt(), memory)
                } else {
                    subject = ImageInt.make(comp, inh.predicate, index, memory)
                    predicate = comp.componentAt(index.toInt())
                }
            }
            else -> {
                return
            }
        }
        val newInh: Inheritance = Inheritance.make(subject, predicate, memory)!!
        var content: Term? = null
        if (indices.size == 2) {
            content = newInh
        } else {
            if (oldContent is Statement && indices[0] == 1.toInt()) {
                content = Statement.make(oldContent, oldContent.componentAt(0), newInh, memory)
            } else {
                val componentList: MutableList<Term>
                val condition: Term? = oldContent.componentAt(0)
                if ((oldContent is Implication || oldContent is Equivalence) && condition is Conjunction) {
                    componentList = (condition as CompoundTerm).cloneComponents()!!
                    componentList[indices[1].toInt()] = newInh
                    val newCond: Term = Util11.make(condition as CompoundTerm, componentList, memory)!!
                    content = Statement.make(oldContent as Statement, newCond, oldContent.predicate, memory)
                } else {
                    componentList = oldContent.cloneComponents()!!
                    componentList[indices[0].toInt()] = newInh
                    if (oldContent is Conjunction) {
                        content = Util11.make(oldContent, componentList, memory)
                    } else if (oldContent is Implication || oldContent is Equivalence) {
                        content = Statement.make(oldContent as Statement, componentList[0], componentList[1], memory)
                    }
                }
            }
        }
        if (content == null) {
            return
        }
        val sentence = memory.currentTask!!.sentence
        val truth = sentence.truth
        val budget: BudgetValue
        budget = if (sentence.isQuestion) {
            memory.compoundBackward(content)
        } else {
            memory.compoundForward(truth, content)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /**
     * Equivalent transformation between products and images when the subject is
     * a compound {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)> {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P> {<S --> (/, P, _,
     * M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param memory    Reference to the memory
    </M></S></S></S> */
    private fun transformSubjectPI(subject: CompoundTerm, predicate: Term, memory: BackingStore) {
        val truth = memory.currentTask!!.sentence.truth
        var budget: BudgetValue
        var inheritance: Inheritance?
        var newSubj: Term
        var newPred: Term
        if (subject is Product) {
            for (i in 0 until subject.size()) {
                newSubj = subject.componentAt(i)
                newPred = ImageExt.make(subject as Product, predicate, i , memory)
                inheritance = Inheritance.make(newSubj, newPred, memory)
                budget = if (truth == null) {
                    memory.compoundBackward(inheritance!!)
                } else {
                    memory.compoundForward(truth, inheritance!!)
                }
                memory.singlePremiseTask(inheritance, truth, budget)
            }
        } else if (subject is ImageInt) {
            val relationIndex = subject.relationIndex.toInt()
            for (i in 0 until subject.size()) {
                if (i == relationIndex) {
                    newSubj = subject.componentAt(relationIndex)
                    newPred = Product.make(subject, predicate, relationIndex, memory)
                } else {
                    newSubj = ImageInt.make(subject, predicate, i , memory)
                    newPred = subject.componentAt(i)
                }
                inheritance = Inheritance.make(newSubj, newPred, memory)
                budget = if (truth == null) {
                    memory.compoundBackward(inheritance!!)
                } else {
                    memory.compoundForward(truth, inheritance!!)
                }
                memory.singlePremiseTask(inheritance, truth, budget)
            }
        }
    }

    /**
     * Equivalent transformation between products and images when the predicate
     * is a compound {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P> {<S --> (/,
     * P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param memory    Reference to the memory
    </M></S></S></S> */
    private fun transformPredicatePI(subject: Term, predicate: CompoundTerm, memory: BackingStore) {
        val truth = memory.currentTask!!.sentence.truth
        var budget: BudgetValue
        var inheritance: Inheritance?
        var newSubj: Term
        var newPred: Term
        if (predicate is Product) {
            for (i in 0 until predicate.size()) {
                newSubj = ImageInt.makeProduct(predicate, subject, i , memory)
                newPred = predicate.componentAt(i)
                inheritance = Inheritance.make(newSubj, newPred, memory)
                budget = if (truth == null) {
                    memory.compoundBackward(inheritance!!)
                } else {
                    memory.compoundForward(truth, inheritance!!)
                }
                memory.singlePremiseTask(inheritance, truth, budget)
            }
        } else if (predicate is ImageExt) {
            val relationIndex = predicate.relationIndex.toInt()
            for (i in 0 until predicate.size()) {
                if (i == relationIndex) {
                    newSubj = Product.make(predicate, subject, relationIndex, memory)
                    newPred = predicate.componentAt(relationIndex)
                } else {
                    newSubj = predicate.componentAt(i)
                    newPred = ImageExt.make(predicate, subject, i, memory)
                }
                inheritance = Inheritance.make(newSubj, newPred, memory)
                if (inheritance != null) { // jmv <<<<<

                    budget = if (truth == null) {
                        memory.compoundBackward(inheritance)
                    } else {
                        memory.compoundForward(truth, inheritance)
                    }
                    memory.singlePremiseTask(inheritance, truth, budget)
                }
            }
        }
    }


    /* --------------- Disjunction and Conjunction transform --------------- */


    /**
     * {(&&, A, B), A@(&&, A, B)} |- A {(||, A, B), A@(||, A, B)} |- A
     *
     * @param compound     The premise
     * @param component    The recognized component in the premise
     * @param compoundTask Whether the compound comes from the task
     * @param memory       Reference to the memory
     */
    internal fun structuralCompound(compound: CompoundTerm?, component: Term, compoundTask: Boolean, memory: BackingStore) {
        if (!component.constant) {
            return
        }
        val content = if (compoundTask) component else compound!!
        val task: Task = memory.currentTask!!
//        if (task.isStructural()) {
//            return;
//        }


        val sentence = task.sentence
        var truth = sentence.truth
        val budget: BudgetValue
        if (sentence.isQuestion) {
            budget = memory.compoundBackward(content)
        } else {
            if (sentence.isJudgment == (compoundTask == compound is Conjunction)) {
                truth = TruthFunctions.deduction(truth!!, RELIANCE)
            } else {
                return
            }
            budget = memory.forward(truth)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /* --------------- Negation related rules --------------- */


    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content The premise
     * @param memory  Reference to the memory
     */
    @JvmStatic
    fun transformNegation(content: Term?, memory: BackingStore) {
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        var truth = sentence.truth
        if (sentence.isJudgment) {
            truth = TruthFunctions.negation(truth!!)
        }
        val budget: BudgetValue
        budget = if (sentence.isQuestion) {
            memory.compoundBackward(content!!)
        } else {
            memory.compoundForward(truth, content!!)
        }
        memory.singlePremiseTask(content, truth, budget)
    }

    /**
     * {<A></A>  B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    internal fun contraposition(statement: Statement, memory: BackingStore) {
        val subj: Term = statement.subject
        val pred: Term = statement.predicate
        val task: Task = memory.currentTask!!
        val sentence = task.sentence
        val term = Negation.make(pred, memory) as Term
        val make = Negation.make(subj, memory)
        val content: Term? = Statement.make(statement, term, make!!, memory) as Term
        var truth = sentence.truth
        val budget: BudgetValue
        if (sentence.isQuestion) {
            budget = if (content is Implication) {
                memory.compoundBackwardWeak(content)
            } else {
                memory.compoundBackward(content!!)
            }
        } else {
            if (content is Implication) {
                truth = TruthFunctions.contraposition(truth!!)
            }
            budget = memory.compoundForward(truth, content!!)
        }
        memory.singlePremiseTask(content, truth, budget)
    }
}