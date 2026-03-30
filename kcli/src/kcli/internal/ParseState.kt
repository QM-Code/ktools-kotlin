package kcli.internal

import kcli.FlagHandler
import kcli.PositionalHandler
import kcli.ValueHandler

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
