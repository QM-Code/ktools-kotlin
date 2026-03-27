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
        testRequiredValueAcceptsOptionLikeFirstToken()
        testBareInlineRootPrintsHelp()
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
