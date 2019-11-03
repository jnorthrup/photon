package nars.io

/**
 *
 */
interface IInferenceRecorder {
    /**
     * Initialize the window and the file
     */
    fun init()

    /**
     * Show the window
     */
    fun show()

    /**
     * Begin the display
     */
    fun play()

    /**
     * Stop the display
     */
    fun stop()

    /**
     * Add new text to display
     *
     * @param s The line to be displayed
     */
    fun append(s: String?)

    /**
     * Open the log file
     */
    fun openLogFile()

    /**
     * Close the log file
     */
    fun closeLogFile()

    /**
     * Check file logging
     *
     * @return If the file logging is going on
     */
    val isLogging: Boolean
}