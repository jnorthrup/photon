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

import nars.entity.BudgetValue;
import nars.entity.TruthValue;
import nars.language.*;
import nars.main_nogui.Parameters;
import nars.storage.Memory;

import java.util.*;

/**
 * Single-premise inference rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
public final class StructuralRules {

    private static final float RELIANCE = Parameters.RELIANCE;

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
     */
    static void structuralCompose2(CompoundTerm compound, short index, Statement statement, short side, Memory memory) {
        if (compound.equals(statement.componentAt(side))) {
            return;
        }
        var sub = statement.getSubject();
        var pred = statement.getPredicate();
        var components = compound.cloneComponents();
        if (((side == 0) && components.contains(pred)) || ((side == 1) && components.contains(sub))) {
            return;
        }
        if (side == 0) {
            if (components.contains(sub)) {
                if (pred instanceof CompoundTerm) {
                    return;
                }
                sub = compound;
                components.set(index, pred);
                pred = CompoundTerm.make(compound, components, memory);
            }
        } else {
            if (components.contains(pred)) {
                if (sub instanceof CompoundTerm) {
                    return;
                }
                components.set(index, sub);
                sub = CompoundTerm.make(compound, components, memory);
                pred = compound;
            }
        }
        if ((sub == null) || (pred == null)) {
            return;
        }
        Term content;
        if (switchOrder(compound, index)) {
            content = Statement.make(statement, pred, sub, memory);
        } else {
            content = Statement.make(statement, sub, pred, memory);
        }
        if (content == null) {
            return;
        }
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackwardWeak(content, memory);
        } else {
            if (compound.size() > 1) {
                if (sentence.isJudgment()) {
                    truth = TruthFunctions.deduction(truth, RELIANCE);
                } else {
                    return;
                }
            }
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void structuralDecompose2(Statement statement, int index, Memory memory) {
        var subj = statement.getSubject();
        var pred = statement.getPredicate();
        if (subj.getClass() != pred.getClass()) {
            return;
        }
        var sub = (CompoundTerm) subj;
        var pre = (CompoundTerm) pred;
        if (sub.size() != pre.size() || sub.size() <= index) {
            return;
        }
        var t1 = sub.componentAt(index);
        var t2 = pre.componentAt(index);
        Term content;
        if (switchOrder(sub, (short) index)) {
            content = Statement.make(statement, t2, t1, memory);
        } else {
            content = Statement.make(statement, t1, t2, memory);
        }
        if (content == null) {
            return;
        }
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
        } else {
            if (!(sub instanceof Product) && (sub.size() > 1) && (sentence.isJudgment())) {
                return;
            }
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
    }

    /**
     * List the cases where the direction of inheritance is revised in
     * conclusion
     *
     * @param compound The compound term
     * @param index    The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private static boolean switchOrder(CompoundTerm compound, short index) {
        return ((((compound instanceof DifferenceExt) || (compound instanceof DifferenceInt)) && (index == 1))
                || ((compound instanceof ImageExt) && (index != ((ImageExt) compound).getRelationIndex()))
                || ((compound instanceof ImageInt) && (index != ((ImageInt) compound).getRelationIndex())));
    }

    /**
     * {<S --> P>, P@(P&Q)} |- <S --> (P&Q)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void structuralCompose1(CompoundTerm compound, short index, Statement statement, Memory memory) {
        if (!memory.currentTask.getSentence().isJudgment()) {
            return;
        }
        var component = compound.componentAt(index);
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        var truthDed = TruthFunctions.deduction(truth, RELIANCE);
        var truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE));
        var subj = statement.getSubject();
        var pred = statement.getPredicate();
        if (component.equals(subj)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(compound, pred, truthDed, memory);
            } else if (compound instanceof IntersectionInt) {
            } else if ((compound instanceof DifferenceExt) && (index == 0)) {
                structuralStatement(compound, pred, truthDed, memory);
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                } else {
                    structuralStatement(compound, pred, truthNDed, memory);
                }
            }
        } else if (component.equals(pred)) {
            if (compound instanceof IntersectionExt) {
            } else if (compound instanceof IntersectionInt) {
                structuralStatement(subj, compound, truthDed, memory);
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                } else {
                    structuralStatement(subj, compound, truthNDed, memory);
                }
            } else if ((compound instanceof DifferenceInt) && (index == 0)) {
                structuralStatement(subj, compound, truthDed, memory);
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
    static void structuralDecompose1(CompoundTerm compound, short index, Statement statement, Memory memory) {
        if (!memory.currentTask.getSentence().isJudgment()) {
            return;
        }
        var component = compound.componentAt(index);
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        var truthDed = TruthFunctions.deduction(truth, RELIANCE);
        var truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE));
        var subj = statement.getSubject();
        var pred = statement.getPredicate();
        if (compound.equals(subj)) {
            if (compound instanceof IntersectionExt) {
            } else if (compound instanceof IntersectionInt) {
                structuralStatement(component, pred, truthDed, memory);
            } else if ((compound instanceof DifferenceExt) && (index == 0)) {
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                    structuralStatement(component, pred, truthDed, memory);
                } else {
                    structuralStatement(component, pred, truthNDed, memory);
                }
            }
        } else if (compound.equals(pred)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(subj, component, truthDed, memory);
            } else if (compound instanceof IntersectionInt) {
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                    structuralStatement(subj, component, truthDed, memory);
                } else {
                    structuralStatement(subj, component, truthNDed, memory);
                }
//            } else if ((compound instanceof DifferenceInt) && (index == 0)) {
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
    private static void structuralStatement(Term subject, Term predicate, TruthValue truth, Memory memory) {
        var task = memory.currentTask;
        var oldContent = task.getContent();
        if (oldContent instanceof Statement) {
            Term content = Statement.make((Statement) oldContent, subject, predicate, memory);
            if (content != null) {
                var budget = BudgetFunctions.compoundForward(truth, content, memory);
                memory.singlePremiseTask(content, truth, budget);
            }
        }
    }

    /* -------------------- set transform -------------------- */

    /**
     * {<S --> {P}>} |- <S <-> {P}>
     *
     * @param compound  The set compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param memory    Reference to the memory
     */
    static void transformSetRelation(CompoundTerm compound, Statement statement, short side, Memory memory) {
        if (compound.size() > 1) {
            return;
        }
        if (statement instanceof Inheritance) {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                return;
            }
        }
        var sub = statement.getSubject();
        var pre = statement.getPredicate();
        Term content;
        if (statement instanceof Inheritance) {
            content = Similarity.make(sub, pre, memory);
        } else {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                content = Inheritance.make(pre, sub, memory);
            } else {
                content = Inheritance.make(sub, pre, memory);
            }
        }
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
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
     */
    static void transformProductImage(Inheritance inh, CompoundTerm oldContent, short[] indices, Memory memory) {
        var subject = inh.getSubject();
        var predicate = inh.getPredicate();
        if (inh.equals(oldContent)) {
            if (subject instanceof CompoundTerm) {
                transformSubjectPI((CompoundTerm) subject, predicate, memory);
            }
            if (predicate instanceof CompoundTerm) {
                transformPredicatePI(subject, (CompoundTerm) predicate, memory);
            }
            return;
        }
        var index = indices[indices.length - 1];
        var side = indices[indices.length - 2];
        var comp = (CompoundTerm) inh.componentAt(side);
        if (comp instanceof Product) {
            if (side == 0) {
                subject = comp.componentAt(index);
                predicate = ImageExt.make((Product) comp, inh.getPredicate(), index, memory);
            } else {
                subject = ImageInt.make((Product) comp, inh.getSubject(), index, memory);
                predicate = comp.componentAt(index);
            }
        } else if ((comp instanceof ImageExt) && (side == 1)) {
            if (index == ((ImageExt) comp).getRelationIndex()) {
                subject = Product.make(comp, inh.getSubject(), index, memory);
                predicate = comp.componentAt(index);
            } else {
                subject = comp.componentAt(index);
                predicate = ImageExt.make((ImageExt) comp, inh.getSubject(), index, memory);
            }
        } else if ((comp instanceof ImageInt) && (side == 0)) {
            if (index == ((ImageInt) comp).getRelationIndex()) {
                subject = comp.componentAt(index);
                predicate = Product.make(comp, inh.getPredicate(), index, memory);
            } else {
                subject = ImageInt.make((ImageInt) comp, inh.getPredicate(), index, memory);
                predicate = comp.componentAt(index);
            }
        } else {
            return;
        }
        var newInh = Inheritance.make(subject, predicate, memory);
        Term content = null;
        if (indices.length == 2) {
            content = newInh;
        } else if ((oldContent instanceof Statement) && (indices[0] == 1)) {
            content = Statement.make((Statement) oldContent, oldContent.componentAt(0), newInh, memory);
        } else {
            List<Term> componentList;
            var condition = oldContent.componentAt(0);
            if (((oldContent instanceof Implication) || (oldContent instanceof Equivalence)) && (condition instanceof Conjunction)) {
                componentList = ((CompoundTerm) condition).cloneComponents();
                componentList.set(indices[1], newInh);
                var newCond = CompoundTerm.make((CompoundTerm) condition, componentList, memory);
                content = Statement.make((Statement) oldContent, newCond, ((Statement) oldContent).getPredicate(), memory);
            } else {
                componentList = oldContent.cloneComponents();
                componentList.set(indices[0], newInh);
                if (oldContent instanceof Conjunction) {
                    content = CompoundTerm.make(oldContent, componentList, memory);
                } else if ((oldContent instanceof Implication) || (oldContent instanceof Equivalence)) {
                    content = Statement.make((Statement) oldContent, componentList.get(0), componentList.get(1), memory);
                }
            }
        }
        if (content == null) {
            return;
        }
        var sentence = memory.currentTask.getSentence();
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
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
    private static void transformSubjectPI(CompoundTerm subject, Term predicate, Memory memory) {
        var truth = memory.currentTask.getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (subject instanceof Product) {
            var product = (Product) subject;
            for (short i = 0; i < product.size(); i++) {
                newSubj = product.componentAt(i);
                newPred = ImageExt.make(product, predicate, i, memory);
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (truth == null) {
                    budget = BudgetFunctions.compoundBackward(inheritance, memory);
                } else {
                    budget = BudgetFunctions.compoundForward(truth, inheritance, memory);
                }
                memory.singlePremiseTask(inheritance, truth, budget);
            }
        } else if (subject instanceof ImageInt) {
            var image = (ImageInt) subject;
            int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = image.componentAt(relationIndex);
                    newPred = Product.make(image, predicate, relationIndex, memory);
                } else {
                    newSubj = ImageInt.make((ImageInt) image, predicate, i, memory);
                    newPred = image.componentAt(i);
                }
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (truth == null) {
                    budget = BudgetFunctions.compoundBackward(inheritance, memory);
                } else {
                    budget = BudgetFunctions.compoundForward(truth, inheritance, memory);
                }
                memory.singlePremiseTask(inheritance, truth, budget);
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
    private static void transformPredicatePI(Term subject, CompoundTerm predicate, Memory memory) {
        var truth = memory.currentTask.getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (predicate instanceof Product) {
            var product = (Product) predicate;
            for (short i = 0; i < product.size(); i++) {
                newSubj = ImageInt.make(product, subject, i, memory);
                newPred = product.componentAt(i);
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (truth == null) {
                    budget = BudgetFunctions.compoundBackward(inheritance, memory);
                } else {
                    budget = BudgetFunctions.compoundForward(truth, inheritance, memory);
                }
                memory.singlePremiseTask(inheritance, truth, budget);
            }
        } else if (predicate instanceof ImageExt) {
            var image = (ImageExt) predicate;
            int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = Product.make(image, subject, relationIndex, memory);
                    newPred = image.componentAt(relationIndex);
                } else {
                    newSubj = image.componentAt(i);
                    newPred = ImageExt.make((ImageExt) image, subject, i, memory);
                }
                inheritance = Inheritance.make(newSubj, newPred, memory);
                if (inheritance != null) { // jmv <<<<<
                    if (truth == null) {
                        budget = BudgetFunctions.compoundBackward(inheritance, memory);
                    } else {
                        budget = BudgetFunctions.compoundForward(truth, inheritance, memory);
                    }
                    memory.singlePremiseTask(inheritance, truth, budget);
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
    static void structuralCompound(CompoundTerm compound, Term component, boolean compoundTask, Memory memory) {
        if (!component.isConstant()) {
            return;
        }
        var content = (compoundTask ? component : compound);
        var task = memory.currentTask;
//        if (task.isStructural()) {
//            return;
//        }
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
        } else {
            if ((sentence.isJudgment()) == (compoundTask == (compound instanceof Conjunction))) {
                truth = TruthFunctions.deduction(truth, RELIANCE);
            } else {
                return;
            }
            budget = BudgetFunctions.forward(truth, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
    }

    /* --------------- Negation related rules --------------- */

    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content The premise
     * @param memory  Reference to the memory
     */
    public static void transformNegation(Term content, Memory memory) {
        var task = memory.currentTask;
        var sentence = task.getSentence();
        var truth = sentence.getTruth();
        if (sentence.isJudgment()) {
            truth = TruthFunctions.negation(truth);
        }
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<A ==> B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void contraposition(Statement statement, Memory memory) {
        var subj = statement.getSubject();
        var pred = statement.getPredicate();
        var task = memory.currentTask;
        var sentence = task.getSentence();
        Term content = Statement.make(statement, Negation.make(pred, memory), Negation.make(subj, memory), memory);
        var truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            if (content instanceof Implication) {
                budget = BudgetFunctions.compoundBackwardWeak(content, memory);
            } else {
                budget = BudgetFunctions.compoundBackward(content, memory);
            }
        } else {
            if (content instanceof Implication) {
                truth = TruthFunctions.contraposition(truth);
            }
            budget = BudgetFunctions.compoundForward(truth, content, memory);
        }
        memory.singlePremiseTask(content, truth, budget);
    }
}
