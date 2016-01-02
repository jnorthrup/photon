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
package nars.inference;

import nars.data.TruthHandle;
import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;

import java.util.HashMap;
import java.util.Map;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
public final class CompositionalRules {

	/* -------------------- intersections and differences -------------------- */

	/**
	 * {<S ==> M>,
	 * <P ==>
	 * M>} |- {<(S|P) ==> M>, <(S&P) ==> M>, <(S-P) ==> M>, <(P-S) ==> M>}
	 * 
	 * @param index
	 *            The location of the shared term
	 * @param memory
	 *            Reference to the memory
	 */
	static void composeCompound(Statement taskContent, Statement beliefContent,
			int index, Memory memory) {
		if (!memory.getCurrentTask().getSentence().isJudgment()
				|| taskContent.getClass() != beliefContent.getClass()) {
			return;
		}
		Term componentT = taskContent.componentAt(1 - index);
		Term componentB = beliefContent.componentAt(1 - index);
		Term componentCommon = taskContent.componentAt(index);
		if (componentT instanceof CompoundTerm
				&& ((CompoundTerm) componentT).containAllComponents(componentB)) {
			decomposeCompound((CompoundTerm) componentT, componentB,
					componentCommon, index, true, memory);
			return;
		} else if (componentB instanceof CompoundTerm
				&& ((CompoundTerm) componentB).containAllComponents(componentT)) {
			decomposeCompound((CompoundTerm) componentB, componentT,
					componentCommon, index, false, memory);
			return;
		}
		TruthValue truthT = memory.getCurrentTask().getSentence().getTruth();
		TruthValue truthB = memory.getCurrentBelief().getTruth();
		TruthValue truthOr = TruthFunctions.union(truthT, truthB);
		TruthValue truthAnd = TruthFunctions.intersection(truthT, truthB);
		Term termOr = null;
		Term termAnd = null;
		if (index == 0) {
			if (taskContent instanceof Inheritance) {
				termOr = IntersectionInt.make(componentT, componentB, memory);
				if (truthB.isNegative()) {
					if (!truthT.isNegative()) {
						termAnd = DifferenceExt.Companion.make(componentT,
								componentB, memory);
						truthAnd = TruthFunctions.intersection(truthT,
								TruthFunctions.negation(truthB));
					}
				} else if (truthT.isNegative()) {
					termAnd = DifferenceExt.Companion.make(componentB,
							componentT, memory);
					truthAnd = TruthFunctions.intersection(truthB,
							TruthFunctions.negation(truthT));
				} else {
					termAnd = IntersectionExt.make(componentT, componentB,
							memory);
				}
			} else if (taskContent instanceof Implication) {
				termOr = Disjunction.Companion.make(componentT, componentB,
						memory);
				termAnd = Conjunction.Companion.make(componentT, componentB,
						memory);
			}
			processComposed(taskContent, componentCommon, termOr, truthOr,
					memory);
			processComposed(taskContent, componentCommon, termAnd, truthAnd,
					memory);
		} else { // index == 1
			if (taskContent instanceof Inheritance) {
				termOr = IntersectionExt.make(componentT, componentB, memory);
				if (truthB.isNegative()) {
					if (!truthT.isNegative()) {
						termAnd = DifferenceInt.Companion.make(componentT,
								componentB, memory);
						truthAnd = TruthFunctions.intersection(truthT,
								TruthFunctions.negation(truthB));
					}
				} else if (truthT.isNegative()) {
					termAnd = DifferenceInt.Companion.make(componentB,
							componentT, memory);
					truthAnd = TruthFunctions.intersection(truthB,
							TruthFunctions.negation(truthT));
				} else {
					termAnd = IntersectionInt.make(componentT, componentB,
							memory);
				}
			} else if (taskContent instanceof Implication) {
				termOr = Conjunction.Companion.make(componentT, componentB,
						memory);
				termAnd = Disjunction.Companion.make(componentT, componentB,
						memory);
			}
			processComposed(taskContent, termOr, componentCommon, truthOr,
					memory);
			processComposed(taskContent, termAnd, componentCommon, truthAnd,
					memory);
		}
		if (taskContent instanceof Inheritance) {
			introVarOuter(taskContent, beliefContent, index, memory);// introVarImage(taskContent,
			// beliefContent,
			// index,
			// memory);
		}
	}

