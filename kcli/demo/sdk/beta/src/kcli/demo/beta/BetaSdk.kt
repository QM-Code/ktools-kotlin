package kcli.demo.beta

import kcli.HandlerContext
import kcli.InlineParser

object BetaSdk {
    fun getInlineParser(): InlineParser {
        val parser = InlineParser("--beta")
        parser.setHandler("-profile", { context, value -> printProcessingLine(context, value) }, "Select beta runtime profile.")
        parser.setHandler("-workers", { context, value ->
            value.toIntOrNull() ?: throw RuntimeException("expected an integer")
            printProcessingLine(context, value)
        }, "Set beta worker count.")
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
