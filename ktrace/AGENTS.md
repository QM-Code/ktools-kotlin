Assume these have already been read:

- `../../ktools/AGENTS.md`
- `../AGENTS.md`

`ktools-kotlin/ktrace/` is the Kotlin implementation of `ktrace`.

## What This Component Owns

This component owns the Kotlin API and implementation details for `ktrace`, including:

- public Kotlin tracing/logging APIs
- selector parsing and logger runtime behavior
- `kcli` inline parser integration for trace controls
- Kotlin demos and tests

Cross-language conceptual behavior belongs to the `ktools/` overview docs.
Kotlin workspace concerns belong to `ktools-kotlin/`.

## Build And Test Expectations

- Use `kbuild` from the component root.
- Prefer end-to-end checks through the generated demo launchers.
- Keep tests explicit and trace-focused.

Useful commands:

```bash
kbuild --build-latest
./build/latest/tests/run-tests
```

After a coherent batch of changes in `ktools-kotlin/ktrace/`, return to the
`ktools-kotlin/` workspace root and run `kbuild --git-sync "<message>"`.
