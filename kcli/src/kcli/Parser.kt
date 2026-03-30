package kcli

import kcli.internal.ParseEngine
import kcli.internal.ParserData
import kcli.internal.Registration

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
        parseOrExit(args?.asList())
    }

    fun parseOrThrow(args: Array<String>?) {
        parseOrThrow(args?.asList())
    }

    fun parseOrExit(args: List<String>?) {
        val argv = withProgramToken(args)
        parseOrExit(argv?.size ?: 0, argv)
    }

    fun parseOrThrow(args: List<String>?) {
        val argv = withProgramToken(args)
        parseOrThrow(argv?.size ?: 0, argv)
    }

    private fun withProgramToken(args: List<String>?): Array<String>? {
        if (args == null) {
            return null
        }

        val argv = Array(args.size + 1) { "" }
        argv[0] = ""
        for ((index, arg) in args.withIndex()) {
            argv[index + 1] = arg
        }
        return argv
    }
}
