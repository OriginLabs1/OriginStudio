# Governance

- **Status:** adopted with this document, subordinate to `CONSTITUTION.md` v0.2.0
- **Applies to:** the OriginStudio repository, its releases, its reference hosts,
  and the constitution that governs them
- **Owner:** OneOrigin, operating the `OriginLabs1` organisation

## 0. Supremacy

This document describes **how** decisions are made. It does not create authority.
Authority comes from [`CONSTITUTION.md`](../../CONSTITUTION.md), which is the
supreme governing text.

The hierarchy is:

1. The Constitution.
2. `REQUIREMENTS.md` — the Constitution as numbered, verifiable obligations.
3. Accepted ADRs — how an obligation is met.
4. This document and the rest of `docs/strategy/`.
5. Everything else, including code comments and release notes.

If this document, an ADR, a build setting or a shipped feature contradicts the
Constitution, the contradiction is a defect to be fixed. The Constitution is not
edited to match the code.

## 1. Roles

| Role | Who | Authority |
| --- | --- | --- |
| Steward | OneOrigin | Final decision on scope, brand, licences and constitutional amendments; holds the release key |
| Maintainer | named in `MAINTAINERS` (currently the Steward) | Merges code, cuts releases, signs artifacts, triages security reports |
| Contributor | anyone | Proposes changes through pull requests and RFCs; no merge or release rights |

Bus factor is one. That is a risk, not a policy, and it is recorded in the risk
register. The Steward must name a successor maintainer before any release that
advertises long-term support.

## 2. How decisions are made

1. **Ordinary change** — a pull request. Two things are required: the change
   builds, and any requirement it touches still has honest evidence.
2. **Architectural decision** — an ADR in `docs/adr/`, merged before the code. An
   ADR may choose among permitted options; it may not widen a constitutional
   promise.
3. **Product scope change** — an edit to `MLP-ALP-SCOPE.md` plus an entry in the
   decision log below. Moving an item between MLP, ALP and Later is a decision,
   not an accident. It may not defer a dual-mode gate (D1–D5).
4. **Constitutional change** — an amendment (section 4).

Silence is not consent for 3 and 4. For 1 and 2, maintainer review is enough.
Every merged change is checked against the Schedule D supremacy checklist in the
Constitution.

## 3. Releases

Only the Steward or a named maintainer may tag a release. A release is valid only
when all of the following are true:

- it is built by the tag-driven workflow from the tagged commit;
- the APK is signed by the OriginLabs1 release certificate and verified by
  `apksigner` before publication;
- `SHA256SUMS`, `signing-certificate.txt` and `release-manifest.json` are
  attached;
- the notes state the supported device tiers and known limitations;
- the claimed profile in `README.md`, the website and the release notes matches
  `REQUIREMENTS.md`;
- every requirement claimed in `REQUIREMENTS.md` has evidence in that release;
- `:core` tests, the reference-host tests, the contract fixtures and the evidence
  check pass in CI.

Published artifacts are immutable. A wrong release is superseded by a higher
patch version; it is never re-tagged, re-signed or silently replaced.

## 4. Amending the constitution

Changes are classified before they are debated:

| Class | Effect | Requires |
| --- | --- | --- |
| **Clarification** | Wording only; no new obligation, no changed obligation | Maintainer merge |
| **Extension** | Adds an obligation | ADR, owner, evidence plan, date |
| **Amendment** | Changes or removes an obligation | ADR, compatibility note, migration path, new constitution version |
| **Entrenchment change** | Alters Article 3 | Not permitted under the Origin Studio name; requires a new product identity and a public fork notice |

Rules:

- The constitution is versioned independently of the app. The app may be `0.0.x`
  while the constitution is `0.2`.
- A new obligation does not take effect until `REQUIREMENTS.md` has a row for it
  and `DEVIATIONS.md` records the gap, if any.
- An amendment that would invalidate user data requires a migration plan and a
  grace release.
- The text lives in the repository. A constitutional change that exists only in a
  chat, a slide or a commit message is not a change.
- The current text is `CONSTITUTION.md` v0.2.0. It is supreme over this
  document.

## 5. The deviations rule

No requirement may be silently unmet. `DEVIATIONS.md` lists every gap with an
owner and an expiry. Expiry means the requirement is either met or the
constitution is formally amended — never that the entry quietly disappears.

Review cadence: at every minor release, and at least once a quarter.

## 6. Security and incident authority

The maintainer may ship an out-of-band patch release without the normal scope
review to fix an actively exploited issue. The release must still be signed,
still carry checksums, and must be followed by a written advisory. Details in
`docs/SECURITY.md`.

## 7. Keys, accounts and succession

| Asset | Holder | Recovery |
| --- | --- | --- |
| Release signing key | Steward, offline | Sealed backup held separately; documented in `RELEASING.md` |
| GitHub organisation `OriginLabs1` | Steward | Second owner account, 2FA, recovery codes stored offline |
| Domain and site host | Steward | Registrar transfer lock; renewal recorded |
| Canonical release archive | GitHub Releases | Mirrors may exist but must link to canonical checksums |

Key rotation: a new signing key is a **new application identity** on Android and
breaks in-place updates. Rotation must therefore be announced in advance, ship a
transition build signed by the old key, and keep the old key available for
verification. Losing the key without a backup ends the update path permanently.

## 8. Licences

- Application and build scripts: Apache License 2.0 (`LICENSE`).
- Documentation in `docs/`: same licence unless a file says otherwise.
- Bundled third-party components: recorded in `THIRD_PARTY_NOTICES.md`.
- Models are not distributed with the app and carry their own licences; the app
  must disclose the licence and hash before a download.

A licence change is a constitutional amendment.

## 9. Decision log

| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-11-07 | QVAC is the reference local-AI engine | ADR-0001 |
| 2026-11-07 | `targetSdk 28` is pinned | ADR-0002; required for PRoot execution. Now a provider decision under the Constitution |
| 2026-11-10 | Brand moves from orange to the portal steel palette | v0.0.3; the orange was not the intended identity |
| 2026-11-10 | P3 and P4 are not claimed before P1 and P2 are complete | One lovable journey, finished |
| 2026-11-10 | `CONSTITUTION.md` v0.2.0 is supreme over the repository | ADR-0003; the existing project is subordinate and gaps are recorded |
| 2026-11-10 | Local + remote is a constitutional obligation, tracked by gates D1–D5 | ADR-0003; the shift may not be deferred without an amendment |
| 2026-11-11 | Remove the bundled QVAC runtime; ship the universal companion, CLI and SBOM | ADR-0010; weight without a surface, and a lighter universal release |
| 2026-11-10 | One Host Protocol (`origin.host/v1`) spans every provider | ADR-0004; local and remote share one contract |
| 2026-11-10 | Portable workspace manifest, hash-chained history and a single-writer lease | ADR-0005; makes ownership and integrity verifiable |
| 2026-11-10 | Handoff first, conflict-explicit sync second, CRDT only later | ADR-0006; no silent merge |
| 2026-11-10 | Capability trust = schema + artifact hash + signature + revocation | ADR-0007; signature alone is insufficient |
| 2026-11-10 | Network is a capability with deny-by-default egress and a visible ledger | ADR-0008; no ambient network right |
| 2026-11-10 | Bring-your-own host first; `hosts/hub/` is the reference implementation | ADR-0009; remote without custody |
