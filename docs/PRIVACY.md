# Privacy

Origin Studio is local-first. It has no account system, no analytics, no
advertising SDK, no crash-reporting SDK and no telemetry. This page states what
leaves the device and when, so the claim is checkable rather than a slogan.

## What stays on the device

| Data | Where | Leaves the device? |
| --- | --- | --- |
| Projects and files | App-private storage / Debian rootfs | Only when you export, share or use a tool that uploads them |
| Terminal and agent transcripts | App-private storage | Only if a tool you run sends them |
| Credentials you create in the guest (SSH keys, tokens) | Guest filesystem | Only if you use them with a network service |
| Model files | App-private storage | No |
| Settings, session state | App-private storage | No |

Android auto-backup is disabled (`android:allowBackup="false"`), so app data is
not copied to a cloud backup by the OS.

## What uses the network, and why

| Action | Destination | Data sent | When |
| --- | --- | --- | --- |
| Update check | `api.github.com` | A plain GET for the latest public release; no identifier | At launch, throttled to once every few hours, and when you tap Check for updates |
| APK download | GitHub Releases | Standard HTTP request | When you accept an update |
| Model download | The source named in the model manifest | Standard HTTP request | When you start a download |
| `apt`, `npm`, `pip`, agent installs | The registries you point them at | Whatever those tools send | When you run them |
| Agents (Claude, Codex, etc.) | Their providers | Whatever the agent sends | When you use an agent that needs a provider |
| Local AI | `:qvac_worker` on the same device | No network | Inference is local |

Anything in the last three rows is software *you* chose to run, under its own
terms. Origin Studio does not proxy, log or analyse it.

## Permissions the app declares

| Permission | Why |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Downloads and update checks |
| `REQUEST_INSTALL_PACKAGES` | Installing an update and agent APKs |
| `FOREGROUND_SERVICE` | Keeping a session alive while you are in another app |
| `POST_NOTIFICATIONS` | Session and completion notices |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional; only if you grant it, so long sessions are not killed |

No contacts, location, camera, microphone, SMS, call log or accessibility access.

## Your controls

- **Delete app data** — Android Settings → Apps → Origin Studio → Storage →
  Clear data. This removes projects and models. Export first.
- **No background download** — a throttled launch-time check asks GitHub for the public latest release (no identifier); nothing is downloaded or installed without your tap.
- **Use it offline** — after the initial setup and any downloads, the editor and
  terminal work without a network.
- **No account to delete** — there isn't one.

## Children and sensitive data

The app is a general-purpose development tool, not directed at children. Do not
store regulated data in a project unless you have independently protected the
device; the app adds no encryption beyond the operating system.

## Changes

If a future release adds optional diagnostics, it will be opt-in, described here
before it ships, and off by default. Silent collection would contradict the
product's premise.
