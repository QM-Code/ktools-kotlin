package ktrace.tests

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import ktrace.Logger
import ktrace.TraceLogger

object TraceChangedApiTests {
    fun run() {
        testTraceChangedSuppressesDuplicates()
        testTraceChangedThreadSafety()
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
