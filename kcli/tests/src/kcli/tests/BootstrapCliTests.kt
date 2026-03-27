package kcli.tests

object BootstrapCliTests {
    fun run() {
        val result = TestSupport.runJava("kcli.demo.bootstrap.Main")
        Assertions.expectEquals(result.exitCode, 0, "bootstrap demo should succeed")
        Assertions.expectContains(result.stdout, "Bootstrap succeeded.", "bootstrap should report success")
    }
}
