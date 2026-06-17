# Tasks — add-app-shortcuts

## 1. Manifest

- [x] 1.1 Add `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />` inside the launcher `<activity>` (`MainActivity`).
- [x] 1.2 Add `<intent-filter>` blocks on `MainActivity` for the five custom actions: `com.chin.minddump.action.NEW_TEXT`, `…PHOTO`, `…RECORD`, `…OPEN_PUBLIC`, `…OPEN_PRIVATE` (with `android:exported="true"` so the launcher can send them; category DEFAULT).

## 2. Static shortcuts resource

- [x] 2.1 Create `res/xml/shortcuts.xml` with 5 `<shortcut>` entries (enabled, with `shortcutId`, `shortcutShortLabel`, `shortcutLongLabel`, `icon`, and an inner `<intent>` per the design table).
- [x] 2.2 Reuse/create simple vector icons for each shortcut (or a shared set); confirm they resolve as `android:drawable`.

## 3. Strings

- [x] 3.1 zh-CN `res/values/strings.xml`: `shortcut_new_text`, `shortcut_photo`, `shortcut_record`, `shortcut_open_public`, `shortcut_open_private` (short + long labels).
- [x] 3.2 Matching en `res/values-en/strings.xml`.

## 4. Dispatch in MainActivity

- [x] 4.1 Add a private `dispatchShortcut(intent: Intent?)` that maps the five actions to calls, invoked from both `onCreate` (after `setContent`) and `onNewIntent`.
- [x] 4.2 NEW_TEXT → open fullscreen editor in new-entry mode (call the existing `onNavigateToFullscreenEdit(null)` equivalent at activity/nav level).
- [x] 4.3 OPEN_PUBLIC → `switchToPublic()`; OPEN_PRIVATE → `switchToPrivate()` (existing gate).
- [x] 4.4 PHOTO / RECORD → set a one-shot pending-action flag on the ViewModel/UI state for the composable to consume via `LaunchedEffect` (staged capture pattern).
- [x] 4.5 Keep existing share-intent handling intact; ensure dispatch does not interfere with `ACTION_SEND`/`ACTION_SEND_MULTIPLE`.

## 5. Consume staged capture in UI

- [x] 5.1 In `MainScreen` (or the relevant composable), observe the pending camera/record action and consume it once: launch camera capture / start recording, then clear the flag.
- [x] 5.2 Confirm permission prompts still fire normally when consumed without prior grants.

## 6. Verify

- [x] 6.1 `./gradlew detekt ktlintCheck` clean.
- [x] 6.2 `./gradlew assembleRelease` builds.
- [ ] 6.3 Manual (device/emulator): long-press icon → 5 shortcuts; NEW_TEXT opens editor; PHOTO opens camera; RECORD starts mic; OPEN_PUBLIC switches; OPEN_PRIVATE shows the biometric/password gate; works from both cold and warm start.
