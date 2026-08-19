# SoundGuard — UI/UX Audit & Redesign Direction

Companion files: `soundguard-redesign-mockups.html` (open in a browser — interactive) and `SoundGuardTheme.kt` (drop-in color tokens + a status-mapping helper).

## TL;DR — highest-impact changes

1. Don't strip color entirely — reserve red/amber/green *only* for incident severity; everything else stays grayscale.
2. Move the sound simulator off the main dashboard into a collapsed "Developer & test tools" section, and never style test buttons in the same red as real alerts.
3. Stop showing raw enum values (`waiting_user`) in the chat — map every state to a human label and a color.
4. Cut duplicate chat entry points down to one per person.
5. Simplify the top bar (avatar + name + one overflow icon); drop the persistent Refresh button in favor of pull-to-refresh.
6. Fix the two text-wrapping bugs (role label, phone number).
7. Give the "Connect" button and code field a normal enabled look, not a disabled one.
8. Move secondary per-row actions (Set Primary, Remove) into an overflow (⋮) menu.
9. Replace emoji icons with a small monochrome icon set.
10. Add a bottom navigation bar instead of scattering navigation across ad hoc header buttons.

---

## 1. Color system

**Recommendation: monochrome-first, semantic-second.** Roughly 95% of the interface — backgrounds, text, buttons, navigation, cards — stays strict black/white/gray. Three colors are reserved *exclusively* for incident severity and are never used decoratively, for branding, or on demo/test buttons:

| Token | Hex | Use |
|---|---|---|
| Ink 900 | `#111111` | Primary text, primary button fill |
| Ink 700 | `#404040` | Secondary text |
| Ink 500 | `#767676` | Tertiary text, inactive icons |
| Ink 300 | `#C2C2C2` | Borders, dividers, disabled state |
| Ink 100 | `#E8E8E6` | Hairline dividers |
| Surface | `#FFFFFF` | Cards |
| Background | `#F6F6F4` | Screen background |
| Danger | `#C62828` on `#FDECEA` | Active / escalated incident only |
| Warning | `#A15C00` on `#FFF4E1` | Waiting / pending only |
| Success | `#2E7D32` on `#EAF5EA` | Resolved / safe only |

**Why not pure black-and-white:** this app's entire job is fast triage under stress — an anxious caregiver checking their phone, or an older adult reacting to something scary. Color is the fastest channel humans have for that, faster than reading a word. Removing it doesn't make the app more minimal in any way that helps; it just means more reading to get the same information. The discipline that keeps it feeling minimal is reserving color strictly for severity and nowhere else — no purple brand accent, no colored demo buttons, no tinted marketing cards.

One unavoidable exception: if you keep Google Sign-In, Google's brand guidelines require the actual multicolor "G" mark — it can't be recolored to match the palette. Everything else on that screen can still be pure ink-on-white.

---

## 2. Information architecture

**Redundant chat entry points.** The beneficiary dashboard currently has three ways to reach the same conversation: a "Chat with Yo" hero card, a "Caregiver Chat Preview" (demo), and an "Open Chat" button inside the caregiver's row. The caregiver dashboard mirrors this. Pick one: tapping a person's row opens the chat. Everything else is noise.

**Persistent header buttons vs. native patterns.** A permanent Refresh button on every screen duplicates a gesture users already know — pull-to-refresh. Removing it frees the header for what actually needs to be there: who you are, and one way to reach Settings.

**Dropdowns / overflow menus (the "hidden tab" pattern).** Any time a card shows more than one secondary action next to a primary one, it's a candidate for a `DropdownMenu`:
- Caregiver/beneficiary row: keep **Call** and **tap-to-chat** visible; move **Set Primary** and **Remove** into a kebab (⋮) menu.
- Settings: "Switch role" and "Sign out" are rare, high-consequence actions — put them at the bottom of the screen, visually separated (a divider + a "danger zone" treatment for Sign out), not styled the same as routine settings.

**Bottom sheets instead of new screens/cards.** `ModalBottomSheet` fits naturally for: generating a pairing code, confirming a photo request, and the "Preview chat" demo tool — lighter weight than a permanent card taking up dashboard space.

**Collapsible sections.** The sound simulator, and any raw technical readout (model name, live mic %, confidence number) belongs in a collapsed section — most users don't need it visible by default, but it shouldn't be deleted since it's genuinely useful during development and demos.

**A real bottom navigation bar.** Right now navigation is implicit — Settings and Refresh live in a header, chats are reached through cards, there's no consistent "home" affordance. Four destinations cover the whole app: **Home** (dashboard for whichever role is active), **Chat**, **People** (Caregivers or Beneficiaries depending on role), **Settings**.

---

## 3. Screen-by-screen

### Login
Not shown in your screenshots, but worth designing deliberately since it's the first impression. Centered wordmark, one outlined email field, a filled "Send code" button, then Google sign-in as an outlined button below a light divider. Keep Google's logo mark in color (brand requirement) — everything else in ink-on-white.

### Role selection
Not currently its own screen in what you shared — build it as two large, fully tappable cards rather than small radio buttons, since this is a one-time, high-consequence choice and your beneficiary users may be older adults who benefit from bigger targets. Selected state = filled dark card; unselected = outlined. (See mockup 5.)

### Setup / onboarding
- Show a step indicator ("Step 2 of 4") so people know how long this takes.
- Before triggering the OS microphone/notification permission dialogs, show a one-line "why we need this" explainer first (pre-permission priming) — improves grant rates and trust.
- The "auto-approve camera requests" toggle is a real privacy tradeoff, not a minor setting — give it a full sentence of explanation inline, and don't default it to on silently; let the person consciously choose.

