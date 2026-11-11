# ADR 0010: Lightweight universal release, no bundled local-AI runtime

- **Status:** Accepted
- **Date:** 2026-11-11
- **Release:** v0.0.9
- **Supersedes:** the shipping decision in [ADR-0001](0001-qvac-local-ai.md), not its interface design

## Context

ADR-0001 chose QVAC as the reference local-AI engine and shipped it in v0.0.2. The engine
never reached a user-visible surface: the Ask/Explain/Fix editor was not exposed, inference
was never verified on a physical device, and the runtime could only run on arm64 API 29+
devices. Meanwhile it carried roughly 177 MB of native payload, a 141 MB build-time Maven
download, an isolated worker process, a manifest `tools:overrideLibrary` and a CI cache —
weight paid by every install for a capability none of them could use.

The v0.0.9 goal is a universal, lightweight, lovable release. A workspace should be usable
from a phone, a laptop, a home server and a second screen, and the app should install and
start quickly on modest hardware.

## Decision

1. **Remove the bundled QVAC runtime from the app.** Delete the dependency, the worker
   wiring, the fetch script, the Maven repository hook, the manifest override and the CI
   cache. Keep ADR-0001 as the record of the design.
2. **Keep the boundary.** The `LocalAiEngine` interface, the availability policy and the
   honest "unsupported" state are the right design; they return with the surface that uses
   them. Nothing in the workspace, editor, terminal or agent path depends on them.
3. **Invest the weight in reach instead.** Ship a zero-build companion web app, a
   dependency-free CLI, two additive host messages and a first-run welcome. These run on
   every device tier and every desktop, not one ABI.
4. **Add an SBOM and universal artifacts to the release.** The signed APK is joined by the
   companion zip, the CLI tarball and a CycloneDX SBOM; all are checksummed together.

## Consequences

- The APK drops from roughly 255 MB to about 78 MB in a debug build and no longer needs a
  one-time 141 MB fetch before it can compile.
- The app supports its full declared matrix (arm64-v8a and x86_64, API 26+) with no
  capability that only one tier can use.
- P2 (Intelligence) is not claimed. The requirements register marks the local-AI rows as
  deferred and the deviations register keeps them visible rather than deleting them.
- A future local engine can implement `LocalAiEngine` without changing the app, and its
  weight will be a deliberate, measured decision rather than an inherited one.
- The universal layer is source-distributed and dependency-free, so it can be audited and
  run without the Android toolchain.

## What this does not change

- The package id `com.oneorigin.studio`, the release signing identity and the canonical
  GitHub Releases channel.
- `origin.host/v1` compatibility: `list_workspaces` and `list_events` are additive.
- The Constitution, the trust core, the workspace format and the deny-by-default egress
  policy.
