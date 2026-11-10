# ADR 0004: One versioned Host Protocol for local and remote execution

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** none
- **Related:** `CONSTITUTION.md` Articles 5 and 6, `contracts/host-envelope.schema.json`

## Context

The app runs work in three places already: the Android app process, the PRoot
guest, and external agents started in the terminal. Adding remote execution
without a shared contract would create a fourth dialect and a second set of
semantics. Retrofitting a contract later is more expensive than adopting one now.

## Decision

All execution providers speak **`origin.host/v1`**, one JSON envelope per message:

```json
{
  "protocol": "origin.host/v1",
  "type": "hello",
  "id": "uuid",
  "seq": 1,
  "idempotencyKey": null,
  "payload": {}
}
```

Rules:

- **Major versions must match exactly.** `v1` and `v2` do not negotiate. Unknown
  majors fail closed.
- **Minor versions negotiate to the lower value.** `v1.2` and `v1.4` use `v1.2`.
- **Unknown message types fail closed** with a structured `error` envelope.
- **Mutating operations carry an idempotency key.**
- **Capabilities are advertised, not assumed.** `hello` returns the full
  Schedule B disclosure: operator, region, location, isolation, retention,
  availability, price, protocol, capabilities, maximum data-flow class.
- **One in-process implementation exists** (`LoopbackOriginHost`) so the same
  protocol is exercised offline and in tests.

The wire contract is specified in `contracts/` with JSON Schema and fixtures.
The reference server is `hosts/hub/`.

## Consequences

- The app can treat local and remote execution through one interface.
- A provider that cannot speak the protocol is not supported, regardless of how
  useful it is.
- Protocol changes require a schema version bump and a fixture; a breaking change
  requires a new major and a migration note.
- `HttpOriginHostClient` is intentionally small: one POST per envelope. Streaming
  is expressed as a sequence of envelopes, not a second transport.

## Alternatives rejected

- **gRPC or WebSocket first.** Rejected for v1: extra runtime, harder to inspect,
  no benefit for request/response turns. Streaming can be added in a minor
  version without changing the envelope.
- **Reuse the Agent Client Protocol.** ACP is excellent for agent chat, but it
  does not describe workspace leases, host disclosure or capability revocation.
  The two protocols coexist; ACP stays the agent surface.
- **Per-provider ad-hoc APIs.** Rejected: that is the failure mode this ADR
  exists to prevent.
