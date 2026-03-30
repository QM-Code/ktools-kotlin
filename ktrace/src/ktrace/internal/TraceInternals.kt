package ktrace.internal

import ktrace.OutputOptions

object TraceInternals {
    fun color(colorName: String): Int = TraceNamingSupport.color(colorName)

    fun colorNames(): List<String> = TraceNamingSupport.colorNames()

    fun trimWhitespace(value: String?): String = TraceNamingSupport.trimWhitespace(value)

    fun normalizeNamespace(traceNamespace: String): String = TraceNamingSupport.normalizeNamespace(traceNamespace)

    fun normalizeChannel(channel: String): String = TraceNamingSupport.normalizeChannel(channel)

    fun addChannel(data: TraceLoggerData, channel: String, color: Int) {
        TraceRegistrySupport.addChannel(data, channel, color)
    }

    fun ensureTraceLoggerCanAttach(traceLogger: TraceLoggerData, loggerData: LoggerData) {
        TraceRegistrySupport.ensureTraceLoggerCanAttach(traceLogger, loggerData)
    }

    fun attachTraceLogger(traceLogger: TraceLoggerData, loggerData: LoggerData) {
        TraceRegistrySupport.attachTraceLogger(traceLogger, loggerData)
    }

    fun attachedLogger(traceLogger: TraceLoggerData): LoggerData? = TraceRegistrySupport.attachedLogger(traceLogger)

    fun mergeTraceLogger(loggerData: LoggerData, traceLogger: TraceLoggerData) {
        TraceRegistrySupport.mergeTraceLogger(loggerData, traceLogger)
    }

    fun retainTraceLogger(loggerData: LoggerData, traceLogger: TraceLoggerData) {
        TraceRegistrySupport.retainTraceLogger(loggerData, traceLogger)
    }

    fun getOutputOptions(loggerData: LoggerData): OutputOptions = TraceRegistrySupport.getOutputOptions(loggerData)

    fun setOutputOptions(loggerData: LoggerData, options: OutputOptions) {
        TraceRegistrySupport.setOutputOptions(loggerData, options)
    }

    fun getNamespaces(loggerData: LoggerData): List<String> = TraceRegistrySupport.getNamespaces(loggerData)

    fun getChannels(loggerData: LoggerData, traceNamespace: String): List<String> =
        TraceRegistrySupport.getChannels(loggerData, traceNamespace)

    fun shouldTraceChannel(traceLogger: TraceLoggerData, channel: String): Boolean =
        TraceRegistrySupport.shouldTraceChannel(traceLogger, channel)

    fun shouldTraceQualifiedChannel(loggerData: LoggerData, qualifiedChannel: String, localNamespace: String): Boolean =
        TraceRegistrySupport.shouldTraceQualifiedChannel(loggerData, qualifiedChannel, localNamespace)

    fun shouldTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean =
        TraceRegistrySupport.shouldTraceChannel(loggerData, traceNamespace, channel)

    fun isRegisteredTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean =
        TraceRegistrySupport.isRegisteredTraceChannel(loggerData, traceNamespace, channel)

    fun enableChannelKeys(loggerData: LoggerData, channelKeys: List<String>) {
        TraceRegistrySupport.enableChannelKeys(loggerData, channelKeys)
    }

    fun disableChannelKeys(loggerData: LoggerData, channelKeys: List<String>) {
        TraceRegistrySupport.disableChannelKeys(loggerData, channelKeys)
    }

    fun resolveExactChannel(loggerData: LoggerData, qualifiedChannel: String, localNamespace: String): ExactChannelResolution =
        TraceSelectorSupport.resolveExactChannel(loggerData, qualifiedChannel, localNamespace)

    fun resolveSelectorExpression(loggerData: LoggerData, selectorsCsv: String, localNamespace: String): SelectorResolution =
        TraceSelectorSupport.resolveSelectorExpression(loggerData, selectorsCsv, localNamespace)

    fun captureCallSite(): CallSite = TraceFormatSupport.captureCallSite()

    fun makeTraceChangedSiteKey(channel: String, callSite: CallSite): String =
        TraceFormatSupport.makeTraceChangedSiteKey(channel, callSite)

    fun formatArgument(value: Any?): String = TraceFormatSupport.formatArgument(value)

    fun formatMessage(formatText: String?, vararg args: Any?): String = TraceFormatSupport.formatMessage(formatText, *args)

    fun emitTrace(loggerData: LoggerData, traceNamespace: String, channel: String, callSite: CallSite, message: String) {
        TraceOutputSupport.emitTrace(loggerData, traceNamespace, channel, callSite, message)
    }

    fun emitLog(
        loggerData: LoggerData,
        traceNamespace: String,
        severity: LogSeverity,
        callSite: CallSite,
        message: String,
    ) {
        TraceOutputSupport.emitLog(loggerData, traceNamespace, severity, callSite, message)
    }

    fun buildTraceMessagePrefix(loggerData: LoggerData, traceNamespace: String, channel: String, callSite: CallSite): String =
        TraceOutputSupport.buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite)

    fun buildLogMessagePrefix(loggerData: LoggerData, traceNamespace: String, severity: LogSeverity, callSite: CallSite): String =
        TraceOutputSupport.buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite)

    fun makeQualifiedChannelKey(traceNamespace: String, channel: String): String =
        TraceNamingSupport.makeQualifiedChannelKey(traceNamespace, channel)

    fun isSelectorIdentifier(token: String): Boolean = TraceNamingSupport.isSelectorIdentifier(token)

    fun isValidChannelPath(channel: String): Boolean = TraceNamingSupport.isValidChannelPath(channel)
}
