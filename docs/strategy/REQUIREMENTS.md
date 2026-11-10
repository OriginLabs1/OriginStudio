# Requirements register

- **Status:** first register. Statuses describe **v0.0.3**, not intentions.
- **Rule:** *Met* requires linked evidence in this repository or in the published
  release. Everything else is *Partial* or *Not met* and appears in
  `DEVIATIONS.md`.
- **Verification methods:** Test · Inspection · Demo · Bench · Audit

Profile key: **P0** Core · **P1** Workspace · **P2** Intelligence · **P3** Ecosystem · **P4** Universal.

## P0 — Core

| ID | Requirement | Verify | Status | Evidence |
| --- | --- | --- | --- | --- |
| OS-CORE-001 | Identity is created locally, without network; key material is held by the platform keystore; destroying it requires explicit confirmation | Test | Not met | No first-class identity yet |
| OS-CORE-002 | No account, registration or cloud service is required to use the workspace | Inspection | Met | First launch works offline |
| OS-CORE-003 | Permissions are explicit, enumerable and revocable by the user | Inspection | Partial | Android manifest permissions only; no capability permissions |
| OS-CORE-010 | Every capability invocation is authorised outside the capability; denial is logged | Test | Not met | No capability layer yet |
| OS-CORE-011 | Consequential actions are confirmed and recorded in an action log | Test | Partial | Update install is user-confirmed; no general action log |
| OS-CORE-020 | Installable capabilities carry a versioned, machine-readable manifest | Inspection | Not met | P3 |
| OS-CORE-021 | Capability contracts are versioned and compatibility-tested | Test | Not met | P3 |
| OS-CORE-030 | No extension can bypass the core's trust boundary | Inspection | Not met | P3 |
| OS-DATA-001 | A workspace is plain files on disk, not a proprietary database | Inspection | Met | Projects live in the Debian rootfs |
| OS-DATA-002 | Export produces files readable without Origin Studio | Demo | Met | Export through SAF; tar/zip in the guest |
| OS-DATA-003 | No silent destructive action; deletes are user-initiated | Inspection | Partial | File manager confirms deletions; no audit |
| OS-DATA-004 | Opening a workspace verifies its integrity | Test | Not met | No checkpoint or manifest |
| OS-DATA-005 | A kill during any write never leaves a silently corrupt project; restart offers recovery | Test | Partial | Editor tracks unsaved state; no fault-injection test |
| OS-DATA-006 | Backup and restore are available and documented | Demo | Partial | Manual export/import; no scheduled backup |
| OS-DATA-010 | Format and distro migrations are versioned and reversible | Test | Partial | `DistroMigrationTest`; no rollback proof |
| OS-EXEC-001 | One execution contract spans providers | Inspection | Not met | PRoot is the only provider and is not abstracted |
| OS-EXEC-002 | Each provider's real isolation class is shown before first run | Demo | Partial | Documented in `ANDROID-COMPATIBILITY.md`; not shown in-app |
| OS-EXEC-003 | Untrusted code never runs in a provider that is not a sandbox | Inspection | Partial | Policy only; nothing enforces it |
| OS-EXEC-004 | Memory, storage and battery budgets are declared and measured | Bench | Partial | No published budget table yet |
| OS-EXEC-005 | Resource pressure degrades functionality rather than losing work | Test | Partial | Android process death restores sessions; unmeasured |
| OS-TRUST-001 | Every release is signed | Test | Met | `release.yml` refuses unsigned builds |
| OS-TRUST-002 | Every artifact has a SHA-256 checksum | Test | Met | `SHA256SUMS` on each release |
| OS-TRUST-003 | The signing certificate fingerprint is published | Inspection | Met | `signing-certificate.txt` + release notes |
| OS-TRUST-004 | Updates verify checksum and signature before install | Test | Met | `UpdateManager`; Android installer verification |
| OS-TRUST-005 | Downgrade is blocked | Test | Met | Android same-key version rule; update flow only offers newer |
| OS-TRUST-006 | Capability and release revocation is verifiable offline with an expiry | Test | Not met | No revocation format |
| OS-TRUST-007 | A release can be rebuilt from its tag, with non-determinism documented | Bench | Partial | CI builds from tags; no bit-for-bit comparison |
| OS-TRUST-008 | A machine-readable SBOM ships with each release | Inspection | Not met | Third-party notices only |
| OS-REL-001 | CI refuses to publish an unsigned or unverifiable APK | Inspection | Met | `release.yml` verification step |
| OS-REL-002 | Supported devices and known limits are published with each release | Inspection | Partial | Release notes + `ANDROID-COMPATIBILITY.md`; no measured matrix |
| OS-PRIV-001 | No analytics, telemetry, advertising or silent egress | Inspection | Met | No such dependency; update check is a plain GitHub GET |
| OS-PRIV-002 | External delegation (agents, downloads) is visible to the user | Demo | Partial | User-initiated installs; no egress log |
| OS-DOC-001 | Reproducible build and verification instructions are published | Inspection | Met | `BUILDING.md`, `RELEASING.md` |
| OS-DOC-002 | Architectural decisions are recorded | Inspection | Partial | ADR-0001, ADR-0002 |

