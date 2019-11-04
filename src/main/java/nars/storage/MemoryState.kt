package nars.storage

import nars.entity.*
import nars.io.IInferenceRecorder
import nars.language.Term
import nars.main_nogui.Parameters
import nars.main_nogui.ReasonerBatch
import java.util.concurrent.atomic.AtomicInteger

class MemoryState {
      var exportStrings = mutableListOf <String>()
    /**
     * Backward pointer to the reasoner
     */
    var reasoner/* ---------- Long-term storage for multiple cycles ---------- */: ReasonerBatch? = null
    /**
     * Concept bag. Containing all Concepts of the system
     */
    var concepts: ConceptBag? = null
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    var novelTasks: NovelTaskBag? = null
    val beliefForgettingRate: AtomicInteger
    val taskForgettingRate: AtomicInteger
    val conceptForgettingRate: AtomicInteger
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    var newTasks/* ---------- Short-term workspace for a single cycle ---------- */: List<Task>? = null
        private set

    /**
     * The selected Term
     */
    var currentTerm: Term? = null
    /**
     * The selected Concept
     */
    var currentConcept: Concept? = null
    /**
     * The selected TaskLink
     */
    var currentTaskLink: TaskLink? = null
    /**
     * The selected Task
     */
    var currentTask: Task? = null
    /**
     * The selected TermLink
     */
    var currentBeliefLink: TermLink? = null
    /**
     * The selected belief
     */
    var currentBelief: Sentence? = null
    /**
     * The new Stamp
     */
    var newStamp: Stamp? = null
    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    var substitute: Map<Term, Term>? = null
    var recorder: IInferenceRecorder? = null

    fun <E> setNewTasks(newTasks: List<Task>?) {
        this.newTasks = newTasks
    }

    init {
        beliefForgettingRate = AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE)
        taskForgettingRate = AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE)
        conceptForgettingRate = AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE)
    }
}