package nars.entity

class TasklinkState {
    /**
     * The number of TermLinks remembered
     */
    var counter = 0
    /**
     * The Task linked. The "target" field in TermLink is not used here.
     */
    var targetTask: Task? = null
    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    lateinit var recordedLinks: Array<String?>
    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    lateinit var recordingTime: LongArray

}