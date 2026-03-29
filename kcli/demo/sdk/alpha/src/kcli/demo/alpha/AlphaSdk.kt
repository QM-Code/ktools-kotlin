package kcli.demo.alpha

import kcli.HandlerContext
import kcli.InlineParser

object AlphaSdk {
    fun getInlineParser(): InlineParser {
        val parser = InlineParser("--alpha")
        parser.setHandler("-message", { context, value -> printProcessingLine(context, value) }, "Set alpha message label.")
        parser.setOptionalValueHandler("-enable", { context, value -> printProcessingLine(context, value) }, "Enable alpha processing.")
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
