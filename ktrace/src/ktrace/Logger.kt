package ktrace

import kcli.HandlerContext
import kcli.InlineParser
import ktrace.internal.TraceInternals
import ktrace.internal.TraceInternals.CallSite
import ktrace.internal.TraceInternals.ExactChannelResolution
import ktrace.internal.TraceInternals.LogSeverity
import ktrace.internal.TraceInternals.LoggerData
import ktrace.internal.TraceInternals.SelectorResolution
import ktrace.internal.TraceInternals.TraceLoggerData

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

    fun getNamespace(): String = data.traceNamespace

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

    fun enableChannel(localTraceLogger: TraceLogger?, qualifiedChannel: String) {
        enableChannel(qualifiedChannel, localTraceLogger?.getNamespace().orEmpty())
    }

    fun enableChannels(selectorsCsv: String) {
        enableChannels(selectorsCsv, "")
    }

    fun enableChannels(localTraceLogger: TraceLogger?, selectorsCsv: String) {
        enableChannels(selectorsCsv, localTraceLogger?.getNamespace().orEmpty())
    }

    fun shouldTraceChannel(qualifiedChannel: String): Boolean = shouldTraceChannel(qualifiedChannel, "")

    fun shouldTraceChannel(localTraceLogger: TraceLogger?, qualifiedChannel: String): Boolean =
        shouldTraceChannel(qualifiedChannel, localTraceLogger?.getNamespace().orEmpty())

    fun disableChannel(qualifiedChannel: String) {
        disableChannel(qualifiedChannel, "")
    }

    fun disableChannel(localTraceLogger: TraceLogger?, qualifiedChannel: String) {
        disableChannel(qualifiedChannel, localTraceLogger?.getNamespace().orEmpty())
    }

    fun disableChannels(selectorsCsv: String) {
        disableChannels(selectorsCsv, "")
    }

    fun disableChannels(localTraceLogger: TraceLogger?, selectorsCsv: String) {
        disableChannels(selectorsCsv, localTraceLogger?.getNamespace().orEmpty())
    }

    fun setOutputOptions(options: OutputOptions) {
        TraceInternals.setOutputOptions(data, options)
        internalTrace.trace("api", "updating output options (enable api.output for details)")
        val next = getOutputOptions()
        internalTrace.trace(
            "api.output",
            "set output options: filenames={} line_numbers={} function_names={} timestamps={}",
            next.filenames,
            next.lineNumbers,
            next.functionNames,
            next.timestamps,
        )
    }

    fun getOutputOptions(): OutputOptions = TraceInternals.getOutputOptions(data)

    fun getNamespaces(): List<String> = TraceInternals.getNamespaces(data)

    fun getChannels(traceNamespace: String): List<String> = TraceInternals.getChannels(data, traceNamespace)

    fun makeInlineParser(localTraceLogger: TraceLogger): InlineParser = makeInlineParser(localTraceLogger, "trace")

    fun makeInlineParser(localTraceLogger: TraceLogger?, traceRoot: String): InlineParser {
        val localNamespace = localTraceLogger?.getNamespace().orEmpty()
        val parser = InlineParser(if (traceRoot.isBlank()) "trace" else traceRoot)
        parser.setRootValueHandler(
            { _, value -> enableChannels(value, localNamespace) },
            "<channels>",
            "Trace selected channels.",
        )
        parser.setHandler("-examples", { context -> handleExamples(context) }, "Show selector examples.")
        parser.setHandler("-namespaces", { _ -> handleNamespaces() }, "Show initialized trace namespaces.")
        parser.setHandler("-channels", { _ -> handleChannels() }, "Show initialized trace channels.")
        parser.setHandler("-colors", { _ -> handleColors() }, "Show available trace colors.")
        parser.setHandler(
            "-files",
            { _ ->
                setOutputOptions(
                    OutputOptions(
                        filenames = true,
                        lineNumbers = true,
                        functionNames = false,
                        timestamps = getOutputOptions().timestamps,
                    ),
                )
            },
            "Include source file and line in trace output.",
        )
        parser.setHandler(
            "-functions",
            { _ ->
                setOutputOptions(
                    OutputOptions(
                        filenames = true,
                        lineNumbers = true,
                        functionNames = true,
                        timestamps = getOutputOptions().timestamps,
                    ),
                )
            },
            "Include function names in trace output.",
        )
        parser.setHandler(
            "-timestamps",
            { _ ->
                val options = getOutputOptions()
                setOutputOptions(
                    OutputOptions(
                        filenames = options.filenames,
                        lineNumbers = options.lineNumbers,
                        functionNames = options.functionNames,
                        timestamps = true,
                    ),
                )
            },
            "Include timestamps in trace output.",
        )
        return parser
    }

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

    private fun handleExamples(context: HandlerContext) {
        val optionRoot = "--${context.root}"
        println()
        println("General trace selector pattern:")
        println("  $optionRoot <namespace>.<channel>[.<subchannel>[.<subchannel>]]")
        println()
        println("Trace selector examples:")
        println("  $optionRoot '.abc'           Select local 'abc' in current namespace")
        println("  $optionRoot '.abc.xyz'       Select local nested channel in current namespace")
        println("  $optionRoot 'otherapp.channel' Select explicit namespace channel")
        println("  $optionRoot '*.*'            Select all <namespace>.<channel> channels")
        println("  $optionRoot '*.*.*'          Select all channels up to 2 levels")
        println("  $optionRoot '*.*.*.*'        Select all channels up to 3 levels")
        println("  $optionRoot 'alpha.*'        Select all top-level channels in alpha")
        println("  $optionRoot 'alpha.*.*'      Select all channels in alpha (up to 2 levels)")
        println("  $optionRoot 'alpha.*.*.*'    Select all channels in alpha (up to 3 levels)")
        println("  $optionRoot '*.net'          Select 'net' across all namespaces")
        println("  $optionRoot '*.scheduler.tick' Select 'scheduler.tick' across namespaces")
        println("  $optionRoot '*.net.*'        Select subchannels under 'net' across namespaces")
        println("  $optionRoot '*.{net,io}'     Select 'net' and 'io' across all namespaces")
        println("  $optionRoot '{alpha,beta}.*' Select all top-level channels in alpha and beta")
        println()
    }

    private fun handleNamespaces() {
        val namespaces = getNamespaces()
        if (namespaces.isEmpty()) {
            println("No trace namespaces defined.")
            println()
            return
        }

        println()
        println("Available trace namespaces:")
        for (traceNamespace in namespaces) {
            println("  $traceNamespace")
        }
        println()
    }

    private fun handleChannels() {
        var printedAny = false
        for (traceNamespace in getNamespaces()) {
            for (channel in getChannels(traceNamespace)) {
                if (!printedAny) {
                    println()
                    println("Available trace channels:")
                    printedAny = true
                }
                println("  $traceNamespace.$channel")
            }
        }
        if (!printedAny) {
            println("No trace channels defined.")
            println()
            return
        }
        println()
    }

    private fun handleColors() {
        println()
        println("Available trace colors:")
        for (name in TraceColors.names()) {
            println("  $name")
        }
        println()
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
