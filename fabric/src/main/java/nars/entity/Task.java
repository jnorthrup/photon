/*
 * Task.java
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


import nars.data.TaskStruct;
import nars.data.TermStruct;
import nars.language.Term;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public class Task extends Item implements TaskStruct {
    private Sentence sentence;
    private Sentence parentBelief;
    private Sentence bestSolution;
    private Task parentTask;

    /**
     * Constructor for input task
     *
     * @param s The sentence
     * @param b The budget
     */
    public Task(Sentence s, BudgetValue b) {
        super(Sentence.toKey(s), b); // change to toKey()
        setSentence(s);
        setKey(Sentence.toKey(getSentence()));
    }

    /**
     * Constructor for a derived task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief) {
        this(s, b);
        setParentTask(parentTask);
        setParentBelief(parentBelief);
    }

    /**
     * Constructor for an activated task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     * @param solution     The belief to be used in future inference
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief, Sentence solution) {
        this(s, b, parentTask, parentBelief);
        setBestSolution(solution);
    }

    /**
     * The sentence of the Task

     * Get the sentence
     *
     * @return The sentence
     */

    public Sentence getSentence() {
        return sentence;
    }

    /**
     * Directly get the content of the sentence
     *
     * @return The content of the sentence
     */
    @Override
    public Term getContent() {
        return getSentence().getContent();
    }

    @Override
    public void setContent(TermStruct $content$) {

    }

     public boolean getInput() {
        return false;
    }

    @Override
    public void setInput(boolean $input$) {

    }

     public boolean getStructural() {
        return false;
    }

    @Override
    public void setStructural(boolean $structural$) {

    }

    /**
     * Directly get the creation time of the sentence
     *
     * @return The creation time of the sentence
     */
    @Override
    public long getCreationTime() {
        return getSentence().getStamp().getCreationTime();
    }

    @Override
    public void setCreationTime(long $creationTime$) {

    }

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */

    @Override
    public boolean isInput() {
        return null == getParentTask();
    }

    /**
     * Check if a Task is derived by a StructuralRule
     *
     * @return Whether the Task is derived by a StructuralRule
     */

    @Override
    public boolean isStructural() {
        return null == getParentBelief() && null != getParentTask();
    }

    /**
     * Merge one Task into another
     *
     * @param task
     * @param that The other Task
     */
    public static void merge(Task task, Item that) {
        if (task.getCreationTime() >= ((TaskStruct) that).getCreationTime()) {
            merge(task.getBudget(), that);
        } else {
            Item.merge(that.getBudget(), task);
        }
    }

    /**
     * For Question and Goal: best solution found so far

     * Get the best-so-far solution for a Question or Goal
     *
     * @return The stored Sentence or null
     */

    public Sentence getBestSolution() {
        return bestSolution;
    }

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer for input question
     *
     * @param judg The solution to be remembered
     */

    public void setBestSolution(Sentence judg) {
        bestSolution = judg;
    }

    /**
     * Belief from which the Task is derived, or null if derived from a theorem

     * Get the parent belief of a task
     *
     * @return The belief from which the task is derived
     */
    public Sentence getParentBelief() {
        return parentBelief;
    }

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */
    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(super.toString() + " ");
        s.append(getSentence().getStamp());
        if (null != getParentTask()) {
            s.append("  \n from task: " + Item.toStringBrief(getParentTask().getBudget(), getParentTask().getKey()));
            if (null != getParentBelief()) {
                s.append("  \n from belief: " + Sentence.toStringBrief(getParentBelief()));
            }
        }
        if (null != getBestSolution()) {
            s.append("  \n solution: " + Sentence.toStringBrief(getBestSolution()));
        }
//        s.append("\n>>>> end of Task");

        return s.toString();
    }


    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
    }

    /**
     * Task from which the Task is derived, or null if input
     */
    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    public void setParentBelief(Sentence parentBelief) {
        this.parentBelief = parentBelief;
    }
}

