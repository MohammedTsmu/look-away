# Look Away — 20·20·20 Eye Care

A lightweight Android app that automates the **20-20-20 rule**: every **20 minutes**,
look at something **20 feet** away for **20 seconds**. It runs quietly in the
background, survives reboots, and gently takes over the screen when it's time to
rest your eyes.

> Built for phones **and** tablets. Material 3, Jetpack Compose, no ads, no tracking.

## Features

- **Runs in the background** as a foreground service with a quiet, ongoing status
  notification showing the time until your next break.
- **Full-screen break reminder** with a calming countdown ring, sound, and
  vibration — shown even over the lock screen.
- **Starts after reboot** and **when you open the app** (both optional).
- **Reliable timing** via an AlarmManager watchdog that revives the service if the
  system kills it.
- **Rich settings**:
  - Work interval (1–60 min) and break length (5–120 s)
  - Full-screen break vs. gentle notification
  - **Strict mode** (hide Skip so you actually take the break)
  - Sound, vibration, and screen-dimming toggles
  - **Quiet hours** to pause reminders overnight
  - Start-after-reboot / start-on-open switches
  - Theme (System / Light / Dark) and six accent colors
- **Privacy-first**: everything stays on-device (Jetpack DataStore). No network
  permission is even requested.

## The 20-20-20 rule

Prolonged screen time strains the eyes' focusing muscles and reduces blink rate.
Looking ~6 meters (20 ft) away every 20 minutes relaxes those muscles and is a
widely recommended habit for reducing digital eye strain.

## Tech

- Kotlin · Jetpack Compose · Material 3
- Foreground `specialUse` service + coroutine countdown engine
- AlarmManager watchdog, `BOOT_COMPLETED` receiver
- Jetpack DataStore for settings
- `minSdk 26` · `targetSdk 35`

## Build

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a connected device/emulator
```

The debug build uses the application id `com.eyecare.lookaway.debug` so it can sit
alongside a release install.

## Permissions

| Permission | Why |
|---|---|
| Notifications | Ongoing status + break alerts |
| Display over other apps | Show the full-screen break reliably |
| Exact alarms | Trigger breaks at the right moment |
| Run at startup | Resume after reboot |
| Ignore battery optimization | Avoid being killed in the background |

All are requested with clear explanations the first time you open the app.

## License

MIT — see `LICENSE`.
