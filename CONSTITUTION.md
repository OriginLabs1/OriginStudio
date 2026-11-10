# The Origin Studio Constitution

**Version:** 0.2.0
**Ratified:** 2026-11-10
**Status:** Supreme governing document of the OriginStudio repository
**Owner:** OneOrigin, operating the `OriginLabs1` organisation
**Supersedes:** Constitution v0.0.1 and every practice that contradicts this text

---

## 0. Supremacy

This Constitution is the highest authority in Origin Studio.

It governs the repository, the build, the released application, the reference
hosts, the contracts, the documentation, and every future product that claims the
Origin name. Where this text and any other document disagree, **this text wins**.

The order of authority is:

1. This Constitution.
2. The [requirements register](docs/strategy/REQUIREMENTS.md), which turns this
   text into numbered, verifiable obligations.
3. Accepted ADRs in `docs/adr/`, which record *how* an obligation is met.
4. `docs/strategy/`, `docs/`, `README.md` and `MLP-ALP-SCOPE.md`.
5. `BLUEPRINT.md`, `BRAND.md`, roadmaps, issues, chat messages and slide decks.
6. Code comments, commit messages and release notes.

A lower layer may narrow a choice. It may never widen a promise. A document, an
ADR, a release or a line of code that contradicts this Constitution is a defect,
and the defect is fixed — not the Constitution.

Three consequences follow immediately, and they are not negotiable:

- **The existing project is subordinate.** The application, its governance,
  its requirements, its ADRs and its shipped behaviour are re-read through this
  text. Where they fall short, `DEVIATIONS.md` records the gap and the gap has an
  owner and an expiry.
- **The strategic shift is binding.** Local and remote execution are both
  first-class, behind one versioned protocol. This is now a constitutional
  requirement, not a roadmap preference.
- **Claims require evidence.** No profile, capability or guarantee may be
  advertised unless the evidence is in the tagged source or a published release.

## 1. Preamble

Origin Studio exists so that a person can own their development environment:
their files, their tools, their identity and their work — on a device they hold,
with or without a network, with or without a vendor.

The product began as an Android workspace. It must not end as an Android silo.
The same workspace, the same contracts and the same guarantees must extend to a
laptop, a home server, a hub, or a rented machine — because the user chose to
extend them, not because the product required it.

We optimise for a person who wants to keep working: on a train, behind a
firewall, on a tablet, on a server they rent, and ten years from now on hardware
that does not exist yet. Every article below is a promise to that person.

## 2. Definitions

| Term | Meaning |
| --- | --- |
| **Workspace** | A directory of plain files plus an `.origin/` metadata directory. It is the unit of ownership, export and continuity. |
| **Local host** | Execution on the device the user is physically holding. Authoritative and available offline. |
| **Remote host** | Execution on a hub, desktop, managed cloud or third-party machine. Never implicit; always disclosed. |
| **Execution provider** | An implementation that runs workloads: the Android app sandbox, PRoot, a container, a microVM, a remote host. |
| **Capability** | A bounded, signed, versioned extension with declared permissions and a declared data-flow class. |
| **Data-flow class** | The highest sensitivity of data a path may carry. Defined in Schedule A. |
| **Egress** | Any movement of bytes off the local host, including metadata. |
| **Host Protocol** | The versioned wire contract in `contracts/` that every host and client speaks. |
| **Lease** | A single-writer token over a workspace. Handoff transfers the lease; it does not migrate a live process. |
| **Disclosure** | A user-visible statement of operator, region, isolation, retention, availability and price before a remote host is used. |
| **Profile** | A conformance level (P0–P5) that may be claimed only with evidence. |
| **Steward** | OneOrigin, holding final authority under this Constitution. |

## 3. Entrenched commitments

The following commitments are **entrenched**. They may be clarified, but an
amendment may not remove or weaken them. A release that breaks one is not an
Origin Studio release.

1. **The user owns the work.** A workspace is plain files. It opens without this
   application and outlives this application.
