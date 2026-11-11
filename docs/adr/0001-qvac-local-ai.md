# ADR 0001: QVAC as the local-AI engine

- **Status:** Superseded for shipping by [ADR-0010](0010-lightweight-universal.md) (v0.0.9)
- **Date:** 2026-11-07
- **Decision owner:** OneOrigin
- **Supersedes:** none
- **Release:** the QVAC foundation shipped in `v0.0.2`; the runtime was removed in `v0.0.9`

## Context

The MLP's local-AI moment is **Ask / Explain / Fix** on the active file or the last
error: no account, no API key, no cloud, offline after one model download. QVAC is the
reference engine for that moment and stays mandatory until an alternative passes the
four-part substitution gate in `MLP-ALP-SCOPE.md`.

The app is unusual for an Android AI integration:

- `minSdk 26`, **`targetSdk 28`** because PRoot executes binaries from the app's native
  library directory, and newer target-SDK behaviour breaks that execution model.
- Two ABIs: `arm64-v8a` (the full product) and `x86_64` (core workspace only).
- Native libraries are shipped **uncompressed and extracted** (`useLegacyPackaging`), so
  the OS will `exec` them. Any new native payload must coexist with that packaging.
- Releases are signed and immutable, built from tags by CI on `OriginLabs1`.

## Decision

Use the **QVAC Kotlin SDK, version `0.23.0`**, LLM profile, as the local inference
engine for `v0.0.2` and the MLP.

| Item | Choice |
| --- | --- |
| Dependency | `io.tether:qvac-sdk-android-llm:0.23.0` |
| SDK release | `sdk-v0.23.0`, published 2026-11-02 |
| Artifact | `qvac-sdk-android-llm-0.23.0-maven.zip` (125,721,461 bytes) |
| SHA-256 | `031ff7eaff1cda9090af4ddc75f2180de8ca88f5ac9966b5e0731b40f6c545e1` |
| Transport | `AndroidServiceTransport` in an isolated `:qvac_worker` process |
| Runtime | `arm64-v8a`, API 29+, physical device |
| Model (MLP) | `QWEN3_600M_INST_Q4` |
| Model fallback | `QWEN3_1_7B_INST_Q4` on stronger devices |
| Client licence | Apache-2.0 |
| Worker host | BareKit, embedded in the AAR |

The Maven ZIP is **vendored and verified**, not resolved from a mutable remote:

1. `tools/fetch-qvac.py` downloads the exact release asset and the release
   `SHA256SUMS`.
2. It verifies the ZIP against the pinned hash above and refuses to continue on any
   mismatch.
3. It extracts to `.qvac/qvac-maven-repository` (git-ignored), which
   `settings.gradle.kts` adds as a local Maven repository when present.
4. `bare-rpc-kotlin` still comes from JitPack, restricted by an `exclusiveContent`
   filter to `com.github.holepunchto.*` so no other dependency can be substituted.

This keeps builds reproducible, works offline after one fetch, and avoids a GitHub
Packages token in CI.

### Model

`QWEN3_600M_INST_Q4` is 382,156,480 bytes with SHA-256
`33bcc57074ec7b6eada5a90651ee546ec0c2b271002c22baf9f1b2dd1e8f75cb`, engine
`llamacpp-completion`. It appeared in QVAC's Pixel 8a verification matrix with a
383 MB catalog size and a 35.5 s cached load. It is the right first model for the
declared device floor (4 GB RAM), and it leaves room for the Debian environment.
`QWEN3_1_7B_INST_Q4` is an explicit opt-in upgrade for devices with headroom, never a
silent download.

The SDK is Apache-2.0; **model licences are separate**. The chosen model's licence and
provenance are recorded in `THIRD_PARTY_NOTICES.md` before `v0.0.2` ships.

## Why not another engine now

All alternatives were checked against the four-part gate (comparable capability,
superior device/performance/licensing, proven through the L3 trust journey/benchmarks,
decisive on a weighted scorecard), not against preference.

| Candidate | Why it is not the MLP choice |
| --- | --- |
| llama.cpp + hand-written JNI | Strong engine, but we would own tokenisation, chat templates, cancellation, model download, embeddings and a JNI surface. That is a platform project, not an MLP. |
| MLC-LLM | Compelling GPU path, but a heavier toolchain and a different packaging/ABI story; no evidence yet that it is decisively better on the target floor. |
| ONNX Runtime GenAI | Viable, but no proven Android arm64 phone matrix comparable to QVAC's, and it adds a second runtime plus model conversion pipeline. |
| MediaPipe LLM Inference | Convenient but constrains model formats and lifecycle; weaker fit for an open, replaceable engine strategy. |
| LiteRT-LM | Promising, but the Android local-LLM path is younger and lacks the device evidence the gate requires. |
| ExecuTorch | Excellent long-term option, but quantised LLM coverage on mid-range Android is not yet proven enough for the MLP. |

The decision is reversible by design: the app talks to a `LocalAiEngine` interface, not
to QVAC types, so a future engine can pass the gate without rewriting the UI.

## Consequences

**Positive**

- One self-contained AAR with the worker, addons and native libraries; no Node runtime
  to assemble at build time.
- Kotlin-first API matching the app's coroutines/Flow model.
- Token streaming, cancellation and progress events are first-class.
- Apache-2.0 client; no account or API key.
- Pinned, checksum-verified artifacts with a documented upgrade path.

