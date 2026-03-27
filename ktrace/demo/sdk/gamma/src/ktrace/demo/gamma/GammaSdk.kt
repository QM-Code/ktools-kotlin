package ktrace.demo.gamma

import ktrace.TraceColors
import ktrace.TraceLogger

object GammaSdk {
    private val sharedTraceLogger: TraceLogger by lazy {
        TraceLogger("gamma").also { logger ->
            logger.addChannel("physics", TraceColors.color("MediumOrchid1"))
            logger.addChannel("metrics", TraceColors.color("LightSkyBlue1"))
        }
    }

    fun getTraceLogger(): TraceLogger = sharedTraceLogger

    fun testTraceLoggingChannels() {
        val trace = getTraceLogger()
        trace.trace("physics", "gamma trace test on channel 'physics'")
        trace.trace("metrics", "gamma trace test on channel 'metrics'")
    }
}
