package ktrace.tests

import ktrace.Logger
import ktrace.TraceColors
import ktrace.TraceLogger

object LoggerApiTests {
    fun run() {
        testSelectorSemantics()
        testLocalNamespaceOverloads()
        testConflictingColorsRejected()
        testTraceLoggerAttachmentRejected()
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
        Assertions.expect(!logger.shouldTraceChannel("tests.cache"), "tests.cache should be disabled by tests.*")

        logger.enableChannel("tests.net")
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "tests.net should be explicitly re-enabled")
        Assertions.expect(!logger.shouldTraceChannel("tests.cache"), "tests.cache should stay disabled")

        logger.disableChannel("tests.net")
        Assertions.expect(!logger.shouldTraceChannel("tests.net"), "explicit disable should turn tests.net back off")

        logger.enableChannels("*.*.*")
        Assertions.expect(logger.shouldTraceChannel("tests.store.requests"), "depth-3 wildcard should enable nested channel")
        Assertions.expect(logger.shouldTraceChannel("tests.net"), "top-level channels should still match depth-3 wildcard")
        Assertions.expect(!logger.shouldTraceChannel("tests.bad name"), "invalid names should never trace")

        logger.enableChannel("tests.missing.child")
        Assertions.expect(!logger.shouldTraceChannel("tests.missing.child"), "missing exact channels should remain disabled")

        logger.enableChannels("tests.missing.child")
        Assertions.expect(!logger.shouldTraceChannel("tests.missing.child"), "unresolved exact selectors should remain disabled")
    }

    private fun testLocalNamespaceOverloads() {
        val logger = Logger()
        val trace = TraceLogger("tests")
        trace.addChannel("net")
        trace.addChannel("cache")
        logger.addTraceLogger(trace)

        logger.enableChannel(trace, ".net")
        logger.enableChannels(trace, ".cache")

        Assertions.expect(logger.shouldTraceChannel(trace, ".net"), "local selectors should resolve against the provided namespace")
        Assertions.expect(logger.shouldTraceChannel(trace, ".cache"), "local CSV selectors should resolve against the provided namespace")

        logger.disableChannel(trace, ".net")
        logger.disableChannels(trace, ".cache")

        Assertions.expect(!logger.shouldTraceChannel(trace, ".net"), "local disable should turn channels back off")
        Assertions.expect(!logger.shouldTraceChannel(trace, ".cache"), "local CSV disable should turn channels back off")
    }

    private fun testConflictingColorsRejected() {
        val logger = Logger()

        val first = TraceLogger("tests")
        first.addChannel("net")
        logger.addTraceLogger(first)

        val duplicate = TraceLogger("tests")
        duplicate.addChannel("net")
        logger.addTraceLogger(duplicate)

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
}
