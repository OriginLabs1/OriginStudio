# Dual mode: local and remote execution

- **Status:** implemented in `:core` and `hosts/hub/`, gates D1 and D2 passed
- **Supreme text:** [`CONSTITUTION.md`](../../CONSTITUTION.md) Articles 4.1, 5 and 6
- **Contracts:** [`contracts/`](../../contracts/)
- **Reference host:** [`hosts/hub/`](../../hosts/hub/)

Origin Studio runs on the device **and** on a host the user controls. The two are
not different products: they speak one protocol, operate on one workspace format,
and are governed by one set of guarantees.

## The five gates

| Gate | Name | What must be true | Where it is proven |
| --- | --- | --- | --- |
| D1 | Dual Open | The same workspace opens on the device and on the reference host with matching manifest hash and event count | `OriginHostProtocolTest`, `hosts/hub/test/server.test.js` |
| D2 | Handoff | A lease transfers device → host and host → device without losing an event | `OriginWorkspaceTest` |
| D3 | Offline | After setup, local read, edit, run and export work with the network disabled | `:core` and loopback are offline; app baseline tracked in `DEVIATIONS.md` |
| D4 | Disclosure | Operator, region, isolation, retention, availability and price are shown before a remote host is used | Protocol and schema implemented; acknowledgement UI pending |
| D5 | Sovereignty | Revoke access, delete remote copies, export everything | `revoke` implemented; deletion receipts pending |

A gate is passed only when its requirement row in `REQUIREMENTS.md` is **Met**.
The table above is a summary, not a claim.

## How execution is chosen

1. A capability names what it needs (`run.turn`, `workspace.open`, …).
2. The host list is filtered to hosts that advertise it.
3. Remote hosts are removed unless the user allowed remote execution.
4. A device host wins a tie; otherwise the lowest data-flow class wins.
5. If nothing matches, the operation fails visibly. There is no silent fallback.

The same rules live in `HostSelection` (Kotlin) and are exercised by
`OriginHostProtocolTest`.

## Running the reference host

Requirements: Node 20 or newer. No dependencies.

```bash
# A directory that contains one or more Origin workspaces
export ORIGIN_WORKSPACE_ROOT="$HOME/origin"
export ORIGIN_HOST_TOKEN="$(head -c 24 /dev/urandom | base64)"   # optional but recommended
export ORIGIN_HOST_PORT=8787

node hosts/hub/server.js
```

Then check the disclosure and open a workspace:

```bash
curl -s http://127.0.0.1:8787/health

curl -s http://127.0.0.1:8787/origin/host/v1 \
  -H 'content-type: application/json' \
  -H "authorization: Bearer $ORIGIN_HOST_TOKEN" \
  -d '{
    "protocol": "origin.host/v1",
    "type": "hello",
    "id": "hello-1",
    "seq": 1,
    "idempotencyKey": null,
    "payload": {}
  }'
```

Replace `hello` with `open_workspace` and `"payload": {"path":"my-project"}` to
verify a workspace. The host refuses paths outside `ORIGIN_WORKSPACE_ROOT`.

Do not expose the reference host directly to the public internet. Bind it to
loopback (the default), or put it behind a TLS terminator the user controls. See
ADR-0009.

## Verifying the contracts

```bash
# Kotlin core, including workspace, identity, egress, capability and protocol
./gradlew :core:test

# Reference host over real HTTP
cd hosts/hub && npm test

# Schema fixtures
node tools/validate-contracts.mjs

# Requirements ↔ deviations consistency
node tools/check-evidence.mjs
```

CI runs all four. A change that breaks one is not mergeable.

## What handoff is not

Handoff transfers **authority** to write. It does not migrate a running process,
a terminal session or an in-memory agent turn. The current holder checkpoints,
releases or transfers the lease, and the new holder opens the same files. The UI
must say which host is the active writer.

## What is deliberately not built yet

- Conflict-explicit sync (ADR-0006 sequences it after handoff).
- A capability installer UI.
- A remote-deletion receipt.
- A managed Origin cloud. If one exists later, it is governed separately and must
  speak this same protocol.
