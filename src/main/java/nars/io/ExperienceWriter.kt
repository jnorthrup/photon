/*
 * ExperienceReader.java
 *
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
package nars.io

import nars.main_nogui.ReasonerBatch
import java.io.PrintWriter

/**
 * To read and write experience as Task streams
 */
class ExperienceWriter
/**
 * Default constructor
 *
 * @param reasoner
 */(private val reasoner: ReasonerBatch) : OutputChannel {
    /**
     * Input experience from a file
     */
    private var outExp: PrintWriter? = null

    constructor(reasoner: ReasonerBatch, outExp: PrintWriter?) : this(reasoner) {
        this.outExp = outExp
    }

    /**
     * Process the next chunk of output data
     *
     * @param lines The text to be displayed
     */

    override fun nextOutput(lines: List<String?>?) {
        if (outExp != null) {
            for (line in lines!!) {
                outExp!!.println(line)
            }
        }
    }

}