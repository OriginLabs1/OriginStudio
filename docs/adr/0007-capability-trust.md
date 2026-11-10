# ADR 0007: Capability trust — manifest, artifact hash, signature, revocation

- **Status:** Accepted
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** the "no capability manifests or contracts" gap in v0.0.3
- **Related:** `CONSTITUTION.md` Article 9, `contracts/capability.schema.json`

## Context

P3 promises installable capabilities. "Signed" alone is not trust: a valid
signature over a substituted artifact, a revoked publisher, or an over-broad
permission set are all failures. The v0.0.3 register marked manifests, contracts
and revocation as not met.

## Decision

A capability is a `origin.capability/v1` manifest plus an artifact:

| Field | Purpose |
| --- | --- |
| `id`, `version` | Identity and compatibility |
| `publisher`, `publisherPublicKey` | Who signed it |
| `permissions` | What it may ask the host for |
| `dataFlowClass` | Maximum sensitivity it may carry |
| `isolation` | The real containment class |
| `artifactSha256` | Exact artifact binding |
| `signature` | ECDSA P-256 over a canonical field order |

Verification order, all fail-closed:

1. Schema is known.
2. Required fields are present.
3. Capability id is not revoked.
4. Artifact exists and its SHA-256 matches.
5. Manifest is signed.
6. Signature verifies against the embedded public key.

A valid signature proves **integrity and publisher identity**. It does not prove
safety. The isolation class and permission list are separate facts shown to the
user before install.

Revocation records carry an issued-at and an expiry so they can be checked
offline. A revoked capability does not run even with a valid signature.

## Consequences

- `:core` implements the check; tests cover tampering, revocation, edited
  manifests and unsigned manifests.
- The public key is embedded, so verification needs no network. Trust in the key
  itself is a separate decision (TOFU + user confirmation for v1).
- Wildcard permissions require an ADR (Article 9.5).
- Signing is ECDSA P-256 because it works on every supported Android level and on
  the JVM. Ed25519 may replace it later via a new manifest schema.

## Alternatives rejected

- **Hash only.** Rejected: no publisher identity.
- **Signature only.** Rejected: a signature over a manifest that names the wrong
  artifact is still a substitution attack.
- **Online revocation only.** Rejected: violates offline verification and makes
  the vendor a gatekeeper.
