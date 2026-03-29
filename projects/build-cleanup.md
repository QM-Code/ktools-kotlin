# Kotlin Build Cleanup Project

## Mission

Add a Kotlin-specific residual checker to `kbuild`, then make sure the Kotlin
workspace keeps generated artifacts under `build/` instead of leaking into the
source tree.

This task spans both `ktools-kotlin/` and the sibling shared build repo
`../kbuild/`.

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `../kbuild/AGENTS.md`
- `../kbuild/README.md`
- `../kbuild/libs/kbuild/residual_ops.py`
- `../kbuild/libs/kbuild/backend_ops.py`
- `../kbuild/libs/kbuild/kotlin_backend.py`
- `../kbuild/tests/test_java_residuals.py`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`

## Current Gaps

- `kbuild` does not yet have a Kotlin backend residual checker.
- Kotlin/JVM builds can easily leak `.class` files, module metadata, or other
  compiler output if any step escapes the staged build layout.
- The current Kotlin workspace should be verified against the new checker and
  cleaned up if necessary.

## Work Plan

1. Add the Kotlin residual checker in `kbuild`.
- Follow the Java checker pattern, but make it Kotlin-appropriate.
- Detect real Kotlin/JVM compiler residuals outside `build/`, such as stray
  `.class` files, Kotlin module metadata, or other direct compiler output.
- Keep the checker narrow and tied to actual build artifacts.

2. Add focused `kbuild` tests.
- Add tests for build refusal and `--git-sync` refusal when known Kotlin build
  residuals appear outside `build/`.
- Add a positive case showing that staged output inside `build/` is allowed.

3. Audit the actual Kotlin workspace build flow.
- Build `kcli/` and `ktrace/` through normal `kbuild` entrypoints.
- Confirm that no Kotlin compiler output lands outside `build/`.
- If any leak exists, fix the build flow rather than weakening the checker.

4. Clean up real residuals if they exist.
- Remove any tracked or generated source-tree build artifacts that violate the
  new checker.
- Tighten ignore rules only if needed after the build flow is corrected.

5. Keep docs aligned.
- Update `kbuild` docs if the checker needs backend-specific mention.
- Update local docs only if they currently imply workflows that leak output.

## Constraints

- Do not simply clone Java’s checker without thinking through Kotlin’s actual
  residuals.
- Do not make the checker broader than the real build products warrant.
- Prefer fixing the build path over just ignoring artifacts.

## Validation

- Run the new Kotlin residual tests in `../kbuild`
- `cd ktools-kotlin && kbuild --batch --build-latest`
- `cd ktools-kotlin/kcli && kbuild --build-demos`
- `cd ktools-kotlin/ktrace && kbuild --build-demos`
- Confirm the workspace stays free of generated Kotlin/JVM artifacts outside
  `build/`

## Done When

- `kbuild` rejects the relevant Kotlin residual class outside `build/`.
- The Kotlin workspace no longer leaks those artifacts in normal use.
- Workspace cleanliness is enforced automatically.
