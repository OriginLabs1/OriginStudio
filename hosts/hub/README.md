# Origin reference host

A dependency-free Node implementation of `origin.host/v1`. It is the conformance
target for the Host Protocol and the first supported remote path: a machine the
user already controls.

```bash
ORIGIN_WORKSPACE_ROOT="$HOME/origin" \
ORIGIN_HOST_TOKEN="a-long-random-token" \
ORIGIN_HOST_PORT=8787 \
node server.js
```

It binds to `127.0.0.1` by default. That is deliberate: v1 has a bearer token and
no transport security of its own. Put it behind your own TLS terminator before
exposing it to a network.

## What it implements

| Message | Behaviour |
| --- | --- |
| `hello`, `capabilities` | Full Schedule B disclosure |
| `health` | Liveness and protocol version |
| `open_workspace` | Verifies manifest and hash chain, returns hash and event count |
| `checkpoint` | Writes `.origin/checkpoints/cp_*.json`, appends an event |
| `revoke` | Releases this host's lease, appends an event |
| everything else | A structured `unsupported_message` error |

It deliberately does **not** fake execution. A host that cannot run a turn says
so; it does not return pretend output.

## Tests

```bash
npm test
```

The suite starts the real server, builds real workspaces, and checks disclosure,
version refusal, tamper detection, checkpointing, traversal refusal and bearer
auth. It is part of CI.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `ORIGIN_WORKSPACE_ROOT` | current directory | Workspaces must live under this path |
| `ORIGIN_HOST_TOKEN` | none | Bearer token; strongly recommended |
| `ORIGIN_HOST_PORT` | `8787` | Listen port |
| `ORIGIN_HOST_BIND` | `127.0.0.1` | Bind address |

## Security posture

- Path traversal is refused before filesystem access.
- Request bodies are capped at 1 MiB.
- Unknown protocol majors and unknown message types fail closed.
- Token comparison is constant-time.
- It never repairs a workspace; a broken chain is reported.

Accepted limitation: this is a single-user reference host, not a multi-tenant
service. It is disclosed as such in `docs/strategy/DEVIATIONS.md`.
