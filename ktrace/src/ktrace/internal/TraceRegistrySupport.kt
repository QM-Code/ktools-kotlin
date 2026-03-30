package ktrace.internal

import ktrace.OutputOptions
import ktrace.TraceColors

object TraceRegistrySupport {
    fun addChannel(data: TraceLoggerData, channel: String, color: Int) {
        val traceNamespace = TraceNamingSupport.normalizeNamespace(data.traceNamespace)
        val channelName = TraceNamingSupport.normalizeChannel(channel)
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
            traceLogger.attachedLoggerRef = java.lang.ref.WeakReference(loggerData)
        }
    }

    fun attachedLogger(traceLogger: TraceLoggerData): LoggerData? =
        synchronized(traceLogger.attachedLoggerLock) { traceLogger.attachedLoggerRef.get() }

    fun mergeTraceLogger(loggerData: LoggerData, traceLogger: TraceLoggerData) {
        val traceNamespace = TraceNamingSupport.normalizeNamespace(traceLogger.traceNamespace)
        synchronized(loggerData.registryLock) {
            loggerData.namespaces.add(traceNamespace)
            val registeredChannels = loggerData.channelsByNamespace.getOrPut(traceNamespace) { mutableListOf() }
            val registeredColors = loggerData.colorsByNamespace.getOrPut(traceNamespace) { linkedMapOf() }
            for (channel in traceLogger.channels) {
                val channelName = TraceNamingSupport.normalizeChannel(channel.name)
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
        val namespaceName = TraceNamingSupport.normalizeNamespace(traceNamespace)
        return synchronized(loggerData.registryLock) {
            loggerData.channelsByNamespace[namespaceName]?.toList()?.sorted() ?: emptyList()
        }
    }

    fun shouldTraceChannel(traceLogger: TraceLoggerData, channel: String): Boolean =
        try {
            val logger = attachedLogger(traceLogger) ?: return false
            shouldTraceChannel(logger, traceLogger.traceNamespace, TraceNamingSupport.normalizeChannel(channel))
        } catch (_: RuntimeException) {
            false
        }

    fun shouldTraceQualifiedChannel(
        loggerData: LoggerData,
        qualifiedChannel: String,
        localNamespace: String,
    ): Boolean =
        try {
            val resolution = TraceSelectorSupport.resolveExactChannel(loggerData, qualifiedChannel, localNamespace)
            shouldTraceChannel(loggerData, resolution.traceNamespace, resolution.channel)
        } catch (_: RuntimeException) {
            false
        }

    fun shouldTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean {
        if (!TraceNamingSupport.isValidChannelPath(channel) || !loggerData.hasEnabledChannels.get()) {
            return false
        }
        if (!isRegisteredTraceChannel(loggerData, traceNamespace, channel)) {
            return false
        }
        val key = TraceNamingSupport.makeQualifiedChannelKey(traceNamespace, channel)
        return synchronized(loggerData.enabledLock) { loggerData.enabledChannelKeys.contains(key) }
    }

    fun isRegisteredTraceChannel(loggerData: LoggerData, traceNamespace: String, channel: String): Boolean {
        val namespaceName = TraceNamingSupport.trimWhitespace(traceNamespace)
        val channelName = TraceNamingSupport.trimWhitespace(channel)
        if (!TraceNamingSupport.isSelectorIdentifier(namespaceName) || !TraceNamingSupport.isValidChannelPath(channelName)) {
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
}
