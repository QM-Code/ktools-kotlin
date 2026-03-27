package kcli.demo.gamma

import kcli.InlineParser
import kcli.demo.common.DemoSupport.printProcessingLine

object GammaSdk {
    fun getInlineParser(): InlineParser {
        val parser = InlineParser("--gamma")
        parser.setOptionalValueHandler("-strict", { context, value -> printProcessingLine(context, value) }, "Enable strict gamma mode.")
        parser.setHandler("-tag", { context, value -> printProcessingLine(context, value) }, "Set a gamma tag label.")
        return parser
    }
}
