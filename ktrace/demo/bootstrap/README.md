# Bootstrap Demo

Exists for CI and as the smallest compile/import usage reference for the Kotlin
`ktrace` SDK.

This demo shows the minimal executable-side setup:

- create a `Logger`
- create a local `TraceLogger("bootstrap")`
- add a local `bootstrap` channel with an explicit color
- `logger.addTraceLogger(...)`
- enable the local selector with `logger.enableChannel(traceLogger, ".bootstrap")`
- emit with `traceLogger.trace(...)`
- print a final `Bootstrap succeeded.` status line
