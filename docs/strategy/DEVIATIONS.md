# Deviations register

Every requirement in `REQUIREMENTS.md` that is not **Met** appears here with an
owner, a reason, and an expiry. An expired entry means the requirement is either
met or the constitution is amended — never that the entry disappears.

- **Register version:** 1 (v0.0.3)
- **Owner column:** `M` maintainer, `S` steward
- **Review:** every minor release and quarterly

## Blocking the MLP (P1 / P2)

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-AI-003 | No diff → approve → apply → undo flow | Ask/Explain/Fix surface is not exposed | M | MLP |
| OS-AI-007 | No egress policy for the AI path | Depends on the agent contract, not yet built | M | MLP |
| OS-AI-009 | Permission checks are not yet in tool handlers | Handlers ship with the surface | M | MLP |
| OS-AI-002 | No physical-device offline inference run | Requires an arm64 device; QVAC does not run on emulators | M | MLP |
| OS-AI-005 | Model hash not verified on device | Same run | M | MLP |
| OS-AI-006 | Download disclosure UI unfinished | Same surface | M | MLP |
| OS-UX-001 | Five-minute onboarding not measured | Needs the five-user protocol | M | MLP |
| OS-UX-002 | No cold-start budget or baseline device | Harness not built | M | MLP + 2 weeks |
| OS-EXEC-004 | No published memory/battery budget | Harness not built | M | MLP + 2 weeks |
| OS-DATA-005 | No fault-injection proof (kill mid-write) | Test suite not written | M | MLP + 4 weeks |
| OS-DATA-004 | No workspace integrity check on open | Needs manifest + checkpoint format | M | P3 |
| OS-DATA-006 | Backup/restore is manual only | Scheduled backup not designed | M | ALP |

## Trust and supply chain

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-CORE-001 | No local identity object or keystore key | No feature needs it yet; adding it wrongly is worse than waiting | M | P3 |
| OS-CORE-010 | No capability authorisation layer | P3 | M | P3 |
| OS-CORE-020/021 | No capability manifests or contracts | P3 | M | P3 |
| OS-CORE-030 | No enforcement that extensions cannot bypass the core | P3 | M | P3 |
| OS-CORE-011 | No general action log | Needed before agents get write access | M | MLP |
| OS-TRUST-006 | No revocation format | No installable capabilities yet; API updates use Android's own rules | M | P3 |
| OS-TRUST-007 | No bit-for-bit rebuild proof | Reproducibility work; CI builds from tags today | M | ALP |
| OS-TRUST-008 | No SBOM | CycloneDX generation planned | M | ALP |
| OS-EXEC-001 | No provider abstraction | PRoot is the only provider; abstraction without a second provider is speculative | M | P4 |
| OS-EXEC-002 | Isolation class not shown in the app | Documented only | M | ALP |
| OS-EXEC-003 | No enforcement that untrusted code stays out of PRoot | Policy only | M | ALP |
| OS-REL-002 | No measured device matrix | Device lab not established | S | ALP |

## Workspace and accessibility

| ID | Gap | Why it is open | Owner | Expiry |
| --- | --- | --- | --- | --- |
| OS-UX-003 | Offline is partial (apt, agents need network) | Inherent to the tools; the editor works offline | M | accepted |
| OS-UX-004 | No TalkBack or contrast audit | Not yet run | M | MLP + 4 weeks |
| OS-UX-005 | Error copy not audited | Copy pass pending | M | ALP |
| OS-UX-006 | 48 dp targets not systematically checked | Audit pending | M | ALP |
| OS-DATA-003 | No audit of destructive actions | Action log pending | M | ALP |
| OS-DATA-010 | Distro migration has no rollback proof | Test exists; rollback not exercised | M | ALP |
| OS-PRIV-002 | No visible egress log | Depends on the capability layer | M | P3 |
| OS-DOC-002 | Only two ADRs | Grows with decisions | M | ongoing |

## Accepted risks (not planned to change)

| Item | Statement |
| --- | --- |
| PRoot isolation | PRoot is a compatibility layer, not a sandbox. Guest code has the app's UID. Disclosed in `THREAT-MODEL.md`. |
| `targetSdk 28` | Pinned for executable extraction. See ADR-0002; reviewed annually. |
| Third-party agents | Agents the user installs are trusted software. The app does not sandbox them. |
| Device compromise | Rooted or compromised devices are out of scope. |

## How to close an entry

1. Add the evidence to `REQUIREMENTS.md` and set the row to **Met**.
2. Link the release or commit that carries the evidence.
3. Remove the row here in the same change.
