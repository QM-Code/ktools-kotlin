package ktrace.internal

import java.io.PrintStream
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import ktrace.OutputOptions
import ktrace.TraceColors

object TraceInternals {
    private val identifier = Regex("[A-Za-z0-9_-]+")
    private val colorNames = listOf(
        "Black",
        "Red",
        "Green",
        "Yellow",
        "Blue",
        "Magenta",
        "Cyan",
        "White",
        "BrightBlack",
        "BrightRed",
        "BrightGreen",
        "BrightYellow",
        "BrightBlue",
        "BrightMagenta",
        "BrightCyan",
        "BrightWhite",
        "DeepSkyBlue1",
        "Gold3",
        "MediumSpringGreen",
        "Orange3",
        "MediumOrchid1",
        "LightSkyBlue1",
        "LightSalmon1",
    )

    enum class LogSeverity(val label: String) {
        INFO("info"),
        WARNING("warning"),
        ERROR("error"),
    }

    data class CallSite(val fileName: String, val lineNumber: Int, val methodName: String)

    data class ChannelSpec(val name: String, val color: Int)

    data class ExactChannelResolution(
        val key: String,
        val traceNamespace: String,
        val channel: String,
        val registered: Boolean,
    )

    data class Selector(
        val anyNamespace: Boolean,
        val traceNamespace: String,
        val channelTokens: List<String>,
        val includeTopLevel: Boolean,
    )

    data class SelectorResolution(val channelKeys: List<String>, val unmatchedSelectors: List<String>)

    class TraceLoggerData(
        val traceNamespace: String,
        val channels: MutableList<ChannelSpec> = mutableListOf(),
        val changedKeys: ConcurrentHashMap<String, String> = ConcurrentHashMap(),
        val attachedLoggerLock: Any = Any(),
        var attachedLoggerRef: WeakReference<LoggerData?> = WeakReference(null),
    )

    class LoggerData {
        val registryLock = Any()
        val enabledLock = Any()
        val outputLock = Any()
        val hasEnabledChannels = AtomicBoolean(false)
        @Volatile
        var filenamesEnabled = false
        @Volatile
        var lineNumbersEnabled = false
        @Volatile
        var functionNamesEnabled = false
        @Volatile
        var timestampsEnabled = false
        val namespaces = linkedSetOf<String>()
        val channelsByNamespace = linkedMapOf<String, MutableList<String>>()
        val colorsByNamespace = linkedMapOf<String, MutableMap<String, Int>>()
        val enabledChannelKeys = linkedSetOf<String>()
        val attachedTraceLoggers = mutableListOf<TraceLoggerData>()
    }

    fun color(colorName: String): Int {
        val token = trimWhitespace(colorName)
        require(token.isNotEmpty()) { "trace color name must not be empty" }
        if (token == "Default" || token == "default") {
            return TraceColors.DEFAULT
        }
        val index = colorNames.indexOf(token)
        require(index >= 0) { "unknown trace color '$token'" }
        return index
    }

    fun colorNames(): List<String> = colorNames

    fun trimWhitespace(value: String?): String = value?.trim().orEmpty()

    fun normalizeNamespace(traceNamespace: String): String {
        val token = trimWhitespace(traceNamespace)
        require(isSelectorIdentifier(token)) { "invalid trace namespace '$token'" }
        return token
    }

    fun normalizeChannel(channel: String): String {
        val token = trimWhitespace(channel)
        require(isValidChannelPath(token)) { "invalid trace channel '$token'" }
        return token
    }

    fun addChannel(data: TraceLoggerData, channel: String, color: Int) {
        val traceNamespace = normalizeNamespace(data.traceNamespace)
        val channelName = normalizeChannel(channel)
        val parentSeparator = channelName.lastIndexOf('.')
        if (parentSeparator >= 0) {
            val parentChannel = channelName.substring(0, parentSeparator)
            require(findChannelSpec(data, parentChannel) != null) {
                "cannot add unparented trace channel '$channelName' (missing parent '$parentChannel')"
            }
        }

        val existing = findChannelSpec(data, channelName)
        if (existing != null) {
            val merged = mergeColor(existing.color, color, traceNamespace, channelName)
            if (merged != existing.color) {
                data.channels[data.channels.indexOf(existing)] = ChannelSpec(existing.name, merged)
            }
            return
        }

        require(color == TraceColors.DEFAULT || color in 0..255) { "invalid trace color id '$color'" }
        data.channels.add(ChannelSpec(channelName, color))
    }

