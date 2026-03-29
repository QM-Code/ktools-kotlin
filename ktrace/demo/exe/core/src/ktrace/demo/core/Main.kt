package ktrace.demo.core

import kcli.Parser
import ktrace.Logger
import ktrace.TraceColors
import ktrace.TraceLogger
import ktrace.demo.alpha.AlphaSdk

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val logger = Logger()

        val trace = TraceLogger("core")
        trace.addChannel("app", TraceColors.color("BrightCyan"))
        trace.addChannel("startup", TraceColors.color("BrightYellow"))

        logger.addTraceLogger(trace)
        logger.addTraceLogger(AlphaSdk.getTraceLogger())

        logger.enableChannel(trace, ".app")
        trace.trace("app", "core initialized local trace channels")

        val parser = Parser()
        parser.addInlineParser(logger.makeInlineParser(trace))
        parser.parseOrExit(args)

        trace.trace("app", "cli processing enabled, use --trace for options")
        trace.trace("startup", "testing imported tracing, use --trace '*.*' to view imported channels")
        AlphaSdk.testTraceLoggingChannels()
    }
}
