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
package nars.inference;

import nars.data.SentenceStruct;
import nars.data.TermLinkStruct;
import nars.data.TermStruct;
import nars.data.TruthHandle;
import nars.entity.*;
import nars.io.Symbols;
import nars.language.*;
import nars.storage.Memory;

import java.util.HashMap;
import java.util.List;

import static nars.inference.BudgetFunctions.compoundBackward;
import static nars.inference.BudgetFunctions.compoundForward;

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirective processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
public class RuleTables {

    /**
     * Entry point of the inference engine
     *
     * @param tLink  The selected TaskLink, which will provide a task
     * @param bLink  The selected TermLink, which may provide a belief
     * @param memory Reference to the memory
     */
    public static void reason(TaskLink tLink, TermLink bLink, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence taskSentence = task.getSentence();
        Term taskTerm = (Term) taskSentence.getContent().clone(); // cloning
        // for
        // substitution
        Term beliefTerm = (Term) bLink.getTerm().clone(); // cloning for
        // substitution
        Concept beliefConcept = Memory.termToConcept(memory, beliefTerm);
        Sentence belief = null;
        if (null != beliefConcept) {
            belief = beliefConcept.getBelief(task);
        }
        memory.setCurrentBelief(belief); // may be null
        if (null != belief) {
            LocalRules.match(task, belief, memory);
        }
        if (!Memory.noResult(memory)) {
            return;
        }
        Integer tIndex = tLink.getIndex(0);
        Integer bIndex = bLink.getIndex(0);
        switch (tLink.getType()) { // dispatch first by TaskLink type
            case TermLink.SELF:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        compoundAndSelf((CompoundTerm) taskTerm, beliefTerm,
                                true, memory);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndSelf((CompoundTerm) beliefTerm, taskTerm,
                                false, memory);
                        break;
                    case TermLink.COMPONENT_STATEMENT:
                        if (null != belief) {
                            detachment(task.getSentence(),
                                    belief, bIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (null != belief) {
                            detachment(belief,
                                    task.getSentence(), bIndex, memory);
                        }
                        break;
                    case TermLink.COMPONENT_CONDITION:
                        if (null != belief) {
                            bIndex = bLink.getIndex(1);
                            conditionalDedInd(
                                    (Implication) taskTerm, bIndex, beliefTerm,
                                    tIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (null != belief) {
                            bIndex = bLink.getIndex(1);
                            conditionalDedInd(
                                    (Implication) beliefTerm, bIndex, taskTerm,
                                    tIndex, memory);
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        compoundAndCompound((CompoundTerm) taskTerm,
                                (CompoundTerm) beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        compoundAndStatement((CompoundTerm) taskTerm, tIndex,
                                (Statement) beliefTerm, bIndex, beliefTerm,
                                memory);
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (null != belief) {
                            if (beliefTerm instanceof Implication) {
                                conditionalDedInd(
                                        (Implication) beliefTerm, bIndex,
                                        taskTerm, -1, memory);
                            } else if (beliefTerm instanceof Equivalence) {
                                conditionalAna(
                                        (Equivalence) beliefTerm, bIndex,
                                        taskTerm, -1, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_STATEMENT:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        componentAndStatement(
                                (CompoundTerm) memory.getCurrentTerm(), bIndex,
                                (Statement) taskTerm, tIndex, memory);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndStatement((CompoundTerm) beliefTerm, bIndex,
                                (Statement) taskTerm, tIndex, beliefTerm,
                                memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (null != belief) {
                            // bIndex = bLink.getIndex(1);
                            syllogisms(tLink, bLink, taskTerm, beliefTerm,
                                    memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (null != belief) {
                            bIndex = bLink.getIndex(1);
                            if (beliefTerm instanceof Implication) {
                                conditionalDedIndWithVar(
                                        (Implication) beliefTerm, bIndex,
                                        (Statement) taskTerm, tIndex, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_CONDITION:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND_STATEMENT:
                        if (null != belief) {
                            if (taskTerm instanceof Implication)
                                // TODO maybe put instanceof test within
                                // conditionalDedIndWithVar()
                                conditionalDedIndWithVar(
                                        (Implication) taskTerm, tIndex,
                                        (Statement) beliefTerm, bIndex, memory);
                        }
                        break;
                }
                break;
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
    private static void syllogisms(TaskLink tLink, TermLink bLink,
                                   Term taskTerm, Term beliefTerm, Memory memory) {
        Sentence taskSentence = memory.getCurrentTask().getSentence();
        Sentence belief = memory.getCurrentBelief();
        int figure;
        if (taskTerm instanceof Inheritance) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else {
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0),
                        memory);
            }
        } else if (taskTerm instanceof Similarity) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            }
        } else if (taskTerm instanceof Implication) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0),
                        memory);
            }
        } else if (taskTerm instanceof Equivalence) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0),
                        memory);
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
    private static int indexToFigure(TermLink link1, TermLink link2) {
        return (link1.getIndex(0) + 1) * 10 + link2.getIndex(0) + 1;
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param sentence The taskSentence in the task
     * @param belief   The judgment in the belief
     * @param figure   The location of the shared term
     * @param memory   Reference to the memory
     */
    private static void asymmetricAsymmetric(Sentence sentence,
                                             Sentence belief, int figure, Memory memory) {
        Statement s1 = (Statement) sentence.cloneContent();
        Statement s2 = (Statement) belief.cloneContent();
        Term t1, t2;
        switch (figure) {
            case 11: // induction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(),
                        s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getPredicate();
                    t2 = s1.getPredicate();
                    abdIndCom(t1, t2, sentence, belief,
                            figure, memory);
                    CompositionalRules.composeCompound(s1, s2, 0, memory);
                }

                break;
            case 12: // deduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(),
                        s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getSubject();
                    t2 = s1.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(memory);
                    } else {
                        dedExe(t1, t2, sentence, belief,
                                memory);
                    }
                }
                break;
            case 21: // exemplification
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(),
                        s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(memory);
                    } else {
                        dedExe(t1, t2, sentence, belief,
                                memory);
                    }
                }
                break;
            case 22: // abduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(),
                        s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getSubject();
                    if (!
                            conditionalAbd(t1, t2, s1, s2, memory)) { // if
                        // conditional
                        // abduction,
                        // skip
                        // the
                        // following
                        abdIndCom(t1, t2, sentence, belief,
                                figure, memory);
                        CompositionalRules.composeCompound(s1, s2, 1, memory);
                    }
                }
                break;
            default:
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
    private static void asymmetricSymmetric(Sentence asym, Sentence sym,
                                            int figure, Memory memory) {
        Statement asymSt = (Statement) asym.cloneContent();
        Statement symSt = (Statement) sym.cloneContent();
        Term t1, t2;
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT,
                        asymSt.getSubject(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getPredicate();
                    if (Variable
                            .unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure,
                                memory);
                    }
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT,
                        asymSt.getSubject(), symSt.getPredicate(), asymSt,
                        symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getSubject();
                    if (Variable
                            .unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure,
                                memory);
                    }
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT,
                        asymSt.getPredicate(), symSt.getSubject(), asymSt,
                        symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getPredicate();
                    if (Variable
                            .unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure,
                                memory);
                    }
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT,
                        asymSt.getPredicate(), symSt.getPredicate(), asymSt,
                        symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getSubject();
                    if (Variable
                            .unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure,
                                memory);
                    }
                }
                break;
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
    private static void symmetricSymmetric(Sentence belief,
                                           Sentence taskSentence, int figure, Memory memory) {
        Statement s1 = (Statement) belief.cloneContent();
        Statement s2 = (Statement) taskSentence.cloneContent();
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(),
                        s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(),
                            s2.getPredicate(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(),
                        s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(),
                            s2.getSubject(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(),
                        s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(),
                            s2.getPredicate(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(),
                        s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(),
                            s2.getSubject(), belief, taskSentence, figure,
                            memory);
                }
                break;
        }
    }

