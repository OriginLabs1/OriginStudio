# Requirements register

- **Status:** second register. Statuses describe the tree after the v0.2.0
  Constitution and the dual-mode core, not intentions.
- **Supreme text:** [`CONSTITUTION.md`](../../CONSTITUTION.md). Where this register
  and the Constitution disagree, the Constitution wins.
- **Rule:** *Met* requires linked evidence in this repository or in the published
  release. Everything else is *Partial* or *Not met* and appears in
  `DEVIATIONS.md`.
- **Verification methods:** Test · Inspection · Demo · Bench · Audit

Profile key: **P0** Core · **P1** Workspace · **P2** Intelligence ·
**P3** Ecosystem · **P4** Universal · **P5** Federation.

## P0 — Core

| ID | Requirement | Verify | Status | Evidence |
| --- | --- | --- | --- | --- |
| OS-CORE-001 | Identity is created locally, without network; key material is held by the platform keystore; destroying it requires explicit confirmation | Test | Partial | `core/src/main/kotlin/com/oneorigin/studio/origin/OriginIdentity.kt`, `OriginIdentityTest.kt`; app-private file, keystore binding still open |
| OS-CORE-002 | No account, registration or cloud service is required to use the workspace | Inspection | Met | First launch works offline |
| OS-CORE-003 | Permissions are explicit, enumerable and revocable by the user | Inspection | Partial | Capability permissions modelled in `OriginCapability.kt`; Android manifest permissions remain separate |
| OS-CORE-010 | Every capability invocation is authorised outside the capability; denial is logged | Test | Partial | `CapabilityTrust.verify` + `OriginCapabilityTest`; per-invocation host authorisation not yet wired into the app |
| OS-CORE-011 | Consequential actions are confirmed and recorded in an action log | Test | Partial | Update install is user-confirmed; no general action log |
| OS-CORE-020 | Installable capabilities carry a versioned, machine-readable manifest | Inspection | Partial | `contracts/capability.schema.json`, `OriginCapability.kt`; install flow not shipped |
| OS-CORE-021 | Capability contracts are versioned and compatibility-tested | Test | Partial | Host protocol compatibility tested; capability schema compatibility not yet exercised across versions |
| OS-CORE-030 | No extension can bypass the core's trust boundary | Inspection | Partial | Trust checks live in `:core`; the app does not route every action through it yet |
| OS-DATA-001 | A workspace is plain files on disk, not a proprietary database | Inspection | Met | Projects live in the Debian rootfs; `.origin/` is plain JSON |
| OS-DATA-002 | Export produces files readable without Origin Studio | Demo | Met | Export through SAF; tar/zip in the guest |
| OS-DATA-003 | No silent destructive action; deletes are user-initiated | Inspection | Partial | File manager confirms deletions; no audit |
| OS-DATA-004 | Opening a workspace verifies its integrity | Test | Partial | `OriginWorkspace.verify` + `OriginWorkspaceTest`; the app ensures metadata but does not yet surface a failed verification |
| OS-DATA-005 | A kill during any write never leaves a silently corrupt project; restart offers recovery | Test | Partial | Editor tracks unsaved state; no fault-injection test |
| OS-DATA-006 | Backup and restore are available and documented | Demo | Partial | Manual export/import; no scheduled backup |
| OS-DATA-010 | Format and distro migrations are versioned and reversible | Test | Partial | `DistroMigrationTest`; no rollback proof |
| OS-EXEC-001 | One execution contract spans providers | Inspection | Met | `OriginHostProtocol.kt`, `contracts/host-envelope.schema.json`, `hosts/hub/server.js` |
| OS-EXEC-002 | Each provider's real isolation class is shown before first run | Demo | Partial | Isolation classes travel in the protocol; not yet shown in-app |
| OS-EXEC-003 | Untrusted code never runs in a provider that is not a sandbox | Inspection | Partial | `IsolationClass` encodes the rule; enforcement is policy only |
| OS-EXEC-004 | Memory, storage and battery budgets are declared and measured | Bench | Partial | No published budget table yet |
| OS-EXEC-005 | Resource pressure degrades functionality rather than losing work | Test | Partial | Android process death restores sessions; unmeasured |
| OS-HOST-001 | Every host exchange uses a versioned envelope; an unknown major fails closed | Test | Met | `OriginHostProtocolTest`, `hosts/hub/test/server.test.js` |
| OS-HOST-002 | A host discloses operator, region, isolation, retention, availability and price before use | Test | Met | `HostCapabilities`, `contracts/host-capabilities.schema.json`, both test suites |
| OS-HOST-003 | Remote execution is opt-in; a local host wins a tie | Test | Met | `HostSelection`, `OriginHostProtocolTest` |
| OS-HOST-004 | A failed host is surfaced, never silently retried against a different host | Test | Met | Selection has no fallback; `HttpOriginHostClient` throws |
| OS-EGRESS-001 | Egress is denied unless a scoped grant matches | Test | Met | `OriginEgressTest` |
| OS-EGRESS-002 | Capability, host and purpose are mandatory for egress | Test | Met | `OriginEgressTest` |
| OS-EGRESS-003 | The egress ledger records grants and denials and is tamper-evident | Test | Met | `OriginEgressTest`, `contracts/egress-entry.schema.json` |
| OS-CAP-010 | A capability manifest binds id, version, publisher, permissions, data-flow class, isolation and artifact hash | Test | Met | `OriginCapabilityTest`, `contracts/capability.schema.json` |
| OS-CAP-011 | A revoked capability does not run | Test | Partial | Revocation set is checked before execution; signed revocation distribution is not shipped |
| OS-WS-001 | A workspace carries a versioned portable manifest; unknown schemas are refused | Test | Met | `OriginWorkspaceTest`, `contracts/workspace.schema.json` |
| OS-WS-002 | Workspace history is hash-chained and verified; tampering is reported, not repaired | Test | Met | `OriginWorkspaceTest`, `hosts/hub/test/server.test.js` |
| OS-WS-003 | At most one writer holds a lease; handoff is explicit and epoch-bound | Test | Met | `OriginWorkspaceTest` |
| OS-WS-004 | Export is standard-format and excludes transient leases | Test | Met | `OriginWorkspaceTest` |
| OS-WS-005 | Divergent edits produce a visible conflict, never a silent merge | Test | Not met | Sync is not implemented; the lease prevents concurrent writers today |
| OS-DUAL-001 | D1: the same workspace opens locally and on a reference remote host with a matching manifest hash and event count | Test | Met | `OriginHostProtocolTest`, `hosts/hub/test/server.test.js` |
| OS-DUAL-002 | D2: a lease transfers device → host and host → device without losing an event | Test | Met | `OriginWorkspaceTest` (epoch-bound transfer in both directions) |
| OS-DUAL-003 | D3: after setup, local read, edit, run and export work with the network disabled | Demo | Partial | Core and loopback are offline; app baseline is bounded by OS-UX-003 |
| OS-DUAL-004 | D4: no remote host is used before disclosure is shown and acknowledged | Demo | Partial | Disclosure travels in the protocol; acknowledgement UI is not built |
| OS-DUAL-005 | D5: the user can revoke remote access, delete remote copies and export everything | Test | Partial | Revocation implemented on both sides; deletion acknowledgement is missing |
| OS-TRUST-001 | Every release is signed | Test | Met | `release.yml` refuses unsigned builds |
| OS-TRUST-002 | Every artifact has a SHA-256 checksum | Test | Met | `SHA256SUMS` on each release |
| OS-TRUST-003 | The signing certificate fingerprint is published | Inspection | Met | `signing-certificate.txt` + release notes |
| OS-TRUST-004 | Updates verify checksum and signature before install | Test | Met | `UpdateManager`; Android installer verification |
| OS-TRUST-005 | Downgrade is blocked | Test | Met | Android same-key version rule; update flow only offers newer |
| OS-TRUST-006 | Capability and release revocation is verifiable offline with an expiry | Test | Partial | Revocation ids are checked offline; no signed record with an expiry yet |
| OS-TRUST-007 | A release can be rebuilt from its tag, with non-determinism documented | Bench | Partial | CI builds from tags; no bit-for-bit comparison |
| OS-TRUST-008 | A machine-readable SBOM ships with each release | Inspection | Not met | Third-party notices only |
| OS-REL-001 | CI refuses to publish an unsigned or unverifiable APK | Inspection | Met | `release.yml` verification step |
| OS-REL-002 | Supported devices and known limits are published with each release | Inspection | Partial | Release notes + `ANDROID-COMPATIBILITY.md`; no measured matrix |
| OS-PRIV-001 | No analytics, telemetry, advertising or silent egress | Inspection | Met | No such dependency; update check is a plain GitHub GET |
| OS-PRIV-002 | External delegation (agents, downloads) is visible to the user | Demo | Partial | Egress ledger exists in `:core`; no user-facing view yet |
| OS-DOC-001 | Reproducible build and verification instructions are published | Inspection | Met | `BUILDING.md`, `RELEASING.md` |
| OS-DOC-002 | Architectural decisions are recorded | Inspection | Met | ADR-0001 … ADR-0009 |

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
| OS-AI-007 | No egress from the AI path without a per-session grant | Test | Partial | `EgressPolicy` is deny-by-default with tests; the AI path is not wired through it yet |
| OS-AI-008 | Unsupported hardware shows honest unavailability | Test | Met | `LocalAiGateTest`; runtime gate |
| OS-AI-009 | Permission checks live in tool handlers, not in the model | Inspection | Not met | Handlers not shipped |

## P3 / P4 / P5 — not claimed

| ID | Requirement | Status |
| --- | --- | --- |
| OS-CAP-001 | A third-party capability can be installed, signed, disabled and removed | Not met |
| OS-CAP-002 | Revoked capabilities stop running end-to-end across hosts | Not met |
| OS-CAP-003 | The same core and contracts run on a second platform | Not met |
| OS-CAP-004 | A portable workspace opens unchanged on that platform | Not met |
| OS-FED-001 | Multiple hosts are discoverable and selectable per capability | Not met |
| OS-FED-002 | Sync is conflict-explicit and never silently merges | Not met |
| OS-FED-003 | Remote copies can be deleted with a verifiable acknowledgement | Not met |

## How this register is maintained

1. A pull request that adds or changes behaviour updates the affected row.
2. *Met* is set only in the same change that adds the evidence link.
3. At release time, every *Not met* and *Partial* row is copied into the release
   notes as a known limitation.
4. At each minor release, `DEVIATIONS.md` is regenerated from this table and
   `tools/check-evidence.mjs` must pass in CI.
