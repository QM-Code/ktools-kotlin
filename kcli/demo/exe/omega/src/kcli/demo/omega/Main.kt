package kcli.demo.omega

import kcli.InlineParser
import kcli.Parser
import kcli.demo.alpha.AlphaSdk
import kcli.demo.beta.BetaSdk
import kcli.demo.common.DemoSupport.withProgram
import kcli.demo.gamma.GammaSdk

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val argv = withProgram("kcli_demo_omega", args)

        val parser = Parser()
        val alphaParser = AlphaSdk.getInlineParser()
        val betaParser = BetaSdk.getInlineParser()
        val gammaParser = GammaSdk.getInlineParser()
        val buildParser = InlineParser("--build")

        gammaParser.setRoot("--newgamma")
        buildParser.setHandler("-profile", { _, _ -> }, "Set build profile.")
        buildParser.setHandler("-clean", { _ -> }, "Enable clean build.")

        parser.addInlineParser(alphaParser)
        parser.addInlineParser(betaParser)
        parser.addInlineParser(gammaParser)
        parser.addInlineParser(buildParser)

        parser.addAlias("-v", "--verbose")
        parser.addAlias("-out", "--output")
        parser.addAlias("-a", "--alpha-enable")
        parser.addAlias("-b", "--build-profile")
        parser.setHandler("--verbose", { _ -> }, "Enable verbose app logging.")
        parser.setHandler("--output", { _, _ -> }, "Set app output target.")
        parser.setPositionalHandler { _ -> }

        parser.parseOrExit(argv.size, argv)

        println()
        println("Usage:")
        println("  kcli_demo_omega --<root>")
        println()
        println("Enabled --<root> prefixes:")
        println("  --alpha")
        println("  --beta")
        println("  --newgamma (gamma override)")
        println()
    }
}