	/* ----- conditional inferences ----- */

    /**
     * The detachment rule, with variable unification
     *
     * @param originalMainSentence The premise that is an Implication or Equivalence
     * @param subSentence          The premise that is the subject or predicate of the first one
     * @param index                The location of the second premise in the first
     * @param memory               Reference to the memory
     */
    private static void detachmentWithVar(Sentence originalMainSentence,
                                          SentenceStruct subSentence, int index, Memory memory) {
        Sentence mainSentence = (Sentence) originalMainSentence.clone(); // for
        // substitution
        Statement statement = (Statement) mainSentence.getContent();
        Term component = statement.componentAt(index);
        TermStruct content = subSentence.getContent();
        if (component instanceof Inheritance
                && null != memory.getCurrentBelief()) {
            if (component.isConstant()) {
                detachment(mainSentence, subSentence, index,
                        memory);
            } else if (Variable.unify(Symbols.VAR_INDEPENDENT, component,
                    content, statement, content)) {
                detachment(mainSentence, subSentence, index,
                        memory);
            } else if (statement instanceof Implication
                    && statement.getPredicate() instanceof Statement
                    && memory.getCurrentTask().getSentence().isJudgment()) {
                Statement s2 = (Statement) statement.getPredicate();
                if (s2.getSubject().equals(((Statement) content).getSubject())) {
                    CompositionalRules.introVarInner((Statement) content, s2,
                            statement, memory);
                }
            }
        }
    }

