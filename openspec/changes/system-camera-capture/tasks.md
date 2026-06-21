## 1. System-camera launchers (photo + video)

- [x] 1.1 In `ui/MainScreen.kt`: add a `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())` for photo capture — on success (`true`), call `viewModel.onMediaCaptured()` (existing reconcile/refresh path); on cancel, delete the pre-allocated photo file if it exists and is empty.
- [x] 1.2 In `ui/MainScreen.kt`: add a `rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo())` for video capture — same success/cancel handling as 1.1 against the pre-allocated video file.
- [x] 1.3 Mint the `EXTRA_OUTPUT` URI via the existing FileProvider (`${applicationId}.fileprovider`, `file_paths.xml` already exposes the external root) from `viewModel.getPhotoFile()` / `getVideoFile()` (resolved for current space + open group dir). Confirm `getPhotoFile`/`getVideoFile` already return files in the right month-bucket path and need no change.

## 2. Launch entry points

- [x] 2.1 Repoint the InputBar `onCameraClick` / `cameraPermissionLauncher` callback in `MainScreen.kt` so that, once `CAMERA` permission is granted, it picks photo vs. video. Decision: short-press the camera button → photo; long-press → video (added `onCameraLongClick` to `InputBarActions`, `combinedClickable` on the button, and a `CaptureKind` enum + `requestCameraCapture` that requests CAMERA then dispatches to the matching launcher).
- [x] 2.2 Before each launch, check `intent.resolveActivity(context.packageManager) != null`; if null, show a short toast ("系统相机不可用" / added `camera_unavailable` string) and launch nothing — do not crash.
- [x] 2.3 Confirm the Take-photo launcher shortcut still works end-to-end: `ShortcutAction.PHOTO` → `requestCameraCapture(PHOTO)` → CAMERA grant → system camera → reconcile.

## 3. Remove the embedded CameraX stack

- [x] 3.1 Delete `app/src/main/java/com/chin/minddump/ui/CameraScreen.kt`.
- [x] 3.2 Delete `app/src/main/java/com/chin/minddump/camera/CameraManager.kt` and remove the `camera/` package directory.
- [x] 3.3 In `MainActivity.kt`: remove the `cameraManager` field and its construction/injection; remove its pass-through to the nav graph.
- [x] 3.4 In `ui/MindDumpNavGraph.kt`: remove the `cameraManager` parameter from `MindDumpNavGraph`, remove the `Screen.Camera` route object + its `composable` block (including the `getPhotoFile`/`getVideoFile` pre-set and `setOutputFiles`), and remove `onNavigateToCamera` wiring (MainScreen no longer navigates to a camera screen).
- [x] 3.5 Remove now-unused imports and the camera-related haptics/shape usages that were CameraScreen-only; verify no dangling references (`CameraScreen`, `CameraManager`, `Screen.Camera`, `cameraManager`).

## 4. Dependencies + manifest

- [x] 4.1 In `app/build.gradle.kts`: remove the five `androidx.camera` implementations (`camerax-core`, `-camera2`, `-lifecycle`, `-view`, `-video`).
- [x] 4.2 In `gradle/libs.versions.toml`: remove the `camerax = "1.4.1"` version ref and the five camera library entries.
- [x] 4.3 Confirm `CAMERA` permission stays declared in `AndroidManifest.xml` (system camera under `EXTRA_OUTPUT` may still need the app to hold it); confirm FileProvider + `file_paths.xml` unchanged (reused, not modified).

## 5. Strings

- [x] 5.1 Add a `camera_unavailable` string (zh-CN "系统相机不可用" / en "System camera unavailable") to `res/values/strings.xml` and `res/values-en/strings.xml`, used by the no-handler toast.
- [x] 5.2 Remove any now-unused strings that were CameraScreen-only (`photo_mode`, `video_mode`, `record_video`, `take_photo`, `switch_camera`, `flash`). Kept `close` (also used by `FullscreenEditScreen`).

## 6. Verify

- [x] 6.1 `./gradlew detekt ktlintCheck` passes.
- [x] 6.2 `./gradlew assembleDebug` builds with no `CameraScreen`/`CameraManager`/`Screen.Camera`/`camerax` dangling references.
- [ ] 6.3 Manual: photo capture → file lands in current month bucket, appears in feed.
- [ ] 6.4 Manual: video capture (long-press) → file lands in current month bucket with audio, appears in feed, plays.
- [ ] 6.5 Manual: cancel system camera → no phantom empty entry appears.
- [ ] 6.6 Manual: Take-photo launcher shortcut → launches system camera into current space.
