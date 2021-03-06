package nars.main_nogui;

import nars.storage.ReasonerBatch;

public class CommandLineParameters {

	public static void decode(String[] args, ReasonerBatch r) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("--silence".equals(arg)) {
				arg = args[++i];
				ReasonerBatch.getSilenceValue(r.getSilenceValue()).set(
						Integer.parseInt(arg));
			}
		}
	}

	public static boolean isReallyFile(String param) {
		return !"--silence".equals(param);
	}
}
