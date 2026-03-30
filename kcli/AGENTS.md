Assume these have already been read:

- `../../ktools/AGENTS.md`
- `../AGENTS.md`

`ktools-kotlin/kcli/` is the Kotlin implementation of `kcli`.

## What This Component Owns

This component owns the Kotlin API and implementation details for `kcli`, including:

- public Kotlin APIs
- parser and inline-parser behavior
- Kotlin demos and tests
- component-local build configuration for the Kotlin workspace

Cross-language conceptual behavior belongs to the `ktools/` overview docs.
Kotlin workspace concerns belong to `ktools-kotlin/`.

## Build And Test Expectations

- Use `kbuild` from the component root.
- Prefer end-to-end checks through the generated demo launchers.
- Keep tests focused on parsing behavior and CLI ergonomics.

Useful commands:

```bash
kbuild --build-latest
./build/latest/tests/run-tests
```

After a coherent batch of changes in `ktools-kotlin/kcli/`, return to the
`ktools-kotlin/` workspace root and run `kbuild --git-sync "<message>"`.
