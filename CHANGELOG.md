# Changelog

## Versioning scheme

`MAJOR.MINOR.PATCH`

- **MAJOR** — release generation. `1` = first public release.
- **MINOR** — number of distinct user-facing **features** in the app.
- **PATCH** — number of **enhancement / fix passes** applied on top of features.

So the version literally encodes the scope: `1.15.3` = generation 1, **15 features**,
**3 enhancement passes**. `versionCode` is `MAJOR*10000 + MINOR*100 + PATCH`.

## Feature inventory (15)

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

## Enhancement passes (3)

| # | Enhancement |
|---|-------------|
| 1 | Lint fixes & API-guard hardening (notification-permission guards, Android-14 foreground-service-type handling, modern back-press, obsolete-SDK cleanup) |
| 2 | Unit tests + GitHub Actions CI + testing docs |
| 3 | Audio-focus fallback so players without a media session (e.g. Cinemana) also pause/resume |

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
