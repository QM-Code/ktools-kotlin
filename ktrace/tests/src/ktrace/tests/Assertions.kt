package ktrace.tests

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
        throw AssertionError("$message | actual=$actual expected=$expected")
    }

    fun expectContains(haystack: String, needle: String, message: String) {
        expect(haystack.contains(needle), "$message | missing=$needle | output=\n$haystack")
    }

    fun expectNotContains(haystack: String, needle: String, message: String) {
        expect(!haystack.contains(needle), "$message | found=$needle | output=\n$haystack")
    }

    inline fun <reified T : Throwable> expectThrows(message: String, runnable: () -> Unit): T {
        try {
            runnable()
        } catch (ex: Throwable) {
            if (ex is T) {
                return ex
            }
            throw AssertionError("$message | unexpected exception $ex", ex)
        }
        throw AssertionError("$message | expected exception ${T::class.java.name}")
    }
}
