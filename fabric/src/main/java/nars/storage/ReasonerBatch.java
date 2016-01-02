package nars.storage;

import nars.entity.Stamp;
import nars.entity.Task;
import nars.io.InputChannel;
import nars.io.OutputChannel;
import nars.io.StringParser;
import nars.io.Symbols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jim on 1/12/16.
 */
public class ReasonerBatch {
    private static final boolean DEBUG = false;
    private String name;
    private Memory memory;
    private List<InputChannel> inputChannels;
    private List<OutputChannel> outputChannels;
    private long clock;
    private boolean running;
    private int walkingSteps;
    private boolean finishedInputs;
    private long timer;

    private final AtomicInteger silenceValue = new AtomicInteger(Parameters.SILENT_LEVEL);

    public ReasonerBatch() {
        setMemory(new Memory(this));
        setInputChannels(new ArrayList<InputChannel>());
        setOutputChannels(new ArrayList<OutputChannel>());
    }

    /**
     * Parse a line of input experience
     * <p>
     * called from ExperienceIO.loadLine
     *
     * @param buffer The line to be parsed
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    public static Task parseExperience(StringBuffer buffer, Memory memory, long time) {
        int i = buffer.indexOf(Symbols.PREFIX_MARK + "");
        if (0 < i) {
            String prefix = buffer.substring(0, i).trim();
            if (prefix.equals(Symbols.OUTPUT_LINE)) {
                return null;
            } else if (prefix.equals(Symbols.INPUT_LINE)) {
                buffer.delete(0, i + 1);
            }
        }
        char c = buffer.charAt(buffer.length() - 1);
        if (Symbols.STAMP_CLOSER == c) {
            int j = buffer.lastIndexOf(Symbols.STAMP_OPENER + "");
            buffer.delete(j - 1, buffer.length());
        }
        return StringParser.parseTask(buffer.toString().trim(), memory, time);
    }

    /**
     * Reset the system with an empty memory and reset clock. Called locally and from MainWindow.
     *
     * @param reasonerBatch
     */
    public static void reset(ReasonerBatch reasonerBatch) {
        reasonerBatch.setRunning(false);
        reasonerBatch.setWalkingSteps(0);
        reasonerBatch.setClock(0);
        Memory.init(reasonerBatch.getMemory());
        Stamp.init();
        //	    timer = 0;
    }

    /**
     * global DEBUG print switch
     */
    public static boolean isDEBUG() {
        return DEBUG;
    }

    /**
     * The memory of the reasoner
     */
    public Memory getMemory() {
        return memory;
    }

    public static void addInputChannel(ReasonerBatch reasonerBatch, InputChannel channel) {
        reasonerBatch.getInputChannels().add(channel);
    }

    public static void removeInputChannel(ReasonerBatch reasonerBatch, InputChannel channel) {
        reasonerBatch.getInputChannels().remove(channel);
    }

    public static void addOutputChannel(ReasonerBatch reasonerBatch, OutputChannel channel) {
        reasonerBatch.getOutputChannels().add(channel);
    }

    public static void removeOutputChannel(ReasonerBatch reasonerBatch, OutputChannel channel) {
        reasonerBatch.getOutputChannels().remove(channel);
    }

    /**
     * Get the current time from the clock
     * Called in nars.entity.Stamp
     *
     * @param clock
     * @return The current time
     */
    public static long getTime(long clock) {
        return clock;
    }

    /**
     * Start the inference process
     *
     * @param reasonerBatch
     */
    public static void run(ReasonerBatch reasonerBatch) {
        reasonerBatch.setRunning(true);
    }

    /**
     * Will carry the inference process for a certain number of steps
     *
     * @param reasonerBatch
     * @param n             The number of inference steps to be carried
     */
    public static void walk(ReasonerBatch reasonerBatch, int n) {
        reasonerBatch.setWalkingSteps(n);
    }

    /**
     * Stop the inference process
     *
     * @param reasonerBatch
     */
    public static void stop(ReasonerBatch reasonerBatch) {
        reasonerBatch.setRunning(false);
    }

    /**
     * A clock tick. Run one working workCycle or read input. Called from NARS only.
     *
     * @param reasonerBatch
     */
    public static void tick(ReasonerBatch reasonerBatch) {
        doTick(reasonerBatch);
    }

