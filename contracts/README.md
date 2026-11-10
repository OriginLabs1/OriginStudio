# Contracts

These schemas are the machine-readable half of the Constitution. The Kotlin
`:core` module and the Node reference host both implement them, and CI validates
the fixtures against both.

| Schema | Governs | Article |
| --- | --- | --- |
| [workspace.schema.json](workspace.schema.json) | `.origin/manifest.json` | 7.1, 7.6 |
| [workspace-event.schema.json](workspace-event.schema.json) | `.origin/events.jsonl` | 7.2 |
| [host-envelope.schema.json](host-envelope.schema.json) | Every Host Protocol message | 5.4, 6.1 |
| [host-capabilities.schema.json](host-capabilities.schema.json) | Pre-use host disclosure | 6.2 |
| [capability.schema.json](capability.schema.json) | Signed capability manifests | 9.1 |
| [egress-entry.schema.json](egress-entry.schema.json) | Egress ledger entries | 10.5 |

## Change rules

1. A schema is versioned in-band (`schema` or `protocol`). Adding a required
   field is a new major.
2. Every schema change needs a fixture under `fixtures/` in the same commit.
3. A fixture that no longer validates is a failing build, not a warning.
4. Schemas are additive in public: a client must refuse an unknown major rather
   than guess.

## Fixtures

`fixtures/*.valid.json` are positive examples used by `:core` tests, the
reference host tests and CI. Hashes in the event and egress fixtures are real
chains computed with the canonical rules, so they also serve as cross-language
conformance vectors.
