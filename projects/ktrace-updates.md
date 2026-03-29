# Kotlin ktrace Project

## Mission

Keep `ktools-kotlin/ktrace/` as a clean, Kotlin-idiomatic peer to the C++
reference with explicit parity coverage and self-contained demos.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `../ktools-cpp/ktrace/README.md`
- `../ktools-cpp/ktrace/include/ktrace.hpp`
- `../ktools-cpp/ktrace/src/ktrace/cli.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_channel_semantics_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_format_api_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_log_api_test.cpp`

## Current Gaps

- Empty leftover `ktrace/demo/sdk/common/` directories are still present and
  should be removed.
- The implementation should be re-audited against the full C++ contract for
  selectors, output formatting, and CLI behavior.
- `src/ktrace/internal/TraceInternals.kt` remains the densest internal file.
- The README still carries most of the contract explanation, so docs and tests
  should be checked to ensure behavior is explicit enough.

## Work Plan

1. Finish the demo cleanup cleanly.
- Remove the now-empty `ktrace/demo/sdk/common/` leftovers.
- Keep bootstrap/sdk/exe demo roles obvious in the file tree.

2. Re-audit parity with C++.
- Verify channel registration, selector parsing, unmatched-selector warnings,
  logger/trace-source attachment rules, output options, `traceChanged(...)`,
  and `makeInlineParser(...)` against the C++ contract.
- Add focused tests for any remaining implicit behavior.

3. Revisit the densest internal file.
- Review whether `TraceInternals.kt` should be split further into smaller,
  coherent pieces.
- Avoid file sprawl and keep the internal package story clear.

4. Keep demos and docs aligned.
- Confirm that bootstrap/sdk/exe demo roles still match the reference.
- Update docs if any current behavior or layout still requires inference.

## Constraints

- Preserve Kotlin-idiomatic APIs while keeping conceptual parity with C++.
- Do not flatten the package structure unnecessarily.
- Keep the demos readable as separate entities that happen to work together.

## Validation

- `cd ktools-kotlin/ktrace && kbuild --build-latest`
- `cd ktools-kotlin/ktrace && kbuild --build-demos`
- `cd ktools-kotlin/ktrace && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-kotlin/ktrace/README.md`

## Done When

- Leftover demo-common directories are gone.
- Kotlin internals are easy to navigate without guesswork.
- Tests and demos cover the reference contract directly.
