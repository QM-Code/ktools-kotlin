# Kotlin Updates

## Mission

Keep `ktools-kotlin/` as a clean, Kotlin-idiomatic peer to the C++ reference
with explicit public and internal boundaries across both `kcli` and `ktrace`.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`
- `../ktools-cpp/ktrace/README.md`
- `../ktools-cpp/ktrace/include/ktrace.hpp`
- `../ktools-cpp/ktrace/src/ktrace/cli.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_channel_semantics_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_format_api_test.cpp`
- `../ktools-cpp/ktrace/cmake/tests/ktrace_log_api_test.cpp`

## kcli Focus

- Review whether `kcli/src/kcli/internal/Model.kt` should be split further.
- Consider splitting `tests/src/kcli/tests/ApiTests.kt` by concern if that
  would make failures easier to localize.
- Re-audit parser parity with C++ for aliases, inline roots, bare-root help,
  required and optional values, double-dash rejection, and validation before
  handlers.

## ktrace Focus

- Remove the leftover `ktrace/demo/sdk/common/` directories. There should not
  be a shared demo layer here.
- Review whether `src/ktrace/internal/TraceInternals.kt` should be split
  further into smaller coherent pieces.
- Re-audit selector, logger, output, and inline-parser behavior against the
  C++ contract.

## Cross-Cutting Rules

- Preserve Kotlin-idiomatic APIs while keeping conceptual parity with C++.
- Keep demos readable as `bootstrap`, `sdk/{alpha,beta,gamma}`, and
  `exe/{core,omega}` rather than a shared helper layer.
- Keep generated output out of version control.

## Validation

- `cd ktools-kotlin/kcli && kbuild --build-latest`
- `cd ktools-kotlin/kcli && kbuild --build-demos`
- `cd ktools-kotlin/kcli && ./build/latest/tests/run-tests`
- `cd ktools-kotlin/ktrace && kbuild --build-latest`
- `cd ktools-kotlin/ktrace && kbuild --build-demos`
- `cd ktools-kotlin/ktrace && ./build/latest/tests/run-tests`
- Run the demo launchers listed in each repo README

## Done When

- The leftover `ktrace` demo-common directories are gone.
- Kotlin internals are easier to navigate without flattening the package story.
- `kcli` and `ktrace` both cover the C++ contract directly.
