package kcli.demo.beta

import kcli.InlineParser
import kcli.demo.common.DemoSupport.printProcessingLine

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
}