2. **Local is authoritative.** The app works offline after first setup. A
   network is an addition, never a precondition for reading, editing, running
   local tools or exporting.
3. **No account is required.** No registration, no cloud identity, no vendor
   service is needed to install, open, edit, run or export.
4. **No silent egress.** Every byte that leaves the local host is authorised by a
   scoped grant, attributable to a capability and a purpose, and recorded.
5. **No silent delegation.** The user is told when work runs somewhere other than
   the device, and by whom that host is operated.
6. **No unverifiable artifact.** Releases, capabilities and models are
   hash-verified; signatures are checked before execution.
7. **No destructive surprise.** Deletion, overwrite, migration and revocation are
   explicit, reversible where possible, and never a side effect of an update.
8. **Exit is always available.** Export is a first-class feature at every profile.
9. **Claims match evidence.** A guarantee without a test, inspection, demo, bench
   or audit is not a guarantee; it is marketing and is forbidden.
10. **The Constitution wins.** No feature, deadline, partner or platform policy
    overrides these commitments. If a platform makes one impossible, the response
    is a disclosed, versioned deviation — or a different product name.

## 4. Profiles and conformance

Conformance is claimed per profile, never in general. A build claims the highest
profile for which every requirement is **Met** with linked evidence.

| Profile | Name | What it promises |
| --- | --- | --- |
| **P0** | Core | Local identity, deny-by-default authorisation, portable workspace, integrity verification, signed releases, honest deviations |
| **P1** | Workspace | Create, edit, run, preview and export on the declared device tier, offline after setup |
| **P2** | Intelligence | Local inference with reversible actions, no mandatory vendor, visible egress |
| **P3** | Ecosystem | Installable signed capabilities, revocation, versioned contracts |
| **P4** | Universal | The same core and contracts on a second platform, and a remote provider behind the same Host Protocol |
| **P5** | Federation | Multiple hosts, explicit sync and handoff, user-chosen residency, hosted-service continuity |

Rules:

- P3, P4 and P5 may not be claimed before the profiles beneath them are complete.
- A partial profile is written as `partial`, never as the bare profile name.
- The website, the release notes and the README must state the claimed profile
  and link to `REQUIREMENTS.md`.

### 4.1 The dual-mode gates

The shift to local + remote is complete only when all five gates pass. Each gate
is a requirement with evidence, not a statement of intent.

| Gate | Name | Pass condition |
| --- | --- | --- |
| **D1** | Dual Open | The same workspace opens on the device and on a reference remote host, with matching manifest hash and event count. |
| **D2** | Handoff | A workspace lease transfers device → host and host → device without losing an event or corrupting history. |
| **D3** | Offline | After setup, every local read, edit, run and export works with the network disabled. |
| **D4** | Disclosure | No remote host is used until operator, region, isolation, retention, availability and price are shown and acknowledged. |
| **D5** | Sovereignty | The user can revoke remote access, delete remote copies and export everything without the vendor's permission. |

## 5. Local guarantee and dual mode

**Article 5.1 — Offline baseline.** After initial setup, the following must work
with no network: open a workspace, read and edit files, browse the file tree, run
the bundled local tools, verify workspace integrity, export the workspace, and
open the reference host locally.

**Article 5.2 — Network is additive.** Any feature that needs the network must
degrade to an honest, specific message and leave the workspace unchanged. It must
not block launch, editing or export.

**Article 5.3 — Remote is opt-in.** Remote execution is selected by the user, per
capability or per session. A default that silently prefers remote is forbidden.

**Article 5.4 — One contract.** Local and remote execution use the same Host
Protocol, the same envelope, the same capability names and the same semantics. A
provider that cannot speak the protocol is not a supported host.

**Article 5.5 — Parity of refusal.** A remote host may refuse more than a local
host; it may never be granted a permission the local host was denied.

**Article 5.6 — Same workspace.** Moving execution must not fork the workspace
into an incompatible format. The manifest schema is versioned and forward
migration is explicit.

## 6. Hosts, delegation and disclosure

