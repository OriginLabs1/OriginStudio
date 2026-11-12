# The universal layer

Origin Studio is an Android app, but a workspace is not an Android artefact. This page
describes the three pieces that make the same workspace usable anywhere — on a laptop, a
home server, a tablet browser or a phone that is not the one holding the app.

Everything here is dependency-free and runs from the tagged source tree. Nothing needs an
account, a build service or a vendor.

## What ships in v0.0.10

| Artifact | Size | What it is |
| --- | --- | --- |
| `OriginCompanion-<version>.zip` | a few KB | Zero-build installable web app |
| `origin-cli-<version>.tgz` | a few KB | The `origin` CLI and reference host |
| `origin-core-<version>-x86_64-unknown-linux-gnu.tar.gz` | ~1 MB | Rust core CLI for x86_64 Linux |
| `origin-core-<version>-aarch64-unknown-linux-gnu.tar.gz` | ~1 MB | Rust core CLI for ARM64 Linux |
| `OriginStudio-<version>.sbom.cdx.json` | a few KB | CycloneDX SBOM for the release |
| `OriginStudio-<version>.apk` | ~62 MB | The Android app |

The APK stays small because the bundled QVAC local-AI runtime was removed (ADR-0010). The
universal layer keeps adding reach without weight: the companion and Node CLI are source
small enough to read, and the Rust core is a single static binary per architecture.

## The companion

The companion is plain HTML, CSS and JavaScript. It has no build step, no framework and no
runtime dependency. It can be:

1. **Served by the host** — start `origin serve` and open the host URL. The host serves the
   companion at `/` and `/companion/`, on the same origin as the protocol.
2. **Opened as a static site** — unzip the release archive on any static web server. Point
   it at a host URL when it loads.
3. **Installed to a home screen** — it is a PWA with a manifest and service worker. The
   shell is cached; workspace status is never cached, because a status must be the host's
   answer, not yesterday's.

The companion can list workspaces, inspect verified event history, create a checkpoint and
run the same health checks as the CLI. It never holds a private key and never writes to a
workspace directly; every action is a protocol message the host verifies.

## The CLI

The CLI is one Node 20+ file. It imports the reference host's own hashing and verification
code, so a workspace created by the CLI opens unchanged in the app and vice versa.

```sh
# from the release tarball (unpacks into package/)
tar -xzf origin-cli-0.0.10.tgz
node package/cli/origin.mjs doctor

# or straight from GitHub
npx github:OriginLabs1/OriginStudio doctor

# if published to npm (not yet; use npx github:... above)
npx @originlabs/origin doctor
```

Commands:

```sh
origin init ~/origin/demo --name "Demo"   # create a portable workspace
origin verify ~/origin/demo               # manifest + every hash-chained event
origin list --root ~/origin --json        # machine-readable workspace index
origin checkpoint ~/origin/demo           # append a checkpoint to the history
origin serve --root ~/origin --port 8787  # host + companion on one origin
origin doctor                             # Node version, files, ports, contracts
```

`origin serve` binds to loopback by default. Put it behind your own TLS terminator before
exposing it, and set `ORIGIN_HOST_TOKEN` (or `--token`) when it is not loopback-only.

## The Rust core

`origin-core` answers the workspace's own questions from a terminal: which environments
exist, which agents and themes are catalogued, how Jump ranks a query, whether a file can be
previewed, how a schedule resolves, and why the product is different. It is a single static
binary, dependency-light (serde only), offline and stateless.

```sh
tar -xzf origin-core-0.0.10-x86_64-unknown-linux-gnu.tar.gz   # or aarch64
./origin-core fingerprint
./origin-core environments
./origin-core recommend --host
./origin-core why --json
```

It is the same contract the app mirrors in Kotlin, so a host, a script or a support session
can check an answer without a device. Building it needs only Cargo:

```sh
cargo test --manifest-path rust/origin-core/Cargo.toml
cargo build --release --manifest-path rust/origin-core/Cargo.toml
```

## The host

The reference host is the same one documented in
[`strategy/DUAL-MODE.md`](strategy/DUAL-MODE.md) and ADR-0009. v0.0.9 added two messages
that the companion needs and the app can use:

- `list_workspaces` — verified summaries of every workspace under the root.
- `list_events` — the verified, bounded event history for one workspace.

Both are additive to `origin.host/v1`; an older client ignores them. The host also answers
CORS preflight and reports `authRequired` in `/health`, so a browser on another origin can
discover whether it needs a token before asking for one.

## The local app still matters

The universal layer is not a replacement for the app. The app owns the Linux userland,
terminals, agent processes, the editor and the preview browser. The companion and CLI own
reach and verification: they let a second screen see the same workspace, and let anyone
prove the history without trusting the app.

## Verification

```sh
cargo test --manifest-path rust/origin-core/Cargo.toml
node --test cli/test/*.test.mjs hosts/hub/test/*.test.js
node tools/validate-contracts.mjs
node tools/check-companion.mjs
node tools/check-evidence.mjs
```

The release workflow runs all of them, cross-builds the Rust core for x86_64 and aarch64,
then packages the companion, CLI, core archives and SBOM beside the signed APK and lists
every artifact in `SHA256SUMS`.
