# Kotlin kcli Project

## Mission

Bring `ktools-kotlin/kcli/` fully up to the C++ reference standard. Kotlin is
already structurally strong, so the emphasis is on repo hygiene, finishing the
public API split, and validating behavior parity.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`

## Current Gaps

- The overall source/test/demo layout is strong and close to Java/C++.
- The public API is still concentrated more than it should be in
  `kcli/src/kcli/Parser.kt`.
- Tracked generated output exists in `kcli/build/latest` and
  `kcli/demo/**/build/latest`.
- Behavior parity with the C++ docs and tests should be re-audited after any
  structural cleanup.

## Work Plan

1. Finish the public API layout.
- Split `Parser.kt` if doing so improves readability and discoverability.
- Prefer dedicated files for `Parser`, `InlineParser`, `CliError`,
  `HandlerContext`, and handler interfaces if the split remains coherent.
- Keep the internal package structure intact.

2. Preserve the good internal structure.
- Keep `internal/` focused on model, normalization, registration, parse engine,
  and help rendering.
- Only split internal files further where a file is doing clearly too much.

3. Clean repo hygiene.
- Remove tracked build output from source control where possible.
- Keep the hand-written source tree easy to navigate.

4. Lock down behavior parity.
- Confirm that help output, alias semantics, optional/required value handling,
  bare inline roots, and error semantics match the reference.
- Add targeted tests for any reference behavior that is not explicitly covered.

5. Keep demos aligned with C++.
- Preserve the current bootstrap/sdk/exe demo topology.
- Use the demos as a contract check, not as disposable examples.

## Constraints

- Preserve Kotlin-idiomatic APIs while keeping conceptual parity with C++.
- Do not replace the current clean package split with flatter file sprawl.
- Prefer precise refactors over broad rewrites.

## Validation

- `cd ktools-kotlin/kcli && kbuild --build-latest`
- `cd ktools-kotlin/kcli && kbuild --build-demos`
- `cd ktools-kotlin/kcli && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-kotlin/kcli/README.md`

## Done When

- The public API is easier to navigate than it is today.
- Generated output no longer dominates the repo structure.
- Kotlin remains one of the closest structural matches to the C++ reference.
