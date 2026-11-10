# Distribution

**Canonical source:** <https://github.com/OriginLabs1/OriginStudio/releases>
**Package:** `com.oneorigin.studio` · **Licence:** Apache-2.0
**Current line:** `v0.0.3`

Every channel below points at the same signed artifact. No channel re-signs,
rebuilds or silently replaces an APK.

## Channels

| Channel | Status | What it is for | Requirement |
| --- | --- | --- | --- |
| **GitHub Releases** | Live, canonical | Signed APK, checksums, certificate, manifest, notes | Built from the tag by CI, signed, verified |
| **Website** (`originlabs1.github.io/OriginStudio`) | Live | Explains the app, links to the latest release | Never hosts an unverifiable copy |
| **Obtainium** | Live | Automatic updates from GitHub Releases | Repo URL only: `github.com/OriginLabs1/OriginStudio` |
| **In-app updater** | Live | Checks GitHub, verifies SHA-256 and signature, hands the APK to Android | Never bypasses the platform installer |
| **IzzyOnDroid** | Candidate | FOSS-friendly APK repository | Application submitted after the SBOM and a clean rebuild |
| **F-Droid** | Not eligible yet | Fully free, reproducible builds | See the assessment below |
| **Google Play** | Not offered | — | `targetSdk 28`, package installs and bundled binaries conflict with current policy |
| **Mirrors** | Allowed | Availability | Must link to canonical checksums and must not re-sign |

### Why not Google Play

Three independent blockers, any one of which is enough:

1. `targetSdk 28` is load-bearing for executing the bundled PRoot binary (ADR-0002).
2. The app installs APKs (agents and self-updates) through `REQUEST_INSTALL_PACKAGES`.
3. The product is a general-purpose Linux environment; Play policy treats that
   category conservatively.

A Play build would be a *different product* with fewer capabilities, published
under a separate application ID. It is not a goal.

### F-Droid assessment (current: not eligible)

| Requirement | State |
| --- | --- |
| FOSS application code | Yes — Apache-2.0 |
| All dependencies FOSS and buildable from source | Partial — QVAC (Apache-2.0) and Termux libs are prebuilt; JitPack supplies binaries |
| No proprietary blobs | Partial — Debian rootfs and agent installers need review |
| Reproducible build | Not proven |
| No anti-features | Model downloads and non-free model licences need disclosure |

Reassess after: an SBOM per release, a build-from-source path for the JitPack
libraries, and a documented rootfs provenance. Until then, do not tell users
F-Droid is coming.

### IzzyOnDroid

The realistic next channel: it accepts developer-built APKs, tolerates the
sideload model, and mirrors the GitHub release. Submission needs the release
manifest, the signing certificate, a description and a licence. Do it after
`v0.0.3` has a stable release page.

## What ships in a release

| Artifact | Purpose |
| --- | --- |
| `OriginStudio-<version>.apk` | The app |
| `OriginStudio-<version>.apk.sha256` | Per-file checksum |
| `SHA256SUMS` | All artifacts in one file |
| `signing-certificate.txt` | `apksigner` output, full certificate |
| `release-manifest.json` | Machine-readable version, size, hash, certificate |
| Release notes | What changed, device tiers, known limitations, rollback |
| Source archive (tag) | The exact tree, including `:core`, `contracts/` and `hosts/hub/` |

The source archive is part of the distribution, not a courtesy. It carries the
Constitution, the requirements register, the contracts and the reference host, so
anyone can verify the claims and run a remote host without the app.

## Distribute the core and the host

The dual-mode shift is distributed as source, deliberately:

| Component | Where | How it is consumed |
| --- | --- | --- |
| `:core` | `core/` in the tagged tree | Built by Gradle; plain JVM, no Android imports |
| Contracts | `contracts/` | JSON Schema 2020-12 plus cross-language fixtures |
| Reference host | `hosts/hub/` | `node hosts/hub/server.js`; no dependencies |
| Constitution | `CONSTITUTION.md` | Linked from the README and the website |

A downstream host implementation is conformant only when it passes the same
fixtures and the reference-host tests. A fork that changes the wire format must
use a new protocol major and may not call itself compatible.

## Verify a download

```bash
sha256sum -c OriginStudio-<version>.apk.sha256
apksigner verify --print-certs OriginStudio-<version>.apk
```

Compare the printed SHA-256 with `signing-certificate.txt`. The certificate must
be the one published with every previous release; a different certificate means
the update is not an update.

Verify the remote-side contract as well:

```bash
node tools/validate-contracts.mjs
node tools/check-evidence.mjs
cd hosts/hub && npm test
```

## Device tiers

| Tier | Device | What runs |
| --- | --- | --- |
| **A (MLP)** | arm64, Android 10+ (API 29+), 4 GB RAM min / 6 GB recommended, ≥ 5 GB free | Full workspace + local AI |
| **B (compat)** | Android 8–9, arm64 or x86_64, 4 GB recommended | Core workspace; local AI shown as unavailable |
| **C (future)** | Devices with a real VM (AVF/microVM) | Stronger isolation for untrusted execution |

`minSdk 26`, `targetSdk 28`, two ABIs: `arm64-v8a` and `x86_64`. The local-AI
runtime is `arm64-v8a` only and does not run on emulators.

## Release procedure

1. `python3 tools/fetch-qvac.py` — hash-verified, git-ignored.
2. `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest`
3. `node tools/validate-contracts.mjs && node tools/check-evidence.mjs`
4. `(cd hosts/hub && npm test)`
5. `ORIGIN_VERSION_NAME=<v> ORIGIN_VERSION_CODE=<code> ./gradlew :app:assembleRelease`
6. Inspect the APK: `minSdk 26`, `targetSdk 28`, `com.oneorigin.studio.ai` present, signed by the release certificate.
7. `git tag v<version> && git push origin v<version>` — CI builds, signs, verifies and publishes.
8. Update the website only by linking to the new release.
9. Announce, with the known limitations from `REQUIREMENTS.md` and the claimed profile.

A tag is immutable. A bad release gets a higher patch version.

## Rollback

There is no server-side rollback: Android does not downgrade an app in place. The
honest options are:

- **Fix forward** with a higher patch version (normal case).
- **Reinstall an older APK** after uninstalling, which loses app-private data —
  document this, and tell users to export first.
- **Keep the previous APK** linked in the release notes for users who must step
  back.

## Mirror rules

1. Link to the canonical release and its checksums.
2. Never re-sign, strip or rebuild.
3. Mirror the whole set: APK, checksum, certificate, manifest, notes, and the
   source archive.
4. State the mirror date; do not serve a stale "latest".
5. Do not mirror a host build that claims conformance without the contract and
   reference-host tests.
