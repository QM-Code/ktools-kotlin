package ktrace.tests

object ApiTests {
    fun run() {
        FormatApiTests.run()
        LoggerApiTests.run()
        TraceChangedApiTests.run()
    }
}
