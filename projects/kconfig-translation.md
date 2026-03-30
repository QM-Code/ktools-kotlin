# Kotlin Kconfig Translation

## Mission

Create a new `ktools-kotlin/kconfig/` component that matches the C++ `kconfig`
behavior while remaining idiomatic for Kotlin.

Use the lessons from `kcli` and `ktrace`: keep the public API native to the
host language, keep package ownership obvious, and do not absorb demo logic
into a shared support module.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- `../ktools-cpp/kconfig/README.md`
- `../ktools-cpp/kconfig/include/kconfig.hpp`
- `../ktools-cpp/kconfig/include/kconfig/json.hpp`
- `../ktools-cpp/kconfig/include/kconfig/asset.hpp`
- `../ktools-cpp/kconfig/include/kconfig/cli.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/fs.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/read.hpp`
- `../ktools-cpp/kconfig/include/kconfig/store/user.hpp`
- `../ktools-cpp/kconfig/cmake/tests/kconfig_json_api_test.cpp`
- `../ktools-cpp/kconfig/demo/bootstrap/README.md`
- `../ktools-cpp/kconfig/demo/sdk/alpha/README.md`
- `../ktools-cpp/kconfig/demo/sdk/beta/README.md`
- `../ktools-cpp/kconfig/demo/sdk/gamma/README.md`
- `../ktools-cpp/kconfig/demo/exe/core/README.md`
- `../ktools-cpp/kconfig/demo/exe/omega/README.md`
- `../ktools-cpp/kconfig/src/kconfig/cli.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/access.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/layers.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/read.cpp`
- `../ktools-cpp/kconfig/src/kconfig/store/bindings.cpp`

## Deliverables

- Add a new `kconfig/` component to the Kotlin workspace.
- Update workspace docs and `.kbuild.json` so `kconfig` joins the normal batch
  order after `kcli` and `ktrace`.
- Keep the Kotlin package structure explicit and reviewable.
- Provide focused tests and demos, not just core library code.

## Translation Scope

- JSON value model, parse, dump, and typed access.
- Store registry, mutability, merge, get, set, erase, and typed read helpers.
- Filesystem-backed store helpers, asset roots, and user-config flows.
- `kcli` inline parser integration for config overrides.
- `ktrace` integration for warnings, errors, and operator-facing diagnostics.

## Demo Contract

- The demo tree must be:
  - `demo/bootstrap`
  - `demo/sdk/{alpha,beta,gamma}`
  - `demo/exe/{core,omega}`
- Do not introduce `demo/common` or `demo/sdk/common`.
- Keep each SDK demo self-contained and keep executable composition local to
  the executable demos.

## Kotlin Rules

- Keep the public API idiomatic for Kotlin.
- Keep packages and source roots obvious to a non-Kotlin reviewer.
- Split overly large files by responsibility, not mechanically.

## Validation

- `cd ktools-kotlin/kconfig && kbuild --build-latest`
- `cd ktools-kotlin/kconfig && kbuild --build-demos`
- Run the component test suite.
- Run the demo commands documented in `ktools-kotlin/kconfig/README.md`.

## Done When

- `ktools-kotlin/kconfig/` exists as a normal Kotlin workspace component.
- The public API is Kotlin-idiomatic and reviewable.
- Demo code is explicit and self-contained.
- The workspace root and batch build flow include `kconfig`.
