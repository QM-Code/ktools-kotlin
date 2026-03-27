package kcli.demo.common

import kcli.HandlerContext

object DemoSupport {
    fun printProcessingLine(context: HandlerContext, value: String) {
        when (context.valueTokens.size) {
            0 -> println("Processing ${context.option}")
            1 -> println("Processing ${context.option} with value \"$value\"")
            else -> {
                val joined = context.valueTokens.joinToString(",") { "\"$it\"" }
                println("Processing ${context.option} with values [$joined]")
            }
        }
    }

    fun withProgram(programName: String, args: Array<String>?): Array<String> {
        val argv = Array((args?.size ?: 0) + 1) { "" }
        argv[0] = programName
        args?.copyInto(argv, destinationOffset = 1)
        return argv
    }
}
