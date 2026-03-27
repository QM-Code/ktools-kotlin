package ktrace.demo.omega

import kcli.Parser
import ktrace.Logger
import ktrace.TraceColors
import ktrace.TraceLogger
import ktrace.demo.alpha.AlphaSdk
import ktrace.demo.beta.BetaSdk
import ktrace.demo.common.DemoSupport.withProgram
import ktrace.demo.gamma.GammaSdk

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val logger = Logger()

        val trace = TraceLogger("omega")
        trace.addChannel("app", TraceColors.color("BrightCyan"))
        trace.addChannel("orchestrator", TraceColors.color("BrightYellow"))
        trace.addChannel("deep")
        trace.addChannel("deep.branch")
        trace.addChannel("deep.branch.leaf", TraceColors.color("LightSalmon1"))

        logger.addTraceLogger(trace)
        logger.addTraceLogger(AlphaSdk.getTraceLogger())
        logger.addTraceLogger(BetaSdk.getTraceLogger())
        logger.addTraceLogger(GammaSdk.getTraceLogger())

        logger.enableChannel(trace, ".app")
        trace.trace("app", "omega initialized local trace channels")
        logger.disableChannel(trace, ".app")

        val parser = Parser()
        parser.addInlineParser(logger.makeInlineParser(trace))
        val argv = withProgram("ktrace_demo_omega", args)
        parser.parseOrExit(argv.size, argv)

        trace.trace("app", "cli processing enabled, use --trace for options")
        trace.trace("app", "testing external tracing, use --trace '*.*' to view top-level channels")
        trace.trace("deep.branch.leaf", "omega trace test on channel 'deep.branch.leaf'")
        AlphaSdk.testTraceLoggingChannels()
        BetaSdk.testTraceLoggingChannels()
        GammaSdk.testTraceLoggingChannels()
        AlphaSdk.testStandardLoggingChannels()
        trace.trace("orchestrator", "omega completed imported SDK trace checks")
        trace.info("testing...")
        trace.warn("testing...")
        trace.error("testing...")
    }
}
