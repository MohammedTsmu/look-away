# Changelog

## Versioning scheme

`MAJOR.MINOR.PATCH`

- **MAJOR** — release generation. `1` = first public release.
- **MINOR** — number of distinct user-facing **features** in the app.
- **PATCH** — number of **enhancement / fix passes** applied on top of features.

So the version literally encodes the scope: `1.21.12` = generation 1, **21 features**,
**12 enhancement passes**. `versionCode` is `MAJOR*10000 + MINOR*100 + PATCH`.

## Feature inventory (21)

| # | Feature |
|---|---------|
| 1 | 20·20·20 reminder engine (work/break countdown state machine) |
| 2 | Background foreground service + live status notification |
| 3 | Full-screen break experience (countdown ring, sound, vibration, screen dim, strict mode) |
| 4 | Auto-start after reboot |
| 5 | Start when the app opens |
| 6 | Reliability watchdog (AlarmManager revives the service) |
| 7 | Configurable settings persisted with Jetpack DataStore |
| 8 | Quiet hours (with past-midnight wrap) |
| 9 | Theming — system / light / dark + 6 accent colors |
| 10 | Guided permission onboarding |
| 11 | Pause & resume video/music on breaks (MediaSession + audio-focus fallback) |
| 12 | Timed pauses that auto-resume (30 m / 1 h / 2 h / until morning) |
| 13 | "Eye-care is off" reminder nudge so it's never silently forgotten |
| 14 | Quick Settings tile (one-tap start / toggle pause) |
| 15 | Home-screen widget (Start / Pause / Resume / Stop) |
| 16 | Arabic localization + in-app language switch (System / English / العربية) with RTL |
| 17 | Selectable break sound (pick any notification tone) |
| 18 | Mindful-usage reminder — gentle, dismissible nudge to step away after a configurable amount of daily screen time (Usage Access; off by default) |
| 19 | Per-app daily soft limits (pick apps, set a limit, gentle nudge when exceeded — no blocking) + a "screen time today" stat on the home screen |
| 20 | Weekly screen-time summary — a 7-day mini bar chart on the home screen |
| 21 | Focus session — mute usage & app-limit nudges for 30 min / 1 h / 2 h |

## Enhancement passes (3)

| # | Enhancement |
|---|-------------|
| 1 | Lint fixes & API-guard hardening (notification-permission guards, Android-14 foreground-service-type handling, modern back-press, obsolete-SDK cleanup) |
| 2 | Unit tests + GitHub Actions CI + testing docs |
| 3 | Audio-focus fallback so players without a media session (e.g. Cinemana) also pause/resume |
| 4 | Screen-off awareness — only count screen-on time; no breaks while locked/asleep; ongoing status hidden from the lock screen |
| 5 | Full-screen break reliability (request USE_FULL_SCREEN_INTENT) + removed the forced screen-brightness change during breaks |
| 6 | Live settings (interval changes restart the countdown immediately) + overlay-window break that shows full-screen even on OEMs that block background activity starts (MIUI) |
| 7 | Break is never invisible — always post the break notification as a fallback even when the overlay path is taken/fails; service owns sound/vibration (no double feedback) |
| 8 | Layout direction (RTL/LTR) flips instantly on language change, driven from the active locale |
| 9 | Overlay break stability — non-focusable overlay window + quiet companion notification so it no longer flickers/drops behind; app icons in the picker and limit lists |
| 10 | App picker lists all installed apps (declared launcher `<queries>` for Android 11+ package visibility) |
| 11 | App limits get a full-screen reminder (Snooze 5 min / Dismiss) shown only while you're in the over-limit app; usage monitoring now runs in its own service loop so limits work even when the 20-20-20 reminder is off |
| 12 | App-limit Snooze vs Dismiss now behave distinctly — Snooze gives true timed quiet; Dismiss is per-visit so reopening the app re-shows the reminder (no sneaking back) |

---

## 1.21.12

- **Smarter Snooze vs Dismiss on app-limit reminders:**
  - **Snooze 5 min** = real quiet for 5 minutes that survives switching apps.
  - **Dismiss** = quiet for *this visit only*. Before, dismissing let you close and
    reopen the app freely for 15 minutes; now **reopening the app brings the
    reminder back**, so you can't sneak past the limit by leaving and returning.
    (Staying in the app after Dismiss keeps it quiet until you leave.)

---

## 1.21.11

- **Stronger app-limit handling (still no blocking):** when you're **using** an
  app that's over its daily limit, Look Away now shows a calming **full-screen
  reminder** with the app name, how long you've used it, and **Snooze 5 min** /
  **Dismiss**. It only appears while you're actually in that app, auto-disappears
  when you leave it, and won't reappear for a few minutes after you act. (Needs
  "Display over other apps"; falls back to a notification otherwise.)
- **Limits run even when eye-care is off:** usage/app-limit monitoring moved to
  its own background loop, so it keeps working even if you Stop the 20-20-20
  reminder. The ongoing notification then reads "Watching your screen time".
  Enabling Mindful usage or adding an app limit starts it automatically (and it
  resumes on boot); turning everything off releases it.
- A Focus session still mutes all of this.

---

## 1.21.10

- **Fix — the per-app-limit picker only showed a few apps:** Android 11+ hides
  most installed apps from other apps by default (package visibility). Added a
  launcher `<queries>` declaration so the picker now lists **all** your launchable
  apps — without the Play-policy-sensitive `QUERY_ALL_PACKAGES` permission.

