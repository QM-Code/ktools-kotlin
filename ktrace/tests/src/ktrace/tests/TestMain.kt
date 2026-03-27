package ktrace.tests

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
        ApiTests.run()
        BootstrapCliTests.run()
        CoreCliTests.run()
        OmegaCliTests.run()
        println("Kotlin ktrace tests passed.")
    }
}
