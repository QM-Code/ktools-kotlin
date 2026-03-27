package kcli.demo.bootstrap

import kcli.Parser

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = Parser()
        parser.parseOrExit(args)
        println("Bootstrap succeeded.")
    }
}
