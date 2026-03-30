package ktrace

import kcli.InlineParser
import ktrace.internal.TraceInternals
import ktrace.internal.CallSite
import ktrace.internal.LogSeverity
import ktrace.internal.LoggerData
import ktrace.internal.TraceLoggerData

class OutputOptions(
    filenames: Boolean,
    lineNumbers: Boolean,
    functionNames: Boolean,
    val timestamps: Boolean,
) {
    val filenames: Boolean = filenames
    val lineNumbers: Boolean = filenames && lineNumbers
    val functionNames: Boolean = filenames && functionNames
}

object TraceColors {
    const val DEFAULT: Int = 0xFFFF

    fun color(colorName: String): Int = TraceInternals.color(colorName)

    fun names(): List<String> = TraceInternals.colorNames()
}

class TraceLogger(traceNamespace: String) {
    internal val data = TraceLoggerData(TraceInternals.normalizeNamespace(traceNamespace))

    fun addChannel(channel: String) {
        addChannel(channel, TraceColors.DEFAULT)
    }

    fun addChannel(channel: String, color: Int) {
        TraceInternals.addChannel(data, channel, color)
    }

    val namespace: String
        get() = data.traceNamespace

    fun shouldTraceChannel(channel: String): Boolean = TraceInternals.shouldTraceChannel(data, channel)

    fun trace(channel: String, formatText: String, vararg args: Any?) {
        val normalizedChannel = TraceInternals.normalizeChannel(channel)
        val logger = TraceInternals.attachedLogger(data) ?: return
        if (!TraceInternals.shouldTraceChannel(logger, data.traceNamespace, normalizedChannel)) {
            return
        }

        val callSite = TraceInternals.captureCallSite()
        val message = TraceInternals.formatMessage(formatText, *args)
        TraceInternals.emitTrace(logger, data.traceNamespace, normalizedChannel, callSite, message)
    }

    fun traceChanged(channel: String, keyExpr: Any?, formatText: String, vararg args: Any?) {
        val normalizedChannel = TraceInternals.normalizeChannel(channel)
        val callSite = TraceInternals.captureCallSite()
        val siteKey = TraceInternals.makeTraceChangedSiteKey(normalizedChannel, callSite)
        val nextKey = TraceInternals.formatArgument(keyExpr)
        val previous = data.changedKeys.put(siteKey, nextKey)
        if (previous == nextKey) {
            return
        }
        trace(channel, formatText, *args)
    }

    fun info(formatText: String, vararg args: Any?) {
        log(LogSeverity.INFO, formatText, *args)
    }

    fun warn(formatText: String, vararg args: Any?) {
        log(LogSeverity.WARNING, formatText, *args)
    }

    fun error(formatText: String, vararg args: Any?) {
        log(LogSeverity.ERROR, formatText, *args)
    }

    private fun log(severity: LogSeverity, formatText: String, vararg args: Any?) {
        val logger = TraceInternals.attachedLogger(data) ?: return
        val callSite = TraceInternals.captureCallSite()
        val message = TraceInternals.formatMessage(formatText, *args)
        TraceInternals.emitLog(logger, data.traceNamespace, severity, callSite, message)
    }
}

class Logger {
    private val data = LoggerData()
    private val internalTrace = TraceLogger("ktrace")

    init {
        configureInternalTrace(internalTrace)
        addTraceLogger(internalTrace)
    }

    fun addTraceLogger(logger: TraceLogger) {
        val traceData = logger.data
        TraceInternals.ensureTraceLoggerCanAttach(traceData, data)
        TraceInternals.mergeTraceLogger(data, traceData)
        TraceInternals.retainTraceLogger(data, traceData)
        TraceInternals.attachTraceLogger(traceData, data)
    }

    fun enableChannel(qualifiedChannel: String) {
        enableChannel(qualifiedChannel, "")
    }

    fun enableChannel(localTraceLogger: TraceLogger, qualifiedChannel: String) {
        enableChannel(qualifiedChannel, localTraceLogger.namespace)
    }

    fun enableChannels(selectorsCsv: String) {
        enableChannels(selectorsCsv, "")
    }

    fun enableChannels(localTraceLogger: TraceLogger, selectorsCsv: String) {
        enableChannels(selectorsCsv, localTraceLogger.namespace)
    }

    fun shouldTraceChannel(qualifiedChannel: String): Boolean = shouldTraceChannel(qualifiedChannel, "")

    fun shouldTraceChannel(localTraceLogger: TraceLogger, qualifiedChannel: String): Boolean =
        shouldTraceChannel(qualifiedChannel, localTraceLogger.namespace)

    fun disableChannel(qualifiedChannel: String) {
        disableChannel(qualifiedChannel, "")
    }

    fun disableChannel(localTraceLogger: TraceLogger, qualifiedChannel: String) {
        disableChannel(qualifiedChannel, localTraceLogger.namespace)
    }

    fun disableChannels(selectorsCsv: String) {
        disableChannels(selectorsCsv, "")
    }

    fun disableChannels(localTraceLogger: TraceLogger, selectorsCsv: String) {
        disableChannels(selectorsCsv, localTraceLogger.namespace)
    }

