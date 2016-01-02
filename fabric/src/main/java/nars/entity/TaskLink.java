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

import nars.data.TaskLinkStruct;
import nars.data.TaskStruct;
import nars.data.TermLinkStruct;
import nars.language.Term;
import nars.storage.Parameters;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be linked from 
 * multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TermLink implements TaskLinkStruct {

    private int counter;
    private Task targetTask;
    private List<Long> recordingTime;
    private List<String>recordedLinks =new CopyOnWriteArrayList<>();

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     * @param t The target Task
     * @param template The TermLink template
     * @param v The budget
     */
    public TaskLink(Task t, TermLinkStruct template, BudgetValue v) {
        super("", v);
        setTargetTask(t);
        if (template == null) {
            setType(TermLink.SELF);
            setIndex(null);
        } else {
            setType(template.getType());
            setIndex(template.getIndex());
        }
        setRecordingTime(new CopyOnWriteArrayList<>());
        setCounter(0);
        setKey();   // as defined in TermLink
        setKey(getKey() + t.getKey());
    }

    /** The Task linked. The "target" field in TermLink is not used here. */ /**
     * Get the target Task
     * @return The linked Task
     */
    @Override
    public Task getTargetTask() {
        return targetTask;
    }


    /**
     * To check whether a TaskLink should use a TermLink, return false if they 
     * interacted recently
     * <p>
     * called in TermLinkBag only
     * @param termLink The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(TermLink termLink, long currentTime) {
        Term bTerm = termLink.getTerm();
        if (bTerm.equals(getTargetTask().getSentence().getContent())) {
            return false;
        }
        String linkKey = termLink.getKey();
        int next = 0;
        int i;
        for (i = 0; i < getCounter(); i++) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(this.getRecordedLinks().get(next))) {
                if (currentTime < getRecordingTime().get(next) + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    getRecordingTime().set(next, currentTime);
                    return true;
                }
            }
        }
        next = i % Parameters.TERM_LINK_RECORD_LENGTH;
        getRecordedLinks().add(next, linkKey);       // add knowledge reference to recordedLinks
        getRecordingTime().add(next, currentTime);
        if (getCounter() < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            setCounter(getCounter() + 1);
        }
        return true;
    }


    @Override
	public String toString() {
		return super.toString() + " " + getTargetTask().getSentence().getStamp();
	}


    public void setTargetTask(TaskStruct targetTask) {
        this.targetTask = (Task) targetTask;
    }

    /** Remember the TermLinks that has been used recently with this TaskLink */
    @Override
    public List<String> getRecordedLinks() {
        return recordedLinks;
    }

    @Override
    public void setRecordedLinks(List<String> recordedLinks) {
        this.recordedLinks = recordedLinks;
    }

    /** Remember the time when each TermLink is used with this TaskLink */
    @Override
    public List<Long> getRecordingTime() {
        return recordingTime;
    }

    @Override
    public void setRecordingTime(List<Long> recordingTime) {
        this.recordingTime = recordingTime;
    }

    /** The number of TermLinks remembered */
    @Override
    public int getCounter() {
        return counter;
    }

    @Override
    public void setCounter(int counter) {
        this.counter = counter;
    }

}