    fun ensureTraceLoggerCanAttach(traceLogger: TraceLoggerData, loggerData: LoggerData) {
        synchronized(traceLogger.attachedLoggerLock) {
            val attached = traceLogger.attachedLoggerRef.get()
            require(attached == null || attached === loggerData) {
                "trace logger is already attached to another logger"
            }
        }
    }

    fun attachTraceLogger(traceLogger: TraceLoggerData, loggerData: LoggerData) {
        synchronized(traceLogger.attachedLoggerLock) {
            val attached = traceLogger.attachedLoggerRef.get()
            require(attached == null || attached === loggerData) {
                "trace logger is already attached to another logger"
            }
            traceLogger.attachedLoggerRef = WeakReference(loggerData)
        }
    }

    fun attachedLogger(traceLogger: TraceLoggerData): LoggerData? =
        synchronized(traceLogger.attachedLoggerLock) { traceLogger.attachedLoggerRef.get() }

    fun mergeTraceLogger(loggerData: LoggerData, traceLogger: TraceLoggerData) {
        val traceNamespace = normalizeNamespace(traceLogger.traceNamespace)
        synchronized(loggerData.registryLock) {
            loggerData.namespaces.add(traceNamespace)
            val registeredChannels = loggerData.channelsByNamespace.getOrPut(traceNamespace) { mutableListOf() }
            val registeredColors = loggerData.colorsByNamespace.getOrPut(traceNamespace) { linkedMapOf() }
            for (channel in traceLogger.channels) {
                val channelName = normalizeChannel(channel.name)
                val parentSeparator = channelName.lastIndexOf('.')
                if (parentSeparator >= 0) {
                    val parentChannel = channelName.substring(0, parentSeparator)
                    require(registeredChannels.contains(parentChannel)) {
                        "cannot register unparented trace channel '$channelName' (missing parent '$parentChannel')"
                    }
                }
                if (!registeredChannels.contains(channelName)) {
                    registeredChannels.add(channelName)
                }
                val existingColor = registeredColors[channelName] ?: TraceColors.DEFAULT
                val mergedColor = mergeColor(existingColor, channel.color, traceNamespace, channelName)
                if (mergedColor != TraceColors.DEFAULT) {
                    registeredColors[channelName] = mergedColor
                }
            }
        }
    }

    fun retainTraceLogger(loggerData: LoggerData, traceLogger: TraceLoggerData) {
        synchronized(loggerData.registryLock) {
            if (!loggerData.attachedTraceLoggers.contains(traceLogger)) {
                loggerData.attachedTraceLoggers.add(traceLogger)
            }
        }
    }

    fun getOutputOptions(loggerData: LoggerData): OutputOptions =
        OutputOptions(
            filenames = loggerData.filenamesEnabled,
            lineNumbers = loggerData.lineNumbersEnabled,
            functionNames = loggerData.functionNamesEnabled,
            timestamps = loggerData.timestampsEnabled,
        )

    fun setOutputOptions(loggerData: LoggerData, options: OutputOptions) {
        loggerData.filenamesEnabled = options.filenames
        loggerData.lineNumbersEnabled = options.filenames && options.lineNumbers
        loggerData.functionNamesEnabled = options.filenames && options.functionNames
        loggerData.timestampsEnabled = options.timestamps
    }

    fun getNamespaces(loggerData: LoggerData): List<String> =
        synchronized(loggerData.registryLock) { loggerData.namespaces.toList().sorted() }

    fun getChannels(loggerData: LoggerData, traceNamespace: String): List<String> {
        val namespaceName = normalizeNamespace(traceNamespace)
        return synchronized(loggerData.registryLock) {
            loggerData.channelsByNamespace[namespaceName]?.toList()?.sorted() ?: emptyList()
        }
    }

