## Why

Two visible gaps in the capture/preview flow undermine the otherwise-polished M3 Expressive UI:

1. **Videos show no preview.** Video cards (`EntryItem.VideoEntryContent`) and group media carousels render `.mp4` files through Coil's `AsyncImage`, but the app ships **no `VideoFrameDecoder`** (`coil-video` is absent, and there is no custom `ImageLoader`). Coil can't decode a video frame, so the thumbnail area is blank — only the play overlay floats over black/empty space. This directly violates the `entry-card` requirement that a video body shows "the video thumbnail in a large rounded media region with a play overlay."
2. **The capture screen is the only screen that ignores the design system.** `CameraScreen` stacks raw `FilterChip` + `FilledIconButton` with hardcoded 32/64dp sizes, no expressive shape tokens, no haptics, no motion curves, no recording timer, and no proper top bar. Every other screen already follows the `expressive-theme` system.

## What Changes

- **Add video frame decoding to the image pipeline.** Bring in Coil's `coil-video` module and register `VideoFrameDecoder.Factory` on the app-wide singleton `ImageLoader` (via `SingletonImageLoader.Factory` on the `Application` — the one mechanism `AsyncImage` actually resolves). Video entries and group carousels then render a real first-frame thumbnail instead of blank space.
- **Redesign the camera/capture screen to M3 Expressive.** Full rebuild on the existing design tokens: expressive shape tokens (`buttonPill`, `buttonRounded`, `cardSmall`), premium haptics (`HapticPattern`), motion (`LocalAnimationDuration` / `LocalMotionCurve`), a top app bar with close + flash, a pill shutter button with a recording ring animation, a recording timer (elapsed `mm:ss`) shown while recording, and a mode toggle upgraded to an M3 segmented control (Photo / Video). Visual states (recording, switching) get animated transitions.
- **In-app video playback.** *(Added — supersedes the earlier "external viewer" scope.)* Tapping a video cover (entry card or carousel tile) opens a full-screen in-app video player using **Media3 ExoPlayer**, mirroring how tapping a photo cover opens the zoomable image preview — instead of handing off to an external app.

## Capabilities

### New Capabilities
- `camera-capture`: the photo/video capture screen — its layout, controls, visual states (idle / capturing / recording), recording timer, and adherence to the expressive theme system.
- `video-playback`: full-screen in-app video playback — opening from a video cover, player controls, and release-on-dismiss behavior.

### Modified Capabilities
- `entry-card`: the video-entry scenario is amended to require a **decoded first-frame thumbnail** (currently specced but unimplemented — blank), and **tapping the video now opens the in-app player** (previously specced as the external viewer — changed by this revision).

## Impact

- **Dependencies:** add `io.coil-kt.coil3:coil-video` (tracks `coil` 3.3.0) **and** `androidx.media3:media3-exoplayer` + `media3-ui` (playback) to `gradle/libs.versions.toml` + `app/build.gradle.kts`.
- **Image pipeline:** `VideoFrameDecoder` registered via `SingletonImageLoader.Factory` on `MindDumpApp`. No Room/migration impact (filesystem stays source of truth; thumbnail + playback read the file on the fly, nothing persisted).
- **UI code:** rewrite `ui/CameraScreen.kt`; add `ui/components/VideoPlayerDialog.kt`; wire `VideoEntryContent` and `GroupMediaCarousel` (in `ui/EntryItem.kt`) to open the player on tap. Strings: add camera labels (timer, flash) + playback labels to `values/strings.xml` (zh-CN) and `values-en/strings.xml` (en).
- **No data/schema/format change** — UI + dependency additions. Filesystem naming and encryption are untouched.
