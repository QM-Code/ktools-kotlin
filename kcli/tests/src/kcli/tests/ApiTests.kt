package kcli.tests

import kcli.CliError
import kcli.InlineParser
import kcli.Parser

object ApiTests {
    fun run() {
        testParserEmptyParseSucceeds()
        testInlineParserRejectsSingleDashRoot()
        testUnknownOptionDoesNotRunHandlers()
        testAliasRewritesOption()
        testAliasPresetTokensSatisfyRequiredValue()
        testAliasPresetTokensApplyToInlineRootValue()
        testAliasPresetTokensRejectedForFlags()
        testParserCanBeReusedAcrossParses()
        testRequiredValueAcceptsOptionLikeFirstToken()
        testBareInlineRootPrintsHelp()
        testInlineRootHelpIncludesRootValueRow()
        testOptionalValueAcceptsExplicitEmptyToken()
        testPositionalHandlerPreservesExplicitEmptyTokens()
        testOptionHandlerExceptionSurfacesAsCliError()
        testInlineRootOverrideApplies()
        testDoubleDashRemainsUnknown()
    }

    private fun testParserEmptyParseSucceeds() {
        val argv = arrayOf("prog")
        val parser = Parser()
        parser.parseOrThrow(argv.size, argv)
        Assertions.expectEquals(argv.toList(), listOf("prog"), "parseOrThrow should leave argv unchanged")
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

    private fun testUnknownOptionDoesNotRunHandlers() {
        val argv = arrayOf("prog", "--verbose", "pos1", "--output", "stdout", "--bogus")
        var verbose = false
        var output = ""
        val positionals = mutableListOf<String>()

        val parser = Parser()
        parser.setHandler("--verbose", { _ -> verbose = true }, "Enable verbose logging.")
        parser.setHandler("--output", { _, value -> output = value }, "Set output target.")
        parser.setPositionalHandler { context -> positionals.addAll(context.valueTokens) }

        val error = Assertions.expectThrows<CliError>(
            "unknown option should fail before handlers run",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expect(!verbose, "verbose handler should not have run")
        Assertions.expectEquals(output, "", "value handler should not have run")
        Assertions.expectEquals(positionals, emptyList<String>(), "positional handler should not have run")
        Assertions.expectEquals(error.option, "--bogus", "CliError option should match unknown token")
        Assertions.expectContains(error.message, "unknown option --bogus", "CliError should describe unknown option")
        Assertions.expectEquals(
            argv.toList(),
            listOf("prog", "--verbose", "pos1", "--output", "stdout", "--bogus"),
            "parseOrThrow should leave argv unchanged on failure",
        )
    }

    private fun testAliasRewritesOption() {
        val argv = arrayOf("prog", "-v")
        var seen = ""

        val parser = Parser()
        parser.addAlias("-v", "--verbose")
        parser.setHandler("--verbose", { context -> seen = context.option }, "Enable verbose logging.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(seen, "--verbose", "alias should rewrite the effective option")
    }

    private fun testAliasPresetTokensSatisfyRequiredValue() {
        val argv = arrayOf("prog", "-p")
        var value = ""
        val tokens = mutableListOf<String>()

        val parser = Parser()
        parser.addAlias("-p", "--profile", "release")
        parser.setHandler("--profile", { context, captured ->
            value = captured
            tokens.addAll(context.valueTokens)
        }, "Set active profile.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, "release", "preset token should satisfy required value")
        Assertions.expectEquals(tokens, listOf("release"), "context tokens should include preset value")
    }

    private fun testAliasPresetTokensApplyToInlineRootValue() {
        val argv = arrayOf("prog", "-c")
        var option = ""
        var value = ""

        val parser = Parser()
        val config = InlineParser("--config")
        config.setRootValueHandler({ context, captured ->
            option = context.option
            value = captured
        }, "<assignment>", "Store a config assignment.")
        parser.addInlineParser(config)
        parser.addAlias("-c", "--config", "user-file=/tmp/user.json")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(option, "--config", "inline root option should be preserved")
        Assertions.expectEquals(value, "user-file=/tmp/user.json", "preset value should reach root handler")
    }

    private fun testAliasPresetTokensRejectedForFlags() {
        val argv = arrayOf("prog", "-v")
        val parser = Parser()
        parser.addAlias("-v", "--verbose", "unexpected")
        parser.setHandler("--verbose", { _ -> }, "Enable verbose logging.")

        val error = Assertions.expectThrows<CliError>(
            "aliases with preset tokens must not target flags",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "-v", "error should surface the alias token")
        Assertions.expectContains(error.message, "does not accept values", "error should explain flag preset rejection")
    }

    private fun testParserCanBeReusedAcrossParses() {
        val firstArgv = arrayOf("prog", "-v")
        val secondArgv = arrayOf("prog", "-v")
        var calls = 0

        val parser = Parser()
        parser.addAlias("-v", "--verbose")
        parser.setHandler("--verbose", { _ -> calls += 1 }, "Enable verbose logging.")

        parser.parseOrThrow(firstArgv.size, firstArgv)
        parser.parseOrThrow(secondArgv.size, secondArgv)

        Assertions.expectEquals(calls, 2, "parser instances should remain reusable across parses")
        Assertions.expectEquals(firstArgv.toList(), listOf("prog", "-v"), "first argv should remain unchanged")
        Assertions.expectEquals(secondArgv.toList(), listOf("prog", "-v"), "second argv should remain unchanged")
    }

    private fun testRequiredValueAcceptsOptionLikeFirstToken() {
        val argv = arrayOf("prog", "--output", "-v")
        var value = ""

        val parser = Parser()
        parser.addAlias("-v", "--verbose")
        parser.setHandler("--output", { _, captured -> value = captured }, "Set output target.")
        parser.setHandler(
            "--verbose",
            { _ -> throw AssertionError("verbose alias should not be treated as an option value") },
            "Enable verbose logging.",
        )
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, "-v", "required values should accept option-like first tokens")
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

    private fun testOptionalValueAcceptsExplicitEmptyToken() {
        val argv = arrayOf("prog", "--alpha-enable", "")
        var value = "unexpected"
        val tokens = mutableListOf<String>()

        val parser = Parser()
        val alpha = InlineParser("--alpha")
        alpha.setOptionalValueHandler("-enable", { context, captured ->
            value = captured
            tokens.addAll(context.valueTokens)
        }, "Enable alpha processing.")
        parser.addInlineParser(alpha)
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, "", "explicit empty optional values should remain empty")
        Assertions.expectEquals(tokens, listOf(""), "context tokens should preserve explicit empty optional values")
        Assertions.expectEquals(argv.toList(), listOf("prog", "--alpha-enable", ""), "argv should remain unchanged")
    }

    private fun testPositionalHandlerPreservesExplicitEmptyTokens() {
        val argv = arrayOf("prog", "", "tail")
        val positionals = mutableListOf<String>()

        val parser = Parser()
        parser.setPositionalHandler { context ->
            positionals.addAll(context.valueTokens)
        }
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(positionals, listOf("", "tail"), "positionals should preserve explicit empty tokens")
        Assertions.expectEquals(argv.toList(), listOf("prog", "", "tail"), "argv should remain unchanged")
    }

    private fun testOptionHandlerExceptionSurfacesAsCliError() {
        val argv = arrayOf("prog", "--verbose")
        val parser = Parser()
        parser.setHandler("--verbose", { _ ->
            throw IllegalStateException("option boom")
        }, "Enable verbose logging.")

        val error = Assertions.expectThrows<CliError>(
            "handler exceptions should surface as CliError",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "--verbose", "handler failures should report the option")
        Assertions.expectContains(error.message, "option boom", "handler error text should be preserved")
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

    private fun testDoubleDashRemainsUnknown() {
        val argv = arrayOf("prog", "--", "-v")
        val parser = Parser()
        parser.addAlias("-v", "--verbose")
        parser.setHandler("--verbose", { _ -> }, "Enable verbose logging.")

        val error = Assertions.expectThrows<CliError>(
            "double dash should remain an unknown option",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "--", "double dash should be reported as the failing option")
    }
}