    fun shouldTraceChannel(traceLogger: TraceLoggerData, channel: String): Boolean =
        try {
            val logger = attachedLogger(traceLogger) ?: return false
            shouldTraceChannel(logger, traceLogger.traceNamespace, normalizeChannel(channel))
        } catch (_: RuntimeException) {
            false
        }

    fun shouldTraceQualifiedChannel(loggerData: LoggerData, qualifiedChannel: String, localNamespace: String): Boolean =
        try {
            val resolution = resolveExactChannel(loggerData, qualifiedChannel, localNamespace)
            shouldTraceChannel(loggerData, resolution.traceNamespace, resolution.channel)
        } catch (_: RuntimeException) {
            false
        }

    fun shouldTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean {
        if (!isValidChannelPath(channel) || !loggerData.hasEnabledChannels.get()) {
            return false
        }
        if (!isRegisteredTraceChannel(loggerData, traceNamespace, channel)) {
            return false
        }
        val key = makeQualifiedChannelKey(traceNamespace, channel)
        return synchronized(loggerData.enabledLock) { loggerData.enabledChannelKeys.contains(key) }
    }

    fun isRegisteredTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean {
        val namespaceName = trimWhitespace(traceNamespace)
        val channelName = trimWhitespace(channel)
        if (!isSelectorIdentifier(namespaceName) || !isValidChannelPath(channelName)) {
            return false
        }
        return synchronized(loggerData.registryLock) {
            loggerData.channelsByNamespace[namespaceName]?.contains(channelName) == true
        }
    }

    fun enableChannelKeys(loggerData: LoggerData, channelKeys: List<String>) {
        synchronized(loggerData.enabledLock) {
            for (key in channelKeys) {
                if (key.isNotEmpty()) {
                    loggerData.enabledChannelKeys.add(key)
                }
            }
            loggerData.hasEnabledChannels.set(loggerData.enabledChannelKeys.isNotEmpty())
        }
    }

    fun disableChannelKeys(loggerData: LoggerData, channelKeys: List<String>) {
        synchronized(loggerData.enabledLock) {
            loggerData.enabledChannelKeys.removeAll(channelKeys.toSet())
            loggerData.hasEnabledChannels.set(loggerData.enabledChannelKeys.isNotEmpty())
        }
    }

    fun resolveExactChannel(loggerData: LoggerData, qualifiedChannel: String, localNamespace: String): ExactChannelResolution {
        val qualified = trimWhitespace(qualifiedChannel)
        val dot = qualified.indexOf('.')
        require(dot >= 0) {
            "invalid channel selector '$qualified' (expected namespace.channel or .channel; use .channel for local namespace)"
        }

        val traceNamespace = if (dot == 0) trimWhitespace(localNamespace) else qualified.substring(0, dot)
        val channel = qualified.substring(dot + 1)
        require(isSelectorIdentifier(traceNamespace)) { "invalid trace namespace '$traceNamespace'" }
        require(isValidChannelPath(channel)) { "invalid trace channel '$channel'" }
        val key = makeQualifiedChannelKey(traceNamespace, channel)
        return ExactChannelResolution(key, traceNamespace, channel, isRegisteredTraceChannel(loggerData, traceNamespace, channel))
    }

    fun resolveSelectorExpression(loggerData: LoggerData, selectorsCsv: String, localNamespace: String): SelectorResolution {
        val selectorText = trimWhitespace(selectorsCsv)
        require(selectorText.isNotEmpty()) { "EnableChannels requires one or more selectors" }

        val invalidTokens = mutableListOf<String>()
        val selectors = parseSelectorList(selectorText, localNamespace, invalidTokens)
        if (invalidTokens.isNotEmpty()) {
            val builder = StringBuilder()
            builder.append("Invalid trace selector")
            if (invalidTokens.size > 1) {
                builder.append('s')
            }
            builder.append(": ")
            invalidTokens.forEachIndexed { index, token ->
                if (index > 0) {
                    builder.append(", ")
                }
                builder.append(formatInvalidSelector(token))
            }
            throw IllegalArgumentException(builder.toString())
        }
        return resolveSelectorsToChannelKeys(loggerData, selectors)
    }

