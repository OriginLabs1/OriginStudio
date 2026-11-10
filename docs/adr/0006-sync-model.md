# ADR 0006: Handoff first, conflict-explicit sync second, CRDT later

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** none
- **Related:** ADR-0005, `CONSTITUTION.md` Articles 4.1 (D2) and 7

## Context

Multi-host editing has three credible models:

1. **Exclusive lease with explicit handoff.** One writer at a time; the other side
   waits or receives authority. Simple, honest, and it cannot lose an edit
   silently.
2. **Conflict-explicit sync.** Both sides may edit; divergence produces visible
   conflict artifacts and a user choice.
3. **CRDT / operational transform.** Automatic merge. Powerful, but the failure
   modes are subtle, the metadata is heavy, and a wrong merge is indistinguishable
   from a correct one to the user.

The product is not a collaborative document editor. It is a workspace with
files, shells and agents.

## Decision

Ship in that order:

- **P4:** exclusive lease + handoff (D2). This is implemented in `:core` and
  covered by tests.
- **P5:** conflict-explicit sync. Divergence is detected by manifest hash and
  event-chain ancestry, written as a conflict artifact, and surfaced for a
  decision. The system never chooses a winner silently.
- **Later:** CRDT only if a concrete use case cannot be served by explicit
  conflicts, and only behind an ADR that names the merge semantics and the
  recovery path.

## Consequences

- Two devices are never silently both "the writer". The UI must name the active
  host.
- Sync can be added without changing the workspace format: it adds event
  ancestry and conflict records.
- A user who wants real-time co-editing is not served yet, and the product says
  so.
- Handoff is a user action with a clear boundary; agents cannot trigger it
  without permission.

## Alternatives rejected

- **CRDT first.** Rejected: unproven need, high metadata cost, and silent merge
  conflicts violate Article 7.5.
- **Last-write-wins by timestamp.** Rejected: clocks lie and data disappears.
- **Server-authoritative sync.** Rejected: it makes remote mandatory, violating
  Article 5.
