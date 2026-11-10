# Deviations register

Every requirement in `REQUIREMENTS.md` that is not **Met** appears here with an
owner, a reason, and an expiry. An expired entry means the requirement is either
met or the Constitution is amended — never that the entry disappears.

- **Register version:** 2 (Constitution v0.2.0)
- **Owner column:** `M` maintainer, `S` steward
- **Review:** every minor release and quarterly
- **Enforced by:** `tools/check-evidence.mjs` in CI

## Dual-mode gates

The strategic shift is tracked by the five constitutional gates (Article 4.1).
No gate may be reported as passed until its row is **Met** in
`REQUIREMENTS.md`.

| Gate | Name | Status | What remains |
| --- | --- | --- | --- |
| D1 | Dual Open | **Passed** | — |
| D2 | Handoff | **Passed** | — |
| D3 | Offline | Partial | App-level offline journey still bounded by OS-UX-003 |
| D4 | Disclosure | Partial | Acknowledgement UI before first remote use |
| D5 | Sovereignty | Partial | Verifiable remote deletion acknowledgement |

## Blocking the MLP (P1 / P2)

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-AI-003 | No diff → approve → apply → undo flow | Ask/Explain/Fix surface is not exposed | M | MLP |
| OS-AI-007 | AI path is not yet routed through the egress policy | `EgressPolicy` exists; the surface that would call it does not | M | MLP |
| OS-AI-009 | Permission checks are not yet in tool handlers | Handlers ship with the surface | M | MLP |
| OS-AI-002 | No physical-device offline inference run | Requires an arm64 device; QVAC does not run on emulators | M | MLP |
| OS-AI-005 | Model hash not verified on device | Same run | M | MLP |
| OS-AI-006 | Download disclosure UI unfinished | Same surface | M | MLP |
| OS-AI-004 | Streaming and cancellation have no user-visible UI | Engine only | M | MLP |
| OS-UX-001 | Five-minute onboarding not measured | Needs the five-user protocol | M | MLP |
| OS-UX-002 | No cold-start budget or baseline device | Harness not built | M | MLP + 2 weeks |
| OS-EXEC-004 | No published memory/battery budget | Harness not built | M | MLP + 2 weeks |
| OS-EXEC-005 | Resource-pressure behaviour is unmeasured | Process-death restore exists; no fault injection | M | MLP + 4 weeks |
| OS-DATA-005 | No fault-injection proof (kill mid-write) | Test suite not written | M | MLP + 4 weeks |
| OS-DATA-004 | Verification result is not surfaced when a project opens | Core verifies; the app does not show a failure | M | P3 |
| OS-DATA-006 | Backup/restore is manual only | Scheduled backup not designed | M | ALP |

## Trust and supply chain

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-CORE-001 | Identity is not bound to the platform keystore and is not created in the app UI | Core implementation exists; Android keystore wrapper pending | M | P3 |
| OS-CORE-003 | Permissions are not one enumerable, revocable surface | Capability permissions and Android permissions are separate today | M | P3 |
| OS-CORE-010 | Per-invocation authorisation is not wired into the app | `:core` verifies capabilities; the app does not yet call an authoriser | M | P3 |
| OS-CORE-020 | Capability install flow is not shipped | Manifest and schema exist; no installer UI | M | P3 |
| OS-CORE-021 | Capability compatibility is not tested across versions | Protocol compatibility is tested; capability schema evolution is not | M | P3 |
| OS-CORE-030 | Not every app action is routed through the core trust boundary | Integration is incremental | M | P3 |
| OS-CORE-011 | No general action log | Needed before agents get write access | M | MLP |
| OS-CAP-011 | Revocation is an in-memory set, not a signed, expiring record | Revocation distribution not designed | M | P3 |
| OS-TRUST-006 | No signed revocation record with an expiry | Same design gap | M | P3 |
| OS-TRUST-007 | No bit-for-bit rebuild proof | Reproducibility work; CI builds from tags today | M | ALP |
| OS-TRUST-008 | No SBOM | CycloneDX generation planned | M | ALP |
| OS-EXEC-002 | Isolation class is not shown in the app | It travels in the protocol, but no first-run screen | M | ALP |
| OS-EXEC-003 | Untrusted code containment is policy, not enforcement | No WASM/container provider in-app yet | M | ALP |
| OS-REL-002 | No measured device matrix | Device lab not established | S | ALP |

## Dual-mode shift (v0.2.0)

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-WS-005 | No conflict-explicit sync | ADR-0006 sequences handoff first; sync is P5 | M | P5 |
| OS-DUAL-003 | Offline gate is partial at app level | Core and loopback are offline; apt and agents are not | M | P4 |
| OS-DUAL-004 | No acknowledgement UI before the first remote host | Protocol carries disclosure; the consent screen is not built | M | P4 |
| OS-DUAL-005 | Remote deletion has no verifiable acknowledgement | `revoke` releases the lease; deletion receipts are not implemented | M | P5 |
| OS-CAP-001 | No capability install/disable/remove UI | P3 | M | P3 |
| OS-CAP-002 | Revocation is not end-to-end across hosts | Same as OS-CAP-011 | M | P5 |
| OS-CAP-003 | No second platform runs the core yet | The reference host is Node and shares the contract, not the Kotlin core | M | P4 |
| OS-CAP-004 | No cross-platform workspace round-trip demo | Needs a desktop or hub client | M | P4 |
| OS-FED-001 | No host discovery or per-capability picker | P5 | M | P5 |
| OS-FED-002 | No conflict-explicit sync | Same as OS-WS-005 | M | P5 |
| OS-FED-003 | No verifiable remote deletion | Same as OS-DUAL-005 | M | P5 |

## Workspace and accessibility

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-UX-003 | Offline is partial (apt, agents need network) | Inherent to the tools; the editor works offline | M | accepted |
| OS-UX-004 | No TalkBack or contrast audit | Not yet run | M | MLP + 4 weeks |
| OS-UX-005 | Error copy not audited | Copy pass pending | M | ALP |
| OS-UX-006 | 48 dp targets not systematically checked | Audit pending | M | ALP |
| OS-DATA-003 | No audit of destructive actions | Action log pending | M | ALP |
| OS-DATA-010 | Distro migration has no rollback proof | Test exists; rollback not exercised | M | ALP |
| OS-PRIV-002 | No visible egress log | Ledger exists; no UI | M | P3 |

## Accepted risks (not planned to change)

| Item | Statement |
| --- | --- |
| PRoot isolation | PRoot is a compatibility layer, not a sandbox. Guest code has the app's UID. Disclosed in `THREAT-MODEL.md`. |
| `targetSdk 28` | Pinned for executable extraction. See ADR-0002; reviewed annually. It is a provider decision under the Constitution, not an exception to it. |
| Third-party agents | Agents the user installs are trusted software. The app does not sandbox them. |
| Device compromise | Rooted or compromised devices are out of scope. |
| Reference host auth | `hosts/hub/` uses a bearer token and binds to loopback by default. It is for a trusted network or the user's own TLS terminator. |

## How to close an entry

1. Add the evidence to `REQUIREMENTS.md` and set the row to **Met**.
2. Link the release or commit that carries the evidence.
3. Remove the row here in the same change.
4. Run `node tools/check-evidence.mjs`; CI runs the same check.
