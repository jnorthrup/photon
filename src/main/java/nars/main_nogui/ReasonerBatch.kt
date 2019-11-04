package nars.main_nogui

import nars.entity.Stamp
import nars.entity.Task
import nars.io.InputChannel
import nars.io.OutputChannel
import nars.io.StringParser
import nars.io.Symbols
import nars.storage.Memory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 */
class ReasonerBatch {
    /**
     * The name of the reasoner
     */
    protected var name: String? = null
    /**
     * The memory of the reasoner
     */
    var memory: Memory
        protected set
    /**
     * The input channels of the reasoner
     */
    protected var inputChannels: MutableCollection<InputChannel>
    /**
     * The output channels of the reasoner
     */
    protected var outputChannels: MutableCollection<OutputChannel>
    /**
     * Get the current time from the clock Called in [nars.entity.Stamp]
     *
     * @return The current time
     */
    /**
     * System clock, relatively defined to guarantee the repeatability of
     * behaviors
     */
    var time: Long = 0
        private set
    /**
     * Flag for running continuously
     */
    private var running = false
    /**
     * The remaining number of steps to be carried out (walk mode)
     */
    private var walkingSteps = 0
    /**
     * determines the end of [NARSBatch] program
     */
    /**
     * determines the end of [NARSBatch] program (set but not accessed in
     * this class)
     */
    var isFinishedInputs = false
        private set
    /**
     * @return System clock : number of cycles since last output
     */
    /**
     * set System clock : number of cycles since last output
     */
    /**
     * System clock - number of cycles since last output
     */
    var timer: Long = 0
        private set
    /**
     * Report Silence Level
     */
    val silenceValue = AtomicInteger(Parameters.SILENT_LEVEL)

    /**
     * Reset the system with an empty memory and reset clock. Called locally and
     * from [MainWindow].
     */
    private fun reset() {
        running = false
        walkingSteps = 0
        time = 0
        memory.clear()
        Stamp.init()
//	    timer = 0;

    }

    /**
     *
     */
    fun addInputChannel(channel: InputChannel) {
        inputChannels.add(channel)
    }

    /**
     *
     */
    fun addOutputChannel(channel: OutputChannel) {
        outputChannels.add(channel)
    }

    /**
     * Will carry the inference process for a certain number of steps
     *
     * @param n The number of inference steps to be carried
     */
    fun walk(n: Int) {
        walkingSteps = n
    }

    /**
     * A clock tick. Run one working workCycle or read input. Called from NARS
     * only.
     */
    fun tick() {
        doTick()
    }

    /**
     *
     */
    fun doTick() {
        if (walkingSteps == 0) {
            var reasonerShouldRun = false
            for (channelIn in inputChannels) {
                reasonerShouldRun = (reasonerShouldRun
                        || channelIn.nextInput())
            }
            isFinishedInputs = !reasonerShouldRun
        }
        // forward to output Channels


        val output: MutableList<String?> = memory.exportStrings
        if (output.isNotEmpty()) {
            for (channelOut in outputChannels) {
                channelOut.nextOutput(output)
            }
            output.clear()    // this will trigger display the current value of timer in Memory.report()
        }
        if (running || walkingSteps > 0) {
            time++
            tickTimer()
            memory.workCycle(time)
            if (walkingSteps > 0) {
                walkingSteps--
            }
        }
    }

    /**
     * To process a line of input text
     *
     * @param text
     */
    fun textInputLine(text: String) {
        if (text.isEmpty()) {
            return
        }
        val c = text[0]
        if (c == Symbols.RESET_MARK) {
            reset()
            memory.exportStrings.add(text)
        } else if (c != Symbols.COMMENT_MARK) {
            // read NARS language or an integer : TODO duplicated code

            try {
                val i = Integer.parseInt(text)
                walk(i)
            } catch (e: NumberFormatException) {
                val task: Task? = StringParser.parseExperience(StringBuffer(text), memory, time)
                if (task != null) {
                    memory.inputTask(task)
                }
            }
        }
    }

    override fun toString(): String {
        return memory.toString()
    }

    /**
     * To get the timer value and then to
     * reset it by [.initTimer];
     * plays the same role as [nars.gui.MainWindow.updateTimer]
     *
     * @return The previous timer value
     */
    fun updateTimer(): Long {
        val i = timer
        initTimer()
        return i
    }

    /**
     * Reset timer;
     * plays the same role as [nars.gui.MainWindow.initTimer]
     */
    fun initTimer() {
        timer = 0
    }

    /**
     * Update timer
     */
    private fun tickTimer() {
        timer += 1
    }

    companion object {
        /**
         * global DEBUG print switch
         */
        const val DEBUG = false
    }

    init {
        memory = Memory(this)
        inputChannels = ArrayList()
        outputChannels = ArrayList()
    }
}