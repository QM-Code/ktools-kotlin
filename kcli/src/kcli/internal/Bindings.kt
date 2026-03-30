package kcli.internal

import kcli.FlagHandler
import kcli.ValueHandler

enum class ValueArity {
    REQUIRED,
    OPTIONAL,
}

data class CommandBinding(
    var expectsValue: Boolean = false,
    var flagHandler: FlagHandler? = null,
    var valueHandler: ValueHandler? = null,
    var valueArity: ValueArity = ValueArity.REQUIRED,
    var description: String = "",
)

data class AliasBinding(
    var alias: String = "",
    var targetToken: String = "",
    val presetTokens: MutableList<String> = mutableListOf(),
)
