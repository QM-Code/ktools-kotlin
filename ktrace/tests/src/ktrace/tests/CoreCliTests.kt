package ktrace.tests

object CoreCliTests {
    fun run() {
        testUnknownOption()
        testBareTraceRoot()
        testNamespacesOption()
        testChannelsOption()
        testColorsOption()
        testTimestampsOption()
        testImportedSelector()
    }

    private fun testUnknownOption() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-f")
        Assertions.expect(result.exitCode != 0, "unknown trace option should fail")
        Assertions.expectContains(result.stderr, "[error] [cli] unknown option --trace-f", "unknown option should be reported")
    }

    private fun testBareTraceRoot() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace")
        Assertions.expectEquals(result.exitCode, 0, "bare trace root should print help")
        Assertions.expectContains(result.stdout, "Available --trace-* options:", "trace root should print help")
        Assertions.expectContains(result.stdout, "--trace <channels>", "trace help should include root value")
        Assertions.expectNotContains(result.stdout, "Trace selector examples:", "bare root should not print examples")
    }

    private fun testNamespacesOption() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-namespaces")
        Assertions.expectEquals(result.exitCode, 0, "namespaces option should succeed")
        Assertions.expectContains(result.stdout, "Available trace namespaces:", "namespaces output should include a heading")
        Assertions.expectContains(result.stdout, "  alpha", "namespaces output should include imported namespaces")
        Assertions.expectContains(result.stdout, "  core", "namespaces output should include the local namespace")
    }

    private fun testChannelsOption() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-channels")
        Assertions.expectEquals(result.exitCode, 0, "channels option should succeed")
        Assertions.expectContains(result.stdout, "Available trace channels:", "channels output should include a heading")
        Assertions.expectContains(result.stdout, "  alpha.net", "channels output should include imported channels")
        Assertions.expectContains(result.stdout, "  core.app", "channels output should include local channels")
    }

    private fun testColorsOption() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace-colors")
        Assertions.expectEquals(result.exitCode, 0, "colors option should succeed")
        Assertions.expectContains(result.stdout, "Available trace colors:", "colors output should include a heading")
        Assertions.expectContains(result.stdout, "  Gold3", "colors output should include known colors")
    }

    private fun testTimestampsOption() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace", ".app", "--trace-timestamps")
        Assertions.expectEquals(result.exitCode, 0, "timestamps option should succeed")
        Assertions.expectContains(result.stdout, "[core] [", "trace output should include namespace and timestamp prefix")
        Assertions.expectContains(result.stdout, "] [app] cli processing enabled, use --trace for options", "trace output should include app channel")
    }

    private fun testImportedSelector() {
        val result = TestSupport.runJava("ktrace.demo.core.Main", "--trace", "*.*")
        Assertions.expectEquals(result.exitCode, 0, "imported selector should succeed")
        Assertions.expectContains(result.stdout, "[core] [app] cli processing enabled, use --trace for options", "local trace output should be visible")
        Assertions.expectContains(result.stdout, "[alpha] [net] testing...", "imported alpha traces should be visible")
    }
}
