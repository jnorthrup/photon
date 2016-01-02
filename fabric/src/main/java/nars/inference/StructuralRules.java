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
package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;
import nars.storage.Parameters;

import java.util.List;

import static nars.inference.BudgetFunctions.compoundBackward;
import static nars.inference.BudgetFunctions.compoundForward;

/**
 * Single-premise inference rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
public final class StructuralRules {

    private static final float RELIANCE = Parameters.RELIANCE;

	/*
     * -------------------- transform between compounds and components
	 * --------------------
	 */

    /**
     * {<S --> P>, S@(S&T)} |- <(S&T) --> (P&T)> {<S --> P>, S@(M-S)} |- <(M-P)
     * --> (M-S)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param memory    Reference to the memory
     */
    static void structuralCompose2(CompoundTerm compound, Integer index, Statement statement, Integer side, Memory memory) {
        if (compound.equals(statement.componentAt(side))) {
            return;
        }
        Term sub = statement.getSubject();
        Term pred = statement.getPredicate();
        List<Term> components = compound.cloneComponents();
        if ((0 == side && components.contains(pred)) || (1 == side && components.contains(sub))) {
            return;
        }
        if (0 == side) {
            if (!components.contains(sub)) {
                if (components.contains(pred)) {
                    if (sub instanceof CompoundTerm) {
                        return;
                    } else {
                        components.set(index, sub);
                        sub = CompoundTerm.make(compound, components, memory);
                        pred = compound;
                    }
                } else {
                    if (pred instanceof CompoundTerm) {
                        return;
                    } else {
                        sub = compound;
                        components.set(index, pred);
                        pred = CompoundTerm.make(compound, components, memory);
                    }
                }
            }
        }
        if (null != sub && null != pred) {
            Term content = switchOrder(compound, index) ? Statement.make(statement, pred, sub, memory) : Statement.make(statement, sub, pred, memory);
            if (null != content) {
                Task task = memory.getCurrentTask();
                Sentence sentence = task.getSentence();
                TruthValue truth = sentence.getTruth();
                BudgetValue budget;
                if (sentence.isQuestion()) {
                    budget = BudgetFunctions.compoundBackwardWeak(content, memory);
                } else {
                    if (1 < compound.size()) {
                        if (sentence.isJudgment()) {
                            truth = TruthFunctions.deduction(truth, RELIANCE);
                        } else {
                            return;
                        }
                    }
                    budget = compoundForward(memory, truth, content);
                }
                Memory.singlePremiseTask(memory, content, truth, budget);
            }
        }
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void structuralDecompose2(Statement statement, Memory memory) {
        Term subj = statement.getSubject();
        Term pred = statement.getPredicate();
        if (subj.getClass() != pred.getClass()) {
            return;
        }
        CompoundTerm sub = (CompoundTerm) subj;
        CompoundTerm pre = (CompoundTerm) pred;
        if (sub.size() != pre.size()) {
            return;
        }
        int index = -1;
        Term t1, t2;
        for (int i = 0; i < sub.size(); i++) {
            t1 = sub.componentAt(i);
            t2 = pre.componentAt(i);
            if (!t1.equals(t2)) {
                if (0 > index) {
                    index = i;
                } else {
                    return;
                }
            }
        }
        t1 = sub.componentAt(index);
        t2 = pre.componentAt(index);
        Term content;
        if (switchOrder(sub, index)) {
            content = Statement.make(statement, t2, t1, memory);
        } else {
            content = Statement.make(statement, t1, t2, memory);
        }
        if (null == content) {
            return;
        }
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = compoundBackward(content, memory);
        } else {
            if (1 < sub.size() && sentence.isJudgment()) {
                return;

            }
            budget = compoundForward(memory, truth, content);
        }
        Memory.singlePremiseTask(memory, content, truth, budget);
    }

    /**
     * List the cases where the direction of inheritance is revised in
     * conclusion
     *
     * @param compound The compound term
     * @param index    The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private static boolean switchOrder(CompoundTerm compound, Integer index) {
        return (compound instanceof DifferenceExt || compound instanceof DifferenceInt) && 1 == index
                || compound instanceof ImageExt && index != ((ImageExt) compound)
                .getRelationIndex() || compound instanceof ImageInt && index != ((ImageInt) compound)
                .getRelationIndex();
    }

    /**
     * {<S --> P>, P@(P&Q)} |- <S --> (P&Q)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void structuralCompose1(CompoundTerm compound, Integer index,
                                   Statement statement, Memory memory) {
        if (memory.getCurrentTask().getSentence().isJudgment()) {
            Term component = compound.componentAt(index);
            Task task = memory.getCurrentTask();
            Sentence sentence = task.getSentence();
            TruthValue truth = sentence.getTruth();
            TruthValue truthDed = TruthFunctions.deduction(truth, RELIANCE);
            TruthValue truthNDed = TruthFunctions.negation(TruthFunctions
                    .deduction(truth, RELIANCE));
            Term subj = statement.getSubject();
            Term pred = statement.getPredicate();

            if (component.equals(subj) && !(compound instanceof IntersectionExt) && !(compound instanceof IntersectionInt)) {
                if (!(compound instanceof DifferenceExt) || 0 != index) {
                    if (compound instanceof DifferenceInt)
                        switch (index) {
                            case 0:
                                if (component.equals(pred) && compound instanceof DifferenceExt)
                                    structuralStatement(subj, compound, truthNDed, memory);
                                else structuralStatement(compound, pred, truthDed, memory);
                                break;
                            default:
                                structuralStatement(compound, pred, truthNDed, memory);
                                break;
                        }
                } else {
                    structuralStatement(compound, pred, truthDed, memory);
                }
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
     */
    static void structuralDecompose1(CompoundTerm compound, Integer index,
                                     Statement statement, Memory memory) {
        if (memory.getCurrentTask().getSentence().isJudgment()) {
            Term component = compound.componentAt(index);
            Task task = memory.getCurrentTask();
            Sentence sentence = task.getSentence();
            TruthValue truth = sentence.getTruth();
            TruthValue truthDed = TruthFunctions.deduction(truth, RELIANCE);
            TruthValue truthNDed = TruthFunctions.negation(TruthFunctions
                    .deduction(truth, RELIANCE));
            Term subj = statement.getSubject();
            Term pred = statement.getPredicate();
            if (!compound.equals(subj) && compound.equals(pred)
                    && !(compound instanceof IntersectionExt)) {
                if (!(compound instanceof IntersectionInt)) {
                    structuralStatement(
                            component,
                            pred,
                            compound instanceof DifferenceExt && 0 == index
                                    || !(compound instanceof DifferenceInt)
                                    ? 0 == index ? truthDed : truthNDed
                                    : truthDed, memory);
                } else {
                    structuralStatement(subj, component, truthDed, memory);
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
    private static void structuralStatement(Term subject, Term predicate,
                                            TruthValue truth, Memory memory) {
        Task task = memory.getCurrentTask();
        Term oldContent = task.getContent();
        if (oldContent instanceof Statement) {
            Term content = Statement.make((Statement) oldContent, subject,
                    predicate, memory);
            if (null != content) {
                BudgetValue budget = compoundForward(memory, truth,
                        content);
                Memory.singlePremiseTask(memory, content, truth, budget);
            }
        }
    }

	/* -------------------- set transform -------------------- */

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
     * @param memory
     */
    static void transformProductImage(Inheritance inh, CompoundTerm oldContent, List<Integer> indices, Memory memory) {
        Term subject = inh.getSubject();
        Term predicate = inh.getPredicate();
        if (inh.equals(oldContent)) {
            if (subject instanceof CompoundTerm) transformSubjectPI((CompoundTerm) subject, predicate, memory);
            if (predicate instanceof CompoundTerm) transformPredicatePI(subject, (CompoundTerm) predicate, memory);
        } else {
            Integer index = indices.get(indices.size() - 1);
            Integer side = indices.get(indices.size() - 2);
            CompoundTerm comp = (CompoundTerm) inh.componentAt(side);
            if ((comp instanceof Product)) {
                if (0 == side) {
                    subject = comp.componentAt(index);
                    predicate = ImageExt.make((Product) comp, inh.getPredicate(), index, memory);

                } else {
                    subject = ImageInt.make((Product) comp, inh.getSubject(), index, memory);
                    predicate = comp.componentAt(index);

                }
            } else if ((comp instanceof ImageExt) && 1 == side) {
                if (index == ((ImageExt) comp).getRelationIndex()) {
                    subject = Product.make(comp, inh.getSubject(), index, memory);
                    predicate = comp.componentAt(index);
                } else {
                    subject = comp.componentAt(index);
                    predicate = ImageExt.make((ImageExt) comp, inh.getSubject(), index, memory);
                }
            } else {
                if (comp instanceof ImageInt && 0 == side) if (index != ((ImageInt) comp).getRelationIndex()) {
                    subject = ImageInt.make((ImageInt) comp, inh.getPredicate(), index, memory);
                    predicate = comp.componentAt(index);
                } else {
                    subject = comp.componentAt(index);
                    predicate = Product.make(comp, inh.getPredicate(), index, memory);
                }
                else {
                    return;
                }
            }
            Inheritance newInh = Inheritance.make(subject, predicate, memory);
            Term content = null;
            if (2 != indices.size()) if (!(oldContent instanceof Statement) || 1 != indices.get(0)) {
                List<Term> componentList;
                Term condition = oldContent.componentAt(0);
                if ((oldContent instanceof Implication || (oldContent instanceof Equivalence))) {
                    if (!(condition instanceof Conjunction)) {
                        componentList = oldContent.cloneComponents();
                        componentList.set(indices.get(0), newInh);
                        content = Statement.make((Statement) oldContent, componentList.get(0), componentList.get(1), memory);
                    } else {
                        componentList = ((CompoundTerm) condition).cloneComponents();
                        componentList.set(indices.get(1), newInh);
                        Term newCond = CompoundTerm.make((CompoundTerm) condition, componentList, memory);
                        content = Statement.make((Statement) oldContent, newCond, ((Statement) oldContent).getPredicate(), memory);
                    }
                } else {
                    componentList = oldContent.cloneComponents();
                    componentList.set(indices.get(0), newInh);
                    if (oldContent instanceof Conjunction) {
                        content = CompoundTerm.make(oldContent, componentList, memory);
                    }
                }
            } else {
                content = Statement.make((Statement) oldContent, oldContent.componentAt(0), newInh, memory);
            }
            else {
                content = newInh;
            }
            if (null != content) {
                Sentence sentence = memory.getCurrentTask().getSentence();
                TruthValue truth = sentence.getTruth();
                BudgetValue budget;
                budget = sentence.isQuestion() ? compoundBackward(content, memory) : compoundForward(memory, truth, content);
                Memory.singlePremiseTask(memory, content, truth, budget);
            }
        }
    }

    /**
     * Equivalent transformation between products and images when the subject is
     * a compound {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)> {<S
     * --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P> {<S --> (/, P, _,
     * M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param memory    Reference to the memory
     */
    private static void transformSubjectPI(CompoundTerm subject,
                                           Term predicate, Memory memory) {
        TruthValue truth = memory.getCurrentTask().getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (subject instanceof Product) {
            Product product = (Product) subject;
            for (Integer i = 0; i < product.size(); i++) {
                newSubj = product.componentAt(i);
                newPred = ImageExt.make(product, predicate, i, memory);
                inheritance = Inheritance.make(newSubj, newPred, memory);
                budget = null == truth ? compoundBackward(inheritance,
                        memory) : compoundForward(memory, truth,
                        inheritance);
                Memory.singlePremiseTask(memory, inheritance, truth, budget);
            }
        } else {
            if (!(subject instanceof ImageInt)) {
                return;
            }
            ImageInt image = (ImageInt) subject;
            int relationIndex = image.getRelationIndex();
            for (Integer i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = image.componentAt(relationIndex);
                    newPred = Product.make(image, predicate, relationIndex,
                            memory);
                } else {
                    newSubj = ImageInt.make(image, predicate, i,
                            memory);
                    newPred = image.componentAt(i);
                }
                inheritance = Inheritance.make(newSubj, newPred, memory);
                budget = null == truth ? compoundBackward(inheritance,
                        memory) : compoundForward(memory, truth,
                        inheritance);
                Memory.singlePremiseTask(memory, inheritance, truth, budget);
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
     */
    private static void transformPredicatePI(Term subject,
                                             CompoundTerm predicate, Memory memory) {
        TruthValue truth = memory.getCurrentTask().getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (predicate instanceof Product) {
            Product product = (Product) predicate;
            for (Integer i = 0; i < product.size(); i++) {
                newSubj = ImageInt.make(product, subject, i, memory);
                newPred = product.componentAt(i);
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (null == truth) {
                    budget = compoundBackward(inheritance,
                            memory);
                } else {
                    budget = compoundForward(memory, truth,
                            inheritance);
                }
                Memory.singlePremiseTask(memory, inheritance, truth, budget);
            }
        } else if (predicate instanceof ImageExt) {
            ImageExt image = (ImageExt) predicate;
            int relationIndex = image.getRelationIndex();
            for (Integer i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = Product.make(image, subject, relationIndex,
                            memory);
                    newPred = image.componentAt(relationIndex);
                } else {
                    newSubj = image.componentAt(i);
                    newPred = ImageExt.make(image, subject, i,
                            memory);
                }
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (null != inheritance) { // jmv <<<<<
                    budget = null == truth ? compoundBackward(inheritance,
                            memory) : compoundForward(memory, truth,
                            inheritance);
                    Memory.singlePremiseTask(memory, inheritance, truth, budget);
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
    static void structuralCompound(CompoundTerm compound, Term component,
                                   boolean compoundTask, Memory memory) {
        if (!component.isConstant()) {
            return;
        }
        Term content = compoundTask ? component : compound;
        Task task = memory.getCurrentTask();
        if (task.isStructural()) {
            return;
        }
        Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = compoundBackward(content, memory);
        } else {
            if (sentence.isJudgment() == compoundTask == compound instanceof Conjunction) {
                truth = TruthFunctions.deduction(truth, RELIANCE);
            } else {
                return;
            }
            budget = BudgetFunctions.forward(memory, truth);
        }
        Memory.singlePremiseTask(memory, content, truth, budget);
    }

	/* --------------- Negation related rules --------------- */

}
