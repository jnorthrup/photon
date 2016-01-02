package nars.storage;

import nars.io.ExperienceReader;
import nars.io.ExperienceWriter;
import nars.main_nogui.CommandLineParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The main class of the project.
 * <p>
 * Define an application with batch functionality; TODO check duplicated code
 * 
 * <p>
 * Manage the internal working thread. Communicate with Reasoner only.
 */
public class NARSBatch {
	private ReasonerBatch reasoner;
	private transient boolean logging;
	private PrintStream out = System.out;
	private transient boolean dumpLastState = true;
	private static boolean standAlone;

	/**
	 * The entry point of the standalone application.
	 * <p>
	 * Create an instance of the class, then run the {@link #init(String[])} and
	 * {@link #run()} methods.
	 * 
	 * @param args
	 *            optional argument used : one input file
	 */
	public static void main(String args[]) {
		NARSBatch nars = new NARSBatch();
		setStandAlone(true);
		CommandLineParameters.decode(args, nars.getReasoner());
		runInference(nars, args);
		// TODO only if single run ( no reset in between )
		if (nars.isDumpLastState())
			System.out.println("\n==== Dump Last State ====\n"
					+ nars.getReasoner());
	}

	public NARSBatch() {
		init(this);
	}

	/**
	 * non-static equivalent to {@link #main(String[])} : run to completion from
	 * an input file
	 */
	public static void runInference(NARSBatch narsBatch, String args[]) {
		init(narsBatch.getOut(), narsBatch.getReasoner(), args);
		run(narsBatch);
	}

	/** initialize from an input file */
	public static void init(PrintStream out, ReasonerBatch reasoner,
			String[] args) {
		if (0 < args.length) {
			ExperienceReader experienceReader = new ExperienceReader(reasoner);
			experienceReader.openLoadFile(args[0]);
		}
		ReasonerBatch.addOutputChannel(reasoner, new ExperienceWriter(reasoner,
				new PrintWriter(out, true)));
	}

	/**
	 * non-static equivalent to {@link #main(String[])} : run to completion from
	 * a BufferedReader
	 */
	public static void runInference(NARSBatch narsBatch, BufferedReader r,
			BufferedWriter w) {
		init(narsBatch.getReasoner(), r, w);
		run(narsBatch);
	}

	private static void init(ReasonerBatch reasoner, BufferedReader r,
			BufferedWriter w) {
		ExperienceReader experienceReader = new ExperienceReader(reasoner);
		experienceReader.setBufferedReader(r);
		ReasonerBatch.addOutputChannel(reasoner, new ExperienceWriter(reasoner,
				new PrintWriter(w, true)));
	}

	/**
	 * Initialize the system at the control center.
	 * <p>
	 * Can instantiate multiple reasoners
	 * 
	 * @param narsBatch
	 */
	public static void init(NARSBatch narsBatch) {
		narsBatch.setReasoner(new ReasonerBatchBuilder().createReasonerBatch());
	}

	/**
	 * Run to completion: repeatedly execute NARS working cycle, until Inputs
	 * are Finished, or 1000 steps. This method is called when the Runnable's
	 * thread is started.
	 * 
	 * @param narsBatch
	 */
	public static void run(NARSBatch narsBatch) {
		while (true) {
			narsBatch.log("NARSBatch.run():" + " step "
					+ ReasonerBatch.getTime(narsBatch.getReasoner().getClock())
					+ " " + narsBatch.getReasoner().isFinishedInputs());
			ReasonerBatch.tick(narsBatch.getReasoner());
			narsBatch.log("NARSBatch.run(): after tick" + " step "
					+ ReasonerBatch.getTime(narsBatch.getReasoner().getClock())
					+ " " + narsBatch.getReasoner().isFinishedInputs());
			if (narsBatch.getReasoner().isFinishedInputs()
					|| 1000 == ReasonerBatch.getTime(narsBatch.getReasoner()
							.getClock()))
				break;
		}
	}

	public static void setPrintStream(NARSBatch narsBatch, PrintStream out) {
		narsBatch.setOut(out);
	}

	private void log(String mess) {
		if (isLogging())
			System.out.println("/ " + mess);
	}

	/** The reasoner */
	public ReasonerBatch getReasoner() {
		return reasoner;
	}

	/**
	 * Flag to distinguish the two running modes of the project.
	 */
	/**
	 * Whether the project running as an application.
	 * 
	 * @return true for application; false for applet.
	 */
	public static boolean isStandAlone() {
		return standAlone;
	}

	public static void setStandAlone(boolean standAlone) {
		NARSBatch.standAlone = standAlone;
	}

	public boolean isDumpLastState() {
		return dumpLastState;
	}

	public void setDumpLastState(boolean dumpLastState) {
		this.dumpLastState = dumpLastState;
	}

	public void setReasoner(ReasonerBatch reasoner) {
		this.reasoner = reasoner;
	}

	public boolean isLogging() {
		return logging;
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	public PrintStream getOut() {
		return out;
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}
}
