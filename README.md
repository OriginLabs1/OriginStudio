<p align="center"><img src="docs/media/app-icon-512.png" width="96" height="96" alt="Origin Studio app icon"></p>

<h1 align="center">Origin Studio</h1>

<p align="center"><strong>One workspace. One coherent ecosystem. Replaceable implementations. User-controlled execution. Reproducible knowledge. Verifiable outcomes.</strong></p>

<p align="center">Made with love from OneOrigin.</p>

<p align="center"><strong>Governed by <a href="CONSTITUTION.md">The Origin Studio Constitution v0.2.0</a>.</strong> Local and remote execution, one protocol, one workspace.</p>

## About

Origin Studio is OneOrigin's flagship development workspace: an Android-first, device-agnostic-by-design environment that unifies and strategically extends the useful capabilities of our earlier tools while establishing reusable foundations for future OneOrigin products.

The Constitution is the supreme governing document. The app, the requirements
register, the ADRs, the build settings and every future host are subordinate to
it. Where they disagree, the Constitution wins.

## Features

- **Real Linux Environment** - A full Debian environment inside the app
- **Native Code Editing** - Built-in file browser and editor with syntax highlighting
- **Integrated Terminal** - Full Linux terminal with PRoot support
- **Portable Workspaces** - Every project carries a plain-JSON manifest and a hash-chained history; export opens without Origin Studio
- **Local + Remote Execution** - One versioned Host Protocol (`origin.host/v1`) for the device and a host you control, with explicit disclosure, leases and handoff
- **Deny-by-default Network** - Egress is a capability with a purpose, a data-flow class and a tamper-evident ledger
- **Local AI Foundation** - Pinned, hash-verified QVAC runtime for the upcoming offline Ask/Explain/Fix surface (arm64, Android 10+)
- **AI Agent Support** - Native chat with ACP agents (Claude, Codex, OpenCode, and more)
- **Browser for Testing** - Built-in browser for dev servers and web pages
- **Project Management** - Open, create, and manage projects seamlessly

## Install

Releases are published by **OriginLabs1** on GitHub:

- **Download:** <https://github.com/OriginLabs1/OriginStudio/releases/latest>
- **Obtainium:** add `github.com/OriginLabs1/OriginStudio` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.
- **Website:** <https://originlabs1.github.io/OriginStudio/>

Every release ships a signed APK, a SHA-256 checksum, the signing certificate fingerprint and a machine-readable release manifest. GitHub Releases is the canonical source; mirrors must point back to it and never re-sign or silently modify an artifact.

## Requirements

- Android 8.0+ (API 26+), 64-bit ARM (arm64-v8a) or x86_64
- Local AI runtime: 64-bit ARM (arm64-v8a) and Android 10+ (API 29+)
- 4 GB RAM or more (optimized for modest hardware)
- Several GB of free storage for the Debian environment and developer tools

## Building

This is an Android application built with Kotlin and Jetpack Compose.

```bash
# One-time: fetch the bundled Linux runtime and CA bundle
python3 tools/fetch-shell-runtime.py
python3 tools/fetch-ca-bundle.py

./gradlew assembleDebug
```

A signed release build additionally needs the release keystore at `termfold-release.jks` and the `TERMFOLD_STORE_PASSWORD` / `TERMFOLD_KEY_PASSWORD` environment variables. Release versions are supplied by the tag-driven GitHub Actions workflow in `.github/workflows/release.yml`:

```bash
git tag v0.0.3
git push origin v0.0.3
```

See `docs/` for the engineering notes and `THIRD_PARTY_NOTICES.md` for bundled components.

## The portable core and the reference host

The trust-critical logic is a plain JVM library with no Android imports, so it is
tested on the JVM and reused by the reference host:

```bash
./gradlew :core:test                 # workspace, identity, egress, capability, protocol
node tools/validate-contracts.mjs    # JSON Schema fixtures
node tools/check-evidence.mjs        # requirements ↔ deviations consistency

cd hosts/hub && npm test             # reference host over real HTTP
```

Run a host you control:

```bash
ORIGIN_WORKSPACE_ROOT="$HOME/origin" ORIGIN_HOST_TOKEN="$(openssl rand -hex 16)" \
  node hosts/hub/server.js
```

See [`docs/strategy/DUAL-MODE.md`](docs/strategy/DUAL-MODE.md) for the gates,
disclosure fields and verification steps.

## Conformance

`v0.0.3` plus this tree claims **P0 (partial)** and **P1 (partial)**, with P3/P4
in progress. P2 is **not claimed**: the engine ships and the availability gate
works, but the Ask/Explain/Fix surface is not exposed and inference has not run on
a physical arm64 device. The authoritative statement is
[`docs/strategy/REQUIREMENTS.md`](docs/strategy/REQUIREMENTS.md); every gap is in
[`docs/strategy/DEVIATIONS.md`](docs/strategy/DEVIATIONS.md).

## Project documents

- [`CONSTITUTION.md`](CONSTITUTION.md) — the supreme governing text (v0.2.0).
- `BRAND.md` — the Origin Loop mark, the steel palette and the asset inventory.
- `BLUEPRINT.md` — the product and architecture blueprint.
- `RELEASING.md` — how a signed release is cut and verified.
- `docs/DISTRIBUTION.md` — channels, device tiers, verification and rollback.
- [`docs/strategy/DUAL-MODE.md`](docs/strategy/DUAL-MODE.md) — local and remote execution, gates and host operation.
- [`contracts/README.md`](contracts/README.md) — the wire schemas and fixtures.
- `docs/adr/` — ADR-0001 … ADR-0009.
- `docs/strategy/` — governance, threat model, requirements register, deviations and personas.
- `docs/PRIVACY.md`, `docs/SECURITY.md`, `docs/ACCESSIBILITY.md` — the published policies.

## License

Apache-2.0. See LICENSE for details.
