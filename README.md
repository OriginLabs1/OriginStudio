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

## Why Origin Studio is unique

- **One workspace, several windows.** A project is plain files with a versioned manifest and a hash-chained history. It opens unchanged in the Android app, the zero-build companion, the Node CLI and the Rust core.
- **Two honest environments.** Debian 13 runs locally on the phone, offline and without root. Dakota OS — Bluefin's atomic community image — runs on a host you own. The app labels which is which and what each cannot do.
- **The room, not another agent.** Origin Agent is a built-in profile over the ACP agents you already use, with one approvals sheet and no second subscription. Any other CLI agent works in a normal shell.
- **A small, auditable core.** Environment, agent, Jump, preview, usage and automation decisions are implemented in a dependency-light Rust core and mirrored in a plain-JVM core. No network, no daemon, no vendor account.
- **Honest limits.** PRoot is not a VM; the prompt count is the device's count, not a provider quota; Dakota is alpha community software.

## Features

- **Studio** - One screen for environments, agents, usage, scheduled commands, device lab and premium finishes
- **Two environments** - Debian 13 on the phone, or Dakota OS on a paired host, each with honest limits
- **Jump Anywhere** - Ctrl/Cmd+K reaches every workspace, session, environment, agent and action
- **Real Linux Environment** - A full Debian environment inside the app
- **Native Code Editing** - Built-in file browser and editor with syntax highlighting
- **Integrated Terminal** - Full Linux terminal with PRoot support
- **Portable Workspaces** - Every project carries a plain-JSON manifest and a hash-chained history; export opens without Origin Studio
- **Universal Companion** - A zero-build installable web app that opens the same workspace from any screen, served by the host or any static server
- **`origin` CLI** - Dependency-free Node commands to init, verify, list, checkpoint and serve workspaces anywhere
- **Fleet** - Every session on the device in one place, grouped by project, with honest live state
- **Local + Remote Execution** - One versioned Host Protocol (`origin.host/v1`) for the device and a host you control, with explicit disclosure, leases and handoff
- **Deny-by-default Network** - Egress is a capability with a purpose, a data-flow class and a tamper-evident ledger
- **AI Agent Support** - Native chat with ACP agents (Claude, Codex, OpenCode, and more), plus Origin Agent and any CLI agent in a shell
- **Rich Previews** - Images, PDF with page navigation, and Markdown with an outline and source toggle
- **Files, images and dictation** - Drag files from another app, attach any text file, or speak a prompt and edit the words
- **Scheduled commands** - Daily or weekly commands run as real, readable shell sessions while the app is open
- **Browser for Testing** - Built-in browser for dev servers and web pages, drivable by the agent with the same approvals
- **Project Management** - Open, create, and manage projects seamlessly

## Install

Releases are published by **OriginLabs1** on GitHub:

- **Download:** <https://github.com/OriginLabs1/OriginStudio/releases/latest>
- **Obtainium:** add `github.com/OriginLabs1/OriginStudio` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.
- **Website:** <https://originlabs1.github.io/OriginStudio/>

Every release ships a signed APK, a SHA-256 checksum, the signing certificate fingerprint and a machine-readable release manifest. GitHub Releases is the canonical source; mirrors must point back to it and never re-sign or silently modify an artifact.

## Requirements

- Android 8.0+ (API 26+), 64-bit ARM (arm64-v8a) or x86_64
- 4 GB RAM or more (optimized for modest hardware)
- Several GB of free storage for the Debian environment and developer tools

## Building

This is an Android application built with Kotlin and Jetpack Compose, with a Rust core and
dependency-free Node tooling. It needs JDK 21, Android SDK 36, Python 3.9+ and — for the
core — a stable Rust toolchain.

```bash
# One-time: fetch the bundled Linux runtime and CA bundle
python3 tools/fetch-shell-runtime.py
python3 tools/fetch-ca-bundle.py

./gradlew assembleDebug
```

A signed release build additionally needs the release keystore at `termfold-release.jks` and the `TERMFOLD_STORE_PASSWORD` / `TERMFOLD_KEY_PASSWORD` environment variables. Release versions are supplied by the tag-driven GitHub Actions workflow in `.github/workflows/release.yml`:

```bash
git tag v0.0.10
git push origin v0.0.10
```

See `docs/` for the engineering notes and `THIRD_PARTY_NOTICES.md` for bundled components.

## The Rust core

The workspace decisions — environments, agents, Jump, previews, usage and automations —
also live in a small, platform-free Rust crate and CLI. It needs no JVM, no Node and no
network:

```bash
cargo test --manifest-path rust/origin-core/Cargo.toml
cargo run --release --manifest-path rust/origin-core/Cargo.toml -- environments
cargo run --release --manifest-path rust/origin-core/Cargo.toml -- why --json
```

The release ships x86_64 and aarch64 Linux archives of `origin-core`. The Android app
mirrors the same answers in Kotlin and does not load Rust at runtime.

## The portable core and the reference host

The trust-critical logic is also a plain JVM library with no Android imports, so it is
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

## Use it anywhere

The same workspace works without the app:

```bash
# zero-build companion, served by the host
npx github:OriginLabs1/OriginStudio serve --root ~/origin

# or from the release tarball (unpacks into package/)
tar -xzf origin-cli-0.0.10.tgz
node package/cli/origin.mjs init ~/origin/demo --name "Demo"
node package/cli/origin.mjs verify ~/origin/demo
node package/cli/origin.mjs checkpoint ~/origin/demo

# or the Rust core, no JVM or Node needed
tar -xzf origin-core-0.0.10-x86_64-unknown-linux-gnu.tar.gz
./origin-core environments
```

The companion is plain HTML/CSS/JS: unzip it on any static server, or open the host URL.
It has no build step, no framework and no runtime dependency. See
[`docs/universal.md`](docs/universal.md).

## Conformance

`v0.0.10` keeps the **P0 (partial)** and **P1 (partial)** claims, with P3/P4 in progress. P2 is
**not claimed**: the bundled local-AI runtime was removed in this line to keep the app
lightweight (ADR-0010); the `LocalAiEngine` boundary stays, and the Ask/Explain/Fix surface
returns when it ships. The authoritative statement is
[`docs/strategy/REQUIREMENTS.md`](docs/strategy/REQUIREMENTS.md); every gap is in
[`docs/strategy/DEVIATIONS.md`](docs/strategy/DEVIATIONS.md).

## Project documents

- [`CONSTITUTION.md`](CONSTITUTION.md) — the supreme governing text (v0.2.0).
- `BRAND.md` — the Origin Loop mark, the steel palette and the asset inventory.
- `BLUEPRINT.md` — the product and architecture blueprint.
- `RELEASING.md` — how a signed release is cut and verified.
- `docs/DISTRIBUTION.md` — channels, device tiers, verification and rollback.
- [`docs/strategy/DUAL-MODE.md`](docs/strategy/DUAL-MODE.md) — local and remote execution, gates and host operation.
- [`docs/universal.md`](docs/universal.md) — companion, CLI, host additions and release artifacts.
- [`contracts/README.md`](contracts/README.md) — the wire schemas and fixtures.
- `docs/adr/` — ADR-0001 … ADR-0011.
- `docs/releases/` — per-version notes, including v0.0.10.
- `docs/strategy/` — governance, threat model, requirements register, deviations and personas.
- `docs/PRIVACY.md`, `docs/SECURITY.md`, `docs/ACCESSIBILITY.md` — the published policies.

## License

Apache-2.0. See LICENSE for details.
