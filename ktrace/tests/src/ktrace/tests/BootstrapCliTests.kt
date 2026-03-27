package ktrace.tests

object BootstrapCliTests {
    fun run() {
        val result = TestSupport.runJava("ktrace.demo.bootstrap.Main")
        Assertions.expectEquals(result.exitCode, 0, "bootstrap demo should succeed")
        Assertions.expectContains(
            result.stdout,
            "[bootstrap] [bootstrap] ktrace bootstrap compile/link check",
            "bootstrap trace output should match the reference demo",
        )
        Assertions.expectContains(result.stdout, "Bootstrap succeeded.", "bootstrap should report success")
    }
}
