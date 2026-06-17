## Why

MindDump's core promise is "start recording a thought within 3 seconds." Today the only entry point is tapping the launcher icon and waiting for the feed to load before the input bar is reachable. Long-pressing the app icon currently offers nothing. Launcher dynamic shortcuts (App Shortcuts) give a zero-screens-deep path straight to the most common capture actions — quick text note, photo, voice recording, and jumping directly into Public or Private — cutting the friction that the ROADMAP (#1) identifies as the lightweight precursor to a desktop widget.

## What Changes

- **Static launcher shortcuts.** A `shortcuts.xml` resource defines five long-press actions: New text note, Take photo, Record audio, Open Public, Open Private. These are *static* shortcuts (defined in XML, always present) — the cheapest, most reliable flavor, which is the right scope for this first batch. Dynamic/pinned shortcuts driven by runtime state are deferred.
- **Custom intent actions + deep-link dispatch.** Each shortcut launches `MainActivity` with a custom action (e.g. `com.chin.minddump.action.NEW_TEXT`, `…PHOTO`, `…RECORD`, `…OPEN_PUBLIC`, `…OPEN_PRIVATE`) and a matching `<intent-filter>` in the manifest. `MainActivity` reads the launching action and routes it to the existing entry points: open the fullscreen editor (new text), trigger the camera capture, start a recording, or switch the active space.
- **Space gating for Private.** "Open Private" routes into the existing biometric/password gate — it never bypasses authentication. The capture shortcuts target the current/last space, never forcing Private without an unlock.
- **`singleTask` preserved.** `MainActivity` is already `singleTask`, so a shortcut delivers its action via `onCreate` (cold start) or `onNewIntent` (warm start); both paths are wired through one dispatcher.

## Capabilities

### New Capabilities
- `app-shortcuts`: the static launcher shortcuts, their intent actions, and the dispatch behavior that routes each shortcut to its capture or navigation entry point.

### Modified Capabilities
- *(none — routing reuses existing capture/space/editor entry points rather than changing their contracts)*

## Impact

- **Manifest**: `AndroidManifest.xml` — add `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />` to the launcher activity, and `<intent-filter>` entries for the custom actions.
- **Resources**: new `res/xml/shortcuts.xml`; shortcut labels/icons in `res/values/strings.xml` (zh-CN) + `res/values-en/strings.xml` (en); reuse existing `ic_launcher` foreground or simple vector icons.
- **Activity**: `MainActivity.kt` — a `ShortcutAction` dispatcher invoked from both `onCreate` and `onNewIntent`, calling existing ViewModel methods (`submitText` path via fullscreen editor open, `startRecording`/capture triggers, `switchToPublic`/`switchToPrivate` with the existing auth gate).
- **No storage/schema/UI-screen changes**: shortcuts only launch; they do not persist anything new. No Room migration.
- **Existing behavior**: unchanged; the default launcher tap still opens the feed as today.