**Article 6.1 — Host classes.** Every host declares a location: `device`, `hub`,
`managed` or `third-party`. The class is part of the protocol, not a UI label.

**Article 6.2 — Mandatory disclosure.** Before first use, a host must present:
host id, operator, region, location class, isolation class, retention policy,
availability claim, price, protocol version, supported capabilities and maximum
data-flow class. Missing fields fail closed.

**Article 6.3 — Isolation honesty.** A compatibility layer is never described as
a sandbox. Each isolation class carries its real guarantee, and untrusted code
may not run in a class that does not contain it.

**Article 6.4 — Bring your own host first.** The supported path for remote
execution begins with a host the user operates or rents directly. A managed
Origin cloud is a later, separately governed offering.

**Article 6.5 — Revocation.** The user can revoke a host's access at any time,
from the device, without contacting the operator. Revocation takes effect for new
work immediately and invalidates the host's lease.

**Article 6.6 — Health and failure.** A host that fails negotiation, health or
integrity checks is not used. Failure is surfaced; it is never silently retried
against a different host.

## 7. Workspace continuity, ownership and sync

**Article 7.1 — Plain files.** A workspace is a normal directory. The `.origin/`
metadata is additive and small; removing it loses history, not content.

**Article 7.2 — Verifiable history.** Mutating workspace events are recorded in
an append-only, hash-chained log. Opening a workspace verifies the chain. A
broken chain is reported, never silently repaired.

**Article 7.3 — Single writer.** A workspace has at most one active writer at a
time, expressed as a lease with an epoch. A second writer must wait for expiry or
receive an explicit handoff.

**Article 7.4 — Handoff, not migration.** Handoff transfers authority. It does
not pretend to migrate a running process. The UI must say which host is the
active writer.

**Article 7.5 — Conflicts are explicit.** Until a conflict-free replicated model
is proven, divergent edits produce a visible conflict artifact and a choice. The
system must never pick a winner silently.

**Article 7.6 — Export is complete.** Export includes all user files and the
metadata needed to reconstruct history, excludes transient leases, and is
readable with standard tools.

**Article 7.7 — Deletion is deliberate.** Deleting a workspace, a checkpoint or a
remote copy requires explicit confirmation and is recorded. Automatic garbage
collection may not remove user content.

## 8. Identity, pairing and trust

**Article 8.1 — Local first identity.** An identity is created on the device,
without network or account, and is held in app-private storage. Platform keystore
binding is required where available; its absence is a recorded deviation.

**Article 8.2 — Portable public identity.** The public key is exportable and
stable. The private key leaves the device only by explicit user export.

**Article 8.3 — Pairing is mutual.** Connecting a device to a host requires
consent on both sides. A host may not enrol a device silently.

**Article 8.4 — Signatures are checked at the boundary.** Host messages, capability
manifests and identities are verified before they are trusted. An unverifiable
peer is refused.

**Article 8.5 — Revocation is offline-verifiable.** A revocation record carries a
version, an issuance time and an expiry, and can be checked without contacting
the issuer.

## 9. Capabilities, supply chain and revocation

**Article 9.1 — Manifest before execution.** A capability carries a signed,
versioned manifest: id, version, publisher, permissions, data-flow class,
isolation class and artifact hash.

**Article 9.2 — Hash and signature both.** A valid signature over the wrong
artifact is still wrong. Both the artifact hash and the publisher signature must
verify.

**Article 9.3 — Revocation precedes execution.** A revoked capability does not
run, even if its signature is valid.

**Article 9.4 — Permissions are enforced outside the capability.** The host
authorises each invocation. A capability cannot grant itself permission.

**Article 9.5 — Least authority.** A capability receives the narrowest permission
set that satisfies its declared function. Wildcards require an ADR.

**Article 9.6 — Removal is complete.** Disabling or removing a capability removes
its stored credentials, its scheduled work and its access grants.

## 10. Network as a capability

**Article 10.1 — No ambient network.** There is no general "internet" permission.
Network use is a capability with a scope, a purpose and a maximum data-flow class.

