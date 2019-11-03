package nars.storage

import nars.io.IInferenceRecorder

internal class NullInferenceRecorder : IInferenceRecorder {
    override fun init() {}
    override fun show() {}
    override fun play() {}
    override fun stop() {}
    override fun append(s: String?) {}
    override fun openLogFile() {}
    override fun closeLogFile() {}
    override val isLogging: Boolean
        get() = false
}