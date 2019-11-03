package nars.main_nogui;

import kotlin.jvm.JvmStatic;

/**
 * The parameters used when the system is invoked from command line
 */
public class CommandLineParameters {

    /**
     * Decode the silence level
     *
     * @param args Given arguments
     * @param r    The corresponding reasoner
     */@JvmStatic
    public static void decode(String[] args, ReasonerBatch r) {
        for (var i = 0; i < args.length; i++) {
            var arg = args[i];
            if ("--silence".equals(arg)) {
                ++i;
                arg = args[i];
                r.getSilenceValue().set(Integer.parseInt(arg));
            }
        }
    }

}