**Article 10.2 — Deny by default.** Absent a matching grant, an egress request is
denied. A denial is a recorded event.

**Article 10.3 — Purpose is mandatory.** A request without a capability, host and
human-readable purpose is invalid.

**Article 10.4 — Data-flow ceiling.** A grant caps the sensitivity it may carry.
Content above the ceiling is refused, not truncated.

**Article 10.5 — Visible ledger.** The user can inspect the egress ledger,
including denials, and verify that it has not been edited.

## 11. Remote data, residency and deletion

**Article 11.1 — Data minimisation.** A remote host receives the bytes required
for the declared task, not the workspace by default.

**Article 11.2 — Residency is stated.** The region is disclosed before use and
recorded with the session.

**Article 11.3 — Retention is a contract.** A host's retention statement is
binding. "Unknown" retention is a failed disclosure.

**Article 11.4 — Deletion is verifiable.** The user can request deletion of remote
copies and receive a verifiable acknowledgement.

**Article 11.5 — No secondary use.** Content sent for a task may not be used to
train, profile or advertise without a separate, explicit, revocable grant.

**Article 11.6 — Cross-border honesty.** If data leaves the user's stated region,
the user is told before it happens.

## 12. AI governance

**Article 12.1 — No mandatory model.** No vendor, account or model is required for
the workspace to function.

**Article 12.2 — Local inference is the default.** When a local model is
available, it is preferred. Remote inference requires a grant.

**Article 12.3 — Output is proposed, not applied.** Model output that changes
files is shown as a diff and requires approval. Consequential actions require a
typed confirmation.

**Article 12.4 — Reversibility.** Every applied AI change has an undo path
recorded in the workspace history.

**Article 12.5 — Prompt injection is a first-class threat.** Tool handlers enforce
permissions regardless of what the model says. Project content is data, not
instructions.

**Article 12.6 — Provenance is visible.** Model source, licence, size and hash are
shown before download; deletion is available.

**Article 12.7 — Honest unavailability.** Unsupported hardware gets a specific
message, not a degraded pretend-success.

## 13. Project continuity and exit

**Article 13.1 — The project survives the product.** A workspace exported today
must remain openable by a future Origin Studio, by the reference host, and by
ordinary tools.

**Article 13.2 — No proprietary database.** Metadata may be JSON; it may not
require a proprietary engine to read.

**Article 13.3 — Migration is reversible.** Format and distribution migrations
ship with a rollback path and a grace release.

**Article 13.4 — Exit without permission.** Export, backup and deletion work
offline and do not require an account, a server or a support ticket.

**Article 13.5 — Documentation is part of the artifact.** A release without
accurate build, verification and recovery instructions is incomplete.

## 14. Sustainability and substitution

**Article 14.1 — Replaceable implementations.** Every major component — model
runtime, terminal, editor, agent, host — must have a documented substitution
point. The product must not depend on a component that cannot be replaced.

**Article 14.2 — No single point of commercial failure.** A vendor change must
degrade a feature, not end the product.

**Article 14.3 — Cost honesty.** Anything that costs money is disclosed before it
is incurred. Free tiers are not promises.

**Article 14.4 — Maintainer continuity.** The Steward names a successor
maintainer before any release that advertises long-term support. Bus factor is
recorded as a risk, not hidden.

**Article 14.5 — Published provenance.** Third-party components, licences and
hashes are recorded. A machine-readable SBOM ships with each release.

## 15. Hosted service continuity

**Article 15.1 — The app is not the service.** A hosted Origin service, if one
exists, is optional. Its shutdown may not disable the app, the workspace or
export.

**Article 15.2 — Notice before shutdown.** A hosted service announces shutdown at
least one release cycle ahead and provides a complete export path.

**Article 15.3 — Keys are held by the user.** A hosted service does not hold the
only copy of a user's identity or workspace.

**Article 15.4 — No forced migration.** A hosted-service change may not silently
move a user's data to a different operator or region.

