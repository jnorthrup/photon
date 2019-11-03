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
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * To read and write experience as Task streams
 */
class ExperienceReader(
        /**
         * Reference to the reasoner
         */
        private val reasoner: ReasonerBatch) : InputChannel {
    /**
     * Input experience from a file
     */
    private var inExp: BufferedReader?
    /**
     * Remaining working cycles before reading the next line
     */
    private var timer = 0

    /**
     * Open an input experience file from given file Path
     *
     * @param filePath File to be read as experience
     */
    fun openLoadFile(filePath: String?) {
        try {
            inExp = BufferedReader(FileReader(filePath))
        } catch (ex: IOException) {
            println("i/o error: " + ex.message)
        }
        reasoner.addInputChannel(this)
    }

    /**
     * Process the next chunk of input data;
     * TODO some duplicated code with
     * [nars.gui.InputWindow.nextInput]
     *
     * @return Whether the input channel should be checked again
     */

    override fun nextInput(): Boolean {
        if (timer > 0) {
            timer--
            return true
        }
        if (inExp == null) {
            return false
        }
        var line: String? = null
        while (timer == 0) {
            try {
                line = inExp!!.readLine()
                if (line == null) {
                    inExp!!.close()
                    inExp = null
                    return false
                }
            } catch (ex: IOException) {
                println("i/o error: " + ex.message)
            }
            line = line!!.trim { it <= ' ' }
// read NARS language or an integer
            if (line.isNotEmpty()) {
                try {
                    timer = Integer.parseInt(line)
                    reasoner.walk(timer)
                } catch (e: NumberFormatException) {
                    reasoner.textInputLine(line)
                }
            }
        }
        return true
    }

    /**
     * Default constructor
     *
     * @param reasoner Backward link to the reasoner
     */

    init {
        inExp = null
    }
}