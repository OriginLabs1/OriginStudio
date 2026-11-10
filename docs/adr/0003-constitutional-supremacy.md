# ADR 0003: Constitutional supremacy and the dual-mode mandate

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** the implicit precedence of `BLUEPRINT.md` and `MLP-ALP-SCOPE.md`
- **Related:** `CONSTITUTION.md` v0.2.0, ADR-0002, ADR-0004, ADR-0009

## Context

Origin Studio already had a governance document, a requirements register, two
ADRs and a shipping Android application. It also had a strategic direction —
local-first execution extended to remote hosts — that lived mostly in planning
documents. Two problems followed:

1. Nothing said which document won when the plan, the code and the constitution
   disagreed. In practice the code won, because it shipped.
2. The remote direction was optional. It could be deferred forever without
   anything failing.

## Decision

1. `CONSTITUTION.md` v0.2.0 is the supreme governing text. The precedence order is
   written in Article 0 and enforced in review.
2. The existing project is explicitly subordinate: its requirements, ADRs,
   build settings and shipped behaviour are re-read through the Constitution, and
   gaps are recorded in `DEVIATIONS.md`.
3. Local + remote is a constitutional obligation, expressed as the five gates
   D1–D5 in Article 4.1.
4. A pure-JVM `:core` module holds the portable contracts and is the only place
   where workspace, identity, egress, capability and host logic may live. Android
   code depends on it; it does not depend on Android.

## Consequences

- A change that contradicts the Constitution is a defect, even if it ships.
- The `:core` module is testable without an Android build, which is how the
  contract is verified in CI.
- `targetSdk 28`, QVAC and PRoot remain valid choices, but they are now
  **provider decisions under ADR-0002**, not exceptions to the Constitution.
- The claimed profile must appear in the README, the website and release notes.

## Alternatives rejected

- **Keep governance as-is and add a strategy note.** Rejected: the note would
  lose to code at the first deadline.
- **Put the Constitution under `docs/`.** Rejected: burying the supreme text
  signals that it is optional. It lives at the repository root.
- **Wait for a second platform before abstracting.** Rejected: the remote host is
  the second provider, and it exists now as a reference implementation.
