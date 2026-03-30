package kcli.tests

import kcli.CliError
import kcli.InlineParser
import kcli.Parser

object ValueApiTests {
    fun run() {
        testRequiredValueRejectsMissingValue()
        testRequiredValueAcceptsOptionLikeFirstToken()
        testRequiredValuePreservesWhitespace()
        testRequiredValueAcceptsExplicitEmptyToken()
        testOptionalValueAllowsMissingValue()
        testOptionalValueAcceptsExplicitEmptyToken()
        testFlagHandlersDoNotConsumeFollowingTokens()
        testPositionalHandlerPreservesExplicitEmptyTokens()
        testPrimaryHandlerRejectsSingleDashOption()
    }

    private fun testRequiredValueRejectsMissingValue() {
        val argv = arrayOf("prog", "--build-value")
        val parser = Parser()
        val build = InlineParser("--build")
        build.setHandler("-value", { _, _ -> }, "Set build value.")
        parser.addInlineParser(build)

        val error = Assertions.expectThrows<CliError>(
            "required value handlers should reject missing values",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "--build-value", "required value errors should report the failing option")
        Assertions.expectContains(error.message, "requires a value", "required value errors should describe the missing value")
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
        Assertions.expectEquals(argv.toList(), listOf("prog", "--output", "-v"), "argv should remain unchanged")
    }

    private fun testRequiredValuePreservesWhitespace() {
        val argv = arrayOf("prog", "--name", " Joe ")
        var value = ""
        val tokens = mutableListOf<String>()

        val parser = Parser()
        parser.setHandler("--name", { context, captured ->
            value = captured
            tokens.addAll(context.valueTokens)
        }, "Set display name.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, " Joe ", "required values should preserve shell whitespace")
        Assertions.expectEquals(tokens, listOf(" Joe "), "context tokens should preserve shell whitespace")
    }

    private fun testRequiredValueAcceptsExplicitEmptyToken() {
        val argv = arrayOf("prog", "--name", "")
        var value = "unexpected"
        val tokens = mutableListOf<String>()

        val parser = Parser()
        parser.setHandler("--name", { context, captured ->
            value = captured
            tokens.addAll(context.valueTokens)
        }, "Set display name.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(value, "", "explicit empty required values should remain empty")
        Assertions.expectEquals(tokens, listOf(""), "required value tokens should preserve explicit empties")
    }

    private fun testOptionalValueAllowsMissingValue() {
        val argv = arrayOf("prog", "--alpha-enable")
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

        Assertions.expectEquals(value, "", "optional handlers should accept missing values")
        Assertions.expectEquals(tokens, emptyList<String>(), "optional handlers should not fabricate value tokens")
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

    private fun testFlagHandlersDoNotConsumeFollowingTokens() {
        val argv = arrayOf("prog", "--build-meta", "data")
        val positionals = mutableListOf<String>()
        var called = false

        val parser = Parser()
        val build = InlineParser("--build")
        build.setHandler("-meta", { _ -> called = true }, "Enable build metadata.")
        parser.addInlineParser(build)
        parser.setPositionalHandler { context ->
            positionals.addAll(context.valueTokens)
        }
        parser.parseOrThrow(argv.size, argv)

        Assertions.expect(called, "flag handlers should still run")
        Assertions.expectEquals(positionals, listOf("data"), "flag handlers should leave following values as positionals")
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

    private fun testPrimaryHandlerRejectsSingleDashOption() {
        val parser = Parser()
        val error = Assertions.expectThrows<IllegalArgumentException>(
            "single-dash end-user options should be rejected",
        ) {
            parser.setHandler("-verbose", { _ -> }, "Enable verbose logging.")
        }

        Assertions.expectContains(error.message, "--name", "top-level handler errors should describe the accepted forms")
    }
}