### Beneficiary dashboard
- The monitoring card exposes "(Local YAMNet)" and a live confidence percentage. Nobody being monitored needs the model name; simplify to "Listening" / "All quiet right now," and put the technical readout behind a "Details" link for your own testing.
- **The simulator buttons are styled in the same solid red as a real emergency.** At a glance — especially for an older adult, or anyone already anxious — a red "Glass Break" test button and an actual glass-break alert are visually identical. This is the single most important fix in this audit: collapse the simulator into "Developer & test tools," and if it's visible at all, style it neutral with a small "TEST" badge, never red.
- "Stop" styled in red for a completely neutral action (pausing monitoring) creates the same kind of clash — reserve red for actual danger; use a neutral outline button for Stop/Pause.
- The header text "Beneficiary Mode" wraps to a second line next to the avatar at the width shown in your screenshot — shorten to just "Beneficiary."
- Consolidate the caregiver list, the chat card, and the demo preview into one caregiver row per person (avatar, name, "Primary" chip, call icon, kebab menu); tapping the row opens chat.

### Caregiver dashboard
- The "Connect" button and code input render in what looks like a disabled/grayed style with no visible reason — reads as broken. Keep inputs in a normal enabled appearance by default; only gray out the button when the field is actually empty.
- The beneficiary's phone number wraps mid-digit ("+658768" / "0000") — give it its own full-width line rather than sharing a line with the name.
- Same consolidation applies: one row per monitored beneficiary, with a status chip ("All quiet" / "Awaiting response") so you can triage without opening each chat.

### Chat screen
- Biggest fix: incident bubbles show the raw backend state (`waiting_user`) as literal text. Map every state to a plain-language label and a color-coded chip — see `SoundGuardTheme.kt` for a ready `IncidentStatus.label()` / `.textColor()` pattern.
- All incident bubbles currently look the same regardless of urgency. Color-code by state — this is exactly where the reserved red/amber/green palette earns its keep — so a caregiver can scan the whole thread and immediately see what's resolved vs. still open.
- Keep the "(Simulated)"/"(Test)" tag, but make it a distinct neutral badge, separate from the severity color, so a test entry is never mistaken for real, even after the fact.
- Show the verification photo as an actual inline thumbnail, not just a text link — no extra tap needed to see there's a photo worth opening.
- Add a persistent banner at the top of the thread while an incident is active ("Active incident · escalates in 1:42") so the caregiver doesn't have to scroll to find current status — a genuinely valuable addition, not just style.
- On the caregiver side, give **Call** and **Request Photo** clearly different visual weight depending on which you want to encourage as the default action — right now both read as equally emphasized.

### Settings & profile
- Replace the plain "○ Microphone Permission" / "○ Push Notification Permission" rows with real toggle switches reflecting actual granted/denied state, plus a "Fix in system settings" link for anything denied.
- "Switch Role to Beneficiary" currently has the same visual weight as routine settings, but it's rare and consequence-heavy — move it toward the bottom, keep it outlined rather than filled, and confirm before switching.
- Isolate **Sign out** at the very bottom, below a divider, styled as a red text button — never bundle a destructive/final action in with routine editing.

### Camera readiness test
- The "Back" button visually overlaps the "Camera Readiness Test" title and a recording indicator in your screenshot — very likely a missing status-bar inset (`windowInsetsPadding` for the top bar in Compose). Worth checking on a real device with a display cutout, not just an emulator.
- The bottom "Capture a photo first" disabled-until-ready button is done correctly — reuse that exact pattern elsewhere (e.g., the Connect button, above).

### Verification photo request dialog
- Structurally in good shape (clear question, Decline/Approve). One addition worth making: a visible countdown or urgency indicator, since this dialog appears *during* an active incident and the beneficiary may not realize how time-sensitive the response is.

---

## 4. Bugs spotted directly in the screenshots

| Issue | Where | Fix |
|---|---|---|
| "Beneficiary Mode" wraps to 2 lines | Beneficiary dashboard header | Shorten label to "Beneficiary" |
| Phone number splits mid-digit across two lines | Caregiver dashboard, monitored beneficiary row | Give the phone number its own full-width line |
| "Connect" button + code field look disabled | Caregiver dashboard | Use normal enabled styling by default |
| Back button overlaps screen title | Camera Readiness Test | Add top status-bar inset padding |
| Raw enum text `waiting_user` shown to users | Chat screen | Map to a human label via a status helper (see `SoundGuardTheme.kt`) |
| Simulator buttons colored identically to real alerts | Beneficiary dashboard | Neutral color + "TEST" badge, never red |

---

## 5. Accessibility notes for beneficiary-facing screens

Your beneficiary users may skew older, so it's worth treating that dashboard differently from the caregiver one:
- Minimum 16sp body text, 20–24sp for anything status-related ("Listening," "All quiet").
- 48dp minimum tap targets, and make the *entire* row/card tappable rather than just a small icon.
- Avoid all-caps micro-labels (harder to read at a glance for low vision).
- High contrast is actually the strongest argument *for* your original black-and-white instinct — lean into it hardest on this specific dashboard, while still reserving the incident colors.

---

## 6. Files included

- **`soundguard-redesign-mockups.html`** — open in any browser. Interactive: the "Developer & test tools" section actually expands/collapses, the settings toggles actually switch, and the role-selection cards actually show a selected state. Covers the beneficiary dashboard, caregiver dashboard, chat, settings, and role selection.
- **`SoundGuardTheme.kt`** — the color tokens above as Compose `Color` values, plus an `IncidentStatus` enum with `.label()` and `.textColor()`/`.surfaceColor()` helpers that solve the raw-enum-in-chat problem directly in code. Rename the enum cases to match whatever your actual state machine calls them.
