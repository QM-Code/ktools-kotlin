package ktrace.tests

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object TestSupport {
    fun captureStdout(runnable: () -> Unit): String {
        val previous = System.out
        val bytes = ByteArrayOutputStream()
        PrintStream(bytes, true, StandardCharsets.UTF_8).use { capture ->
            try {
                System.setOut(capture)
                runnable()
            } finally {
                System.setOut(previous)
            }
        }
        return bytes.toString(StandardCharsets.UTF_8)
    }

    fun runJava(mainClass: String, vararg args: String): ProcessResult {
        val javaBin = System.getProperty("java.home") + "/bin/java"
        val classpath = System.getProperty("java.class.path")
        val command = mutableListOf(javaBin, "-cp", classpath, mainClass)
        command.addAll(args)
        val process = ProcessBuilder(command).start()
        val stdout = process.inputStream.readAllBytes()
        val stderr = process.errorStream.readAllBytes()
        val exitCode = process.waitFor()
        return ProcessResult(
            exitCode = exitCode,
            stdout = String(stdout, StandardCharsets.UTF_8),
            stderr = String(stderr, StandardCharsets.UTF_8),
        )
    }

    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)
}