    fun captureCallSite(): CallSite {
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
            return CallSite(
                fileName = simplifyFileName(frame.fileName, className),
                lineNumber = frame.lineNumber,
                methodName = frame.methodName,
            )
        }
        return CallSite("unknown", -1, "")
    }

    fun makeTraceChangedSiteKey(channel: String, callSite: CallSite): String =
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

    fun emitTrace(loggerData: LoggerData, traceNamespace: String, channel: String, callSite: CallSite, message: String) {
        emitLine(loggerData, buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite), message, System.out)
    }

    fun emitLog(
        loggerData: LoggerData,
        traceNamespace: String,
        severity: LogSeverity,
        callSite: CallSite,
        message: String,
    ) {
        emitLine(loggerData, buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite), message, System.out)
    }

    fun buildTraceMessagePrefix(loggerData: LoggerData, traceNamespace: String, channel: String, callSite: CallSite): String {
        val out = StringBuilder()
        appendNamespace(out, traceNamespace)
        appendTimestamp(loggerData, out)
        out.append('[').append(channel).append(']')
        appendLocation(loggerData, out, callSite)
        return out.toString()
    }

    fun buildLogMessagePrefix(loggerData: LoggerData, traceNamespace: String, severity: LogSeverity, callSite: CallSite): String {
        val out = StringBuilder()
        appendNamespace(out, traceNamespace)
        appendTimestamp(loggerData, out)
        out.append('[').append(severity.label).append(']')
        appendLocation(loggerData, out, callSite)
        return out.toString()
    }

    fun makeQualifiedChannelKey(traceNamespace: String, channel: String): String {
        val namespaceName = trimWhitespace(traceNamespace)
        val channelName = trimWhitespace(channel)
        if (namespaceName.isEmpty() || channelName.isEmpty()) {
            return ""
        }
        return "$namespaceName.$channelName"
    }

    fun isSelectorIdentifier(token: String): Boolean {
        val value = trimWhitespace(token)
        return value.isNotEmpty() && identifier.matches(value)
    }

    fun isValidChannelPath(channel: String): Boolean {
        val parts = splitChannelPath(channel)
        if (parts.isEmpty() || parts.size > 3) {
            return false
        }
        return parts.all { isSelectorIdentifier(it) }
    }

    private fun findChannelSpec(data: TraceLoggerData, channelName: String): ChannelSpec? =
        data.channels.firstOrNull { it.name == channelName }

    private fun mergeColor(existingColor: Int, newColor: Int, traceNamespace: String, channelName: String): Int {
        if (newColor == TraceColors.DEFAULT) {
            return existingColor
        }
        require(newColor in 0..255) { "invalid trace color id '$newColor'" }
        if (existingColor == TraceColors.DEFAULT) {
            return newColor
        }
        require(existingColor == newColor) { "conflicting trace color for '$traceNamespace.$channelName'" }
        return existingColor
    }

    private fun parseSelectorList(value: String, localNamespace: String, invalidTokens: MutableList<String>): List<Selector> {
        val selectors = mutableListOf<Selector>()
        val selectorTokens = mutableListOf<String>()
        val splitError = splitByTopLevelCommas(value, selectorTokens)
        if (splitError != null) {
            invalidTokens.add(splitError)
            return selectors
        }

        val invalidSeen = linkedSetOf<String>()
        for (token in selectorTokens) {
            val name = trimWhitespace(token)
            if (name.isEmpty()) {
                if (invalidSeen.add("<empty>")) {
                    invalidTokens.add("<empty>")
                }
                continue
            }

            val expandedTokens = mutableListOf<String>()
            val expandError = expandBraceExpression(name, expandedTokens)
            if (expandError != null) {
                val reason = "$name ($expandError)"
                if (invalidSeen.add(reason)) {
                    invalidTokens.add(reason)
                }
                continue
            }

            for (expanded in expandedTokens) {
                try {
                    selectors.add(parseSelector(expanded, localNamespace))
                } catch (ex: IllegalArgumentException) {
                    val reason = "$expanded (${ex.message})"
                    if (invalidSeen.add(reason)) {
                        invalidTokens.add(reason)
                    }
                }
            }
        }
        return selectors
    }

    private fun resolveSelectorsToChannelKeys(loggerData: LoggerData, selectors: List<Selector>): SelectorResolution {
        val channelKeys = mutableListOf<String>()
        val unmatchedSelectors = mutableListOf<String>()
        val seenKeys = linkedSetOf<String>()
        val matched = BooleanArray(selectors.size)

        synchronized(loggerData.registryLock) {
            for ((traceNamespace, channels) in loggerData.channelsByNamespace) {
                for (channel in channels) {
                    for (index in selectors.indices) {
                        if (!matchesSelector(selectors[index], traceNamespace, channel)) {
                            continue
                        }
                        matched[index] = true
                        val key = makeQualifiedChannelKey(traceNamespace, channel)
                        if (seenKeys.add(key)) {
                            channelKeys.add(key)
                        }
                    }
                }
            }
        }

        val unmatchedSeen = linkedSetOf<String>()
        for (index in selectors.indices) {
            if (matched[index]) {
                continue
            }
            val selectorText = formatSelector(selectors[index])
            if (unmatchedSeen.add(selectorText)) {
                unmatchedSelectors.add(selectorText)
            }
        }

        return SelectorResolution(channelKeys, unmatchedSelectors)
    }

    private fun parseSelector(rawToken: String, localNamespace: String): Selector {
        val dot = rawToken.indexOf('.')
        require(dot >= 0) { "did you mean '.*'?" }

        val namespaceToken = rawToken.substring(0, dot)
        val channelPattern = rawToken.substring(dot + 1)
        var anyNamespace = false
        var traceNamespace = ""
        when {
            namespaceToken == "*" -> anyNamespace = true
            namespaceToken.isEmpty() -> {
                traceNamespace = trimWhitespace(localNamespace)
                require(isSelectorIdentifier(traceNamespace)) { "missing namespace" }
            }
            isSelectorIdentifier(namespaceToken) -> traceNamespace = namespaceToken
            else -> throw IllegalArgumentException("invalid namespace '$namespaceToken'")
        }

        val tokens = splitChannelPath(channelPattern)
        require(tokens.isNotEmpty()) { "missing channel expression" }
        require(tokens.size <= 3) { "channel depth exceeds 3" }
        for (token in tokens) {
            require(token == "*" || isSelectorIdentifier(token)) { "invalid channel token '$token'" }
        }
        val includeTopLevel = tokens.size == 2 && tokens[0] == "*" && tokens[1] == "*"
        return Selector(anyNamespace, traceNamespace, tokens.toList(), includeTopLevel)
    }

    private fun matchesSelector(selector: Selector, traceNamespace: String, channel: String): Boolean {
        if (!selector.anyNamespace && traceNamespace != selector.traceNamespace) {
            return false
        }
        val channelParts = splitChannelPath(channel)
        if (channelParts.isEmpty()) {
            return false
        }
        val pattern = selector.channelTokens
        return when (pattern.size) {
            1 -> channelParts.size == 1 && matchesSelectorSegment(pattern[0], channelParts[0])
            2 -> {
                if (channelParts.size == 1 && selector.includeTopLevel) {
                    true
                } else {
                    channelParts.size == 2 &&
                        matchesSelectorSegment(pattern[0], channelParts[0]) &&
                        matchesSelectorSegment(pattern[1], channelParts[1])
                }
            }
            3 -> {
                if (pattern[0] == "*" && pattern[1] == "*" && pattern[2] == "*") {
                    channelParts.size in 1..3
                } else {
                    channelParts.size == 3 &&
                        matchesSelectorSegment(pattern[0], channelParts[0]) &&
                        matchesSelectorSegment(pattern[1], channelParts[1]) &&
                        matchesSelectorSegment(pattern[2], channelParts[2])
                }
            }
            else -> false
        }
    }

    private fun matchesSelectorSegment(pattern: String, value: String): Boolean = pattern == "*" || pattern == value

    private fun splitChannelPath(channel: String): List<String> {
        val token = trimWhitespace(channel)
        if (token.isEmpty()) {
            return emptyList()
        }
        return token.split('.')
    }

    private fun splitByTopLevelCommas(value: String, parts: MutableList<String>): String? {
        var braceDepth = 0
        var start = 0
        for (index in value.indices) {
            when (value[index]) {
                '{' -> braceDepth += 1
                '}' -> {
                    if (braceDepth == 0) {
                        return "unmatched '}'"
                    }
                    braceDepth -= 1
                }
                ',' -> if (braceDepth == 0) {
                    parts.add(trimWhitespace(value.substring(start, index)))
                    start = index + 1
                }
            }
        }
        if (braceDepth != 0) {
            return "unmatched '{'"
        }
        parts.add(trimWhitespace(value.substring(start)))
        return null
    }

    private fun expandBraceExpression(value: String, expanded: MutableList<String>): String? {
        val open = value.indexOf('{')
        if (open < 0) {
            expanded.add(value)
            return null
        }

        var depth = 0
        var close = -1
        for (index in open until value.length) {
            when (value[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        close = index
                        break
                    }
                }
            }
        }
        if (close < 0) {
            return "unmatched '{'"
        }

        val prefix = value.substring(0, open)
        val suffix = value.substring(close + 1)
        val inside = value.substring(open + 1, close)
        val alternatives = mutableListOf<String>()
        val splitError = splitByTopLevelCommas(inside, alternatives)
        if (splitError != null) {
            return splitError
        }
        if (alternatives.isEmpty()) {
            return "empty brace group"
        }
        for (alternative in alternatives) {
            if (alternative.isEmpty()) {
                return "empty brace alternative"
            }
            val nestedError = expandBraceExpression(prefix + alternative + suffix, expanded)
            if (nestedError != null) {
                return nestedError
            }
        }
        return null
    }

    private fun formatInvalidSelector(token: String): String {
        val reasonPos = token.indexOf(" (")
        return if (reasonPos >= 0) {
            "'${token.substring(0, reasonPos)}'${token.substring(reasonPos)}"
        } else {
            "'$token'"
        }
    }

    private fun formatSelector(selector: Selector): String =
        buildString {
            append(if (selector.anyNamespace) "*" else selector.traceNamespace)
            append('.')
            selector.channelTokens.forEachIndexed { index, token ->
                if (index > 0) {
                    append('.')
                }
                append(token)
            }
        }

    private fun emitLine(loggerData: LoggerData, prefix: String, message: String, out: PrintStream) {
        synchronized(loggerData.outputLock) {
            out.print(prefix)
            out.print(' ')
            out.print(message)
            out.print('\n')
            out.flush()
        }
    }

    private fun appendNamespace(out: StringBuilder, traceNamespace: String) {
        if (traceNamespace.isEmpty()) {
            return
        }
        out.append('[').append(traceNamespace).append("] ")
    }

    private fun appendTimestamp(loggerData: LoggerData, out: StringBuilder) {
        if (!loggerData.timestampsEnabled) {
            return
        }
        val now = Instant.now()
        out.append('[')
            .append(now.epochSecond)
            .append('.')
            .append(String.format(Locale.ROOT, "%06d", now.nano / 1000))
            .append("] ")
    }

    private fun appendLocation(loggerData: LoggerData, out: StringBuilder, callSite: CallSite) {
        if (!loggerData.filenamesEnabled) {
            return
        }
        out.append(" [").append(callSite.fileName)
        if (loggerData.lineNumbersEnabled && callSite.lineNumber > 0) {
            out.append(':').append(callSite.lineNumber)
        }
        if (loggerData.functionNamesEnabled && callSite.methodName.isNotEmpty()) {
            out.append(':').append(callSite.methodName)
        }
        out.append(']')
    }

    private fun simplifyFileName(fileName: String?, className: String): String {
        val candidate = trimWhitespace(fileName)
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
