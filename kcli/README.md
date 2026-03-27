# Karma CLI Parsing SDK

`kcli` is the Kotlin command-line parsing SDK in the ktools ecosystem.
It is designed around two common CLI shapes:

- top-level options such as `--verbose` and `--output`
- inline roots such as `--trace-*`, `--config-*`, and `--build-*`

The library gives you two explicit entrypoints:

- `parseOrExit(...)` for normal executable startup
- `parseOrThrow(...)` when the caller wants to intercept `CliError`

## Documentation

- [Overview and quick start](docs/index.md)
- [API guide](docs/api.md)
- [Parsing behavior](docs/behavior.md)
- [Examples](docs/examples.md)

## Quick Start

```kotlin
import kcli.InlineParser
import kcli.Parser

val parser = Parser()
val build = InlineParser("--build")

build.setHandler("-profile", { _, value ->
    println("profile=$value")
}, "Set build profile.")

parser.addInlineParser(build)
parser.addAlias("-v", "--verbose")
parser.setHandler("--verbose", { _ ->
}, "Enable verbose logging.")

parser.parseOrExit(args)
```

## Behavior Highlights

- The full command line is validated before any registered handler runs.
- `parseOrExit()` prints invalid CLI input to `stderr` as `[error] [cli] ...` and exits with code `2`.
- `parseOrThrow()` throws `CliError`.
- Bare inline roots such as `--build` print inline help unless a root value handler is registered and a root value is supplied.
- `setHandler(..., ValueHandler, ...)` registers a required-value option.
- `setOptionalValueHandler(...)` registers an optional-value option.
- Required values may consume a first token that begins with `-`.
- Literal `--` is rejected as an unknown option; it is not treated as an option terminator.

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

# Explicit demo-only run (uses .kbuild.json build.demos when no args are passed).
kbuild --build-demos
```

Demo directories:

- Bootstrap compile/import check: `demo/bootstrap/`
- SDK demos: `demo/sdk/{alpha,beta,gamma}`
- Executable demos: `demo/exe/{core,omega}`

Useful demo commands:

```bash
./demo/exe/core/build/latest/test
./demo/exe/core/build/latest/test --alpha
./demo/exe/core/build/latest/test --alpha-message "hello"
./demo/exe/core/build/latest/test --output stdout
./demo/exe/omega/build/latest/test --beta-workers 8
./demo/exe/omega/build/latest/test --newgamma-tag "prod"
./demo/exe/omega/build/latest/test --build
```

## Repository Layout

- Public API: `src/kcli/Parser.kt`
- Parser internals: `src/kcli/internal/`
- API and CLI coverage: `tests/src/`
- Integration demos: `demo/`
- Hand-written docs: `docs/`
