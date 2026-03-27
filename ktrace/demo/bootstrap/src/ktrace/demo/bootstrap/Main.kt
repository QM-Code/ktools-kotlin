package ktrace.demo.bootstrap

import ktrace.Logger
import ktrace.TraceColors
import ktrace.TraceLogger

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val logger = Logger()
        val trace = TraceLogger("bootstrap")
        trace.addChannel("bootstrap", TraceColors.color("BrightGreen"))
        logger.addTraceLogger(trace)
        logger.enableChannel(trace, ".bootstrap")
        trace.trace("bootstrap", "ktrace bootstrap compile/link check")
        println("Bootstrap succeeded.")
    }
}
