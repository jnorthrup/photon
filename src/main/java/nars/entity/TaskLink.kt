/*
 * TaskLink.java
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
package nars.entity

import nars.entity.TermLinkConstants.SELF
import nars.main_nogui.Parameters

/**
 * Reference to a Task.
 *
 *
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
class TaskLink(t: Task?, template: TermLink?, v: BudgetValue?) : TermLink("", v) {
    val tasklinkState = TasklinkState()
    override val key: String
        get() = super.key  + tasklinkState.targetTask!!.key

    /**
     * Get the target Task
     *
     * @return The linked Task
     */
    val targetTask: Task?
        get() = tasklinkState.targetTask

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     *
     *
     * called in TermLinkBag only
     *
     * @param termLink    The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    fun novel(termLink: TermLink, currentTime: Long): Boolean {
        val bTerm = termLink.target
        if (bTerm == tasklinkState.targetTask!!.sentence.content) {
            return false
        }
        val linkKey: String = termLink.key
        var next: Int
        var i: Int
        i = 0
        while (i < tasklinkState.counter) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH
            if (linkKey == tasklinkState.recordedLinks[next]) {
                return if (currentTime < tasklinkState.recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    false
                } else {
                    tasklinkState.recordingTime[next] = currentTime
                    true
                }
            }
            i++
        }
        next = i % Parameters.TERM_LINK_RECORD_LENGTH
        tasklinkState.recordedLinks[next] = linkKey // add knowledge reference to recordedLinks
        tasklinkState.recordingTime[next] = currentTime
        if (tasklinkState.counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            tasklinkState.counter = tasklinkState.counter + 1
        }
        return true
    }

    override fun toString(): String {
        return super.toString() + " " + targetTask!!.sentence.stamp
    }

    /**
     * Constructor
     *
     *
     * only called in BackingStore.continuedProcess
     *
     * @param t        The target Task
     * @param template The TermLink template
     * @param v        The budget
     */
    init {
        tasklinkState.targetTask = t
        if (template == null) {
            type = SELF
            index = null
        } else {
            type = template.type
            index = template.indices
        }
        tasklinkState.recordedLinks = arrayOfNulls<String >(  Parameters.TERM_LINK_RECORD_LENGTH)
        tasklinkState.recordingTime = LongArray(Parameters.TERM_LINK_RECORD_LENGTH)
        tasklinkState.counter = 0
    }
}