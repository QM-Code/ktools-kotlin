# Examples

This page shows a few common `kcli` patterns. For complete compiling examples, also see:

- [`demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.kt`](../demo/sdk/alpha/src/kcli/demo/alpha/AlphaSdk.kt)
- [`demo/sdk/beta/src/kcli/demo/beta/BetaSdk.kt`](../demo/sdk/beta/src/kcli/demo/beta/BetaSdk.kt)
- [`demo/sdk/gamma/src/kcli/demo/gamma/GammaSdk.kt`](../demo/sdk/gamma/src/kcli/demo/gamma/GammaSdk.kt)
- [`demo/exe/core/src/kcli/demo/core/Main.kt`](../demo/exe/core/src/kcli/demo/core/Main.kt)
- [`demo/exe/omega/src/kcli/demo/omega/Main.kt`](../demo/exe/omega/src/kcli/demo/omega/Main.kt)

## Minimal Executable

```kotlin
val parser = Parser()

parser.addAlias("-v", "--verbose")
parser.setHandler("--verbose", { _ ->
}, "Enable verbose logging.")

parser.parseOrExit(args)
```

## Inline Root With Subcommands-Like Options

```kotlin
val parser = Parser()
val build = InlineParser("--build")

build.setHandler("-profile", { _, value ->
    println("profile=$value")
}, "Set build profile.")
build.setHandler("-clean", { _ ->
}, "Enable clean build.")

parser.addInlineParser(build)
parser.parseOrExit(args)
```

This enables:

```text
--build
--build-profile release
--build-clean
```

## Bare Root Value Handler

```kotlin
val config = InlineParser("--config")

config.setRootValueHandler({ _, value ->
    println(value)
}, "<assignment>", "Store a config assignment.")
```

This enables:

```text
--config
--config user=alice
```

Behavior:

- `--config` prints inline help
- `--config user=alice` invokes the root value handler

## Alias Preset Tokens

```kotlin
val parser = Parser()

parser.addAlias("-c", "--config-load", "user-file")
parser.setHandler("--config-load", { context, value ->
    println("${context.option}: $value")
}, "Load config.")
```

This makes:

```text
-c settings.json
```

behave like:

```text
--config-load user-file settings.json
```

Inside the handler:

- `context.option` is `--config-load`
- `context.valueTokens` starts with `"user-file"`

## Optional Values

```kotlin
parser.setOptionalValueHandler("--color", { _, value ->
    println(value)
}, "Set or auto-detect color output.")
```

This enables both:

```text
--color
--color always
```

## Positionals

```kotlin
parser.setPositionalHandler { context ->
    for (token in context.valueTokens) {
        usePositional(token)
    }
}
```

The positional handler receives all remaining non-option tokens after option parsing succeeds.

## Custom Error Handling

If you want your own formatting or exit policy, use `parseOrThrow()`:

```kotlin
try {
    parser.parseOrThrow(args)
} catch (ex: CliError) {
    System.err.println("custom cli error: ${ex.message}")
}
```

Use this when:

- you want custom error text
- you want custom logging
- you want a different exit code policy
