# Karma Trace Logging SDK

Trace logging SDK with:

- namespaced channel tracing via `TraceLogger.trace(...)`
- always-visible operational logging via `TraceLogger.info/warn/error(...)`
- a library-facing `TraceLogger` source object
- an executable-facing `Logger` registry, filter, formatter, and output sink

## Build SDK

```bash
kbuild --build-latest
```

SDK output:

- `build/latest/sdk/classes`

## Build And Test

```bash
kbuild --build-latest
./build/latest/tests/run-tests
```

## Build And Run Demos

```bash
# Builds the SDK plus demos listed in .kbuild.json build.defaults.demos.
kbuild --build-latest

# Explicit demo-only run (uses build.demos when no args are provided).
kbuild --build-demos

./demo/exe/core/build/latest/test
```

Demos:

- Bootstrap compile/import check: `demo/bootstrap/`
- SDKs: `demo/sdk/{alpha,beta,gamma}`
- Executables: `demo/exe/{core,omega}`

Trace CLI examples:

```bash
./demo/exe/core/build/latest/test --trace
./demo/exe/core/build/latest/test --trace '.*'
./demo/exe/omega/build/latest/test --trace '*.*'
./demo/exe/omega/build/latest/test --trace '*.*.*.*'
./demo/exe/omega/build/latest/test --trace '*.{net,io}'
./demo/exe/omega/build/latest/test --trace-namespaces
./demo/exe/omega/build/latest/test --trace-channels
./demo/exe/omega/build/latest/test --trace-colors
```

## API Model

`TraceLogger` is the namespace-bearing source object. Construct it with an explicit namespace and declare channels on it:

```kotlin
val trace = TraceLogger("alpha")
trace.addChannel("net", TraceColors.color("DeepSkyBlue1"))
trace.addChannel("cache", TraceColors.color("Gold3"))
```

SDKs should usually expose a shared handle from `getTraceLogger()`:

```kotlin
object AlphaSdk {
    private val sharedTraceLogger: TraceLogger by lazy {
        TraceLogger("alpha").also { logger ->
            logger.addChannel("net", TraceColors.color("DeepSkyBlue1"))
            logger.addChannel("cache", TraceColors.color("Gold3"))
        }
    }

    fun getTraceLogger(): TraceLogger = sharedTraceLogger
}
```

`Logger` is the executable-facing runtime. It imports one or more `TraceLogger`s, maintains the central channel registry, and owns filtering, formatting, and final output:

```kotlin
val logger = Logger()

val appTrace = TraceLogger("core")
appTrace.addChannel("app", TraceColors.color("BrightCyan"))
appTrace.addChannel("startup", TraceColors.color("BrightYellow"))

logger.addTraceLogger(appTrace)
logger.addTraceLogger(AlphaSdk.getTraceLogger())
```

## Logging APIs

Channel-based trace output:

```kotlin
trace.trace("channel", "message {}", value)
trace.traceChanged("channel", key, "message {}", value)
```

Always-visible operational logging:

```kotlin
trace.info("message")
trace.warn("configuration file '{}' was not found", path)
trace.error("fatal startup failure")
```

Operational logging is independent of channel enablement. It is still namespaced and uses the same formatting options as trace output.

Message formatting supports sequential `{}` placeholders and escaped braces `{{` and `}}`.

## CLI Integration

The inline parser is logger-bound rather than global. Pass the executable's local `TraceLogger` so leading-dot selectors resolve against the right namespace:

```kotlin
val logger = Logger()
val appTrace = TraceLogger("core")
appTrace.addChannel("app", TraceColors.color("BrightCyan"))

logger.addTraceLogger(appTrace)

val parser = kcli.Parser()
parser.addInlineParser(logger.makeInlineParser(appTrace))

parser.parseOrExit(args)
```

## Channel Expression Forms

Single-selector APIs on `Logger`:

- `.channel[.sub[.sub]]` for a local channel in the provided local namespace
- `namespace.channel[.sub[.sub]]` for an explicit namespace

List APIs on `Logger`:

- `enableChannels(...)`
- `disableChannels(...)`
- list APIs accept selector patterns such as `*`, `{}`, and CSV
- list APIs resolve selectors against the channels currently registered at call time
- leading-dot selectors in list APIs resolve against the provided local namespace
- empty or whitespace selector lists are rejected
- unregistered channels remain disabled and do not emit, even if a selector pattern would otherwise match

Examples:

- `logger.enableChannel(appTrace, ".app")`
- `logger.enableChannel("alpha.net")`
- `logger.enableChannels("alpha.*,{beta,gamma}.net.*")`
- `logger.enableChannels(appTrace, ".net.*,otherapp.scheduler.tick")`

Formatting options:

- `--trace-files`
- `--trace-functions`
- `--trace-timestamps`

These affect both `trace(...)` output and `info/warn/error(...)` output.

## Repository Layout

- Public API: `src/ktrace/Logger.kt`
- Runtime internals: `src/ktrace/internal/`
- API and CLI coverage: `tests/src/`
- Integration demos: `demo/`