    public static void doTick(ReasonerBatch reasonerBatch) {
        if (isDEBUG() && (reasonerBatch.isRunning() || 0 < reasonerBatch.getWalkingSteps() || !reasonerBatch.isFinishedInputs())) {
            System.out.println("// doTick: "
                    + "walkingSteps " + reasonerBatch.getWalkingSteps()
                    + ", clock " + reasonerBatch.getClock()
                    + ", getTimer " + reasonerBatch.getTimer()
                    + "\n//    memory.getExportStrings() " + reasonerBatch.getMemory().getExportStrings()
            );
            System.out.flush();
        }
        switch (reasonerBatch.getWalkingSteps()) {
            case 0:
                boolean reasonerShouldRun = false;
                for (InputChannel channelIn : reasonerBatch.getInputChannels()) {
                    reasonerShouldRun = reasonerShouldRun ||
                            channelIn.nextInput();
                }
                reasonerBatch.setFinishedInputs(!reasonerShouldRun);
                break;
        }
        // forward to output Channels
        List<String> output = reasonerBatch.getMemory().getExportStrings();
        if (!output.isEmpty()) {
            for (OutputChannel channelOut : reasonerBatch.getOutputChannels()) {
                channelOut.nextOutput(output);
            }
            output.clear();    // this will trigger display the current value of timer in Memory.report()
        }
        if (reasonerBatch.isRunning() || 0 < reasonerBatch.getWalkingSteps()) {
            reasonerBatch.setClock(reasonerBatch.getClock() + 1);
            ReasonerBatch.tickTimer(reasonerBatch);
            Memory.workCycle(reasonerBatch.getMemory(), reasonerBatch.getClock());
            if (0 < reasonerBatch.getWalkingSteps()) {
                reasonerBatch.setWalkingSteps(reasonerBatch.getWalkingSteps() - 1);
            }
        }
    }

    /**
     * determines the end of {@link NARSBatch} program
     * (set but not accessed in this class)
     */ /**
     * determines the end of {@link NARSBatch} program
     */
    public boolean isFinishedInputs() {
        return finishedInputs;
    }

    /**
     * To process a line of input text
     *
     * @param reasonerBatch
     * @param text
     */
    public static void textInputLine(ReasonerBatch reasonerBatch, String text) {
        if (!text.isEmpty()) {
            char c = text.charAt(0);
            switch (c) {
                case Symbols.RESET_MARK:
                    reset(reasonerBatch);
                    reasonerBatch.getMemory().getExportStrings().add(text);
                    break;
                case Symbols.COMMENT_MARK:
                    return;
                default:
                    // read NARS language or an integer : TODO duplicated code
                    try {
                        int i = Integer.parseInt(text);
                        walk(reasonerBatch, i);
                    } catch (NumberFormatException e) {
                        Task task = parseExperience(new StringBuffer(text), reasonerBatch.getMemory(), reasonerBatch.getClock());
                        if (null != task) {
                            Memory.inputTask(reasonerBatch.getMemory(), task);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return asString(this);
    }

    private static String asString(ReasonerBatch reasonerBatch) {
        return Memory.toString(reasonerBatch.getMemory());
    }

    /**
     * Report Silence Level
     *
     * @param silenceValue
     */
    public static AtomicInteger getSilenceValue(AtomicInteger silenceValue) {
        return silenceValue;
    }

    /**
     * To get the timer value and then to reset it
     *
     * @return The previous timer value
     * @param reasonerBatch
     */
    public static long updateTimer(ReasonerBatch reasonerBatch) {
        long i = reasonerBatch.getTimer();
        initTimer(reasonerBatch);
        return i;
    }

    public static void initTimer(ReasonerBatch reasonerBatch) {
        reasonerBatch.setTimer(0);
    }

    /**
     * Update timer
     * @param reasonerBatch
     */
    public static void tickTimer(ReasonerBatch reasonerBatch) {
        reasonerBatch.setTimer(reasonerBatch.getTimer() + 1);
    }

    /**
     * System clock - number of cycles since last output
     */
    public long getTimer() {
        return timer;
    }

    private void setTimer(long timer) {
        this.timer = timer;
    }

    public AtomicInteger getSilenceValue() {
        return silenceValue;
    }

    /**
     * System clock, relatively defined to guarantee the repeatability of behaviors
     */
    public long getClock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    /**
     * The name of the reasoner
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    /**
     * The input channels of the reasoner
     */
    public Collection<InputChannel> getInputChannels() {
        return inputChannels;
    }

    public void setInputChannels(List<InputChannel> inputChannels) {
        this.inputChannels = inputChannels;
    }

    /**
     * The output channels of the reasoner
     */
    public Collection<OutputChannel> getOutputChannels() {
        return outputChannels;
    }

    public void setOutputChannels(List<OutputChannel> outputChannels) {
        this.outputChannels = outputChannels;
    }

    /**
     * Flag for running continuously
     */
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * The remaining number of steps to be carried out (walk mode)
     */
    public int getWalkingSteps() {
        return walkingSteps;
    }

    public void setWalkingSteps(int walkingSteps) {
        this.walkingSteps = walkingSteps;
    }

    public void setFinishedInputs(boolean finishedInputs) {
        this.finishedInputs = finishedInputs;
    }
}
