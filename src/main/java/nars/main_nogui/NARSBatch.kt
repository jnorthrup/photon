/*
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.main_nogui

import nars.io.ExperienceReader
import nars.io.ExperienceWriter
import java.io.PrintStream
import java.io.PrintWriter

/**
 * The main class of the project.
 *
 *
 * Define an application with batch functionality; TODO check duplicated code
 * with [nars.main.NARS]
 *
 *
 * Manage the internal working thread. Communicate with Reasoner only.
 */
class NARSBatch {
    /**
     * The reasoner
     */
    var reasoner: ReasonerBatch? = null
        internal set
    private val out: PrintStream? = System.out
    private val dumpLastState = true
    /**
     * non-static equivalent to [.main] : run to completion from
     * an input file
     */
    fun runInference(args: Array<String>) {
        init(args)
        run()
    }

    /**
     * initialize from an input file
     */
    fun init(args: Array<String>) {
        if (args.size > 0) {
            val experienceReader = ExperienceReader(reasoner!!)
            experienceReader.openLoadFile(args[0])
        }
        reasoner!!.addOutputChannel(ExperienceWriter(reasoner!!,
                PrintWriter(out, true)))
    }

    /**
     * Initialize the system at the control center.
     *
     *
     * Can instantiate multiple reasoners
     */
    fun init() {
        reasoner = ReasonerBatch()
    }

    /**
     * Run to completion: repeatedly execute NARS working cycle, until Inputs
     * are Finished, or 1000 steps. This method is called when the Runnable's
     * thread is started.
     */
    fun run() {
        while (true) {
            log("NARSBatch.run():"
                    + " step " + reasoner!!.time
                    .toString() + " " + reasoner!!.isFinishedInputs)
            reasoner!!.tick()
            log("NARSBatch.run(): after tick"
                    + " step " + reasoner!!.time
                    .toString() + " " + reasoner!!.isFinishedInputs)
            if (reasoner!!.isFinishedInputs
                    || reasoner!!.time == 1000L) {
                break
            }
        }
    }

    private fun log(mess: String) {
        if (false) {
            println("/ $mess")
        }
    }

    companion object {
        /**
         * Whether the project running as an application.
         *
         * @return true for application; false for applet.
         */
        /**
         * Flag to distinguish the two running modes of the project.
         */
        @set:JvmStatic
        var standAlone = false

        /**
         * The entry point of the standalone application.
         *
         *
         * Create an instance of the class, then run the [.init] and
         * [.run] methods.
         *
         * @param args optional argument used : one input file
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val nars = NARSBatch()
            standAlone = true
            CommandLineParameters.decode(args, nars.reasoner!!)
            nars.runInference(args)
            // TODO only if single run ( no reset in between )


            if (nars.dumpLastState) {
                println("\n==== Dump Last State ====\n"
                        + nars.reasoner.toString())
            }
        }

    }

    init {
        init()
    }
}