---

## 1.21.9

- **New — Weekly screen-time summary:** a small 7-day bar chart on the home
  screen (when Usage Access is granted), so you can see your trend at a glance.
- **New — Focus session:** tap **Focus → Start** and pick 30 min / 1 h / 2 h to
  **mute all usage and per-app-limit nudges** for that window — so you can work
  without interruptions. Shows "On until HH:mm" with a Stop button.

## 1.19.9

- **Fix — break overlay flickered / dropped behind other apps:** when the overlay
  showed, the app still posted a *high-importance* break notification, whose
  heads-up popped over the overlay and stole focus (especially on MIUI). Now the
  overlay's companion notification is **quiet** (silent status channel, no
  heads-up), and the overlay window is **non-focusable** so it stops fighting the
  app underneath. Touches on Skip still work; the break stays put.
- **App icons** now appear in the app picker and the per-app limit list, instead
  of bare package names.

---

## 1.19.8

- **New — Per-app daily limits (gentle):** pick specific apps and set a daily
  time limit each; when you pass a limit, a soft, dismissible notification nudges
  you ("You've spent 1h on YouTube today — time for a real break?"). No blocking,
  one nudge per app per day. Manage them in Settings → Mindful usage → Per-app
  limits, with an app picker.
- **New — "Screen time today" on the home screen:** when Usage Access is granted,
  the home screen shows today's total screen time at a glance.

## 1.18.8

- **New — Mindful usage reminder (gentle):** an optional, **off-by-default**
  reminder that watches today's total screen time (via Usage Access) and shows a
  soft, dismissible notification — "You've been on screen Xh today, maybe step
  away" — after a threshold you set, then repeats every N minutes of further use.
  No blocking, no nagging. Settings → Mindful usage.

## 1.17.8

- **Fix — switching language didn't flip RTL/LTR instantly:** the text changed but
  the layout direction only updated after leaving and re-entering the screen.
  Layout direction is now derived from the active locale and provided to Compose
  directly, so Arabic ↔ English flips the direction immediately on change.

---

## 1.17.7

- **Fix — break was invisible on some MIUI devices in 1.17.6:** when "Display over
  other apps" was granted, the app took the overlay path but the overlay
  `addView` could fail silently on MIUI, and that branch no longer posted a
  notification — so nothing showed (even though media still paused). Now the
  break notification is **always** posted as a guaranteed-visible fallback, the
  overlay reports success/failure and the code falls back cleanly, and the skip
  button is a plain styled view (no theme dependency) so the overlay builds
  reliably.
- Sound/vibration are now played by the service (single source) instead of the
  break Activity, avoiding any double chime.

---

## 1.17.6

- **Live timing changes:** changing the work interval (or break length) now
  restarts the current countdown immediately, so the main screen reflects it at
  once instead of waiting for the next cycle.
- **Break shows full-screen on Xiaomi/MIUI:** the break is now drawn as an
  **overlay window** (when "Display over other apps" is granted) instead of
  launching an Activity. MIUI blocks background activity starts, which is why the
  break previously appeared only as a notification you had to tap. The overlay is
  honored reliably; the notification path remains a fallback when overlay isn't
  granted.
- **Media still pauses without notification access:** on devices where MIUI
  blocks notification access (a "restricted setting"), video/music is still paused
  during breaks via audio focus — the notification-access grant only adds precise
  control for apps like YouTube.

---

## 1.17.5

- **Arabic + language switch:** full Arabic translation with RTL layout, plus a
  language selector in Settings (System / English / العربية). On Arabic devices
  the app now shows Arabic automatically.
- **Selectable break sound:** "Play sound" toggle now has a **Break sound** row
  that opens the system tone picker — choose any notification sound.
- **Fix — break only appeared as a notification on some devices:** the break can
  now request the *full-screen* permission (USE_FULL_SCREEN_INTENT) so it takes
  over the screen on Android 14+; the onboarding card surfaces it when needed.
  (Granting "Display over other apps" remains the most reliable path on
  locked-down OEMs like MIUI.)
- **Fix — brightness changed during breaks:** removed the forced screen-brightness
  override (it could *raise* brightness in dark rooms). The break screen is
  already a dark, low-strain view.

---

## 1.15.4

- **Fix:** reminders no longer fire while the screen is off/locked. The work
  countdown now freezes when the screen turns off and resumes when it turns back
  on, so only actual screen-on time counts toward a break (eye strain only
  happens while you're looking at the screen). A break in progress is dismissed
  if the screen goes off. New **"Pause when screen is off"** setting (on by default).
- The ongoing status notification is now hidden from the lock screen (still shown
  when unlocked).
- Removed the GitHub Actions workflow (build/lint/test are run locally instead).

---

## 1.15.3

First public release. Everything in the inventory above:

- Automates the 20·20·20 rule entirely in the background; survives reboots.
- Full-screen, calming break with sound, vibration, optional dim and strict mode.
- Pauses video & music during breaks and resumes after (MediaSession + audio focus).
- Never silently off: timed pauses that auto-resume, plus a configurable
  "it's off" reminder.
- One-tap control from a Quick Settings tile and a home-screen widget.
- Material 3 UI, light/dark/accent theming, phone + tablet.
- Privacy-first: **no internet permission** is requested; all state is on-device.
- Verified: `assembleDebug` + `lintDebug` (0 errors) + `testDebugUnitTest` (7 tests) green.
