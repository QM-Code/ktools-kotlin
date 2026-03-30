package kcli.tests

import kcli.CliError
import kcli.InlineParser
import kcli.Parser

object InlineApiTests {
    fun run() {
        testInlineParserRejectsSingleDashRoot()
        testInlineHandlersAcceptShortAndFullForms()
        testInlineHandlerRejectsWrongRoot()
        testBareInlineRootPrintsHelp()
        testInlineRootHelpIncludesRootValueRow()
        testInlineRootValueJoinsTokens()
        testInlineMissingRootValueHandlerFails()
        testUnknownInlineOptionFails()
        testInlineRootOverrideApplies()
        testDuplicateInlineRootRejected()
    }

    private fun testInlineParserRejectsSingleDashRoot() {
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "single-dash inline roots should be rejected",
        ) {
            InlineParser("-build")
        }

        Assertions.expectContains(
            error.message,
            "must use '--root' or 'root'",
            "invalid inline roots should explain the accepted forms",
        )
    }

    private fun testInlineHandlersAcceptShortAndFullForms() {
        val argv = arrayOf("prog", "--build-flag", "--build-value", "data")
        var flag = false
        var value = ""

        val parser = Parser()
        val build = InlineParser("--build")
        build.setHandler("-flag", { _ -> flag = true }, "Enable build flag.")
        build.setHandler("--build-value", { _, captured -> value = captured }, "Set build value.")
        parser.addInlineParser(build)
        parser.parseOrThrow(argv.size, argv)

        Assertions.expect(flag, "short-form inline handlers should dispatch")
        Assertions.expectEquals(value, "data", "fully-qualified inline handlers should dispatch")
        Assertions.expectEquals(
            argv.toList(),
            listOf("prog", "--build-flag", "--build-value", "data"),
            "argv should remain unchanged after inline parsing",
        )
    }

    private fun testInlineHandlerRejectsWrongRoot() {
        val build = InlineParser("--build")
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "mismatched inline roots should be rejected",
        ) {
            build.setHandler("--other-flag", { _ -> }, "Bad handler.")
        }

        Assertions.expectContains(error.message, "--build-name", "inline handler errors should reference the registered root")
    }

    private fun testBareInlineRootPrintsHelp() {
        val argv = arrayOf("prog", "--alpha")
        val parser = Parser()
        val alpha = InlineParser("--alpha")
        alpha.setOptionalValueHandler("-enable", { _, _ -> }, "Enable alpha processing.")
        parser.addInlineParser(alpha)

        val stdout = TestSupport.captureStdout {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectContains(stdout, "Available --alpha-* options:", "bare inline root should print help")
        Assertions.expectContains(stdout, "--alpha-enable [value]", "help should include optional value syntax")
        Assertions.expectEquals(argv.toList(), listOf("prog", "--alpha"), "argv should remain unchanged")
    }

    private fun testInlineRootHelpIncludesRootValueRow() {
        val argv = arrayOf("prog", "--build")
        val parser = Parser()
        val build = InlineParser("--build")
        build.setRootValueHandler({ _, _ -> }, "<selector>", "Select build targets.")
        build.setHandler("-flag", { _ -> }, "Enable build flag.")
        parser.addInlineParser(build)

        val stdout = TestSupport.captureStdout {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectContains(stdout, "--build <selector>", "bare root help should include root value syntax")
        Assertions.expectContains(stdout, "Select build targets.", "bare root help should include root value description")
    }

    private fun testInlineRootValueJoinsTokens() {
        val argv = arrayOf("prog", "--build", "fast", "mode")
        var option = ""
        var value = ""
        val tokens = mutableListOf<String>()

        val parser = Parser()
        val build = InlineParser("--build")
        build.setRootValueHandler({ context, captured ->
            option = context.option
            value = captured
            tokens.addAll(context.valueTokens)
        }, "<selector>", "Select build targets.")
        parser.addInlineParser(build)
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(option, "--build", "root value handlers should observe the root token")
        Assertions.expectEquals(value, "fast mode", "root value handlers should receive joined tokens")
        Assertions.expectEquals(tokens, listOf("fast", "mode"), "root value handlers should preserve token boundaries")
    }

    private fun testInlineMissingRootValueHandlerFails() {
        val argv = arrayOf("prog", "--build", "fast")
        val parser = Parser()
        parser.addInlineParser(InlineParser("--build"))

        val error = Assertions.expectThrows<CliError>(
            "bare inline roots with values should fail without a root value handler",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "--build", "root value errors should report the root token")
        Assertions.expectContains(error.message, "unknown value for option '--build'", "missing root handlers should be explicit")
    }

    private fun testUnknownInlineOptionFails() {
        val argv = arrayOf("prog", "--build-unknown")
        val parser = Parser()
        parser.addInlineParser(InlineParser("--build"))

        val error = Assertions.expectThrows<CliError>(
            "unknown inline options should fail",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "--build-unknown", "unknown inline options should report the token")
        Assertions.expectContains(error.message, "unknown option --build-unknown", "unknown inline options should preserve the standard error text")
    }

    private fun testInlineRootOverrideApplies() {
        val argv = arrayOf("prog", "--newgamma-tag", "prod")
        var value = ""

        val parser = Parser()
        val gamma = InlineParser("--gamma")
        gamma.setHandler("-tag", { _, captured -> value = captured }, "Set gamma tag.")
        gamma.setRoot("--newgamma")
        parser.addInlineParser(gamma)
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, "prod", "overridden inline roots should dispatch registered handlers")
        Assertions.expectEquals(
            argv.toList(),
            listOf("prog", "--newgamma-tag", "prod"),
            "argv should remain unchanged after overridden root dispatch",
        )
    }

    private fun testDuplicateInlineRootRejected() {
        val parser = Parser()
        parser.addInlineParser(InlineParser("--build"))

        val error = Assertions.expectThrows<IllegalArgumentException>(
            "duplicate inline roots should be rejected",
        ) {
            parser.addInlineParser(InlineParser("--build"))
        }

        Assertions.expectContains(error.message, "already registered", "duplicate roots should explain the conflict")
    }
}
