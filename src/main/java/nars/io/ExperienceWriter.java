
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
package nars.io;

import nars.main_nogui.ReasonerBatch;

import java.io.PrintWriter;
import java.util.List;

/**
 * To read and write experience as Task streams
 */
public class ExperienceWriter implements OutputChannel {

    private ReasonerBatch reasoner;
    /**
     * Input experience from a file
     */
    private PrintWriter outExp;

    /**
     * Default constructor
     *
     * @param reasoner
     */
    public ExperienceWriter(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
    }

    public ExperienceWriter(ReasonerBatch reasoner, PrintWriter outExp) {
        this(reasoner);
        this.outExp = outExp;
    }

    /**
     * Process the next chunk of output data
     *
     * @param lines The text to be displayed
     */

    @Override
    public void nextOutput(List<String> lines) {
        if (outExp != null) {
            for (String line : lines) {
                outExp.println(line.toString());
            }
        }
    }
}
