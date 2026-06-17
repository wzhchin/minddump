# Design — add-app-shortcuts

## Goal

Add long-press launcher shortcuts that jump straight into the common capture/navigation
actions, routing onto existing entry points, with the smallest footprint possible.

## Non-goals (this batch)

- **Dynamic / pinned shortcuts** updated at runtime (e.g. "recent groups", "resume last
  recording"). Static XML shortcuts cover the 80% case and avoid ShortcutManager runtime
  lifecycle complexity. Pinned shortcuts (add-to-home-screen) are out too.
- **A dedicated compose/quick-capture Activity.** We reuse `MainActivity` + existing
  ViewModel methods. A separate task-root activity for capture is a possible later
  enhancement if launch latency matters, but `singleTask` MainActivity is fast enough now.
- **Changing any capture contract.** The editor/camera/recorder/space-switch paths stay
  exactly as they are; shortcuts are pure triggers.

## Static vs dynamic

- **Static** (`<shortcuts>` in XML): always present, no runtime code, survive reboot
  immediately, appear the moment the app is installed. Max 4–5 shown by launchers
  (the 5th may be folded into a "more" sheet on some OEMs). This is what we ship.
- **Dynamic** (`ShortcutManager`/`ShortcutCompat`): runtime-managed, can reflect state,
  but add lifecycle code and rank/limits management. Deferred.

We keep the count to **5**: New text, Take photo, Record audio, Open Public, Open
Private. (Some launchers cap visible static shortcuts at 4; if so, the 5th still exists
and is reachable via the system's "shortcuts" long-press expanded view — acceptable.)

## Intent model

Each shortcut is a `<shortcut>` whose inner `<intent>` targets `MainActivity` with:

| Shortcut        | Action                                  | Effect                                  |
|-----------------|-----------------------------------------|-----------------------------------------|
| New text        | `com.chin.minddump.action.NEW_TEXT`     | Open fullscreen editor in new-entry mode|
| Take photo      | `com.chin.minddump.action.PHOTO`        | Open camera capture in current space    |
| Record audio    | `com.chin.minddump.action.RECORD`       | Start an audio recording                |
| Open Public     | `com.chin.minddump.action.OPEN_PUBLIC`  | Switch active space to Public           |
| Open Private    | `com.chin.minddump.action.OPEN_PRIVATE` | Request Private (routes through gate)   |

All use `android:targetPackage="${applicationId}"` and `targetClass=".MainActivity"`.
The custom actions get `<intent-filter>` blocks on the launcher activity so Android
resolves them. `MainActivity` is `singleTask`, so:

- **Cold start** → `onCreate` receives the launching `Intent`; dispatch there.
- **Warm start** (app already foreground/alive) → `onNewIntent`; dispatch there.

A single private `dispatchShortcut(intent)` method handles both, so behavior is
identical regardless of process state.

## Dispatch mapping (reuses existing entry points)

| Action          | ViewModel / UI call                                                     |
|-----------------|-------------------------------------------------------------------------|
| NEW_TEXT        | `onNavigateToFullscreenEdit(null)` — same path as the inline "fullscreen edit" button today. Lands the cursor in the editor; submit uses the existing `submitText`/save flow. |
| PHOTO           | Set a "pending camera capture" signal the UI consumes on resume (mirror how the camera is launched from `MainScreen`), targeting `currentSpace`. |
| RECORD          | `startRecording()` (UI then drives stop/save via the existing recorder). |
| OPEN_PUBLIC     | `switchToPublic()` (no auth).                                           |
| OPEN_PRIVATE    | `switchToPrivate()` → existing biometric/password gate; never bypassed. |

### Routing note: capture happens after the screen composes

Camera/record can't fire before the Compose tree exists (permission launchers and the
camera controller live in composables). So for PHOTO/RECORD we set a **one-shot pending
intent flag** on the ViewModel/UI state at dispatch time, and the relevant composable
consumes it in a `LaunchedEffect` on first composition (exactly the pattern a
"deep-link-then-act" needs). This avoids reimplementing capture outside Compose.

NEW_TEXT and the space-switch actions are pure state calls — no such staging needed.

## Space choice for capture shortcuts

Photo/Record target the **current space** (default Public on a fresh launch). We do not
add per-shortcut "capture into Private" variants in this batch, to avoid implying a
Private write that bypasses the gate. The Open-Private shortcut lets the user unlock
first, after which capture naturally targets Private. Simple and safe.

## Icons and labels

- **Labels**: zh-CN + en strings (`shortcut_new_text`, `shortcut_photo`,
  `shortcut_record`, `shortcut_open_public`, `shortcut_open_private`).
- **Icons**: reuse existing vector drawables or simple monochrome vectors. Shortcut icons
  must be `android:drawable`; we keep them simple (no per-density bitmaps) to stay cheap.
- **Short labels** used where launchers show a truncated form.

## Edge cases

- **Permission not yet granted** for camera/mic: the existing permission-request flow
  fires when the staged capture is consumed; the user is not silently dropped. The
  shortcut still opens the app and the permission dialog appears as usual.
- **Work directory not configured / storage permission missing**: capture/write falls into
  the same empty-state/permission flow the app already shows. No special shortcut handling.
- **Private locked on OPEN_PRIVATE**: the gate appears; nothing is revealed until unlock.
- **Action replayed via onNewIntent while a dialog is open**: dispatch is idempotent enough
  (switching to the already-current space is a no-op; opening the editor when already open
  is acceptable). We do not over-engineer de-dup for this batch.

## Verification hooks

- Long-press icon → all shortcuts appear.
- Each shortcut lands on the expected screen/action from both cold and warm start.
- OPEN_PRIVATE always goes through the biometric/password gate.
- ktlint/detekt clean; `assembleRelease` builds.
