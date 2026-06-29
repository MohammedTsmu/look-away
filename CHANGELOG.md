# Changelog

## Versioning scheme

`MAJOR.MINOR.PATCH`

- **MAJOR** — release generation. `1` = first public release.
- **MINOR** — number of distinct user-facing **features** in the app.
- **PATCH** — number of **enhancement / fix passes** applied on top of features.

So the version literally encodes the scope: `1.17.7` = generation 1, **17 features**,
**7 enhancement passes**. `versionCode` is `MAJOR*10000 + MINOR*100 + PATCH`.

## Feature inventory (17)

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
