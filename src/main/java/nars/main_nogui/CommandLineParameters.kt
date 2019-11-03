package nars.main_nogui

/**
 * The parameters used when the system is invoked from command line
 */
object CommandLineParameters {
    /**
     * Decode the silence level
     *
     * @param args Given arguments
     * @param r    The corresponding reasoner
     */
    @JvmStatic
    fun decode(args: Array<String>, r: ReasonerBatch) {
        var i = 0
        while (i < args.size) {
            var arg = args[i]
            if ("--silence" == arg) {
                ++i
                arg = args[i]
                r.silenceValue.set(Integer.parseInt(arg))
            }
            i++
        }
    }
}