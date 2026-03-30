# Kotlin Conventions Refactor

## Mission

Refactor `ktools-kotlin/` so that `kcli` and `ktrace` preserve the shared
ktools behavior while reading like Kotlin designed for Kotlin users, not like
Java or C++ APIs translated into Kotlin syntax.

Assume a fresh agent should perform a full audit and complete refactor pass.

## Scope

This brief applies to:

- `ktools-kotlin/kcli/`
- `ktools-kotlin/ktrace/`
- `ktools-kotlin/README.md`

## Required Reading

- `../ktools/AGENTS.md`
- `AGENTS.md`
- `README.md`
- `kcli/AGENTS.md`
- `kcli/README.md`
- `ktrace/AGENTS.md`
- `ktrace/README.md`
- local docs for parser and trace behavior
- the matching C++ docs and tests for the same behavior

There is no existing `projects/updates.md` in this workspace at the time this
brief was written.

## Core Principle

Preserve semantics, not foreign API texture.

Preserve:

- parser semantics
- alias and inline-root behavior
- trace selector behavior
- help/error behavior
- demo contract behavior

Do not preserve:

- Java/C++ getter/setter ceremony where Kotlin properties are better
- nullable or mutable shapes inherited from older designs
- package/file structure that ignores Kotlin norms
- large state bags where data classes or sealed models would be clearer

## Assignment Model

A fresh agent should assume:

- public API redesign is in scope
- internal structure redesign is in scope
- docs and tests must be updated as part of the refactor

## Public API Refactor Goals

Prefer Kotlin-native surfaces:

- properties over explicit accessors
- `data class` and sealed models where they clarify public value/config state
- immutable-first APIs
- local naming that reads naturally in Kotlin call sites
- nullability that is explicit and intentional

`kcli` should feel like a Kotlin parser library.
`ktrace` should feel like a Kotlin tracing library that composes cleanly with
the parser layer.

## Internal Refactor Goals

Review and refactor:

- file and package ownership
- giant utility/object files
- mutable state that could be localized
- translated control-flow patterns
- poorly expressed nullability

Prefer:

- cohesive files
- Kotlin-first responsibility splits
- clear immutable/mutable boundaries
- concise local abstractions instead of Java/C++ ceremony

## Demo, Test, And Docs Expectations

- demos are contract checks
- bootstrap, SDK, and executable entity ownership should stay explicit
- docs must describe the current structure directly
- key parity behaviors must be covered explicitly in tests

## Validation

Use the local component docs for exact commands.
At minimum, final validation should include:

- the documented `kbuild` flow
- Kotlin library tests
- demo validation commands where documented
- direct smoke runs for the documented demo entrypoints

## Done When

- the public API reads naturally to a Kotlin reviewer
- internal code no longer reads like translated Java/C++
- nullability and data-model choices are coherent
- demos/tests/docs align with the current structure
- validation passes

## Final Checklist

- read all required docs
- audit every public type and package
- refactor toward Kotlin conventions without changing behavior
- update tests and docs to match
- validate the workspace
