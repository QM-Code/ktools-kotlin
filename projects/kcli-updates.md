# Kotlin kcli Project

## Mission

Keep `ktools-kotlin/kcli/` as a clean, Kotlin-idiomatic peer to the C++
reference with explicit public/internal boundaries and strong parity coverage.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `../ktools-cpp/kcli/README.md`
- `../ktools-cpp/kcli/docs/behavior.md`
- `../ktools-cpp/kcli/cmake/tests/kcli_api_cases.cpp`

## Current Gaps

- `kcli/src/kcli/internal/Model.kt` still bundles many internal types into one
  file.
- API behavior coverage is still concentrated in `tests/src/kcli/tests/ApiTests.kt`
  even though the demo CLI tests are separate.
- The implementation should be re-audited against the full C++ contract now
  that the public API split is clearer.
- Docs and demos should be checked to make sure they describe the current file
  layout directly.

## Work Plan

1. Revisit the internal model layout.
- Review whether splitting `internal/Model.kt` would make the repo easier to
  navigate.
- Keep the current internal package structure coherent and avoid file sprawl.

2. Tighten test organization where it helps.
- Consider splitting `ApiTests.kt` by concern if that would make failures
  easier to localize.
- Preserve the existing demo CLI coverage.

3. Re-audit parity with C++.
- Verify aliases, inline roots, bare-root help, required/optional values,
  double-dash rejection, error behavior, and validation-before-handlers against
  the C++ docs and case list.
- Add focused tests for any remaining implicit behavior.

4. Keep demos and docs aligned.
- Confirm that bootstrap/sdk/exe demo roles still match the reference.
- Update docs if any current behavior or layout still requires inference.

5. Maintain repo hygiene.
- Keep generated output out of version control.
- Make the handwritten source and test tree easy to scan.

## Constraints

- Preserve Kotlin-idiomatic APIs while keeping conceptual parity with C++.
- Do not flatten the current package structure unnecessarily.
- Prefer small, precise edits over another broad refactor.

## Validation

- `cd ktools-kotlin/kcli && kbuild --build-latest`
- `cd ktools-kotlin/kcli && kbuild --build-demos`
- `cd ktools-kotlin/kcli && ./build/latest/tests/run-tests`
- Run the demo launchers listed in `ktools-kotlin/kcli/README.md`

## Done When

- Internal types are easy to find.
- Tests and demos cover the reference contract directly.
- Docs describe the current repo shape without stale assumptions.
