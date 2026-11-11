# Threat model

- **Status:** living document, first version
- **Review:** every minor release, or when a trust boundary changes
- **Companion:** `docs/strategy/REQUIREMENTS.md` (OS-TRUST, OS-DATA, OS-AI rows)

## 1. What we are protecting

| Asset | Why it matters | Where it lives |
| --- | --- | --- |
| Project files | The user's work; the reason the app exists | App-private storage and the Debian rootfs |
| Credentials and tokens | SSH keys, git tokens, agent logins | Inside the guest (`~/.ssh`, `~/.config`, env) |
| Terminal and agent transcripts | May contain secrets, private code, personal context | App storage, session state |
| User identity | Device identity, update trust, signing | Not yet implemented as a first-class object |
| Model files | Large downloads; integrity affects output trust | App storage |
| Release artifacts | Every installed copy depends on their integrity | GitHub Releases |
| Release key | Compromise allows malicious updates | Offline, outside the repo |
| Availability of work | A lost project is worse than a crash | Workspace + checkpoints |
| Remote workspace copies | Exist only while a remote host holds the lease | A host the user controls |
| Host tokens | Access to a reference host | Host environment / user configuration |
| Egress ledger and workspace history | Prove what left the device and what changed | App storage (`:core`) |
| Capability manifests and artifacts | Signed code that may run later | Capability store, `contracts/` |

## 2. Adversaries

| ID | Adversary | Capability | In scope |
| --- | --- | --- | --- |
| A1 | Malicious project content | Files, configs, prompts, symlinks, archives planted in a project or cloned repo | Yes |
| A2 | Malicious package or agent | `npm`, `pip`, `apt`, agent installers the user runs in the guest | Partly — the user chose to run it; the app must not make it worse |
| A3 | Compromised dependency | A Maven/npm artifact or a bundled binary | Yes |
| A4 | Hostile network | MITM on apt, model download, update check | Yes |
| A5 | Malicious update | A forged APK or downgrade | Yes |
| A6 | Another app on the device | Intent redirection, clipboard, shared storage, accessibility abuse | Yes |
| A7 | Thief with a locked device | Physical access | Out of scope (OS encryption is the control) |
| A8 | Thief with an unlocked device | Full access to app data | Out of scope |
| A9 | Compromised OS, root, or malicious OEM | Kernel-level control | Out of scope |
| A10 | Coerced disclosure | Legal or physical pressure on the user | Out of scope (no backdoor, no hidden copy) |
| A11 | Malicious or careless remote host | Reads, retains or alters workspace content | Yes |
| A12 | Malicious capability publisher | Signed but harmful extension | Yes |
| A13 | Network observer | Sees traffic to a remote host | Yes, if the host is not behind TLS |
| A14 | Silent vendor service | Becomes a required dependency | Yes, by architecture |

## 3. Trust boundaries

```
UI (Compose)  ──1──  app process
                          │
                          2  (ptrace; same UID; NO security boundary)
                          ▼
                    PRoot guest (Debian, user-installed tools, agents)
                          │
                          3                       4
                          ▼                       ▼
                     network (TLS)          SAF / shared storage
                          ▲
                          5
                    GitHub Releases / update channel
                           │
                           6  (origin.host/v1, explicit disclosure)
                           ▼
                 remote host (hub / user-rented machine)

build host ──7── release key ──7── signed APK ──7── user device
```

**Boundary 2 is the important one.** PRoot uses `ptrace` to translate syscalls.
It is a *compatibility* layer, not a container. Code in the guest runs with the
app's UID and can read the app's private data. Any statement that the guest is
"isolated" or "sandboxed" is false and must not appear in the UI, the docs or the
release notes.

**Boundary 6 is new and explicit.** A remote host is a different operator's
machine unless the user says otherwise. It is used only after disclosure, only
under a lease, and only for the capabilities it advertises. The app may not fall
back to it silently.

## 4. Threats and controls

