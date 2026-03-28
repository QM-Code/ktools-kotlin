package ktrace.internal

import java.io.PrintStream
import java.time.Instant
import java.util.Locale

object TraceOutputSupport {
    fun emitTrace(
        loggerData: TraceInternals.LoggerData,
        traceNamespace: String,
        channel: String,
        callSite: TraceInternals.CallSite,
        message: String,
    ) {
        emitLine(loggerData, buildTraceMessagePrefix(loggerData, traceNamespace, channel, callSite), message, System.out)
    }

    fun emitLog(
        loggerData: TraceInternals.LoggerData,
        traceNamespace: String,
        severity: TraceInternals.LogSeverity,
        callSite: TraceInternals.CallSite,
        message: String,
    ) {
        emitLine(loggerData, buildLogMessagePrefix(loggerData, traceNamespace, severity, callSite), message, System.out)
    }

    fun buildTraceMessagePrefix(
        loggerData: TraceInternals.LoggerData,
        traceNamespace: String,
        channel: String,
        callSite: TraceInternals.CallSite,
    ): String {
        val out = StringBuilder()
        appendNamespace(out, traceNamespace)
        appendTimestamp(loggerData, out)
        out.append('[').append(channel).append(']')
        appendLocation(loggerData, out, callSite)
        return out.toString()
    }

    fun buildLogMessagePrefix(
        loggerData: TraceInternals.LoggerData,
        traceNamespace: String,
        severity: TraceInternals.LogSeverity,
        callSite: TraceInternals.CallSite,
    ): String {
        val out = StringBuilder()
        appendNamespace(out, traceNamespace)
        appendTimestamp(loggerData, out)
        out.append('[').append(severity.label).append(']')
        appendLocation(loggerData, out, callSite)
        return out.toString()
    }

    private fun emitLine(loggerData: TraceInternals.LoggerData, prefix: String, message: String, out: PrintStream) {
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

    private fun appendTimestamp(loggerData: TraceInternals.LoggerData, out: StringBuilder) {
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

    private fun appendLocation(
        loggerData: TraceInternals.LoggerData,
        out: StringBuilder,
        callSite: TraceInternals.CallSite,
    ) {
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
}
