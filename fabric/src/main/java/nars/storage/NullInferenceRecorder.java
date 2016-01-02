package nars.storage;

import nars.io.IInferenceRecorder;

/**
 * Created by jim on 1/12/16.
 */
class NullInferenceRecorder implements IInferenceRecorder {

    @Override
    public void init() {
    }

    @Override
    public void show() {
    }

    @Override
    public void play() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void append(String s) {
    }

    @Override
    public void openLogFile() {
    }

    @Override
    public void closeLogFile() {
    }

    @Override
    public boolean isLogging() {
        return false;
    }
}