	/**
	 * Finish composing implication term
	 * 
	 * @param subject
	 *            Subject of contentInd
	 * @param predicate
	 *            Predicate of contentInd
	 * @param truth
	 *            TruthValue of the contentInd
	 * @param memory
	 *            Reference to the memory
	 */
	private static void processComposed(Statement statement, Term subject,
			Term predicate, TruthValue truth, Memory memory) {
		if (subject == null || predicate == null) {
			return;
		}
		Term content = Statement.make(statement, subject, predicate, memory);
		if (content == null || content.equals(statement)
				|| content.equals(memory.getCurrentBelief().getContent())) {
			return;
		}
		BudgetValue budget = BudgetFunctions.compoundForward(memory, truth,
				content);
		Memory.doublePremiseTask(memory, content, truth, budget);
	}

	/**
	 * {<(S|P) ==> M>,
	 * <P ==>
	 * M>} |- <S ==> M>
	 * 
	 * 
	 * 
	 * @param term1
	 *            The other term in the contentInd
	 * @param index
	 *            The location of the shared term: 0 for subject, 1 for
	 *            predicate
	 * @param compoundTask
	 *            Whether the implication comes from the task
	 * @param memory
	 *            Reference to the memory
	 */
	private static void decomposeCompound(CompoundTerm compound,
			Term component, Term term1, int index, boolean compoundTask,
			Memory memory) {
		if (!(compound instanceof Statement)) {
			Term term2 = CompoundTerm.reduceComponents(compound, component,
					memory);
			if (term2 != null) {
				Task task = memory.getCurrentTask();
				Sentence sentence = task.getSentence();
				Sentence belief = memory.getCurrentBelief();
				Statement oldContent = (Statement) task.getContent();
				TruthValue v1, v2;
				if (compoundTask) {
					v1 = sentence.getTruth();
					v2 = belief.getTruth();
				} else {
					v1 = belief.getTruth();
					v2 = sentence.getTruth();
				}
				TruthValue truth = null;
				Term content;
				if (index == 0) {
					content = Statement.make(oldContent, term1, term2, memory);
					if (content != null && oldContent instanceof Inheritance) {
						if (!(compound instanceof IntersectionExt)) {
							if (!(compound instanceof IntersectionInt)) {
								if (!(compound instanceof SetInt)
										|| !(component instanceof SetInt)) {
									if (!(compound instanceof SetExt)
											|| !(component instanceof SetExt)) {
										if (!(compound instanceof DifferenceExt)) {
											if (!(oldContent instanceof Implication)) {
												return;
											}
											if (compound instanceof Conjunction) {
												truth = TruthFunctions
														.reduceConjunction(v1,
																v2);
											} else if (compound instanceof Disjunction) {
												truth = reduceDisjunction(v1,
														v2);
											}
										} else {
											if (compound.componentAt(0).equals(
													component)) {
												truth = reduceDisjunction(v2,
														v1);
											} else {
												truth = reduceConjunctionNeg(
														v1, v2);
											}
										}
									} else {
										truth = reduceDisjunction(v1, v2);
									}
								} else {
									truth = TruthFunctions.reduceConjunction(
											v1, v2);
								}
							} else {
								truth = reduceDisjunction(v1, v2);
							}
						} else {
							truth = TruthFunctions.reduceConjunction(v1, v2);
						}
					}
				} else {
					content = Statement.make(oldContent, term2, term1, memory);
					if (content == null) {
						return;
					}
					if (oldContent instanceof Inheritance) {
						if (compound instanceof IntersectionInt) {
							truth = TruthFunctions.reduceConjunction(v1, v2);
						} else if (compound instanceof IntersectionExt) {
							truth = reduceDisjunction(v1, v2);
						} else if (compound instanceof SetExt
								&& component instanceof SetExt) {
							truth = TruthFunctions.reduceConjunction(v1, v2);
						} else if (compound instanceof SetInt
								&& component instanceof SetInt) {
							truth = reduceDisjunction(v1, v2);
						} else if (compound instanceof DifferenceInt) {
							if (compound.componentAt(1).equals(component)) {
								truth = reduceDisjunction(v2, v1);
							} else {
								truth = reduceConjunctionNeg(v1, v2);
							}
						}
					} else if (oldContent instanceof Implication)
						if (compound instanceof Disjunction) {
							truth = TruthFunctions.reduceConjunction(v1, v2);
						} else if (compound instanceof Conjunction) {
							truth = reduceDisjunction(v1, v2);
						}
				}
				if (truth != null) {
					BudgetValue budget = BudgetFunctions.compoundForward(
							memory, truth, content);
					Memory.doublePremiseTask(memory, content, truth, budget);
				}
			}
		}
	}

