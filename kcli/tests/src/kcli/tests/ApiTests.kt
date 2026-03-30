package kcli.tests

object ApiTests {
    fun run() {
        AliasApiTests.run()
        InlineApiTests.run()
        ParseLifecycleApiTests.run()
        ValueApiTests.run()
    }
}
