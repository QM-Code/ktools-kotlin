package kcli.internal

import kcli.PositionalHandler
import kcli.ValueHandler

enum class InlineTokenKind {
    NONE,
    BARE_ROOT,
    DASH_OPTION,
}

class InlineTokenMatch {
    var kind = InlineTokenKind.NONE
    var parser: InlineParserData? = null
    var suffix = ""
}

class InlineParserData {
    var rootName = ""
    var rootValueHandler: ValueHandler? = null
    var rootValuePlaceholder = ""
    var rootValueDescription = ""
    val commands = linkedMapOf<String, CommandBinding>()

    fun copy(): InlineParserData {
        val copy = InlineParserData()
        copy.rootName = rootName
        copy.rootValueHandler = rootValueHandler
        copy.rootValuePlaceholder = rootValuePlaceholder
        copy.rootValueDescription = rootValueDescription
        for ((key, value) in commands) {
            copy.commands[key] = value.copy()
        }
        return copy
    }

    fun copyFrom(other: InlineParserData) {
        rootName = other.rootName
        rootValueHandler = other.rootValueHandler
        rootValuePlaceholder = other.rootValuePlaceholder
        rootValueDescription = other.rootValueDescription
        commands.clear()
        for ((key, value) in other.commands) {
            commands[key] = value.copy()
        }
    }
}

class ParserData {
    var positionalHandler: PositionalHandler? = null
    val aliases = linkedMapOf<String, AliasBinding>()
    val commands = linkedMapOf<String, CommandBinding>()
    val inlineParsers = linkedMapOf<String, InlineParserData>()
}
