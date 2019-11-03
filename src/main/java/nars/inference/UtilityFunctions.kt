/*
 * UtilityFunctions.java
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
package nars.inference

import nars.main_nogui.Parameters
import kotlin.math.pow

/**
 * Common functions on real numbers, mostly in [0,1].
 */
open class UtilityFunctions {
    companion object {
        /**
         * A function where the output is conjunctively determined by the inputs
         *
         * @param arr The inputs, each in [0, 1]
         * @return The output that is no larger than each input
         */
     @JvmStatic      fun and(vararg arr: Float): Float {
            var product = 1f
            arr.forEach { f -> product *= f }
            return product
        }

        /**
         * A function where the output is disjunctively determined by the inputs
         *
         * @param arr The inputs, each in [0, 1]
         * @return The output that is no smaller than each input
         */
     @JvmStatic      fun or(vararg arr: Float): Float {
            var product = 1f
            arr.forEach { f ->
                product *= 1 - f
            }
            return 1 - product
        }

        /**
         * A function where the output is the arithmetic average the inputs
         *
         * @param arr The inputs, each in [0, 1]
         * @return The arithmetic average the inputs
         */
     @JvmStatic      fun aveAri(vararg arr: Float)  = arr.average()

        /**
         * A function where the output is the geometric average the inputs
         *
         * @param arr The inputs, each in [0, 1]
         * @return The geometric average the inputs
         */
       @JvmStatic    fun aveGeo(vararg arr: Float): Float {
            var product = 1f
            for (f in arr) {
                product *= f
            }
            return product.toDouble().pow(1.00 / arr.size).toFloat()
        }

        /**
         * A function to convert weight to confidence
         *
         * @param w Weight of evidence, a non-negative real number
         * @return The corresponding confidence, in [0, 1)
         */
       @JvmStatic    fun w2c(w: Float): Float {
            return w / (w + Parameters.HORIZON)
        }

        /**
         * A function to convert confidence to weight
         *
         * @param c confidence, in [0, 1)
         * @return The corresponding weight of evidence, a non-negative real number
         */
  @JvmStatic      fun c2w(c: Float): Float {
            return Parameters.HORIZON * c / (1 - c)
        }
    }
}