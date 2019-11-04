package nars.entity;

public class TasklinkState {
    /**
     * The number of TermLinks remembered
     */
    int counter;
    /**
     * The Task linked. The "target" field in TermLink is not used here.
     */
    Task targetTask;
    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    String[] recordedLinks;
    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    long[] recordingTime;

    public TasklinkState() {
    }
}