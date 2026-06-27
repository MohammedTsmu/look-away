# Testing & running Look Away

## What's verified automatically

- `./gradlew testDebugUnitTest` — JVM unit tests for the pure logic
  (quiet-hours window math incl. midnight wrap, clock formatting, settings
  defaults).
- `./gradlew lintDebug` — Android Lint (0 errors).
- `./gradlew assembleDebug` — produces `app/build/outputs/apk/debug/app-debug.apk`.

CI runs all three on every push (see `.github/workflows/android.yml`).

## Try it on a real device (fastest)

1. Enable **Developer options → USB debugging** on the phone/tablet.
2. Plug it in and run:
   ```bash
   ./gradlew installDebug
   ```
   or sideload `app/build/outputs/apk/debug/app-debug.apk` directly.
3. Open the app, grant the permissions on the home card, tap **Start**, then
   **Preview a break now** to see the full-screen break immediately.

## Try it on an emulator

The emulator and a system image aren't installed yet. Install them with the SDK
manager (this is a large download):

```bash
# from the Android SDK cmdline-tools
sdkmanager "emulator" "system-images;android-34;google_atd;x86_64"

# create an AVD (phone)
echo no | avdmanager create avd -n lookaway_phone \
  -k "system-images;android-34;google_atd;x86_64" -d pixel_7

# create an AVD (tablet)
echo no | avdmanager create avd -n lookaway_tablet \
  -k "system-images;android-34;google_atd;x86_64" -d "pixel_tablet"

# launch
emulator -avd lookaway_phone
```

Then `./gradlew installDebug` and launch the app.

### Manual test checklist

- [ ] Start → ongoing notification shows "Next break in m:ss" and counts down.
- [ ] Preview a break now → full-screen countdown ring, sound + vibration.
- [ ] Skip button ends the break early (hidden when **Strict mode** is on).
- [ ] Pause/Resume from the app and from the notification.
- [ ] Settings persist across app restarts.
- [ ] Toggle **Full-screen break** off → break arrives as a heads-up notification.
- [ ] Enable **Start after reboot**, reboot the emulator → reminder auto-starts.
- [ ] Quiet hours window pauses the countdown.
- [ ] Theme + accent changes apply immediately.
- [ ] Enable **Pause video & music on breaks**, grant notification access, play a
      YouTube/Netflix/music clip → it pauses on break start and resumes on break end.
- [ ] Tap **Stop** → bottom sheet offers timed pauses; "Pause 1 hour" shows
      "Paused — resumes at HH:mm" and auto-resumes.
- [ ] **Turn off completely** with "Remind me it's off" on → after the configured
      delay, the "Eye-care is off" nudge appears; its **Turn on** action restarts it.