    fun setOutputOptions(options: OutputOptions) {
        TraceInternals.setOutputOptions(data, options)
        internalTrace.trace("api", "updating output options (enable api.output for details)")
        val next = outputOptions
        internalTrace.trace(
            "api.output",
            "set output options: filenames={} line_numbers={} function_names={} timestamps={}",
            next.filenames,
            next.lineNumbers,
            next.functionNames,
            next.timestamps,
        )
    }

    val outputOptions: OutputOptions
        get() = TraceInternals.getOutputOptions(data)

    val namespaces: List<String>
        get() = TraceInternals.getNamespaces(data)

    fun channels(traceNamespace: String): List<String> = TraceInternals.getChannels(data, traceNamespace)

    fun makeInlineParser(localTraceLogger: TraceLogger): InlineParser = makeInlineParser(localTraceLogger, "trace")

    fun makeInlineParser(localTraceLogger: TraceLogger, traceRoot: String): InlineParser =
        TraceCliSupport.makeInlineParser(this, localTraceLogger, traceRoot)

    fun makeInlineParser(traceRoot: String): InlineParser =
        TraceCliSupport.makeInlineParser(this, null, traceRoot)

    private fun enableChannel(qualifiedChannel: String, localNamespace: String) {
        val resolution = TraceInternals.resolveExactChannel(data, qualifiedChannel, localNamespace)
        if (!resolution.registered) {
            log(
                LogSeverity.WARNING,
                localNamespace,
                TraceInternals.captureCallSite(),
                TraceInternals.formatMessage(
                    "enable ignored channel '{}' because it is not registered",
                    resolution.key,
                ),
            )
            return
        }
        TraceInternals.enableChannelKeys(data, listOf(resolution.key))
        internalTrace.trace("api.channels", "enabled channel '{}'", TraceInternals.trimWhitespace(qualifiedChannel))
    }

    private fun enableChannels(selectorsCsv: String, localNamespace: String) {
        val selectorText = TraceInternals.trimWhitespace(selectorsCsv)
        val resolution = TraceInternals.resolveSelectorExpression(data, selectorText, localNamespace)
        TraceInternals.enableChannelKeys(data, resolution.channelKeys)
        val callSite = TraceInternals.captureCallSite()
        for (unmatched in resolution.unmatchedSelectors) {
            log(
                LogSeverity.WARNING,
                localNamespace,
                callSite,
                TraceInternals.formatMessage(
                    "enable ignored channel selector '{}' because it matched no registered channels",
                    unmatched,
                ),
            )
        }
        internalTrace.trace(
            "api",
            "processing channels (enable api.channels for details): enabled {} channel(s), {} unmatched selector(s)",
            resolution.channelKeys.size,
            resolution.unmatchedSelectors.size,
        )
        internalTrace.trace(
            "api.channels",
            "enabled {} channel(s) from '{}' ({} unmatched selector(s))",
            resolution.channelKeys.size,
            selectorText,
            resolution.unmatchedSelectors.size,
        )
    }

    private fun shouldTraceChannel(qualifiedChannel: String, localNamespace: String): Boolean =
        TraceInternals.shouldTraceQualifiedChannel(data, qualifiedChannel, localNamespace)

    private fun disableChannel(qualifiedChannel: String, localNamespace: String) {
        val resolution = TraceInternals.resolveExactChannel(data, qualifiedChannel, localNamespace)
        if (!resolution.registered) {
            log(
                LogSeverity.WARNING,
                localNamespace,
                TraceInternals.captureCallSite(),
                TraceInternals.formatMessage(
                    "disable ignored channel '{}' because it is not registered",
                    resolution.key,
                ),
            )
            return
        }
        TraceInternals.disableChannelKeys(data, listOf(resolution.key))
        internalTrace.trace("api.channels", "disabled channel '{}'", TraceInternals.trimWhitespace(qualifiedChannel))
    }

    private fun disableChannels(selectorsCsv: String, localNamespace: String) {
        val selectorText = TraceInternals.trimWhitespace(selectorsCsv)
        val resolution = TraceInternals.resolveSelectorExpression(data, selectorText, localNamespace)
        TraceInternals.disableChannelKeys(data, resolution.channelKeys)
        val callSite = TraceInternals.captureCallSite()
        for (unmatched in resolution.unmatchedSelectors) {
            log(
                LogSeverity.WARNING,
                localNamespace,
                callSite,
                TraceInternals.formatMessage(
                    "disable ignored channel selector '{}' because it matched no registered channels",
                    unmatched,
                ),
            )
        }
        internalTrace.trace(
            "api",
            "processing channels (enable api.channels for details): disabled {} channel(s), {} unmatched selector(s)",
            resolution.channelKeys.size,
            resolution.unmatchedSelectors.size,
        )
        internalTrace.trace(
            "api.channels",
            "disabled {} channel(s) from '{}' ({} unmatched selector(s))",
            resolution.channelKeys.size,
            selectorText,
            resolution.unmatchedSelectors.size,
        )
    }

    private fun log(severity: LogSeverity, traceNamespace: String, callSite: CallSite, message: String) {
        TraceInternals.emitLog(data, traceNamespace, severity, callSite, message)
    }

    private fun configureInternalTrace(logger: TraceLogger) {
        logger.addChannel("api", 6)
        logger.addChannel("api.channels")
        logger.addChannel("api.cli")
        logger.addChannel("api.output")
        logger.addChannel("selector", 3)
        logger.addChannel("selector.parse")
        logger.addChannel("registry", 5)
        logger.addChannel("registry.query")
    }
}
