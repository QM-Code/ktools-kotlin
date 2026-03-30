package ktrace.internal

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
