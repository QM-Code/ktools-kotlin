package kcli.demo.core

import kcli.Parser
import kcli.demo.alpha.AlphaSdk

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val exeName = "kcli_demo_core"
        val argv = withProgram(exeName, args)

        val parser = Parser()
        parser.addInlineParser(AlphaSdk.getInlineParser())
        parser.addAlias("-v", "--verbose")
        parser.addAlias("-out", "--output")
        parser.addAlias("-a", "--alpha-enable")
        parser.setHandler("--verbose", { _ -> }, "Enable verbose app logging.")
        parser.setHandler("--output", { _, _ -> }, "Set app output target.")
        parser.parseOrExit(argv.size, argv)

        println()
        println("KCLI kotlin demo core import/integration check passed")
        println()
        println("Usage:")
        println("  $exeName --alpha")
        println("  $exeName --output stdout")
        println()
        println("Enabled inline roots:")
        println("  --alpha")
        println()
    }

    private fun withProgram(programName: String, args: Array<String>?): Array<String> {
        val argv = Array((args?.size ?: 0) + 1) { "" }
        argv[0] = programName
        args?.copyInto(argv, destinationOffset = 1)
        return argv
    }
}
