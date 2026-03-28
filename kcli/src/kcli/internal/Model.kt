package kcli.internal

import kcli.FlagHandler
import kcli.PositionalHandler
import kcli.ValueHandler

enum class ValueArity {
    REQUIRED,
    OPTIONAL,
}

class CommandBinding {
    var expectsValue = false
    var flagHandler: FlagHandler? = null
    var valueHandler: ValueHandler? = null
    var valueArity: ValueArity = ValueArity.REQUIRED
    var description = ""

    fun copy(): CommandBinding {
        val copy = CommandBinding()
        copy.expectsValue = expectsValue
        copy.flagHandler = flagHandler
        copy.valueHandler = valueHandler
        copy.valueArity = valueArity
        copy.description = description
        return copy
    }
}

class AliasBinding {
    var alias = ""
    var targetToken = ""
    val presetTokens = mutableListOf<String>()

    fun copy(): AliasBinding {
        val copy = AliasBinding()
        copy.alias = alias
        copy.targetToken = targetToken
        copy.presetTokens.addAll(presetTokens)
        return copy
    }
}

class MutableParseOutcome {
    var ok = true
    var errorOption = ""
    var errorMessage = ""

    fun reportError(option: String?, message: String?) {
        if (!ok) {
            return
        }

        ok = false
        errorOption = option ?: ""
        errorMessage = message ?: ""
    }
}

class CollectedValues {
    var hasValue = false
    val parts = mutableListOf<String>()
    var lastIndex = -1
}

enum class InvocationKind {
    FLAG,
    VALUE,
    POSITIONAL,
    PRINT_HELP,
}

class Invocation {
    var kind = InvocationKind.FLAG
    var root = ""
    var option = ""
    var command = ""
    val valueTokens = mutableListOf<String>()
    var flagHandler: FlagHandler? = null
    var valueHandler: ValueHandler? = null
    var positionalHandler: PositionalHandler? = null
    val helpRows = mutableListOf<HelpRow>()
}

data class HelpRow(val lhs: String, val rhs: String)

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
