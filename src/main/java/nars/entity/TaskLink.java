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
package nars.entity;

import nars.main_nogui.Parameters;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TermLink {

    final TasklinkState tasklinkState = new TasklinkState();

    /**
     * Constructor
     * <p>
     * only called in BackingStore.continuedProcess
     *
     * @param t        The target Task
     * @param template The TermLink template
     * @param v        The budget
     */
    public TaskLink(Task t, TermLink template, BudgetValue v) {
        super("", v);
        tasklinkState.targetTask = t;
        if (template == null) {
            type = TermLinkConstants.SELF;
            index = null;
        } else {
            type = template.getType();
            index = template.getIndices();
        }
        tasklinkState.recordedLinks = new String[Parameters.TERM_LINK_RECORD_LENGTH];
        tasklinkState.recordingTime = new long[Parameters.TERM_LINK_RECORD_LENGTH];
        tasklinkState.counter = 0;
    }

    public String getKey() {
        return super.getKey() + tasklinkState.targetTask.getKey();
    }

    /**
     * Get the target Task
     *
     * @return The linked Task
     */
    public Task getTargetTask() {
        return tasklinkState.targetTask;
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     *
     * @param termLink    The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(TermLink termLink, long currentTime) {
        var bTerm = termLink.getTarget();
        if (bTerm.equals(tasklinkState.targetTask.getSentence().getContent())) {
            return false;
        }
        var linkKey = termLink.getKey();
        int next, i;
        for (i = 0; i < tasklinkState.counter; i++) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(tasklinkState.recordedLinks[next])) {
                if (currentTime < tasklinkState.recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    tasklinkState.recordingTime[next] = currentTime;
                    return true;
                }
            }
        }
        next = i % Parameters.TERM_LINK_RECORD_LENGTH;
        tasklinkState.recordedLinks[next] = linkKey;       // add knowledge reference to recordedLinks
        tasklinkState.recordingTime[next] = currentTime;
        if (tasklinkState.counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            tasklinkState.counter++;
        }
        return true;
    }


    public String toString() {
        return super.toString() + " " + getTargetTask().getSentence().getStamp();
    }
}
