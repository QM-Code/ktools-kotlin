package kcli.tests

import kcli.CliError
import kcli.InlineParser
import kcli.Parser

object ParseLifecycleApiTests {
    fun run() {
        testParserEmptyParseSucceeds()
        testUnknownOptionDoesNotRunHandlers()
        testParserCanBeReusedAcrossParses()
        testPositionalsRunAfterOptionValidation()
        testOptionHandlerExceptionSurfacesAsCliError()
        testPositionalHandlerExceptionSurfacesAsCliError()
        testDoubleDashRemainsUnknown()
    }

    private fun testParserEmptyParseSucceeds() {
        val argv = arrayOf("prog")
        val parser = Parser()
        parser.parseOrThrow(argv.size, argv)
        Assertions.expectEquals(argv.toList(), listOf("prog"), "parseOrThrow should leave argv unchanged")
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

    private fun testPositionalsRunAfterOptionValidation() {
        val argv = arrayOf("prog", "tail", "--alpha-message", "hello", "--output", "stdout")
        val seen = mutableListOf<String>()
        var alphaMessage = ""
        var output = ""

        val parser = Parser()
        parser.setPositionalHandler { context ->
            seen.add("positionals:${context.valueTokens.joinToString(",")}")
        }
        val alpha = InlineParser("--alpha")
        alpha.setHandler("-message", { _, captured ->
            alphaMessage = captured
            seen.add("alpha")
        }, "Set alpha message.")
        parser.addInlineParser(alpha)
        parser.setHandler("--output", { _, captured ->
            output = captured
            seen.add("output")
        }, "Set output target.")
        parser.parseOrThrow(argv.size, argv)

        Assertions.expectEquals(
            seen,
            listOf("alpha", "output", "positionals:tail"),
            "validated parses should execute inline, top-level, and positional handlers exactly once",
        )
        Assertions.expectEquals(alphaMessage, "hello", "inline options should dispatch in the validated pass")
        Assertions.expectEquals(output, "stdout", "top-level value handlers should dispatch in the validated pass")
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

    private fun testPositionalHandlerExceptionSurfacesAsCliError() {
        val argv = arrayOf("prog", "tail")
        val parser = Parser()
        parser.setPositionalHandler {
            throw IllegalStateException("position boom")
        }

        val error = Assertions.expectThrows<CliError>(
            "positional handler exceptions should surface as CliError",
        ) {
            parser.parseOrThrow(argv.size, argv)
        }

        Assertions.expectEquals(error.option, "", "positional handler failures should not report an option")
        Assertions.expectContains(error.message, "position boom", "positional handler failures should preserve the exception text")
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
        Assertions.expectEquals(argv.toList(), listOf("prog", "--", "-v"), "argv should remain unchanged on double-dash failures")
    }
}
