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

## 3. Trust boundaries

```
UI (Compose)  ──1──  app process  ──2──  :qvac_worker (Binder, isolated process)
                          │
                          3  (ptrace; same UID; NO security boundary)
                          ▼
                    PRoot guest (Debian, user-installed tools, agents)
                          │
                          4                       5
                          ▼                       ▼
                     network (TLS)          SAF / shared storage
                          ▲
                          6
                    GitHub Releases / update channel

build host ──7── release key ──7── signed APK ──7── user device
```

**Boundary 3 is the important one.** PRoot uses `ptrace` to translate syscalls.
It is a *compatibility* layer, not a container. Code in the guest runs with the
app's UID and can read the app's private data. Any statement that the guest is
"isolated" or "sandboxed" is false and must not appear in the UI, the docs or the
release notes.

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
| T13 | Supply-chain substitution | A3 | QVAC Maven ZIP pinned by SHA-256 and vendored; `bare-rpc-kotlin` restricted to `com.github.holepunchto.*`; Gradle dependency verification is not enabled | Partial |
| T14 | Compromised release key | A5, boundary 7 | Key held offline, secrets only in CI; no rotation plan yet | Partial |
| T15 | Malicious agent binary | A2 | The user installs it deliberately; the app does not auto-run agents | Accepted risk (disclosed) |
| T16 | Terminal transcript contains secrets | A8 | Stored in app-private storage; no encryption at rest beyond the OS | Accepted risk |
| T17 | Notification or intent leaks content | A6 | Notifications show session titles, not full output; pending intents are immutable | Met |
| T18 | Backup of app data includes credentials | A8 | Android auto-backup is disabled for the app (verify per release) | Met |

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

## 7. Open items before P2

- Egress policy: deny by default, per-session grants, visible log.
- Prompt-injection corpus and tool-allowlist tests.
- Model provenance UI: source, licence, size, hash, delete.
- Approval classes: reversible (apply/undo) versus consequential (typed
  confirmation).
