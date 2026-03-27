package kcli.tests

object OmegaCliTests {
    fun run() {
        testBetaWorkersInvalidOption()
        testNewGammaHelpRoot()
        testKnownAndUnknownOption()
        testAlphaAliasOption()
        testBuildAliasOption()
    }

    private fun testBetaWorkersInvalidOption() {
        val result = TestSupport.runJava("kcli.demo.omega.Main", "--beta-workers", "abc")
        Assertions.expect(result.exitCode != 0, "invalid beta workers value should fail")
        Assertions.expectContains(result.stderr, "[error] [cli] option '--beta-workers': expected an integer", "beta workers parse error should be surfaced")
        Assertions.expectNotContains(result.stdout, "Processing --beta-workers", "failing handlers should not leave success output")
    }

    private fun testNewGammaHelpRoot() {
        val result = TestSupport.runJava("kcli.demo.omega.Main", "--newgamma")
        Assertions.expectEquals(result.exitCode, 0, "newgamma root should print help")
        Assertions.expectContains(result.stdout, "Available --newgamma-* options:", "newgamma help should be printed")
        Assertions.expectContains(result.stdout, "--newgamma-tag <value>", "newgamma help should include the tag option")
    }

    private fun testKnownAndUnknownOption() {
        val result = TestSupport.runJava("kcli.demo.omega.Main", "--alpha-message", "hello", "--bogus")
        Assertions.expect(result.exitCode != 0, "known and unknown option mix should fail")
        Assertions.expectContains(result.stderr, "[error] [cli] unknown option --bogus", "unknown option should be reported")
        Assertions.expectNotContains(result.stdout, "Processing --alpha-message with value \"hello\"", "no handlers should run on partially valid input")
    }

    private fun testAlphaAliasOption() {
        val result = TestSupport.runJava("kcli.demo.omega.Main", "-a")
        Assertions.expectEquals(result.exitCode, 0, "alpha alias should succeed")
        Assertions.expectContains(result.stdout, "Processing --alpha-enable", "alpha alias should target alpha enable")
    }

    private fun testBuildAliasOption() {
        val result = TestSupport.runJava("kcli.demo.omega.Main", "-b", "debug")
        Assertions.expectEquals(result.exitCode, 0, "build alias should succeed")
        Assertions.expectContains(result.stdout, "Enabled --<root> prefixes:", "successful omega run should reach the usage summary")
    }
}
