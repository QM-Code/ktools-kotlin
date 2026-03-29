package kcli.demo.gamma

import kcli.HandlerContext
import kcli.InlineParser

object GammaSdk {
    fun getInlineParser(): InlineParser {
        val parser = InlineParser("--gamma")
        parser.setOptionalValueHandler("-strict", { context, value -> printProcessingLine(context, value) }, "Enable strict gamma mode.")
        parser.setHandler("-tag", { context, value -> printProcessingLine(context, value) }, "Set a gamma tag label.")
        return parser
    }

    private fun printProcessingLine(context: HandlerContext, value: String) {
        when (context.valueTokens.size) {
            0 -> println("Processing ${context.option}")
            1 -> println("Processing ${context.option} with value \"$value\"")
            else -> {
                val joined = context.valueTokens.joinToString(",") { "\"$it\"" }
                println("Processing ${context.option} with values [$joined]")
            }
        }
    }
}