## P1 — Workspace

| ID | Requirement | Verify | Status | Evidence |
| --- | --- | --- | --- | --- |
| OS-UX-001 | A newcomer can understand and start within five minutes | Demo | Partial | Not yet measured on the Tier A device |
| OS-UX-002 | Cold start ≤ 1.5 s p95 on the baseline device | Bench | Not met | No baseline device or harness yet |
| OS-UX-003 | The workspace works offline after setup | Demo | Partial | Editor and terminal work; apt and agents need network |
| OS-UX-004 | 200 % font scale, screen-reader order and 4.5:1 contrast on core screens | Audit | Partial | Compose text scales; no TalkBack audit |
| OS-UX-005 | Every error has a next step; no dead ends | Demo | Partial | Errors surface output; copy not audited |
| OS-UX-006 | Touch targets are at least 48 dp | Audit | Partial | Most controls; not systematically checked |
| OS-UX-007 | The core journey is keyboard and pointer friendly on tablets | Demo | Met | Key row, hardware keyboard support |
| OS-EDIT-001 | Syntax-aware editing for HTML/CSS/JS/TS/JSON/MD | Test | Met | CodeMirror bundle |
| OS-EDIT-002 | Find and replace, external-change reload | Test | Met | Editor search; file watcher |
| OS-EXEC-010 | A real Linux userland runs without root | Demo | Met | Bundled Debian + PRoot |
| OS-EXEC-011 | The user can stop a running workload | Demo | Met | Stop action in terminal and notifications |
| OS-EXEC-012 | Logs are visible and copyable | Demo | Met | Terminal scrollback and share |

## P2 — Intelligence

| ID | Requirement | Verify | Status | Evidence |
| --- | --- | --- | --- | --- |
| OS-AI-001 | No mandatory model, vendor or account | Inspection | Met | Local engine; external agents optional |
| OS-AI-002 | After one model download, inference works offline | Test | Partial | Engine ships; physical-device run outstanding |
| OS-AI-003 | Model output is never applied without diff and approval | Test | Not met | Ask/Explain/Fix surface not exposed |
| OS-AI-004 | Streaming and cancellation are user-visible | Demo | Partial | Engine supports it; no UI |
| OS-AI-005 | A model whose hash does not match is refused | Test | Partial | Manifest carries SHA-256; not yet verified on device |
| OS-AI-006 | Model size, licence, source and deletion are disclosed before download | Demo | Partial | Gate exists; no download UI |
| OS-AI-007 | No egress from the AI path without a per-session grant | Test | Not met | No egress policy |
| OS-AI-008 | Unsupported hardware shows honest unavailability | Test | Met | `LocalAiGateTest`; runtime gate |
| OS-AI-009 | Permission checks live in tool handlers, not in the model | Inspection | Not met | Handlers not shipped |

## P3 / P4 — not claimed

| ID | Requirement | Status |
| --- | --- | --- |
| OS-CAP-001 | A third-party capability can be installed, signed, disabled and removed | Not met |
| OS-CAP-002 | Revoked capabilities stop running | Not met |
| OS-CAP-003 | The same core and contracts run on a second platform | Not met |
| OS-CAP-004 | A portable workspace opens unchanged on that platform | Not met |

## How this register is maintained

1. A pull request that adds or changes behaviour updates the affected row.
2. *Met* is set only in the same change that adds the evidence link.
3. At release time, every *Not met* and *Partial* row is copied into the release
   notes as a known limitation.
4. At each minor release, `DEVIATIONS.md` is regenerated from this table.
