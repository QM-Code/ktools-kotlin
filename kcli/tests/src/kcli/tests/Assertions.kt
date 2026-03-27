package kcli.tests

object Assertions {
    fun expect(condition: Boolean, message: String) {
        if (!condition) {
            throw AssertionError(message)
        }
    }

    fun expectEquals(actual: Any?, expected: Any?, message: String) {
        if (actual == expected) {
            return
        }
        throw AssertionError("$message\nexpected: $expected\nactual:   $actual")
    }

    fun expectContains(actual: String?, needle: String, message: String) {
        if (actual != null && actual.contains(needle)) {
            return
        }
        throw AssertionError("$message\nmissing:  $needle\nactual:   $actual")
    }

    fun expectNotContains(actual: String?, needle: String, message: String) {
        if (actual == null || !actual.contains(needle)) {
            return
        }
        throw AssertionError("$message\nunexpected: $needle\nactual:     $actual")
    }

    inline fun <reified T : Throwable> expectThrows(message: String, runnable: () -> Unit): T {
        try {
            runnable()
        } catch (ex: Throwable) {
            if (ex is T) {
                return ex
            }
            throw AssertionError("$message\nunexpected exception: $ex", ex)
        }
        throw AssertionError("$message\nexpected exception: ${T::class.java.name}")
    }
}
