## REMOVED Requirements

### Requirement: Capture screen follows the expressive theme system

**Reason:** The embedded CameraX capture screen is removed; photo/video capture is delegated to the system camera app. There is no in-app capture surface to theme, so the expressive-theme requirement for capture controls no longer applies.

**Migration:** No user-facing data migration. Users who captured via the embedded screen lose the in-app capture UI; capture continues to work via the system camera. Stored files are unaffected (same format, same month-bucket paths).

### Requirement: Photo and video mode selection

**Reason:** Mode selection (Photo/Video segmented control) was a property of the embedded capture screen. The system camera provides its own capture modes, so the app no longer exposes a mode toggle.

**Migration:** None — users pick photo vs. video inside the system camera.

### Requirement: Video recording shows an elapsed timer and recording state

**Reason:** The in-app elapsed timer and recording shutter state were rendered by the embedded screen. The system camera shows its own recording UI, so the app no longer renders a capture timer.

**Migration:** None.

### Requirement: Capture screen chrome

**Reason:** Close, flash-toggle, and camera-switch chrome were elements of the embedded capture screen. The system camera provides these controls, so the app no longer renders capture chrome.

**Migration:** None — close/flash/switch-camera are handled by the system camera app.

## ADDED Requirements

### Requirement: Capture delegates to the system camera

Photo and video capture SHALL be performed by the system camera application. The app SHALL launch the system camera via `MediaStore.ACTION_IMAGE_CAPTURE` (photo) or `MediaStore.ACTION_VIDEO_CAPTURE` (video) with `EXTRA_OUTPUT` set to a content URI for a pre-allocated file in the current space's month-bucket directory. The app SHALL NOT render its own preview, shutter, or recording UI.

#### Scenario: Photo capture writes into the app's storage
- **WHEN** the user starts a photo capture from the input bar or the Take-photo shortcut
- **THEN** the app launches the system camera in still-image mode, with `EXTRA_OUTPUT` pointing at a pre-allocated `.jpg` file under the current space's current month directory
- **AND** when the system camera returns successfully, the captured image exists at that file path and is indexed into the feed by reconciliation

#### Scenario: Video capture writes into the app's storage
- **WHEN** the user starts a video capture
- **THEN** the app launches the system camera in video mode, with `EXTRA_OUTPUT` pointing at a pre-allocated `.mp4` file under the current space's current month directory
- **AND** when the system camera returns successfully, the captured video exists at that file path (with audio, as produced by the system camera) and is indexed into the feed by reconciliation

#### Scenario: Capture is cancelled
- **WHEN** the user cancels (backs out of) the system camera without producing a capture
- **THEN** no file is indexed and the pre-allocated file (if created but left empty) is removed so it does not appear as a phantom entry

### Requirement: Capture result is reconciled from disk

The app SHALL treat the captured file as the source of truth and index it via the existing disk-reconciliation path on return from the system camera, rather than via a dedicated capture-only code path. The file naming, month-bucket placement, and per-space routing are unchanged from non-camera captures.

#### Scenario: Indexing on return
- **WHEN** the system camera returns a successful result for a photo or video
- **THEN** the app reconciles the space with disk, and the new file appears in the feed under the same naming, ordering, and grouping rules as any other entry

### Requirement: Graceful failure when no system camera is available

Because `ACTION_VIDEO_CAPTURE` (and, rarely, `ACTION_IMAGE_CAPTURE`) is not guaranteed to have a handler on every device, the app SHALL check that an activity can resolve the capture intent before launching it, and SHALL surface a brief user-facing message instead of crashing when no handler exists.

#### Scenario: No camera app installed
- **WHEN** the user starts a capture and no activity resolves the capture intent
- **THEN** the app shows a short toast/message indicating the system camera is unavailable, and launches nothing

#### Scenario: Camera permission denied
- **WHEN** the user denies the `CAMERA` runtime permission
- **THEN** the system camera intent is not launched and the user remains on the feed
