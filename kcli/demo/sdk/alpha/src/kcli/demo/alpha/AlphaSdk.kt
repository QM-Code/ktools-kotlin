package kcli.demo.alpha

import kcli.InlineParser
import kcli.demo.common.DemoSupport.printProcessingLine

object AlphaSdk {
    fun getInlineParser(): InlineParser {
        val parser = InlineParser("--alpha")
        parser.setHandler("-message", { context, value -> printProcessingLine(context, value) }, "Set alpha message label.")
        parser.setOptionalValueHandler("-enable", { context, value -> printProcessingLine(context, value) }, "Enable alpha processing.")
        return parser
    }
}