## 16. Evidence, requirements, deviations and enforcement

**Article 16.1 — Every MUST has an ID.** A constitutional obligation is expressed
as a row in `REQUIREMENTS.md` with a verification method and a status.

**Article 16.2 — Statuses are honest.** *Met* requires a linked test, inspection,
demo, bench or audit in the tagged source or published release. Everything else
is *Partial* or *Not met*.

**Article 16.3 — Every gap has an owner and an expiry.** `DEVIATIONS.md` records
it. Expiry means the gap is closed or the Constitution is amended — never that
the row disappears.

**Article 16.4 — CI is an enforcement mechanism.** The build refuses a change that
breaks the core contract tests, the schema fixtures, the reference host tests or
the evidence links.

**Article 16.5 — Security overrides are narrow.** An out-of-band security release
may skip scope review, but not signing, checksums, the advisory or this Article.

## 17. Amendment

| Class | Effect | Requires |
| --- | --- | --- |
| **Clarification** | Wording only; no obligation changes | Maintainer merge |
| **Extension** | Adds an obligation | ADR, owner, evidence plan, date |
| **Amendment** | Changes or removes an obligation | ADR, compatibility note, migration path, new version |
| **Entrenchment change** | Alters Article 3 | Not permitted under this name; requires a new product identity and a public fork notice |

Rules:

- The Constitution is versioned independently of the app.
- A new obligation does not take effect until `REQUIREMENTS.md` has a row and
  `DEVIATIONS.md` records the gap.
- An amendment that would invalidate user data requires a migration plan and a
  grace release.
- The text lives in the repository. A change that exists only in a chat, a slide
  or a commit message is not a change.
- Amendments are recorded in `GOVERNANCE.md` and signed off by the Steward.

---

## Schedule A — Data-flow classes

| Level | Name | Meaning | Example |
| --- | --- | --- | --- |
| 0 | `NONE` | No egress | Local edit |
| 1 | `METADATA` | Metadata only | Update check, capability index |
| 2 | `ENCRYPTED_CONTENT` | Encrypted workspace content | Encrypted backup to a hub |
| 3 | `PLAINTEXT_COMPUTE` | Plaintext to a compute host | Build on a user's server |
| 4 | `THIRD_PARTY_MODEL` | Content to a third-party model | Remote LLM inference |

A grant names the maximum level it permits. A request above the ceiling is denied.

## Schedule B — Host disclosure fields

`hostId`, `operator`, `region`, `location`, `isolation`, `retention`,
`availability`, `price`, `protocol`, `capabilities`, `dataFlowClass`.

A host that omits a field, or answers `unknown` for `operator`, `region` or
`retention`, fails disclosure and is not used.

## Schedule C — Isolation classes and their real guarantee

| Class | Contains untrusted code | Honest description |
| --- | --- | --- |
| `app-sandbox` | No | Android app sandbox; the app's own UID |
| `wasm` | Yes | Capability sandbox with no ambient filesystem or network |
| `compatibility` | No | PRoot-style layer; **not a sandbox** |
| `container` | Yes | OS-level isolation on a host the user controls |
| `microvm` | Yes | Hardware-virtualised boundary |
| `remote-disclosed` | No | Another operator's machine, disclosed before use |

## Schedule D — Supremacy checklist for pull requests

A change is mergeable only when all of the following are true:

- [ ] It does not weaken an entrenched commitment in Article 3.
- [ ] Every new MUST has a requirement ID and evidence plan.
- [ ] Every unmet MUST has a deviation with an owner and expiry.
- [ ] Local behaviour still works with the network disabled.
- [ ] Any new egress path has a grant, a purpose and a ledger test.
- [ ] Any new host declares the full Schedule B disclosure.
- [ ] Any new capability is signed, hashed and revocable.
- [ ] Any schema change is versioned and has a fixture.
- [ ] The claimed profile in `README.md`, the website and the release notes is
      still true.

---

*This Constitution is Apache-2.0, like the rest of the repository. It is a
governing text, not a licence exception.*
