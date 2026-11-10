# Strategy and governance documents

These documents turn **The Origin Studio Constitution** into things that can be
checked. They are normative for this repository, but a document that claims more
than the code does is a bug: the status column is part of the record.

**The supreme text is [`CONSTITUTION.md`](../../CONSTITUTION.md) v0.2.0.** If any
document here contradicts it, the Constitution wins and the document is fixed.

| Document | Purpose |
| --- | --- |
| [GOVERNANCE.md](GOVERNANCE.md) | Who decides, how the Constitution changes, how releases are authorised |
| [THREAT-MODEL.md](THREAT-MODEL.md) | Assets, adversaries, trust boundaries, and what is explicitly out of scope |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Every constitutional MUST as a numbered, verifiable requirement with evidence |
| [DEVIATIONS.md](DEVIATIONS.md) | Every requirement not yet met, with owner, reason and expiry |
| [DUAL-MODE.md](DUAL-MODE.md) | Local + remote execution: the five gates, host disclosure and how to run the reference host |
| [PERSONAS.md](PERSONAS.md) | Who the first users are, the job they hire the app for, and how the five-user gate is run |
| [../DISTRIBUTION.md](../DISTRIBUTION.md) | Channels, verification, rollback and the rules a mirror must follow |
| [../../contracts/README.md](../../contracts/README.md) | The versioned wire schemas and cross-language fixtures |
| [../../hosts/hub/README.md](../../hosts/hub/README.md) | The dependency-free reference host |
| [../adr/0001-qvac-local-ai.md](../adr/0001-qvac-local-ai.md) | Why QVAC is the reference local-AI engine |
| [../adr/0002-execution-model.md](../adr/0002-execution-model.md) | Why `targetSdk` is pinned to 28, and what replaces it |
| [../adr/0003-constitutional-supremacy.md](../adr/0003-constitutional-supremacy.md) | Why the Constitution wins and the dual-mode shift is binding |
| [../adr/0004-host-protocol.md](../adr/0004-host-protocol.md) | One Host Protocol for local and remote execution |
| [../adr/0005-workspace-continuity.md](../adr/0005-workspace-continuity.md) | Manifest, hash-chained history and the single-writer lease |
| [../adr/0006-sync-model.md](../adr/0006-sync-model.md) | Handoff first, conflict-explicit sync second, CRDT later |
| [../adr/0007-capability-trust.md](../adr/0007-capability-trust.md) | Hash, signature and revocation before execution |
| [../adr/0008-network-as-capability.md](../adr/0008-network-as-capability.md) | Deny-by-default egress and data-flow classes |
| [../adr/0009-reference-host.md](../adr/0009-reference-host.md) | Bring-your-own host first, and why the reference host is Node |

## The rule that keeps this honest

A requirement is **Met** only when its evidence is linked in `REQUIREMENTS.md` and
that evidence is part of the tagged source or the published release. Anything else
is **Partial** or **Not met**, and appears in `DEVIATIONS.md`.

`node tools/check-evidence.mjs` enforces this in CI: every non-Met row must have a
deviation, every deviation must name a real requirement, and evidence paths must
exist.

## Profiles

Conformance is claimed per profile, never in general.

| Profile | Meaning | Claimed by |
| --- | --- | --- |
| **P0 Core** | Trust, authorisation, storage, recovery, signed release | every public build |
| **P1 Workspace** | Create, edit, run, preview, export on the declared device tier | the MLP |
| **P2 Intelligence** | Local AI with reversible actions, offline | the MLP |
| **P3 Ecosystem** | Installable, signed, revocable capabilities | the ALP |
| **P4 Universal** | A second platform and a remote provider behind the same contracts | later |
| **P5 Federation** | Multiple hosts, conflict-explicit sync, user-chosen residency, hosted-service continuity | later |

## Current claim

The tree after v0.0.3 claims **P0 (partial)** and **P1 (partial)**. P2 is
**not claimed**: the engine ships and the availability gate works, but the
Ask / Explain / Fix surface is not exposed and inference has not run on a
physical arm64 device.

The dual-mode shift has two gates passed — **D1 Dual Open** and **D2 Handoff** —
and three partially met: **D3 Offline**, **D4 Disclosure** and **D5 Sovereignty**.
See `DEVIATIONS.md`.
