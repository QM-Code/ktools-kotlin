package ktrace.demo.alpha

import ktrace.TraceColors
import ktrace.TraceLogger

object AlphaSdk {
    private val sharedTraceLogger: TraceLogger by lazy {
        TraceLogger("alpha").also { logger ->
            logger.addChannel("net", TraceColors.color("DeepSkyBlue1"))
            logger.addChannel("net.alpha")
            logger.addChannel("net.beta")
            logger.addChannel("net.gamma")
            logger.addChannel("net.gamma.deep")
            logger.addChannel("cache", TraceColors.color("Gold3"))
            logger.addChannel("cache.gamma", TraceColors.color("Gold3"))
            logger.addChannel("cache.delta")
            logger.addChannel("cache.special", TraceColors.color("Red"))
        }
    }

    fun getTraceLogger(): TraceLogger = sharedTraceLogger

    fun testTraceLoggingChannels() {
        val trace = getTraceLogger()
        trace.trace("net", "testing...")
        trace.trace("net.alpha", "testing...")
        trace.trace("net.beta", "testing...")
        trace.trace("net.gamma", "testing...")
        trace.trace("net.gamma.deep", "testing...")
        trace.trace("cache", "testing...")
        trace.trace("cache.gamma", "testing...")
        trace.trace("cache.delta", "testing...")
        trace.trace("cache.special", "testing...")
    }

    fun testStandardLoggingChannels() {
        val trace = getTraceLogger()
        trace.info("testing...")
        trace.warn("testing...")
        trace.error("testing...")
    }
}
