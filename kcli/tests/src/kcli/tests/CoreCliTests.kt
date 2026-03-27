package kcli.tests

object CoreCliTests {
    fun run() {
        testUnknownAlphaOption()
        testKnownAlphaOption()
        testAlphaOptionalNoValue()
        testAlphaHelpRoot()
        testOutputAliasOption()
        testDoubleDashNotSeparator()
    }

    private fun testUnknownAlphaOption() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-d")
        Assertions.expect(result.exitCode != 0, "unknown alpha option should fail")
        Assertions.expectContains(result.stderr, "[error] [cli] unknown option --alpha-d", "unknown alpha option should be reported")
        Assertions.expectNotContains(result.stdout, "KCLI kotlin demo core import/integration check passed", "failed parse should not print success")
    }

    private fun testKnownAlphaOption() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-message", "hello")
        Assertions.expectEquals(result.exitCode, 0, "known alpha option should succeed")
        Assertions.expectContains(result.stdout, "Processing --alpha-message with value \"hello\"", "alpha message should be processed")
    }

    private fun testAlphaOptionalNoValue() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "--alpha-enable")
        Assertions.expectEquals(result.exitCode, 0, "optional alpha flag should succeed")
        Assertions.expectContains(result.stdout, "Processing --alpha-enable", "optional alpha flag should run without a value")
    }

    private fun testAlphaHelpRoot() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "--alpha")
        Assertions.expectEquals(result.exitCode, 0, "bare alpha root should print help")
        Assertions.expectContains(result.stdout, "Available --alpha-* options:", "alpha help root should print a heading")
        Assertions.expectContains(result.stdout, "--alpha-enable [value]", "alpha help should include optional value syntax")
    }

    private fun testOutputAliasOption() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "-out", "stdout")
        Assertions.expectEquals(result.exitCode, 0, "output alias should succeed")
        Assertions.expectContains(result.stdout, "KCLI kotlin demo core import/integration check passed", "output alias should reach the demo success path")
    }

    private fun testDoubleDashNotSeparator() {
        val result = TestSupport.runJava("kcli.demo.core.Main", "--", "--alpha-message", "hello")
        Assertions.expect(result.exitCode != 0, "double dash should not be treated as a separator")
        Assertions.expectContains(result.stderr, "[error] [cli] unknown option --", "double dash should be reported as unknown")
        Assertions.expectNotContains(result.stdout, "Processing --alpha-message", "invalid parses should not execute handlers")
    }
}