| ID | Threat | Path | Control today | Status |
| --- | --- | --- | --- | --- |
| T1 | Forged APK installed as an update | A5, boundary 6 | APK signature verified by Android; update flow also checks SHA-256 from the release manifest | Met |
| T2 | Downgrade to a vulnerable release | A5 | Android rejects same-key lower `versionCode` installs; update flow only offers a higher version | Met (platform) |
| T3 | Tampered model file | A1, A4 | Download manifest carries SHA-256; worker refuses a mismatched file | Partial — verify on device |
| T4 | MITM on apt or downloads | A4 | HTTPS with a bundled CA store; guest TLS for agent installs depends on the guest | Partial |
| T5 | Prompt injection from project files | A1 | Not applicable yet: the Ask/Explain/Fix surface is not exposed. ACP agents are user-installed tools and read what they are told to read | Open — design in `docs/AI-POLICY.md` before P2 |
| T6 | Tool call escapes the approval boundary | A1 | Same as T5: the local-AI tool handlers do not ship yet | Open |
| T7 | Silent network egress by the AI path | A1 | None yet; no egress policy exists | Open — P2 blocker |
| T8 | Malicious file escapes the project root | A1 | File operations are rooted at the selected project folder; symlinks inside the guest are not confined | Partial |
| T9 | Zip/tar bomb during import | A1 | Pure-Kotlin extractor with size checks (see `TarExtractorTest`) | Met |
| T10 | Another app reads exported files | A6 | SAF is user-mediated; the app does not export to shared storage without the user picking a destination | Met |
| T11 | Clipboard leaks secrets | A6 | No automatic clipboard read; copy is user-initiated | Met |
| T12 | Update check leaks usage | A4 | Only a GET to the public GitHub releases API; no identifier is sent | Met |
| T13 | Supply-chain substitution | A3 | JitPack is restricted to `com.github.termux.*`; the release is signed and every artifact is checksummed; a CycloneDX SBOM ships per release; Gradle dependency verification is not enabled | Partial |
| T14 | Compromised release key | A5, boundary 7 | Key held offline, secrets only in CI; no rotation plan yet | Partial |
| T15 | Malicious agent binary | A2 | The user installs it deliberately; the app does not auto-run agents | Accepted risk (disclosed) |
| T16 | Terminal transcript contains secrets | A8 | Stored in app-private storage; no encryption at rest beyond the OS | Accepted risk |
| T17 | Notification or intent leaks content | A6 | Notifications show session titles, not full output; pending intents are immutable | Met |
| T18 | Backup of app data includes credentials | A8 | Android auto-backup is disabled for the app (verify per release) | Met |
| T19 | A rogue or careless remote host reads or retains workspace content | A11, boundary 8 | Explicit disclosure, data minimisation, a lease, and `revoke`; no deletion receipt yet | Partial |
| T20 | Silent fallback to a remote host | A11, boundary 8 | `HostSelection` requires `allowRemote`, prefers the device, and has no fallback; `HttpOriginHostClient` throws on failure | Met |
| T21 | Protocol downgrade or version confusion | A11, boundary 8 | Major versions must match exactly; unknown majors and message types fail closed | Met |
| T22 | A malicious capability with a plausible manifest | A12 | Artifact SHA-256, publisher signature and revocation are all checked before execution | Met (`:core`); installer UI pending |
| T23 | Workspace history is edited or truncated | A1, A11 | Hash chain verified on open by the app core and the reference host; failures are reported, never repaired | Met |
| T24 | Two writers corrupt a workspace | A11, boundary 8 | Single-writer lease with epochs; a second host is refused until expiry or explicit handoff | Met |
| T25 | Egress happens without a grant | A10, boundary 4 | Deny-by-default policy with mandatory capability, host and purpose; denials are logged | Partial — AI path not wired yet |
| T26 | A remote host keeps a copy after access is revoked | A11, boundary 8 | `revoke` releases the lease and is recorded; deletion receipts are not implemented | Open — P5 |
| T27 | A leaked host token grants access to the reference host | A11, boundary 8 | Bearer token, constant-time comparison, loopback bind by default, no token in URLs | Partial — no rotation UI |
| T28 | Data leaves the user's stated region without notice | A11, boundary 8 | `region` is a required disclosure field and is recorded with the session | Partial — no residency UI yet |

## 5. What is explicitly not defended

- A rooted device, a compromised kernel, or a malicious OEM build.
- A person holding the unlocked phone.
- Code the user chooses to run inside the guest. The guest is their machine.
- Side channels (timing, power, thermal) between guest processes.
- Denial of service by the user's own workloads.

Saying this plainly is part of the trust claim. A security page that implies
otherwise is worse than no page.

## 6. Rules that follow from this model

1. Never describe PRoot as a sandbox; label it "compatibility" wherever isolation
   is mentioned.
2. Never run untrusted code in the guest on the user's behalf without an explicit
   action. Agents the user installs are their choice.
3. No feature may add network access to a path that did not have it without an
   entry here and evidence in the requirements register.
4. Any new IPC boundary needs its own row in this document before it ships.
5. A release that weakens a control needs an ADR and a note in the release notes.
6. A remote host is never used because the local one failed. Failure is shown;
   substitution is the user's decision.
7. A capability is never trusted because it is signed. Hash, signature,
   revocation and isolation class are separate facts.

## 7. Open items

- Egress UI: the deny-by-default policy and ledger exist in `:core`; the AI path
  is not wired to them yet.
- Prompt-injection corpus and tool-allowlist tests.
- Model provenance UI: source, licence, size, hash, delete.
- Approval classes: reversible (apply/undo) versus consequential (typed
  confirmation).
- Remote deletion receipts, so D5 can be claimed honestly.
- Rotation and revocation UI for host tokens.
- A residency and retention screen before the first remote use (D4).
