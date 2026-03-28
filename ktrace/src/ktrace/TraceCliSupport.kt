package ktrace

import kcli.HandlerContext
import kcli.InlineParser

internal object TraceCliSupport {
    fun makeInlineParser(logger: Logger, localTraceLogger: TraceLogger?, traceRoot: String): InlineParser {
        val parser = InlineParser(if (traceRoot.isBlank()) "trace" else traceRoot)
        parser.setRootValueHandler(
            { _, value ->
                if (localTraceLogger != null) {
                    logger.enableChannels(localTraceLogger, value)
                } else {
                    logger.enableChannels(value)
                }
            },
            "<channels>",
            "Trace selected channels.",
        )
        parser.setHandler("-examples", { context -> handleExamples(context) }, "Show selector examples.")
        parser.setHandler("-namespaces", { _ -> handleNamespaces(logger) }, "Show initialized trace namespaces.")
        parser.setHandler("-channels", { _ -> handleChannels(logger) }, "Show initialized trace channels.")
        parser.setHandler("-colors", { _ -> handleColors() }, "Show available trace colors.")
        parser.setHandler(
            "-files",
            { _ ->
                logger.setOutputOptions(
                    OutputOptions(
                        filenames = true,
                        lineNumbers = true,
                        functionNames = false,
                        timestamps = logger.getOutputOptions().timestamps,
                    ),
                )
            },
            "Include source file and line in trace output.",
        )
        parser.setHandler(
            "-functions",
            { _ ->
                logger.setOutputOptions(
                    OutputOptions(
                        filenames = true,
                        lineNumbers = true,
                        functionNames = true,
                        timestamps = logger.getOutputOptions().timestamps,
                    ),
                )
            },
            "Include function names in trace output.",
        )
        parser.setHandler(
            "-timestamps",
            { _ ->
                val options = logger.getOutputOptions()
                logger.setOutputOptions(
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

    private fun handleNamespaces(logger: Logger) {
        val namespaces = logger.getNamespaces()
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

    private fun handleChannels(logger: Logger) {
        var printedAny = false
        for (traceNamespace in logger.getNamespaces()) {
            for (channel in logger.getChannels(traceNamespace)) {
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
}
