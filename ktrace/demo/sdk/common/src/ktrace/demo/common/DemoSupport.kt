package ktrace.demo.common

object DemoSupport {
    fun withProgram(programName: String, args: Array<String>?): Array<String> {
        val argv = Array((args?.size ?: 0) + 1) { "" }
        argv[0] = programName
        args?.copyInto(argv, destinationOffset = 1)
        return argv
    }
}
