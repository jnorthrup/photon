package nars.entity;

public class TasklinkState {
    private int counter;
    private Task targetTask;
    private String[] recordedLinks;
    private long[] recordingTime;


    /**
     * The number of TermLinks remembered
     */
    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    /**
     * The Task linked. The "target" field in TermLink is not used here.
     */
    public Task getTargetTask() {
        return targetTask;
    }

    public void setTargetTask(Task targetTask) {
        this.targetTask = targetTask;
    }

    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    public String[] getRecordedLinks() {
        return recordedLinks;
    }

    public void setRecordedLinks(String[] recordedLinks) {
        this.recordedLinks = recordedLinks;
    }

    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    public long[] getRecordingTime() {
        return recordingTime;
    }

    public void setRecordingTime(long[] recordingTime) {
        this.recordingTime = recordingTime;
    }
}