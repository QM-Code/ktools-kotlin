package ktrace.tests

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import ktrace.Logger
import ktrace.OutputOptions
import ktrace.TraceColors
import ktrace.TraceLogger
import ktrace.internal.TraceInternals

object ApiTests {
    fun run() {
        testFormatMessage()
        testWarnLoggingOutput()
        testTraceOutput()
        testSelectorSemantics()
        testConflictingColorsRejected()
        testTraceLoggerAttachmentRejected()
        testTraceChangedSuppressesDuplicates()
        testTraceChangedThreadSafety()
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

        Assertions.expectContains(text, "[tests] [info]", "info output should include namespace/severity")
        Assertions.expectContains(text, "[tests] [warning]", "warn output should include severity")
        Assertions.expectContains(text, "[tests] [error]", "error output should include severity")
        Assertions.expectContains(text, "warn value 7", "warn output should include formatted value")
        Assertions.expectContains(text, "[ApiTests:", "output should include source labels when enabled")
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

    private fun testSelectorSemantics() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("net")
        trace.addChannel("cache")
        trace.addChannel("store")
        trace.addChannel("store.requests")
        logger.addTraceLogger(trace)

        logger.enableChannels("tests.*")
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "tests.net should be enabled by tests.*")
        Assertions.expect(logger.shouldTraceChannel("tests.cache"), "tests.cache should be enabled by tests.*")

        logger.disableChannels("tests.*")
        Assertions.expect(!logger.shouldTraceChannel("tests.net"), "tests.net should be disabled by tests.*")

        logger.enableChannel("tests.net")
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "tests.net should be explicitly re-enabled")
        Assertions.expect(!logger.shouldTraceChannel("tests.cache"), "tests.cache should stay disabled")

        logger.enableChannels("*.*.*.*")
        Assertions.expect(logger.shouldTraceChannel("tests.store.requests"), "depth3 wildcard should enable nested channel")
        Assertions.expect(!logger.shouldTraceChannel("tests.bad name"), "invalid names should never trace")

        logger.enableChannel("tests.missing.child")
        Assertions.expect(!logger.shouldTraceChannel("tests.missing.child"), "missing exact channels should remain disabled")

        logger.enableChannels("tests.missing.child")
        Assertions.expect(!logger.shouldTraceChannel("tests.missing.child"), "unresolved exact selectors should remain disabled")
    }

    private fun testConflictingColorsRejected() {
        val logger = Logger()

        val first = TraceLogger("tests")
        first.addChannel("net")
        logger.addTraceLogger(first)

        val explicitColor = TraceLogger("tests")
        explicitColor.addChannel("net", TraceColors.color("Gold3"))
        logger.addTraceLogger(explicitColor)

        val conflicting = TraceLogger("tests")
        conflicting.addChannel("net", TraceColors.color("Orange3"))
        Assertions.expectThrows<IllegalArgumentException>("conflicting colors should be rejected") {
            logger.addTraceLogger(conflicting)
        }
    }

    private fun testTraceLoggerAttachmentRejected() {
        val firstLogger = Logger()
        val secondLogger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("net")

        firstLogger.addTraceLogger(trace)
        Assertions.expectThrows<IllegalArgumentException>("trace loggers should not attach to multiple loggers") {
            secondLogger.addTraceLogger(trace)
        }
    }

    private fun testTraceChangedSuppressesDuplicates() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("changed")
        logger.addTraceLogger(trace)
        logger.enableChannel("tests.changed")

        val text = TestSupport.captureStdout {
            emitChanged(trace, "key-1")
            emitChanged(trace, "key-1")
            emitChanged(trace, "key-2")
        }

        val count = text.split('\n').count { it.contains("changed") }
        Assertions.expectEquals(count.toLong(), 2L, "traceChanged should suppress duplicate keys from the same call site")
    }

    private fun emitChanged(trace: TraceLogger, key: String) {
        trace.traceChanged("changed", key, "changed")
    }

    private fun testTraceChangedThreadSafety() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("changed")
        logger.addTraceLogger(trace)

        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val workers = List(8) { threadIndex ->
            Thread {
                try {
                    ready.countDown()
                    start.await()
                    repeat(2000) { iteration ->
                        emitChanged(trace, "$threadIndex:${iteration and 1}")
                    }
                } catch (throwable: Throwable) {
                    failure.compareAndSet(null, throwable)
                }
            }
        }

        workers.forEach { it.start() }
        ready.await()
        start.countDown()
        workers.forEach { it.join() }

        val throwable = failure.get()
        if (throwable != null) {
            throw AssertionError("traceChanged should remain safe under concurrent use", throwable)
        }
    }
}