    /**
     * Conditional deduction or induction, with variable unification
     *
     * @param conditional The premise that is an Implication with a Conjunction as
     *                    condition
     * @param index       The location of the shared term in the condition
     * @param statement   The second premise that is a statement
     * @param side        The location of the shared term in the statement
     * @param memory      Reference to the memory
     */
    private static void conditionalDedIndWithVar(Implication conditional,
                                                 Integer index, Statement statement, Integer side, Memory memory) {
        CompoundTerm condition = (CompoundTerm) conditional.getSubject();
        Term component = condition.componentAt(index);
        Term component2 = null;
        if (statement instanceof Inheritance) {
            component2 = statement;
            side = -1;
        } else if (statement instanceof Implication) {
            component2 = statement.componentAt(side);
        }
        if (null != component2
                && Variable.unify(Symbols.VAR_INDEPENDENT, component,
                component2, conditional, statement)) {
            conditionalDedInd(conditional, index, statement,
                    side, memory);
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
    private static void compoundAndSelf(CompoundTerm compound, Term component,
                                        boolean compoundTask, Memory memory) {
        if (compound instanceof Conjunction
                || compound instanceof Disjunction) {
            if (null != memory.getCurrentBelief()) {
                decomposeStatement(compound, component,
                        compoundTask, memory);
            } else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(compound, component,
                        compoundTask, memory);
            }
        } else if (compound instanceof Negation
                && !memory.getCurrentTask().isStructural()) {
            if (compoundTask) {
                transformNegation(
                        compound.componentAt(0), memory);
            } else {
                transformNegation(compound, memory);
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
    private static void compoundAndCompound(CompoundTerm taskTerm,
                                            CompoundTerm beliefTerm, Memory memory) {
        if (taskTerm.getClass() == beliefTerm.getClass()) {
            if (taskTerm.size() > beliefTerm.size()) {
                compoundAndSelf(taskTerm, beliefTerm, true, memory);
            } else if (taskTerm.size() < beliefTerm.size()) {
                compoundAndSelf(beliefTerm, taskTerm, false, memory);
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
    private static void compoundAndStatement(CompoundTerm compound,
                                             Integer index, Statement statement, Integer side, Term beliefTerm,
                                             Memory memory) {
        Term component = compound.componentAt(index);
        Task task = memory.getCurrentTask();
        if (component.getClass() == statement.getClass()) {
            if (compound instanceof Conjunction
                    && null != memory.getCurrentBelief()) {
                if (Variable.unify(Symbols.VAR_DEPENDENT, component, statement,
                        compound, statement)) {
                    elimiVarDep(compound, component,
                            statement.equals(beliefTerm), memory);
                } else if (task.getSentence().isJudgment()) { // &&
                    // !compound.containComponent(component))
                    // {
                    CompositionalRules.introVarInner(statement,
                            (Statement) component, compound, memory);
                }
            }
        } else {
            if (!task.isStructural() && task.getSentence().isJudgment()) {
                if (statement instanceof Inheritance) {
                    StructuralRules.structuralCompose1(compound, index,
                            statement, memory);
                    // if (!(compound instanceof SetExt) && !(compound
                    // instanceof SetInt)) {
                    if (!(compound instanceof SetExt
                            || compound instanceof SetInt || compound instanceof Negation)) {
                        StructuralRules.structuralCompose2(compound, index,
                                statement, side, memory);
                    } // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                } else if (statement instanceof Similarity
                        && !(compound instanceof Conjunction)) {
                    StructuralRules.structuralCompose2(compound, index,
                            statement, side, memory);
                } // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
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
    private static void componentAndStatement(CompoundTerm compound,
                                              Integer index, Statement statement, Integer side, Memory memory) {
        if (!memory.getCurrentTask().isStructural()) {
            if (statement instanceof Inheritance) {
                StructuralRules.structuralDecompose1(compound, index,
                        statement, memory);
                if (!(compound instanceof SetExt)
                        && !(compound instanceof SetInt)) {
                    StructuralRules.structuralDecompose2(statement, memory); // {(C-B)
                    // -->
                    // (C-A),
                    // A
                    // @
                    // (C-A)}
                    // |-
                    // A
                    // -->
                    // B
                } else {
                    transformSetRelation(compound, statement,
                            side, memory);
                }
            } else if (statement instanceof Similarity) {
                StructuralRules.structuralDecompose2(statement, memory); // {(C-B)
                // -->
                // (C-A),
                // A
                // @
                // (C-A)}
                // |-
                // A
                // -->
                // B
                if (compound instanceof SetExt
                        || compound instanceof SetInt) {
                    transformSetRelation(compound, statement,
                            side, memory);
                }
            } else if (statement instanceof Implication
                    && compound instanceof Negation) {
                contraposition(statement, memory);
            }
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
    public static void transformTask(TermLinkStruct tLink, Memory memory) {
        CompoundTerm content = (CompoundTerm) memory.getCurrentTask()
                .getContent().clone();
        List<Integer> indices = tLink.getIndex();
        Term inh = null;
        if (2 == indices.size() || content instanceof Inheritance) { // <(*,
            // term,
            // #)
            // -->
            // #>
            inh = content;
        } else if (3 == indices.size()) { // <<(*, term, #) --> #> ==> #>
            inh = content.componentAt(indices.get(0));
        } else if (4 == indices.size()) { // <(&&, <(*, term, #) --> #>, #) ==>
            // #>
            Term component = content.componentAt(indices.get(0));
            if (component instanceof Conjunction
                    && (content instanceof Implication && 0 == indices.get(0) || content instanceof Equivalence)) {
                inh = ((CompoundTerm) component).componentAt(indices.get(1));
            } else {
                return;
            }
        }
        if (inh instanceof Inheritance) {
            StructuralRules.transformProductImage((Inheritance) inh, content,
                    indices, memory);
        }
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>>, <M --> P>} |- <M --> S>
     *
     * @param compound     The compound term to be decomposed
     * @param component    The part of the compound to be removed
     * @param compoundTask Whether the compound comes from the task
     * @param memory       Reference to the memory
     */
    static void elimiVarDep(CompoundTerm compound, Term component,
                            boolean compoundTask, Memory memory) {
        Term content = CompoundTerm.reduceComponents(compound, component,
                memory);
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        Sentence belief = memory.getCurrentBelief();
        TruthValue v1 = sentence.getTruth();
        TruthValue v2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = compoundTask
                    ? BudgetFunctions.backward(memory, v2)
                    : BudgetFunctions.backwardWeak(memory, v2);
        } else {
            truth = compoundTask
                    ? anonymousAnalogy(v1, v2)
                    : anonymousAnalogy(v2, v1);
            budget = compoundForward(memory, truth, content);
        }
        Memory.doublePremiseTask(memory, content, truth, budget);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     *
     * @param cond1  The condition of the first premise
     * @param cond2  The condition of the second premise
     * @param st2    The second premise
     * @param memory Reference to the memory
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1,
                                  Statement st2, Memory memory) {
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) {
            return false;
        }
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) {
            return false;
        }
        Term term1 = null;
        Term term2 = null;
        if (cond1 instanceof Conjunction) {
            term1 = CompoundTerm.reduceComponents((Conjunction) cond1, cond2,
                    memory);
        }
        if (cond2 instanceof Conjunction) {
            term2 = CompoundTerm.reduceComponents((Conjunction) cond2, cond1,
                    memory);
        }
        if (null == term1 && null == term2) {
            return false;
        }
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        Sentence belief = memory.getCurrentBelief();
        TruthValue value1 = sentence.getTruth();
        TruthValue value2 = belief.getTruth();
        Term content;
        TruthValue truth = null;
        BudgetValue budget;
        if (null != term1) {
            if (null != term2) {
                content = Statement.make(st2, term2, term1, memory);
            } else {
                content = term1;
            }
            if (sentence.isQuestion()) {
                budget = BudgetFunctions.backwardWeak(memory, value2);
            } else {
                truth = TruthFunctions.abduction(value2, value1);
                budget = BudgetFunctions.forward(memory, truth);
            }
            Memory.doublePremiseTask(memory, content, truth, budget);
        }
        if (null != term2) {
            if (null != term1) {
                content = Statement.make(st1, term1, term2, memory);
            } else {
                content = term2;
            }
            if (sentence.isQuestion()) {
                budget = BudgetFunctions.backwardWeak(memory, value2);
            } else {
                truth = TruthFunctions.abduction(value1, value2);
                budget = BudgetFunctions.forward(memory, truth);
            }
            Memory.doublePremiseTask(memory, content, truth, budget);
        }
        return true;
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     *
     * @param premise1 The equivalence premise
     * @param index    The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the condition
     *                 of premise1
     * @param side     The location of the shared term in premise2: 0 for subject, 1
     *                 for predicate, -1 for the whole term
     * @param memory   Reference to the memory
     */
    static void conditionalAna(Equivalence premise1, Integer index,
                               Term premise2, int side, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence taskSentence = task.getSentence();
        Sentence belief = memory.getCurrentBelief();
        boolean conditionalTask = hasSubstitute(
                Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
        Term commonComponent;
        Term newComponent = null;
        if (0 == side) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (1 == side) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
        }
        Conjunction oldCondition = (Conjunction) premise1.getSubject();
        boolean match = Variable.unify(Symbols.VAR_DEPENDENT,
                oldCondition.componentAt(index), commonComponent, premise1,
                premise2);
        if (!match && commonComponent.getClass() == oldCondition.getClass()) {
            match = Variable.unify(Symbols.VAR_DEPENDENT,
                    oldCondition.componentAt(index),
                    ((CompoundTerm) commonComponent).componentAt(index),
                    premise1, premise2);
        }
        if (!match) {
            return;
        }
        Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index,
                    newComponent, memory);
        }
        Term content;
        if (null != newCondition) {
            content = Statement.make(premise1, newCondition,
                    premise1.getPredicate(), memory);
        } else {
            content = premise1.getPredicate();
        }
        if (null == content) {
            return;
        }
        TruthValue truth1 = taskSentence.getTruth();
        TruthValue truth2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence.isQuestion()) {
            budget = BudgetFunctions.backwardWeak(memory, truth2);
        } else {
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, truth2);
            } else {
                truth = TruthFunctions.analogy(truth1, truth2);
            }
            budget = BudgetFunctions.forward(memory, truth);
        }
        Memory.doublePremiseTask(memory, content, truth, budget);
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P> {<(&&, S2, S3) ==>
     * P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P> {<(&&, S1, S3) ==> P>, <S1 ==>
     * S2>} |- <(&&, S2, S3) ==> P>
     *
     * @param premise1 The conditional premise
     * @param index    The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the condition
     *                 of premise1
     * @param side     The location of the shared term in premise2: 0 for subject, 1
     *                 for predicate, -1 for the whole term
     * @param memory   Reference to the memory
     */
    static void conditionalDedInd(Implication premise1, Integer index,
                                  Term premise2, int side, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence taskSentence = task.getSentence();
        Sentence belief = memory.getCurrentBelief();
        boolean deduction = 0 != side;
        boolean conditionalTask = hasSubstitute(
                Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
        Term commonComponent;
        Term newComponent = null;
        if (0 == side) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (1 == side) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
        }
        Conjunction oldCondition = (Conjunction) premise1.getSubject();
        int index2 = oldCondition.getComponents().indexOf(commonComponent);
        if (0 <= index2) {
            index = index2
            ;
        } else {
            boolean match = Variable.unify(Symbols.VAR_INDEPENDENT,
                    oldCondition.componentAt(index), commonComponent, premise1,
                    premise2);
            if (!match
                    && commonComponent.getClass() == oldCondition.getClass()) {
                match = Variable.unify(Symbols.VAR_INDEPENDENT,
                        oldCondition.componentAt(index),
                        ((CompoundTerm) commonComponent).componentAt(index),
                        premise1, premise2);
            }
            if (!match) {
                return;
            }
        }
        Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index,
                    newComponent, memory);
        }
        Term content;
        if (null != newCondition) {
            content = Statement.make(premise1, newCondition,
                    premise1.getPredicate(), memory);
        } else {
            content = premise1.getPredicate();
        }
        if (null == content) {
            return;
        }
        TruthValue truth1 = taskSentence.getTruth();
        TruthValue truth2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence.isQuestion()) {
            budget = BudgetFunctions.backwardWeak(memory, truth2);
        } else {
            if (deduction) {
                truth = TruthFunctions.deduction(truth1, truth2);
            } else if (conditionalTask) {
                truth = TruthFunctions.induction(truth2, truth1);
            } else {
                truth = TruthFunctions.induction(truth1, truth2);
            }
            budget = BudgetFunctions.forward(memory, truth);
        }
        Memory.doublePremiseTask(memory, content, truth, budget);
    }

    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P> {<<M --> S> ==> <M
     * --> P>>, <M --> P>} |- <M --> S> {<<M --> S> <=> <M --> P>>, <M --> S>}
     * |- <M --> P> {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence  The premise on part of s1
     * @param side         The location of s2 in s1
     * @param memory
     */
    static void detachment(Sentence mainSentence, SentenceStruct subSentence,
                           int side, Memory memory) {
        e:
        {
            Statement statement = (Statement) mainSentence.getContent();
            if (!(statement instanceof Implication)
                    && !(statement instanceof Equivalence)) return;
            Term subject = statement.getSubject();
            Term predicate = statement.getPredicate();
            Term content;
            TermStruct term = subSentence.getContent();
            if (0 != side || !term.equals(subject)) {
                if (1 == side && term.equals(predicate)) {
                    content = subject;
                } else {
                    break e;
                }
            } else {
                content = predicate;
            }
            if (content instanceof Statement && ((Statement) content).invalid()) {
                break e;
            }
            functionBeggingToBeAsync(mainSentence, subSentence, side, memory, statement, content);
        }
    }

    private static void functionBeggingToBeAsync(SentenceStruct mainSentence, SentenceStruct subSentence, int side, Memory memory, Statement statement, Term content) {
        Task currentTask = memory.getCurrentTask();
        Sentence taskSentence = currentTask.getSentence();
        Sentence beliefSentence = memory.getCurrentBelief();
        TruthValue beliefTruth = beliefSentence.getTruth();
        TruthHandle truth1 = mainSentence.getTruth();
        TruthHandle truth2 = subSentence.getTruth();
        TruthValue truth = null;
        BudgetValue budget = taskSentence.isQuestion() ? statement instanceof Equivalence ? BudgetFunctions.backward(memory, beliefTruth) : 0 == side ? BudgetFunctions.backwardWeak(memory, beliefTruth) : BudgetFunctions.backward(memory, beliefTruth) : BudgetFunctions.forward(memory, truth = !(statement instanceof Equivalence) ? 0 == side ? TruthFunctions.deduction(truth1, truth2) : TruthFunctions.abduction(truth2, truth1) : TruthFunctions.analogy(truth2, truth1));
        Memory.doublePremiseTask(memory, content, truth, budget);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>,
     * <P ==>
     * S>, <S <=> P>}
     *
     * @param term1        Subject of the first new task
     * @param term2        Predicate of the first new task
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param figure       Locations of the shared term in premises
     * @param memory
     */
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence,
                          SentenceStruct belief, int figure, Memory memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        Statement taskContent = (Statement) taskSentence.getContent();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        TruthValue truth3 = null;
        BudgetValue budget1, budget2, budget3;
        TruthValue value1 = taskSentence.getTruth();
        TruthHandle value2 = belief.getTruth();
        if (taskSentence.isQuestion()) {
            budget1 = BudgetFunctions.backward(memory, value2);
            budget2 = BudgetFunctions.backwardWeak(memory, value2);
            budget3 = BudgetFunctions.backward(memory, value2);
        } else {
            truth1 = TruthFunctions.abduction(value1, value2);
            truth2 = TruthFunctions.abduction(value2, value1);
            truth3 = TruthFunctions.comparison(value1, value2);
            budget1 = BudgetFunctions.forward(memory, truth1);
            budget2 = BudgetFunctions.forward(memory, truth2);
            budget3 = BudgetFunctions.forward(memory, truth3);
        }
        Statement statement1 = Statement
                .make(taskContent, term1, term2, memory);
        Statement statement2 = Statement
                .make(taskContent, term2, term1, memory);
        Statement statement3 = makeSym(taskContent, term1, term2,
                memory);
        Memory.doublePremiseTask(memory, statement1, truth1, budget1);
        Memory.doublePremiseTask(memory, statement2, truth2, budget2);
        Memory.doublePremiseTask(memory, statement3, truth3, budget3);
    }

    /**
     * <pre>
     * {<S ==> M>, <M ==> P>} |- {<S ==> P>, <P ==> S>}
     * </pre>
     *
     * @param term1    Subject of the first new task
     * @param term2    Predicate of the first new task
     * @param sentence The first premise
     * @param belief   The second premise
     * @param memory
     */
    static void dedExe(Term term1, Term term2, Sentence sentence,
                       SentenceStruct belief, Memory memory) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        TruthValue value1 = sentence.getTruth();
        TruthHandle value2 = belief.getTruth();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        BudgetValue budget1, budget2;
        if (sentence.isQuestion()) {
            budget1 = BudgetFunctions.backwardWeak(memory, value2);
            budget2 = BudgetFunctions.backwardWeak(memory, value2);
        } else {
            truth1 = TruthFunctions.deduction(value1, value2);
            truth2 = exemplification(value1, value2);
            budget1 = BudgetFunctions.forward(memory, truth1);
            budget2 = BudgetFunctions.forward(memory, truth2);
        }
        Statement content = (Statement) sentence.getContent();
        Statement content1 = Statement.make(content, term1, term2, memory);
        Statement content2 = Statement.make(content, term2, term1, memory);
        Memory.doublePremiseTask(memory, content1, truth1, budget1);
        Memory.doublePremiseTask(memory, content2, truth2, budget2);
    }

    /**
     * {<A ==> B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     *
     * @param statement The premise
     * @param memory    Reference to the memory
     */
    static void contraposition(Statement statement, Memory memory) {
        Term subj = statement.getSubject();
        Term pred = statement.getPredicate();
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        Term content = Statement.make(statement, Negation.make(pred, memory),
                Negation.make(subj, memory), memory);
        TruthValue truth = sentence.getTruth();
        BudgetValue budget;
        if (sentence.isQuestion()) {
            if (content instanceof Implication) {
                budget = BudgetFunctions.compoundBackwardWeak(content, memory);
            } else {
                budget = compoundBackward(content, memory);
            }
        } else {
            if (content instanceof Implication) {
                truth = contraposition(truth);
            }
            budget = compoundForward(memory, truth, content);
        }
        Memory.singlePremiseTask(memory, content, truth, budget);
    }

    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content The premise
     * @param memory  Reference to the memory
     */
    public static void transformNegation(Term content, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        if (sentence.isJudgment()) {
            truth = TruthFunctions.negation(truth);
        }
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = compoundBackward(content, memory);
        } else {
            budget = compoundForward(memory, truth, content);
        }
        Memory.singlePremiseTask(memory, content, truth, budget);
    }

    /**
     * {<S --> {P}>} |- <S <-> {P}>
     *
     * @param compound  The set compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param memory    Reference to the memory
     */
    static void transformSetRelation(CompoundTerm compound,
                                     Statement statement, Integer side, Memory memory) {
        if (1 >= compound.size() && (!(statement instanceof Inheritance) || (!(compound instanceof SetExt) || 0 != side)
                && (!(compound instanceof SetInt) || 1 != side))) {
            Term sub = statement.getSubject();
            Term pre = statement.getPredicate();
            Term content;
            content = !(statement instanceof Inheritance) ? compound instanceof SetExt && 0 == side
                    || compound instanceof SetInt && 1 == side ? Inheritance.make(pre, sub, memory) : Inheritance.make(sub, pre, memory) : Similarity.make(sub, pre, memory);
            Task task = memory.getCurrentTask();
            Sentence sentence = task.getSentence();
            TruthValue truth = sentence.getTruth();
            BudgetValue budget;
            budget = sentence.isQuestion() ? compoundBackward(content, memory) : compoundForward(memory, truth, content);
            Memory.singlePremiseTask(memory, content, truth, budget);
        }
    }

    /**
     * {(||, S, P), P} |- S {(&&, S, P), P} |- S
     *
     * @param compoundTask Whether the implication comes from the task
     * @param memory       Reference to the memory
     */
    static void decomposeStatement(CompoundTerm compound, Term component,
                                   boolean compoundTask, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence sentence = task.getSentence();
        if (sentence.isQuestion()) {
            return;
        }
        Sentence belief = memory.getCurrentBelief();
        Term content = CompoundTerm.reduceComponents(compound, component,
                memory);
        if (null != content) {
            TruthValue v1, v2;
            if (compoundTask) {
                v1 = sentence.getTruth();
                v2 = belief.getTruth();
            } else {
                v1 = belief.getTruth();
                v2 = sentence.getTruth();
            }
            TruthValue truth = null;
            if (compound instanceof Conjunction) truth = TruthFunctions.reduceConjunction(v1, v2);
            Memory.doublePremiseTask(memory, content, truth, compoundForward(memory, truth, content
            ));
        }
    }

    /**
     * {<M ==> S>,
     * <P ==>
     * M>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue exemplification(TruthHandle v1, TruthHandle v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = UtilityFunctions.and(f1, f2, c1, c2);
        float c = UtilityFunctions.w2c(w);
        return new TruthValue(1, c);
    }

    /**
     * Make a symmetric Statement from given components and temporal information, called by the rules
     *
     * @param statement A sample asymmetric statement providing the class type
     * @param subj      The first component
     * @param pred      The second component
     * @param memory    Reference to the memeory
     * @return The Statement built
     */
    public static Statement makeSym(Statement statement, Term subj, Term pred, Memory memory) {
        if (statement instanceof Inheritance) {
            return Similarity.make(subj, pred, memory);
        }
        if (statement instanceof Implication) {
            return Equivalence.Companion.make(subj, pred, memory);
        }
        return null;
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue anonymousAnalogy(TruthHandle v1, TruthHandle v2) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        TruthHandle v0 = new TruthValue(f1, UtilityFunctions.w2c(c1));
        return TruthFunctions.analogy(v2, v0);
    }

    /**
     * Check if two terms can be unified
     *
     * @param type  The type of variable that can be substituted
     * @param term1 The first term to be unified
     * @param term2 The second term to be unified
     * @return Whether there is a substitution
     */
    public static boolean hasSubstitute(char type, TermStruct term1, TermStruct term2) {
        return Expr3.INSTANCE.findSubstitute(type, term1, term2, new HashMap<>(), new HashMap<>());
    }

    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue contraposition(TruthHandle v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = UtilityFunctions.and(1 - f1, c1);
        float c = UtilityFunctions.w2c(w);
        return new TruthValue(0, c);
    }
}
