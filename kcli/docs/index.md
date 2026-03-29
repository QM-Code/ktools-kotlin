# Kcli Documentation

`kcli` is a compact Kotlin SDK for executable startup and command-line parsing.
It is intentionally opinionated about normal CLI behavior:

- parse first
- fail early on invalid input
- do not run handlers until the full command line validates
- preserve the caller's effective argument list
- support grouped inline roots such as `--trace-*` and `--config-*`

## Start Here

- [API guide](api.md)
- [Parsing behavior](behavior.md)
- [Examples](examples.md)

## Typical Flow

```kotlin
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

## Core Concepts

`Parser`

- Owns top-level handlers, aliases, inline parser registrations, and the single parse pass.

`InlineParser`

- Defines one inline root namespace such as `--alpha`, `--trace`, or `--build`.

`HandlerContext`

- Exposes the effective option, command, root, and value tokens seen by the handler after alias expansion.

`CliError`

- Used by `parseOrThrow()` to surface invalid CLI input and handler failures.

## Which Entry Point Should I Use?

Use `parseOrExit()` when:

- you are in a normal executable `main()`
- invalid CLI input should print a standardized error and exit with code `2`
- you do not need custom formatting or recovery

Use `parseOrThrow()` when:

- you want to customize error formatting
- you want custom exit codes
- you want to intercept and test parse failures directly

## Build And Explore

```bash
kbuild --help
kbuild --build-latest
./demo/exe/core/build/latest/test --alpha-message "hello"
./demo/exe/omega/build/latest/test --build
```

## Working References

If you want to see complete, compiling examples, start with:

- [`demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.kt`](../demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.kt)
- [`demo/exe/core/src/kcli/demo/core/Main.kt`](../demo/exe/core/src/kcli/demo/core/Main.kt)
- [`demo/exe/omega/src/kcli/demo/omega/Main.kt`](../demo/exe/omega/src/kcli/demo/omega/Main.kt)
- [`tests/src/kcli/tests/ApiTests.kt`](../tests/src/kcli/tests/ApiTests.kt)

The public API contract lives in [`src/kcli/`](../src/kcli/), with `Parser.kt`
and `InlineParser.kt` as the main entry points.
