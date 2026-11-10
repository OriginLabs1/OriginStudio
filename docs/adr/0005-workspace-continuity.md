# ADR 0005: Portable workspace manifest, hash-chained history and a lease

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** the "no manifest or checkpoint format" gap in the v0.0.3 register
- **Related:** `CONSTITUTION.md` Article 7, `core/.../OriginWorkspace.kt`

## Context

Projects were plain directories in the Debian guest. That satisfied portability,
but it gave the product no way to answer four questions a multi-host product must
answer:

- Is this directory an Origin workspace, and which one?
- Has its history been edited or truncated?
- Which host is allowed to write right now?
- What does a complete export contain?

## Decision

A workspace is a plain directory plus `.origin/`:

```
project/
  ... user files ...
  .origin/
    manifest.json     workspace id, name, created time, owners, policy
    events.jsonl      append-only, hash-chained events
    checkpoints/      future restore points
    lease.json        transient single-writer token (never exported)
```

1. **The manifest is small JSON** with a versioned schema (`origin.workspace/v1`).
   An unknown schema is refused.
2. **Events are hash-chained.** Each event hashes `seq`, `at`, `type`, sorted
   payload, `prev` and its own contents. Verification replays the chain and fails
   on a gap, a reorder, an edit or a truncated tail.
3. **The lease is single-writer.** It carries `workspaceId`, `hostId`, `epoch`,
   `acquiredAt` and `expiresAt`. A different host cannot acquire an active lease;
   it can acquire an expired one.
4. **Handoff transfers the lease.** `transferLease(from, to)` requires the
   current holder and increments the epoch. It is not process migration.
5. **Opening verifies.** `verify()` never repairs. The caller decides what to do.
6. **Export is complete and standard.** Zip of user files plus `.origin/`, with
   the transient lease excluded. No Origin Studio is needed to read it.

## Consequences

- The app, a desktop and the reference host can all agree on workspace identity
  and integrity without a database.
- History grows as a text log. Compaction is a future ADR; deletion is explicit.
- Workspaces created before this ADR open as ordinary folders until `ensure()` is
  called. `ensure()` is additive and never overwrites an existing manifest.
- The model is deliberately boring. A workspace from 2026 should still open in
  2036.

## Alternatives rejected

- **SQLite metadata.** Rejected: breaks plain-file continuity and makes export
  depend on a library.
- **Git as the event log.** Rejected for metadata: git is the user's tool, not
  the product's authority; mixing them makes history unverifiable and surprising.
  Users may still put a workspace in git.
- **Last-write-wins timestamps.** Rejected: silent loss is the failure this
  design exists to prevent.
