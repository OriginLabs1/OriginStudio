# Accessibility

- **Target:** WCAG 2.2 AA for the core journey, and a usable app with TalkBack at
  200 % font scale.
- **Status:** partial. The audit has not been run yet; this page is the standard
  it will be measured against, not a claim that it passes.

## Commitments

| Area | Standard | How it is checked |
| --- | --- | --- |
| Contrast | 4.5:1 body text, 3:1 large text and UI borders | Token audit against `Palette`; no colour-only meaning |
| Touch targets | ≥ 48 dp, ≥ 8 dp apart | Layout inspection on the smallest supported width |
| Font scale | Usable at 200 %, no clipped labels or hidden actions | Device test with the system font scale at maximum |
| Screen reader | Every control has a label; order follows the visual order; state is announced | TalkBack pass over the five core screens |
| Keyboard | Every action reachable without touch; visible focus | Hardware keyboard test on a tablet |
| Motion | No essential information conveyed only by animation | Inspection; respect the system animation scale |
| Timeouts | No action expires while the user is reading | Inspection |

## Core screens for the first audit

1. Project list / first run
2. Editor with the file tree open
3. Terminal with the key row
4. Preview pane
5. Settings, including the update card

## Known gaps

- The editor canvas is a web view; its caret and selection contrast have not been
  verified with a screen reader or at 200 % scale.
- The terminal font size is user-controlled but its default is small on a phone.
- Some icon-only controls rely on content descriptions that exist but have not
  been reviewed for meaning.
- Dynamic type in Compose is honoured; text baked into the web editor and the
  terminal is not automatically scaled.
- Colour is never the only signal in the palette, but this has not been audited
  screen by screen.

## How to report

Open an issue titled `a11y:` with the screen, the assistive technology, and the
step that failed. Accessibility fixes are release-gate items, not backlog items,
once confirmed on a core screen.

## Palette note

The 2026-11 palette moved the interface from orange to steel on ink. The steel
accent (`#9EC1E6`) on ink (`#05070E`) measures 10.8:1; muted text (`#A7AECB`)
9.2:1; faint text (`#7B83A3`) 5.4:1, so it is still above the 4.5:1 body minimum. Rose
(`#FF9FBC`) and soft green (`#9FD4A8`) are used for status with an icon or a word,
never alone.
