# Implementation Tasks

## 1. Dependencies & image pipeline (video preview fix)

- [x] 1.1 Add `coil-video` library alias to `gradle/libs.versions.toml` (group `io.coil-kt.coil3`, name `coil-video`, reuse existing `coil` = 3.3.0 version ref).
- [x] 1.2 Add `implementation(libs.coil.video)` to `app/build.gradle.kts` (next to the other coil lines).
- [x] 1.3 Provide an app-scoped `ImageLoader` — implemented via Coil 3's `SingletonImageLoader.Factory` on `MindDumpApp` (the mechanism AsyncImage actually resolves; a Hilt-provided loader would NOT be discovered by AsyncImage). Registers `VideoFrameDecoder.Factory()` + `crossfade(true)`.
- [x] 1.4 Verify `AsyncImage` in `EntryItem.VideoEntryContent` and `GroupMediaCarousel` now renders a decoded first-frame thumbnail for `.mp4` entries (no call-site change needed; confirmed on device).

## 2. Capture screen — M3 Expressive rebuild (`ui/CameraScreen.kt`)

- [x] 2.1 Replace the `FilterChip` Photo/Video pair with an M3 `SegmentedButton` row; keep `isVideoMode` local state as the source of truth; selecting Photo cancels `isRecordingVideo`.
- [x] 2.2 Build the shutter button on expressive tokens: `buttonPill` shape, default capture affordance; in video/record state render an animated recording ring (red) that fills/scales while `isRecordingVideo`.
- [x] 2.3 Add a live elapsed timer (`mm:ss`) shown only while recording, via a `produceState`/coroutine keyed on `isRecordingVideo`; reset to 00:00 on stop.
- [x] 2.4 Add a top bar with close (left, releases preview + `onClose`) and a flash toggle (right); wire flash through `CameraManager` if supported, else make it a UI-only toggle with a TODO (do not crash if unsupported).
- [x] 2.5 Restyle the switch-camera affordance to match; call `cameraManager.switchCamera()`.
- [x] 2.6 Apply `rememberPremiumHaptics` to shutter press, mode switch, and camera flip (`HapticPattern.Tick`); apply `LocalAnimationDuration`/`LocalMotionCurve` to state transitions.

## 3. Strings (locale parity)

- [x] 3.1 Add zh-CN strings in `res/values/strings.xml` for new camera UI: recording timer label, flash on/off, photo/video segments, close/switch (reuse existing where present).
- [x] 3.2 Add matching en strings in `res/values-en/strings.xml`.

## 4. Verification & lint

- [x] 4.1 Build the app and confirm no compile errors from the new dependency / `SegmentedButton` APIs.
- [x] 4.2 Run `./gradlew detekt ktlintCheck`; fix any reported issues.
- [x] 4.3 On-device smoke test: (a) video entry shows a thumbnail in feed + group carousel; (b) capture screen renders with new chrome, timer counts during record, segments/haptics/flip work; (c) tapping a video cover opens the in-app player — verified on device.

## 5. In-app video playback (Media3)

- [x] 5.1 Add a `media3` version pin (`1.x` stable) + `media3-exoplayer` and `media3-ui` library aliases to `gradle/libs.versions.toml`; both must share the version.
- [x] 5.2 Add `implementation(libs.media3.exoplayer)` + `implementation(libs.media3.ui)` to `app/build.gradle.kts`.
- [x] 5.3 Create `ui/components/VideoPlayerDialog.kt`: a full-screen `Dialog` (black, `usePlatformDefaultWidth = false`) hosting a `PlayerView` over an `ExoPlayer` created via `remember`, playing `MediaItem.fromUri(Uri.fromFile(file))`; `DisposableEffect` calls `release()` on dispose; `useController = true`.
- [x] 5.4 Wire `VideoEntryContent` (`ui/EntryItem.kt`): tap the cover (thumbnail/play overlay) toggles local `showPlayer` state and renders `VideoPlayerDialog`.
- [x] 5.5 Wire `GroupMediaCarousel` (`ui/EntryItem.kt`): tapping a video tile opens `VideoPlayerDialog` for that member (remember selected index/path in composable state).
- [x] 5.6 Add player strings (zh-CN + en): none needed — `PlayerView` ships its own controller resources; no new user-facing strings.
- [x] 5.7 Build + `./gradlew detekt ktlintCheck`; fix issues (Media3 `PlayerView` interop, unused imports, detekt forbidden tokens).
