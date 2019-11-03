/*
 * Instance.java
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

package nars.language

import nars.storage.Memory

/**
 * A Statement about an Instance relation, which is used only in Narsese for I/O,
 * and translated into Inheritance for internal use.
 */
object Instance   {
    /**
     * Try to make a new compound from two components. Called by the inference rules.
     *
     *
     * A {-- B becomes {A} --> B
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
  @JvmStatic     fun make(subject: Term, predicate: Term, memory: Memory): Statement? {
        return Inheritance.make(SetExt.make(subject, memory), predicate, memory)
    }
}