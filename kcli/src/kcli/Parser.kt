package kcli

import kcli.internal.InlineParserData
import kcli.internal.ParseEngine
import kcli.internal.ParserData
import kcli.internal.Registration

fun interface FlagHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext)
}

fun interface ValueHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext, value: String)
}

fun interface PositionalHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext)
}

data class HandlerContext(
    val root: String,
    val option: String,
    val command: String,
    val valueTokens: List<String>,
)

class CliError(option: String?, message: String?) :
    RuntimeException(if (message.isNullOrEmpty()) "kcli parse failed" else message) {
    val option: String = option ?: ""
}

class Parser {
    private val data = ParserData()

    fun addAlias(alias: String, target: String) {
        Registration.setAlias(data, alias, target)
    }

    fun addAlias(alias: String, target: String, vararg presetTokens: String) {
        Registration.setAlias(data, alias, target, *presetTokens)
    }

    fun setHandler(option: String, handler: FlagHandler, description: String) {
        Registration.setPrimaryHandler(data, option, handler, description)
    }

    fun setHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setPrimaryHandler(data, option, handler, description)
    }

    fun setOptionalValueHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setPrimaryOptionalValueHandler(data, option, handler, description)
    }

    fun setPositionalHandler(handler: PositionalHandler) {
        Registration.setPositionalHandler(data, handler)
    }

    fun addInlineParser(parser: InlineParser) {
        Registration.addInlineParser(data, parser.snapshot())
    }

    fun parseOrExit(argc: Int, argv: Array<String>?) {
        ParseEngine.parseOrExit(data, argc, argv)
    }

    fun parseOrThrow(argc: Int, argv: Array<String>?) {
        ParseEngine.parse(data, argc, argv)
    }

    fun parseOrExit(args: Array<String>?) {
        val argv = withProgramToken(args)
        parseOrExit(argv?.size ?: 0, argv)
    }

    fun parseOrThrow(args: Array<String>?) {
        val argv = withProgramToken(args)
        parseOrThrow(argv?.size ?: 0, argv)
    }

    private fun withProgramToken(args: Array<String>?): Array<String>? {
        if (args == null) {
            return null
        }

        val argv = Array(args.size + 1) { "" }
        argv[0] = ""
        args.copyInto(argv, destinationOffset = 1)
        return argv
    }
}

class InlineParser(root: String) {
    private val data = InlineParserData()

    init {
        Registration.setInlineRoot(data, root)
    }

    fun setRoot(root: String) {
        Registration.setInlineRoot(data, root)
    }

    fun setRootValueHandler(handler: ValueHandler) {
        Registration.setRootValueHandler(data, handler)
    }

    fun setRootValueHandler(handler: ValueHandler, valuePlaceholder: String, description: String) {
        Registration.setRootValueHandler(data, handler, valuePlaceholder, description)
    }

    fun setHandler(option: String, handler: FlagHandler, description: String) {
        Registration.setInlineHandler(data, option, handler, description)
    }

    fun setHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setInlineHandler(data, option, handler, description)
    }

    fun setOptionalValueHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setInlineOptionalValueHandler(data, option, handler, description)
    }

    fun copy(): InlineParser {
        val copy = InlineParser("--placeholder")
        copy.data.copyFrom(data.copy())
        return copy
    }

    internal fun snapshot(): InlineParserData = data.copy()
}
