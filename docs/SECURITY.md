# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| Latest published release | Yes |
| Previous minor line | Best effort |
| Anything older | No |

Fixes ship as a new signed release. There are no in-place patches.

## Reporting a vulnerability

Use **GitHub private vulnerability reporting** on
`github.com/OriginLabs1/OriginStudio` (Security → Report a vulnerability), or
email the maintainer listed in `MAINTAINERS` with `SECURITY` in the subject.

Please include: affected version, device and Android version, steps to reproduce,
impact, and whether you plan to disclose publicly. Do not open a public issue for
an unpatched vulnerability.

**Response target:** acknowledgement within 7 days, an assessment within 14 days,
and a fix or a written explanation within 90 days. There is no bug bounty.

## In scope

- The APK: signature verification, update flow, file handling, intent handling.
- The release pipeline: tag integrity, secret handling, artifact checksums.
- The local-AI path: model verification, tool authorisation, egress.
- Data loss caused by the app: corruption on write, recovery failure, migration.

## Out of scope

- A rooted, compromised or malicious OS; kernel or OEM tampering.
- A person with physical access to an unlocked device.
- Code the user chooses to run inside the Debian guest. The guest is their
  machine, and PRoot is a compatibility layer, not a sandbox.
- Third-party agents and packages the user installs.
- Model output quality or prompt injection that only affects what the model says
  (as opposed to what the app executes).

## Disclosure

We publish an advisory for every fixed security issue, with the version that
contains the fix and the credit the reporter requests. If a release must be
expedited, it may skip normal scope review; it is still signed, checksummed and
documented.

## Hardening rules for contributors

1. No new permission without an entry in the threat model.
2. No new network call without a visible reason and, for AI paths, an explicit
   user grant.
3. No secret in source, tests, fixtures or logs.
4. New IPC boundaries need a threat-model row before merge.
5. Never describe PRoot as isolated.
