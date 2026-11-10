# ADR 0009: Bring-your-own host first; a dependency-free reference host

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** none
- **Related:** ADR-0004, ADR-0006, `hosts/hub/`

## Context

Remote execution can be delivered as:

- a **managed cloud** operated by OneOrigin,
- a **third-party** service,
- or a **host the user already has**: a laptop, a home server, a small VPS they
  rent, a Raspberry Pi.

A managed cloud is the easiest to monetise and the hardest to govern: it creates
custody of user data, residency questions, billing, abuse handling and a
single-point-of-failure that contradicts Article 15. It also cannot be built
honestly before the protocol, identity and deletion semantics exist.

## Decision

1. The first supported remote path is **bring-your-own host**. The user runs the
   reference host on a machine they control.
2. The reference implementation lives in `hosts/hub/`, is dependency-free Node,
   and is small enough to audit in one sitting.
3. A managed Origin cloud is a later, separately governed offering. It must speak
   the same protocol, disclose the same fields, and be removable without
   disabling the app.
4. The reference host is a **conformance target**, not the product. It proves the
   protocol is implementable without the Android app.

## Consequences

- Remote execution can ship without custody of user data.
- The `hosts/hub/` server must pass the same contract fixtures as the app.
- Managed hosting needs an ADR that covers residency, deletion, pricing,
  incident response and shutdown.
- The reference host has no authentication beyond a bearer token in v1. It is
  documented as suitable for a trusted network or behind the user's own TLS
  terminator, not as an internet-exposed default.

## Alternatives rejected

- **Managed cloud first.** Rejected: custody before contracts, and it would make
  the product's promises depend on a business that does not exist yet.
- **Rent-a-VPS integration per provider.** Rejected: provider-specific APIs
  recreate the fragmentation the Host Protocol removes.
- **No reference host.** Rejected: an unimplemented protocol is a wish.
