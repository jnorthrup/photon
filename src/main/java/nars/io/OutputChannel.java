/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nars.io;

import java.util.List;

/**
 * An interface to be implemented in all output channel
 */
public interface OutputChannel {
    public void nextOutput(List<String> output);

}
