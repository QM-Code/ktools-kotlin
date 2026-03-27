package ktrace.demo.beta

import ktrace.TraceColors
import ktrace.TraceLogger

object BetaSdk {
    private val sharedTraceLogger: TraceLogger by lazy {
        TraceLogger("beta").also { logger ->
            logger.addChannel("io", TraceColors.color("MediumSpringGreen"))
            logger.addChannel("scheduler", TraceColors.color("Orange3"))
        }
    }

    fun getTraceLogger(): TraceLogger = sharedTraceLogger

    fun testTraceLoggingChannels() {
        val trace = getTraceLogger()
        trace.trace("io", "beta trace test on channel 'io'")
        trace.trace("scheduler", "beta trace test on channel 'scheduler'")
    }
}