	/* --------------- rules used for variable introduction --------------- */

	/**
	 * Introduce a dependent variable in an outer-layer conjunction
	 * 
	 * @param taskContent
	 *            The first premise <M --> S>
	 * @param beliefContent
	 *            The second premise <M --> P>
	 * @param index
	 *            The location of the shared term: 0 for subject, 1 for
	 *            predicate
	 * @param memory
	 *            Reference to the memory
	 */
	private static void introVarOuter(Statement taskContent, Statement beliefContent, int index, Memory memory) {
        TruthValue truthT = memory.getCurrentTask().getSentence().getTruth();
        TruthValue truthB = memory.getCurrentBelief().getTruth();
        Variable varInd = new Variable("$varInd1");
        Variable varInd2 = new Variable("$varInd2");
        Term term11, term12, term21, term22, commonTerm;
        Map<Term, Term> subs = new HashMap<Term, Term>();
        if (index == 0) {
            term11 = varInd;
            term21 = varInd;
            term12 = taskContent.getPredicate();
            term22 = beliefContent.getPredicate();
            if (term12 instanceof ImageExt && term22 instanceof ImageExt) {
                commonTerm = ((ImageExt) term12).getTheOtherComponent();
                if (commonTerm == null || !Expr4.INSTANCE.hasOrIsTerm(term22, commonTerm)) {
                    commonTerm = ((ImageExt) term22).getTheOtherComponent();
                    if (commonTerm == null || !Expr4.INSTANCE.hasOrIsTerm(term12, commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((CompoundTerm) term12).applySubstitute(subs);
                    ((CompoundTerm) term22).applySubstitute(subs);
                }
            }
        } else {
            term11 = taskContent.getSubject();
            term21 = beliefContent.getSubject();
            term12 = varInd;
            term22 = varInd;
            if (term11 instanceof ImageInt && term21 instanceof ImageInt) {
                commonTerm = ImageInt.getTheOtherComponent(((ImageInt) term11));
                if (commonTerm == null || !Expr4.INSTANCE.hasOrIsTerm(term21, commonTerm)) {
                    commonTerm = ImageInt.getTheOtherComponent(((ImageInt) term21));
                    if (commonTerm == null || !Expr4.INSTANCE.hasOrIsTerm(term11, commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((CompoundTerm) term11).applySubstitute(subs);
                    ((CompoundTerm) term21).applySubstitute(subs);
                }
            }
        }
        Statement state1 = Inheritance.make(term11, term12, memory);
        Statement state2 = Inheritance.make(term21, term22, memory);
        Term content = Implication.Companion.make(state1, state2, memory);
        TruthValue truth = TruthFunctions.induction(truthT, truthB);
        BudgetValue budget = BudgetFunctions.compoundForward(memory, truth, content);
        Memory.doublePremiseTask(memory, content, truth, budget);
        content = Implication.Companion.make(state2, state1, memory);
        truth = TruthFunctions.induction(truthB, truthT);
        budget = BudgetFunctions.compoundForward(memory, truth, content);
        Memory.doublePremiseTask(memory, content, truth, budget);
        content = Equivalence.Companion.make(state1, state2, memory);
        truth = TruthFunctions.comparison(truthT, truthB);
        budget = BudgetFunctions.compoundForward(memory, truth, content);
        Memory.doublePremiseTask(memory, content, truth, budget);
        Variable varDep = new Variable("#varDep");
        if (index == 0) {
            state1 = Inheritance.make(varDep, taskContent.getPredicate(), memory);
            state2 = Inheritance.make(varDep, beliefContent.getPredicate(), memory);
        } else {
            state1 = Inheritance.make(taskContent.getSubject(), varDep, memory);
            state2 = Inheritance.make(beliefContent.getSubject(), varDep, memory);
        }
        content = Conjunction.Companion.make(state1, state2, memory);
        truth = TruthFunctions.intersection(truthT, truthB);
        budget = BudgetFunctions.compoundForward(memory, truth, content);
        Memory.doublePremiseTask(memory, content, truth, budget, false);
    }
	// /**
	// * Introduce a second independent variable into two terms with a common
	// component
	// * @param term1 The first term
	// * @param term2 The second term
	// * @param index The index of the terms in their statement
	// */
	// private static void introVarSecond(Term term1, Term term2, int index) {
	// Variable varInd2 = new Variable("$varIndSec");
	// Term commonTerm;
	// HashMap<Term, Term> subs = new HashMap<Term, Term>();
	// if (index == 0) {
	// if ((term1 instanceof ImageExt) && (term2 instanceof ImageExt)) {
	// commonTerm = ((ImageExt) term1).getTheOtherComponent();
	// if ((commonTerm == null) || !((ImageExt) term2).containTerm(commonTerm))
	// {
	// commonTerm = ((ImageExt) term2).getTheOtherComponent();
	// if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm))
	// {
	// commonTerm = null;
	// }
	// }
	// if (commonTerm != null) {
	// subs.put(commonTerm, varInd2);
	// ((ImageExt) term1).applySubstitute(subs);
	// ((ImageExt) term2).applySubstitute(subs);
	// }
	// }
	// } else {
	// if ((term1 instanceof ImageInt) && (term2 instanceof ImageInt)) {
	// commonTerm = ((ImageInt) term1).getTheOtherComponent();
	// if ((commonTerm == null) || !((ImageInt) term2).containTerm(commonTerm))
	// {
	// commonTerm = ((ImageInt) term2).getTheOtherComponent();
	// if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm))
	// {
	// commonTerm = null;
	// }
	// }
	// if (commonTerm != null) {
	// subs.put(commonTerm, varInd2);
	// ((ImageInt) term1).applySubstitute(subs);
	// ((ImageInt) term2).applySubstitute(subs);
	// }
	// }
	// }
	// }

	/**
	 * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
	 * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
	 * 
	 * @param oldCompound
	 *            The whole contentInd of the first premise, Implication or
	 *            Conjunction
	 * @param memory
	 *            Reference to the memory
	 */
	// also handle the intensional situation
	static void introVarInner(Statement premise1, Statement premise2, CompoundTerm oldCompound, Memory memory) {
        Task task = memory.getCurrentTask();
        Sentence taskSentence = task.getSentence();
        if (!taskSentence.isJudgment() || premise1.getClass() != premise2.getClass() || oldCompound.containComponent(premise1)) {
            return;
        }
        Term subject1 = premise1.getSubject();
        Term subject2 = premise2.getSubject();
        Term predicate1 = premise1.getPredicate();
        Term predicate2 = premise2.getPredicate();
//        Statement stateInd1, stateInd2, stateDep1, stateDep2;
        Term commonTerm1, commonTerm2;
        if (subject1.equals(subject2)) {
            commonTerm1 = subject1;
            commonTerm2 = secondCommonTerm(predicate1, predicate2, 0);
//            stateDep1 = Statement.make(premise1, varDep, predicate1, memory);
//            stateDep2 = Statement.make(premise2, varDep, predicate2, memory);
//            Term predicate1C = (Term) predicate1.clone();
//            Term predicate2C = (Term) predicate2.clone();
//            introVarSecond(predicate1C, predicate2C, 0);
//            stateInd1 = Statement.make(premise1, varInd, predicate1C, memory);
//            stateInd2 = Statement.make(premise2, varInd, predicate2C, memory);
        } else if (predicate1.equals(predicate2)) {
            commonTerm1 = predicate1;
            commonTerm2 = secondCommonTerm(subject1, subject2, 0);
//            stateDep1 = Statement.make(premise1, subject1, varDep, memory);
//            stateDep2 = Statement.make(premise2, subject2, varDep, memory);
//            Term subject1C = (Term) subject1.clone();
//            Term subject2C = (Term) subject2.clone();
//            introVarSecond(subject1C, subject2C, 1);
//            stateInd1 = Statement.make(premise1, subject1C, varInd, memory);
//            stateInd2 = Statement.make(premise2, subject2C, varInd, memory);
        } else {
            return;
        }
//        Term implication, contentInd, contentDep = null;
//        if (oldCompound instanceof Implication) {
//            implication = Statement.make((Statement) oldCompound, oldCompound.componentAt(0), stateInd2, memory);
//            contentInd = Statement.make((Statement) oldCompound, stateInd1, implication, memory);
//            if (oldCompound.equals(premise1)) {
//                return;
//            }
//            contentDep = Conjunction.make(oldCompound, premise1, memory);
//            if (contentDep == null || !(contentDep instanceof CompoundTerm)) {
//                return;
//            }
//            HashMap<Term, Term> substitute = new HashMap<Term, Term>();
//            substitute.put(memory.currentTerm, new Variable("#varDep"));
//            ((CompoundTerm) contentDep).applySubstitute(substitute);
//        } else if (oldCompound instanceof Conjunction) {
//            implication = Implication.make(stateInd1, stateInd2, memory);
//            HashMap<Term, Term> subs = new HashMap<Term, Term>();
//            subs.put(premise2, implication);
//            contentInd = (Term) oldCompound.clone();
//            ((CompoundTerm) contentInd).applySubstitute(subs);
//            contentDep = Conjunction.make(stateDep1, oldCompound, memory);
//            subs.clear();
//            subs.put(premise2, stateDep2);
//            ((CompoundTerm) contentDep).applySubstitute(subs);
//        } else {
//            return;
//        }
        Sentence belief = memory.getCurrentBelief();
        Map<Term, Term> substitute = new HashMap<Term, Term>();
        substitute.put(commonTerm1, new Variable("#varDep2"));
        CompoundTerm content = (CompoundTerm) Conjunction.Companion.make(premise1, oldCompound, memory);
        content.applySubstitute(substitute);
        TruthValue truth = TruthFunctions.intersection(taskSentence.getTruth(), belief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(memory, truth);
        Memory.doublePremiseTask(memory, content, truth, budget, false);
        substitute.clear();
        substitute.put(commonTerm1, new Variable("$varInd1"));
        if (commonTerm2 != null) {
            substitute.put(commonTerm2, new Variable("$varInd2"));
        }
        content = Implication.Companion.make(premise1, oldCompound, memory);
        content.applySubstitute(substitute);
        if (premise1.equals(taskSentence.getContent())) {
            truth = TruthFunctions.induction(belief.getTruth(), taskSentence.getTruth());
        } else {
            truth = TruthFunctions.induction(taskSentence.getTruth(), belief.getTruth());
        }
        budget = BudgetFunctions.forward(memory, truth);
        Memory.doublePremiseTask(memory, content, truth, budget);
    }
	/**
	 * Introduce a second independent variable into two terms with a common
	 * component
	 * 
	 * @param term1
	 *            The first term
	 * @param term2
	 *            The second term
	 * @param index
	 *            The index of the terms in their statement
	 */
	private static Term secondCommonTerm(Term term1, Term term2, int index) {
		Term commonTerm = null;
		if (index == 0) {
			if (term1 instanceof ImageExt && term2 instanceof ImageExt) {
				commonTerm = ((ImageExt) term1).getTheOtherComponent();
				if (commonTerm == null
						|| !Expr4.INSTANCE.hasOrIsTerm(term2, commonTerm)) {
					commonTerm = ((ImageExt) term2).getTheOtherComponent();
					if (commonTerm == null
							|| !Expr4.INSTANCE.hasOrIsTerm(term1, commonTerm)) {
						commonTerm = null;
					}
				}
			}
		} else {
			if (term1 instanceof ImageInt && term2 instanceof ImageInt) {
				commonTerm = ImageInt.getTheOtherComponent(((ImageInt) term1));
				if (commonTerm == null
						|| !Expr4.INSTANCE.hasOrIsTerm(term2, commonTerm)) {
					commonTerm = ImageInt
							.getTheOtherComponent(((ImageInt) term2));
					if (commonTerm == null
							|| !Expr4.INSTANCE.hasOrIsTerm(term1, commonTerm)) {
						commonTerm = null;
					}
				}
			}
		}
		return commonTerm;
	}

	/**
	 * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue reduceConjunctionNeg(TruthHandle v1, TruthHandle v2) {
		return TruthFunctions
				.reduceConjunction(v1, TruthFunctions.negation(v2));
	}

	/**
	 * {(||, A, B), (--, B)} |- A
	 * 
	 * @param v1
	 *            Truth value of the first premise
	 * @param v2
	 *            Truth value of the second premise
	 * @return Truth value of the conclusion
	 */
	static TruthValue reduceDisjunction(TruthHandle v1, TruthHandle v2) {
		TruthValue v0 = TruthFunctions.intersection(v1,
				TruthFunctions.negation(v2));
		return TruthFunctions.deduction(v0, 1f);
	}
}
