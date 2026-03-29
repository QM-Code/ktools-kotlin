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

- `ktrace/demo/sdk/common/` exists and should not.
- A substantial amount of generated output is still tracked under
  `ktrace/build/latest` and demo build trees.
- The implementation should be re-audited against the full C++ contract for
  selectors, output formatting, and CLI behavior.
- README coverage should be checked so it explains the current contract and
  layout directly.

## Work Plan

1. Clean repo hygiene.
- Remove tracked generated output from `build/latest` and demo build trees.
- Tighten ignore rules so build products do not return.
- Make the handwritten source and test tree easy to scan.

2. Eliminate shared demo code.
- Remove `ktrace/demo/sdk/common/`.
- Make `demo/sdk/alpha`, `demo/sdk/beta`, and `demo/sdk/gamma` self-contained.
- Keep bootstrap-specific logic under `demo/bootstrap/`.
- Keep executable composition logic under `demo/exe/core/` and
  `demo/exe/omega/`.
- Do not replace the current common module with another shared demo layer.

3. Re-audit parity with C++.
- Verify channel registration, selector parsing, unmatched-selector warnings,
  logger/trace-source attachment rules, output options, `traceChanged(...)`,
  and `makeInlineParser(...)` against the C++ contract.
- Add focused tests for any remaining implicit behavior.

4. Keep demos and docs aligned.
- Confirm that bootstrap/sdk/exe demo roles still match the reference.
- Update docs if any current behavior or layout still requires inference.

5. Maintain a coherent internal layout.
- Keep Kotlin internals easy to navigate.
- Prefer small, precise edits over another broad refactor.

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

- Generated build output no longer dominates the repo.
- Shared demo code is gone.
- Tests and demos cover the reference contract directly.
