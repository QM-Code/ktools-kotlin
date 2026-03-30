package ktrace.tests

import ktrace.Logger
import ktrace.OutputOptions
import ktrace.TraceColors
import ktrace.TraceLogger
import ktrace.internal.TraceInternals

object FormatApiTests {
    fun run() {
        testFormatMessage()
        testWarnLoggingOutput()
        testTraceOutput()
    }

    private fun testFormatMessage() {
        Assertions.expectEquals(
            TraceInternals.formatMessage("value {} {}", 7, "done"),
            "value 7 done",
            "formatMessage should replace ordered placeholders",
        )
        Assertions.expectEquals(
            TraceInternals.formatMessage("escaped {{}}"),
            "escaped {}",
            "formatMessage should preserve escaped braces",
        )
        Assertions.expectEquals(
            TraceInternals.formatMessage("bool {}", true),
            "bool true",
            "formatMessage should stringify booleans",
        )

        Assertions.expectThrows<IllegalArgumentException>("missing arguments should fail") {
            TraceInternals.formatMessage("value {} {}", 7)
        }
        Assertions.expectThrows<IllegalArgumentException>("extra arguments should fail") {
            TraceInternals.formatMessage("value", 7)
        }
        Assertions.expectThrows<IllegalArgumentException>("unterminated open brace should fail") {
            TraceInternals.formatMessage("{")
        }
        Assertions.expectThrows<IllegalArgumentException>("unmatched closing brace should fail") {
            TraceInternals.formatMessage("}")
        }
        Assertions.expectThrows<IllegalArgumentException>("unsupported tokens should fail") {
            TraceInternals.formatMessage("{:x}", 7)
        }
    }

    private fun testWarnLoggingOutput() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        logger.addTraceLogger(trace)
        logger.setOutputOptions(OutputOptions(true, true, false, false))

        val text = TestSupport.captureStdout {
            trace.info("info message")
            trace.warn("warn value {}", 7)
            trace.error("error message")
        }

        Assertions.expect(text.startsWith("[tests] [info] "), "info output should start with namespace then severity")
        Assertions.expectContains(text, "\n[tests] [warning] ", "warn output should include severity")
        Assertions.expectContains(text, "\n[tests] [error] ", "error output should include severity")
        Assertions.expectContains(text, "warn value 7", "warn output should include formatted value")
        Assertions.expectContains(text, "[FormatApiTests:", "output should include source labels when enabled")
        Assertions.expectNotContains(text, "[info] [tests] [info]", "info output should not duplicate severity prefixes")
        Assertions.expectNotContains(text, "[warning] [tests] [warning]", "warn output should not duplicate severity prefixes")
        Assertions.expectNotContains(text, "[error] [tests] [error]", "error output should not duplicate severity prefixes")
    }

    private fun testTraceOutput() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("trace", TraceColors.color("Gold3"))
        logger.addTraceLogger(trace)
        logger.enableChannel("tests.trace")

        val text = TestSupport.captureStdout {
            trace.trace("trace", "member {} {{ok}}", 42)
        }
        Assertions.expectContains(text, "[tests] [trace]", "trace output should include channel")
        Assertions.expectContains(text, "member 42 {ok}", "trace output should format messages")
    }
}
