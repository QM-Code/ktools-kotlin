package ktrace.tests

object OmegaCliTests {
    fun run() {
        testBadSelector()
        testExamplesOption()
        testExactSelectorWarning()
        testWildcardSelectorWarning()
        testFilesOption()
        testFunctionsOption()
        testWildcardDepth3()
        testBraceSelector()
    }

    private fun testBadSelector() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", "*")
        Assertions.expect(result.exitCode != 0, "invalid selector should fail")
        Assertions.expectContains(
            result.stderr,
            "[error] [cli] option '--trace': Invalid trace selector: '*' (did you mean '.*'?)",
            "invalid selector should surface a helpful error",
        )
    }

    private fun testExamplesOption() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace-examples")
        Assertions.expectEquals(result.exitCode, 0, "examples option should succeed")
        Assertions.expectContains(result.stdout, "Trace selector examples:", "examples output should include the examples header")
        Assertions.expectContains(result.stdout, "alpha.net,beta.io", "examples output should include explicit multi-selector examples")
        Assertions.expectContains(result.stdout, "'{alpha,beta}.net'", "examples output should include brace namespace examples")
    }

    private fun testExactSelectorWarning() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", ".missing")
        Assertions.expectEquals(result.exitCode, 0, "unmatched exact selector should warn but succeed")
        Assertions.expectContains(
            result.stdout,
            "[omega] [warning] enable ignored channel selector 'omega.missing' because it matched no registered channels",
            "exact unmatched selector should warn",
        )
    }

    private fun testWildcardSelectorWarning() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", "missing.*")
        Assertions.expectEquals(result.exitCode, 0, "unmatched wildcard selector should warn but succeed")
        Assertions.expectContains(
            result.stdout,
            "[omega] [warning] enable ignored channel selector 'missing.*' because it matched no registered channels",
            "wildcard unmatched selector should warn",
        )
    }

    private fun testFilesOption() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", ".app", "--trace-files")
        Assertions.expectEquals(result.exitCode, 0, "files option should succeed")
        Assertions.expectContains(result.stdout, "[omega] [app] [Main:", "files option should include source labels")
    }

    private fun testFunctionsOption() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", ".app", "--trace-functions")
        Assertions.expectEquals(result.exitCode, 0, "functions option should succeed")
        Assertions.expectContains(result.stdout, "[omega] [app] [Main:", "functions option should include source file")
        Assertions.expectContains(result.stdout, ":main]", "functions option should include method names")
    }

    private fun testWildcardDepth3() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", "*.*.*.*")
        Assertions.expectEquals(result.exitCode, 0, "depth3 wildcard should succeed")
        Assertions.expectContains(result.stdout, "[omega] [app] omega initialized local trace channels", "omega app trace should be visible")
        Assertions.expectContains(result.stdout, "omega trace test on channel 'deep.branch.leaf'", "deep leaf trace should be visible")
        Assertions.expectContains(result.stdout, "beta trace test on channel 'io'", "imported beta trace should be visible")
        Assertions.expectContains(result.stdout, "gamma trace test on channel 'physics'", "imported gamma trace should be visible")
    }

    private fun testBraceSelector() {
        val result = TestSupport.runJava("ktrace.demo.omega.Main", "--trace", "*.{net,io}")
        Assertions.expectEquals(result.exitCode, 0, "brace selector should succeed")
        Assertions.expectContains(result.stdout, "[alpha] [net] testing...", "alpha net should match")
        Assertions.expectContains(result.stdout, "beta trace test on channel 'io'", "beta io should match")
        Assertions.expectNotContains(result.stdout, "[alpha] [cache] testing...", "alpha cache should not match")
        Assertions.expectNotContains(result.stdout, "beta trace test on channel 'scheduler'", "beta scheduler should not match")
        Assertions.expectNotContains(result.stdout, "gamma trace test on channel 'physics'", "gamma physics should not match")
    }
}