**Negative / accepted costs**

- **API 29+ and arm64 only.** The app keeps `minSdk 26`; QVAC is unavailable on API
  26–28 and on `x86_64`. Those devices keep the full workspace and see a clear
  "Local AI unavailable on this device" state, never a crash or a fake button.
- The AAR declares `minSdk 29`. The app manifest must merge it with
  `tools:overrideLibrary="io.tether.qvac.sdk.barekit"`, and **every** QVAC entry point
  must be behind a runtime `SDK_INT >= 29 && arm64` gate. This is a reviewed invariant,
  covered by a unit test on the gate predicate.
- The worker runs in `:qvac_worker` so a native addon crash cannot take down the editor.
- The APK grows by the LLM profile payload; the model is downloaded after install, not
  bundled. The app checks free space before offering the download.
- CI cannot run inference: QVAC does not support emulators. CI proves the build, the
  manifest merge, the gate predicate and the signed artifact; functional inference is
  verified on a physical arm64 device and recorded in the release notes.
- A Kotlin compiler of 2.3 or newer is required (`bare-rpc-kotlin` is built with 2.4).
  The project already uses 2.3.21.

## Integration shape

```
FilesPanel / editor
        │  Ask · Explain · Fix
        ▼
LocalAiEngine (interface, app-owned)
        │
        ▼
QvacEngine ── availability gate (API 29+, arm64, free space)
        │
        ▼
AndroidServiceTransport ──► :qvac_worker ──► QVAC runtime
        │
        ├── models.downloadWithProgress(...)  → progress UI + cancel
        ├── models.loadWithProgress(...)      → load state
        └── completion.run(...)               → streaming tokens
```

- Permissions are enforced in app code, outside the model. The model proposes; the app
  shows a diff; the user approves; the app applies. No tool call writes a file directly.
- Every apply is undoable and the original bytes are kept until the user accepts.
- Model download is explicit, resumable where QVAC supports it, and verified.
- The engine exposes states: `Unsupported`, `Idle` (not downloaded), `Downloading`, `Loading`,
  `Ready`, `Running`, `Failed(reason)`. The UI is honest in every state.

## Rollback and upgrade

- Pin the SDK version and checksum; upgrading is a reviewed change with a new ADR
  entry, a fresh device run, and a new app release. Never a floating range.
- If the integration fails validation, `v0.0.2` can ship without the dependency while
  keeping the branding and release fixes; the QVAC surface stays hidden. The MLP gate
  remains open, and the release notes say so.
- The model can be removed from the app's storage without affecting the workspace.

## Release and branding decision

`v0.0.1` is published with the retired folder mark. Because published artifacts are
never re-signed, re-tagged or silently modified, `v0.0.1` is not rewritten. The Origin
Loop identity and the QVAC foundation both debut in **`v0.0.2`**, which supersedes
`v0.0.1` as the branded release.

## Verification checklist for `v0.0.2`

Verified locally on 2026-11-07 against the signed release APK unless stated otherwise.

- [x] `tools/fetch-qvac.py` verifies the pinned SHA-256 and extracts cleanly.
      (SHA-256 `031ff7ea…45e1`, 125,721,461 bytes, extracted to `.qvac/`.)
- [x] `assembleRelease` merges the AAR manifest with the override and keeps
      `minSdk 26` / `targetSdk 28` in the built APK, with `QvacWorkerService` in
      `:qvac_worker` and the arm64 QVAC libraries packaged.
- [x] On API 26–28 and on `x86_64`, the AI entry point reports `Unsupported`.
      (`LocalAiGateTest` covers the shipped predicate; the gate is present in the
      release dex.)
- [x] The release APK contains the foundation rather than losing it to shrinking:
      R8 keeps `com.oneorigin.studio.ai.**` explicitly (see `proguard-rules.pro`).
- [x] Model licence and SDK notice are in `THIRD_PARTY_NOTICES.md`.
- [ ] On a physical arm64 device (API 29+): download, checksum, load, stream, cancel.
      Not performed: this environment has no device or adb access. CI cannot substitute,
      because QVAC does not run on emulators.
- [ ] Airplane mode after download: Ask/Explain/Fix still work. Not performed (no device).
- [ ] Process death during generation leaves the editor content intact. Not performed (no device).
- [x] Signed APK, checksums, manifest and certificate fingerprint are published.
      `v0.0.2` is live: APK SHA-256 `382d8c32dfc957cd461491ee4c633cba4b248ee92d6bafc2c02de0a6cb29bb52`,
      252,522,259 bytes, certificate
      `50b127efc5f6db273d22f9487da9a133572404b78cc46548b33646f3a0bf76db` — the same
      certificate as `v0.0.1`, so updates install in place. The published dex contains
      `com.oneorigin.studio.ai` and `io.tether.qvac`, and the APK keeps
      `minSdk 26` / `targetSdk 28`.

The three device items are the only checklist entries that block the MLP release, not this
foundation release. They are also why the Ask/Explain/Fix surface is not exposed yet.

## Notes

- The engine's states are `Unsupported`, `Idle` (no model on the device),
  `Downloading`, `Loading`, `Ready`, `Running` and `Failed`, matching `LocalAiEngine.kt`.
  `Idle` is the honest "not downloaded yet" state; it never pretends a model is present.
