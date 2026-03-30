package kcli.tests

import kcli.CliError
import kcli.InlineParser
import kcli.Parser

object AliasApiTests {
    fun run() {
        testAliasRewritesOption()
        testAliasPresetTokensAppendToValueHandler()
        testAliasPresetTokensSatisfyRequiredValue()
        testAliasPresetTokensApplyToInlineRootValue()
        testAliasPresetTokensRejectedForFlags()
        testAliasRejectsInvalidAlias()
        testAliasRejectsInvalidTarget()
        testAliasRejectsSingleDashTarget()
    }

    private fun testAliasRewritesOption() {
        val argv = arrayOf("prog", "-v")
        var seen = ""

        val parser = Parser()
        parser.addAlias("-v", "--verbose")
        parser.setHandler("--verbose", { context -> seen = context.option }, "Enable verbose logging.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(seen, "--verbose", "alias should rewrite the effective option")
        Assertions.expectEquals(argv.toList(), listOf("prog", "-v"), "alias handling should not rewrite argv")
    }

    private fun testAliasPresetTokensAppendToValueHandler() {
        val argv = arrayOf("prog", "-c", "settings.json")
        var option = ""
        var value = ""
        val tokens = mutableListOf<String>()

        val parser = Parser()
        parser.addAlias("-c", "--config-load", "user-file")
        parser.setHandler("--config-load", { context, captured ->
            option = context.option
            value = captured
            tokens.addAll(context.valueTokens)
        }, "Load configuration.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(option, "--config-load", "alias targets should dispatch canonical options")
        Assertions.expectEquals(value, "user-file settings.json", "preset alias tokens should prepend captured values")
        Assertions.expectEquals(tokens, listOf("user-file", "settings.json"), "context tokens should expose preset values")
        Assertions.expectEquals(argv.toList(), listOf("prog", "-c", "settings.json"), "argv should remain unchanged")
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
        Assertions.expectEquals(argv.toList(), listOf("prog", "-p"), "argv should remain unchanged")
    }

    private fun testAliasPresetTokensApplyToInlineRootValue() {
        val argv = arrayOf("prog", "-c")
        var option = ""
        var value = ""
        val tokens = mutableListOf<String>()

        val parser = Parser()
        val config = InlineParser("--config")
        config.setRootValueHandler({ context, captured ->
            option = context.option
            value = captured
            tokens.addAll(context.valueTokens)
        }, "<assignment>", "Store a config assignment.")
        parser.addInlineParser(config)
        parser.addAlias("-c", "--config", "user-file=/tmp/user.json")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(option, "--config", "inline root option should be preserved")
        Assertions.expectEquals(value, "user-file=/tmp/user.json", "preset value should reach root handler")
        Assertions.expectEquals(tokens, listOf("user-file=/tmp/user.json"), "root handlers should observe effective tokens")
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

    private fun testAliasRejectsInvalidAlias() {
        val parser = Parser()
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "double-dash aliases should be rejected",
        ) {
            parser.addAlias("--verbose", "--output")
        }

        Assertions.expectContains(error.message, "single-dash form", "invalid aliases should describe the accepted form")
    }

    private fun testAliasRejectsInvalidTarget() {
        val parser = Parser()
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "alias targets with whitespace should be rejected",
        ) {
            parser.addAlias("-v", "--bad target")
        }

        Assertions.expectContains(error.message, "double-dash form", "invalid targets should describe the accepted form")
    }

    private fun testAliasRejectsSingleDashTarget() {
        val parser = Parser()
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "single-dash alias targets should be rejected",
        ) {
            parser.addAlias("-v", "-verbose")
        }

        Assertions.expectContains(error.message, "double-dash form", "alias targets should require double-dash syntax")
    }
}
