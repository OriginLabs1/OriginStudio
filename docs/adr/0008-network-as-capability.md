# ADR 0008: Network as a capability — deny-by-default egress and data-flow classes

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** "no egress policy for the AI path" and "no visible egress log"
- **Related:** `CONSTITUTION.md` Articles 10 and 11, `contracts/egress-entry.schema.json`

## Context

The app already makes network requests: the update check, apt inside the guest,
agent installs and agents themselves. Adding remote hosts and remote inference
multiplies the paths. A permission prompt per request is unusable; a blanket
"internet" permission is dishonest.

## Decision

Network is a **capability**, not an ambient right.

1. Every request names a capability, a host, a human-readable purpose and a
   data-flow class (Schedule A, levels 0–4).
2. An `EgressPolicy` holds grants. Each grant is scoped to a capability and host,
   has a maximum class, and may expire.
3. Absence of a matching grant means **deny**. The denial is recorded.
4. A request above the grant's ceiling is denied, not truncated.
5. A hash-chained `EgressLedger` records grants **and** denials. The user can
   verify it has not been edited.
6. Data-flow classes are part of the wire contract. A remote host declares its
   maximum; a capability declares its need.

## Consequences

- The app can show a truthful egress history instead of a vague privacy claim.
- Adding a network path requires a grant, a purpose and a ledger test — enforced
  by the Schedule D checklist.
- The ledger is local and append-only; shipping it off-device is itself egress
  and requires a grant.
- Class 4 (`THIRD_PARTY_MODEL`) is never a default. It requires an explicit,
  revocable grant and a visible disclosure.
- The policy is deliberately coarser than per-request prompting. Grants are
  remembered; the user can revoke them.

## Alternatives rejected

- **Android INTERNET permission as the model.** Rejected: all-or-nothing, not
  attributable to a capability, and invisible after install.
- **Prompt on every request.** Rejected: unusable and users learn to tap yes.
- **Silent allow for "trusted" domains.** Rejected: allowlists drift and become
  an unstated policy.
