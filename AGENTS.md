# ktools-kotlin

Assume `../ktools/AGENTS.md` has already been read.

`ktools-kotlin/` is the Kotlin workspace for the ktools ecosystem.

## What This Level Owns

This workspace owns Kotlin-specific concerns such as:

- project/module layout
- Kotlin build and test flow
- Kotlin-specific API naming and integration patterns
- coordination across Kotlin tool implementations when more than one component is present

Cross-language conceptual definitions belong at the overview/spec level, not here.

## Current Scope

This workspace currently contains:

- `kcli/`
- `ktrace/`

The shared `kbuild` tool lives outside this workspace in `../kbuild/` and is
expected on `PATH`.

## Guidance For Agents

1. Treat this workspace as a language root, not an implementation component.
2. Prefer making changes in the narrowest child component that owns the behavior.
3. Keep root-level changes focused on workspace coordination, documentation, and shared tooling.
4. Read the relevant child component `AGENTS.md` and `README.md` files before changing code there.

## Git Sync

Use the shared `kbuild` workflow for commit/push sync from this workspace root:

```bash
kbuild --git-sync "<message>"
```

Treat that as the standard sync command unless a more local doc explicitly
overrides it.
After a coherent batch of changes in this workspace or one of its child components,
return to `ktools-kotlin/` and run that sync command promptly.
