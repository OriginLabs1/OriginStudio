# Personas and the five-user gate

The constitution says "universal". That is a direction, not a market. The MLP is
for three specific people, and the release is judged by them.

## Persona A — "The tablet builder"

- **Situation:** has an Android tablet and a keyboard, no laptop, or a laptop that
  is shared, broken or impractical to carry.
- **Job:** *"Let me change this project, run it, and see it work, without setting
  up a Linux system from scratch."*
- **What must be true:** terminal works on first try; editor does not lose work;
  preview shows the result; an error explains the next step.
- **What loses them:** a setup wizard longer than five minutes, a broken package
  install, an app that dies when they switch to the browser.

## Persona B — "The student on metered data"

- **Situation:** phone-first, intermittent connectivity, expensive data, 4 GB RAM.
- **Job:** *"Let me practise real development on the device I already own."*
- **What must be true:** the app installs and opens offline; downloads are
  explicit, resumable and sized before they start; nothing updates silently.
- **What loses them:** a 2 GB surprise download, an update that breaks the
  environment, a crash that costs an hour of work.

## Persona C — "The field maintainer"

- **Situation:** needs to inspect a repository, read logs, or make a small fix on
  a phone while away from a desk.
- **Job:** *"Let me see what broke and fix the one line, safely."*
- **What must be true:** the file is easy to find; the change is reviewable; the
  terminal output is readable; nothing is applied automatically.
- **What loses them:** buried files, ambiguous AI actions, no history.

## Who the MLP is *not* for

Teams, CI operators, designers, gamers, people who want a full IDE with language
servers, and anyone expecting Google Play billing or cloud sync. Saying this keeps
the scope honest.

## The five-user gate (L5)

The release gate says five unaided users complete the journey and three reuse it
within a week. That is a qualitative signal, not statistics. Run it like this:

1. **Recruit** five people who match A, B or C and have never seen the app.
2. **Consent** in writing: what is recorded (screen, audio, notes), how long it is
   kept, and that they can stop at any time.
3. **Observe** without helping for the first five minutes. Note every pause longer
   than ten seconds and every wrong turn.
4. **Ask** afterwards: what did you think it was for, what almost made you stop,
   would you use it again next week.
5. **Follow up** after seven days: did you open it, what did you do, what stopped
   you.
6. **Publish** the result in the release notes, including failures. A gate that is
   always passed is not a gate.

Pass conditions: all five install and open a project; at least four save and run
something; at least three return within a week unprompted; no participant
describes the product as something it is not.

## Evidence we do not collect

No analytics, no session recording, no crash upload. Retention is measured by
asking people, not by watching them. If a future release wants aggregate metrics,
it is opt-in, listed in `PRIVACY.md`, and off by default.
