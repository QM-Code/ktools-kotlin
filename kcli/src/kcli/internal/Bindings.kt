package kcli.internal

import kcli.FlagHandler
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
