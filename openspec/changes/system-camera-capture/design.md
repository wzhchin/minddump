## Context

MindDump today embeds a CameraX capture stack: `CameraScreen.kt` (446 lines, the in-app preview/shutter/timer/flash/switch UI) and `camera/CameraManager.kt` (197 lines, CameraX preview/photo/video wiring), plus five `androidx.camera` artifacts (`camera-core`, `-camera2`, `-lifecycle`, `-view`, `-video`, all 1.4.1). The codebase has recently committed to a "restrained, Unix-philosophy" direction (see `restrained-file-storage`): don't carry machinery the OS already provides. The embedded camera is such machinery and just produced a real bug (silent video — missing `withAudioEnabled()`).

Photo/video files already have a stable home: `FileStorageEngine.getPhotoFile`/`getVideoFile` allocate a `{ts}-f.{jpg,mp4}` in the current space's month bucket (or the open group's directory). The FileProvider is already declared (`${applicationId}.fileprovider`, `file_paths.xml` exposes the external root) and is used by outbound share.

## Goals / Non-Goals

**Goals:**
- Photo and video capture work via the system camera, writing into the same pre-allocated month-bucket files, so the rest of the pipeline (naming, grouping, FTS, playback, encryption) is untouched.
- Delete the embedded CameraX stack and its dependencies; shrink the APK and the maintenance surface.
- Keep the Take-photo launcher shortcut working.

**Non-Goals:**
- No in-app camera preview, shutter, recording timer, flash control, or camera switching. Those become the system camera's job.
- No change to file format, naming, paths, indexing, or playback.
- No support for devices with no system camera at all (handled by a graceful message, not a fallback camera).

## Decisions

### Decision 1: `EXTRA_OUTPUT` + a pre-allocated file, not `onActivityResult` data

The launch passes `EXTRA_OUTPUT` as a `content://` URI (via the existing FileProvider) pointing at the pre-allocated file, rather than reading a thumbnail from the returned intent `data`. With `EXTRA_OUTPUT`, the system camera writes the full-resolution capture straight to that URI, which already lives in the app's month-bucket path — so the file lands exactly where `getPhotoFile`/`getVideoFile` already put it, and no copy/move step is needed.

**Why not read the result from `intent.data`:** the returned data is a low-res thumbnail. `EXTRA_OUTPUT` is the only way to get the full capture into a chosen location, and it avoids an extra file copy.

**URI from FileProvider, not `file://`:** `file://` URIs trigger `FileUriExposedException` on API 24+; FileProvider is the supported path and already exists here.

### Decision 2: One launcher per media type, keyed on the pre-allocated file

Each capture sets `EXTRA_OUTPUT` to the URI of `getPhotoFile()` / `getVideoFile()` (resolved for the current space + open group dir), launches the matching intent, and on success calls the existing reconcile/refresh path (`onMediaCaptured` → `refreshForCurrentScope`). The file's existence and mtime are the signal — no capture-specific indexing code.

### Decision 3: Cancelled captures are cleaned up

Some system-camera implementations create the `EXTRA_OUTPUT` file (empty) even when the user cancels. On a non-OK result, the launcher deletes the pre-allocated file if it exists and is empty, so a cancelled capture never becomes a phantom 0-byte entry after the next reconcile.

### Decision 4: `resolveActivity` guard + permission check before launch

Before launching, the app checks `resolveActivity` for the intent; if null (no camera app), it shows a short toast and does nothing — this covers the rare custom-ROM-without-video-capture case without a crash. The `CAMERA` permission request stays (gated on the launcher, as today) because several stock cameras check it even under `EXTRA_OUTPUT`; if the user denies, no launch happens.

**Alternative considered:** rely on the system camera to request its own permissions. Rejected — under `EXTRA_OUTPUT` some devices fail silently without the app holding `CAMERA`, so keeping the app-level request is the safe default.

### Decision 5: Drop CameraX entirely, not just hide it

Delete the source files, the `camera/` package, the `cameraManager` plumbing through `MainActivity`/`MindDumpNavGraph`, the `Screen.Camera` route, and all five `androidx.camera` artifacts + the `camerax` version ref. Keeping the dependency while not using it would defeat the size/maintenance goal.

## Risks / Trade-offs

- **[UX shift: in-app camera → system-app handoff]** → Accepted cost; explicitly chosen. Mitigation: the capture still writes into the same place and returns to the same feed.
- **[`ACTION_VIDEO_CAPTURE` has no handler on some custom ROMs]** → `resolveActivity` guard + toast. Acceptable for a notes app; users with a system camera (the overwhelming majority) are unaffected.
- **[System-camera result contract variance]** → A non-OK result cleans up the empty pre-allocated file; an OK result relies on file existence (not the result intent extras), which is the robust signal across OEM cameras.
- **[Take-photo shortcut relies on the same launcher]** → No new surface; the shortcut dispatch already routes to `cameraPermissionLauncher`, which is re-pointed at the system intent.

## Migration Plan

1. Add the system-camera launchers + result handlers in `MainScreen` (photo, video), reusing `getPhotoFile`/`getVideoFile` + FileProvider + reconcile.
2. Delete `CameraScreen.kt`, `camera/CameraManager.kt`, the `camera/` package; remove `cameraManager` from `MainActivity` + `MindDumpNavGraph`; remove `Screen.Camera` and its composable.
3. Remove the five `androidx.camera` artifacts from `app/build.gradle.kts` and the `camerax` entry from `gradle/libs.versions.toml`.
4. Update the `camera-capture` + `app-shortcuts` specs (this change) and sync them.
5. Verify on device: photo capture, video capture (with sound), cancel, no-camera-app toast.

**Rollback:** revert the commit. The deleted files return; CameraX deps return. No on-disk data changes to roll back (captures land in the same paths).

## Open Questions

- Whether to keep the standalone video capture entry at all (it is rarely the point of a notes app). Out of scope here — this change preserves parity (photo + video) and leaves that product question to ROADMAP.
