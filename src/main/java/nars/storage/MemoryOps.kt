package nars.storage

import nars.entity.*
import nars.io.IInferenceRecorder
import nars.language.Term
import nars.main_nogui.ReasonerBatch
import java.util.concurrent.atomic.AtomicInteger

interface MemoryOps {
    fun clear() /* ---------- access utilities ---------- */
    val exportStrings: List<String?>?
    var recorder: IInferenceRecorder?
    val time: Long
    fun noResult(): Boolean
    fun nameToConcept(name: String?): Concept?
    fun nameToListedTerm(name: String?): Term?
    fun termToConcept(term: Term?): Concept?
    fun getConcept(term: Term?): Concept?
    fun getConceptActivation(t: Term?): Float
    fun activateConcept(c: Concept?, b: BudgetValue?)
    fun inputTask(task: Task?)
    fun activatedTask(budget: BudgetValue?, sentence: Sentence?, candidateBelief: Sentence?)
    fun derivedTask(task: Task?)
    fun doublePremiseTask(newContent: Term?, newTruth: TruthValue?, newBudget: BudgetValue?)
    fun doublePremiseTask(newContent: Term?, newTruth: TruthValue?, newBudget: BudgetValue?, revisible: Boolean)
    fun singlePremiseTask(newContent: Term?, newTruth: TruthValue?, newBudget: BudgetValue?)
    fun singlePremiseTask(newContent: Term?, punctuation: Any, newTruth: TruthValue?, newBudget: BudgetValue?)
    fun workCycle(clock: Long)
    fun processNewTask()
    fun processNovelTask()
    fun processConcept()
    fun immediateProcess(task: Task?)
    fun report(sentence: Sentence?, input: Boolean)
    val taskForgettingRate: AtomicInteger?
    val beliefForgettingRate: AtomicInteger?
    val conceptForgettingRate: AtomicInteger?
    val reasoner: ReasonerBatch?
    val concepts: ConceptBag?
    val novelTasks: NovelTaskBag?
    val newTasks: List<Task?>?
    var currentTerm: Term?
    var currentConcept: Concept?
    var currentTaskLink: TaskLink?
    var currentTask: Task?
    var currentBeliefLink: TermLink?
    var currentBelief: Sentence?
    var newStamp: Stamp?
    fun getSubstitute(): Map<Term?, Term?>?
    fun setSubstitute(substitute: Map<Term?, Term?>?)
}