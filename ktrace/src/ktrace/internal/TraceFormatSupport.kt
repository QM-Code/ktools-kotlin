package ktrace.internal

object TraceFormatSupport {
    fun captureCallSite(): TraceInternals.CallSite {
        val frames = Thread.currentThread().stackTrace
        var sawPublicApi = false
        for (frame in frames) {
            val className = frame.className
            if (className == Thread::class.java.name) {
                continue
            }
            if (className == "ktrace.TraceLogger" || className == "ktrace.Logger" || className.startsWith("ktrace.internal.")) {
                sawPublicApi = true
                continue
            }
            if (!sawPublicApi) {
                continue
            }
            return TraceInternals.CallSite(
                fileName = simplifyFileName(frame.fileName, className),
                lineNumber = frame.lineNumber,
                methodName = frame.methodName,
            )
        }
        return TraceInternals.CallSite("unknown", -1, "")
    }

    fun makeTraceChangedSiteKey(channel: String, callSite: TraceInternals.CallSite): String =
        "${callSite.fileName}:${callSite.lineNumber}:${callSite.methodName}:$channel"

    fun formatArgument(value: Any?): String = value.toString()

    fun formatMessage(formatText: String?, vararg args: Any?): String {
        val text = formatText.orEmpty()
        val formattedArgs = args.map { formatArgument(it) }
        val out = StringBuilder(text.length)
        var argIndex = 0
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            if (ch == '{') {
                require(index + 1 < text.length) { "unterminated '{' in trace format string" }
                val next = text[index + 1]
                when (next) {
                    '{' -> {
                        out.append('{')
                        index += 2
                        continue
                    }
                    '}' -> {
                        require(argIndex < formattedArgs.size) { "not enough arguments for trace format string" }
                        out.append(formattedArgs[argIndex++])
                        index += 2
                        continue
                    }
                    else -> throw IllegalArgumentException("unsupported trace format token")
                }
            }
            if (ch == '}') {
                if (index + 1 < text.length && text[index + 1] == '}') {
                    out.append('}')
                    index += 2
                    continue
                }
                throw IllegalArgumentException("unmatched '}' in trace format string")
            }
            out.append(ch)
            index += 1
        }
        require(argIndex == formattedArgs.size) { "too many arguments for trace format string" }
        return out.toString()
    }

    private fun simplifyFileName(fileName: String?, className: String): String {
        val candidate = TraceNamingSupport.trimWhitespace(fileName)
        if (candidate.isNotEmpty()) {
            val dot = candidate.lastIndexOf('.')
            return if (dot > 0) candidate.substring(0, dot) else candidate
        }
        val separator = className.lastIndexOf('.')
        val simple = if (separator >= 0) className.substring(separator + 1) else className
        val dollar = simple.indexOf('$')
        return if (dollar >= 0) simple.substring(0, dollar) else simple
    }
}
