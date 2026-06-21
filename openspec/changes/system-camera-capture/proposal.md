## Why

The app ships an embedded CameraX capture screen (~640 lines: `CameraScreen.kt` + `camera/CameraManager.kt` + 5 CameraX artifacts). It is pure maintenance overhead for a note-taking app — it recently shipped a silent-video bug (missing `withAudioEnabled()`) and re-implements what the system camera already does better. The "restrained, Unix-philosophy" direction this codebase has committed to says don't keep entities you don't need; the embedded camera is one.

## What Changes

- **BREAKING**: Photo capture delegates to the system camera via `MediaStore.ACTION_IMAGE_CAPTURE` + `EXTRA_OUTPUT`, writing into the pre-allocated month-bucket file. No in-app preview/capture surface.
- **BREAKING**: Video capture delegates to the system camera via `MediaStore.ACTION_VIDEO_CAPTURE` + `EXTRA_OUTPUT`, writing into the pre-allocated month-bucket file. Audio is the system camera's responsibility (fixes the silent-video regression structurally).
- Remove `CameraScreen.kt`, `camera/CameraManager.kt`, the `camera` package, the `Screen.Camera` nav route, and the `cameraManager` plumbing through `MainActivity` / `MindDumpNavGraph`.
- Remove all CameraX dependencies (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-video`; `camerax = "1.4.1"`).
- Reuse the existing `FileProvider` (`${applicationId}.fileprovider`, `file_paths.xml` already exposes the external root) to mint the `EXTRA_OUTPUT` content URI.
- The `CAMERA` runtime-permission request is re-scoped: the launcher still gates the first photo/video capture, but it now fires the system-camera intent rather than navigating to an embedded screen.
- After the system camera returns a result, the existing `reconcileWithDisk` path indexes the file — no new indexing logic. File naming, paths, and `video-playback` are unchanged.

## Capabilities

### New Capabilities
<!-- None — this replaces, not introduces. -->

### Modified Capabilities
- `camera-capture`: rewritten from an embedded-CameraX capture-screen contract to a system-camera-delegation contract. The theme/shutter/timer/flash/switch-camera requirements (which were all about the embedded UI) are removed; the new requirement is "launch the system camera, write the result into the app's storage, index it."
- `app-shortcuts`: the "Take photo" shortcut still works but its wording shifts from "opens camera capture" to "launches the system camera"; behavior is otherwise unchanged (still captures into the current space). Record-audio is unaffected.

## Impact

- **Code removed**: `app/src/main/java/com/chin/minddump/ui/CameraScreen.kt`, `app/src/main/java/com/chin/minddump/camera/CameraManager.kt` (and the `camera/` package). ~640 lines.
- **Code modified**: `MainActivity.kt` (drop `cameraManager`), `MindDumpNavGraph.kt` (drop `Screen.Camera` + `cameraManager` param + the photo/video file pre-set), `MainScreen.kt` (the camera launcher now fires the system intent), `MindDumpViewModel.kt` (`getPhotoFile`/`getVideoFile` remain; the launcher callback triggers reconcile).
- **Dependencies**: drop the 5 `androidx.camera` artifacts and the `camerax` version ref from `gradle/libs.versions.toml` + `app/build.gradle.kt`. APK shrinks.
- **Risk**: `ACTION_VIDEO_CAPTURE` has no guaranteed handler on a handful of custom ROMs without a system camera — acceptable for this app, and `resolveActivity` is checked before launch to fail gracefully. UX shifts from in-app camera to a system-app handoff (the expected cost of this change).
- **Unchanged**: `file-metadata-parsing`, `file-naming-format`, `entry-card`, `video-playback`, `encryption-transparent-storage` (file format/path are identical; only the producer changes).
