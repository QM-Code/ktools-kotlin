# Core Demo

Basic local-plus-imported tracing showcase for the Kotlin `ktrace` SDK and the alpha demo SDK.

This demo shows:

- executable-local tracing defined with a local `TraceLogger`
- imported SDK tracing added via `AlphaSdk.getTraceLogger()`
- logger-managed selector state and output formatting
- local CLI integration through `parser.addInlineParser(logger.makeInlineParser(localTraceLogger))`
