/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.io

/**
 * An interface to be implemented in all output channel
 */
interface OutputChannel {
    fun nextOutput(output: List<String?>?)
}