## Context

MindDump captures photos and videos via CameraX (`camera/CameraManager.kt`) and renders them back in the feed (`ui/EntryItem.kt` → `VideoEntryContent`, `GroupMediaCarousel`) using Coil 3's `AsyncImage`. Two problems:

- **No video frame decoding.** `AsyncImage(model = a .mp4 File)` returns nothing because no `VideoFrameDecoder` is registered. Coil 3 splits video-frame decoding into a separate `coil-video` artifact; without it, the `ImageLoader` has no `Fetcher`/`Decoder` that understands video MIME types. Result: blank thumbnails everywhere a video is shown.
- **Capture screen is off-design.** `ui/CameraScreen.kt` is the lone screen that doesn't use the M3 Expressive token system (`LocalExpressiveShapes`, `rememberPremiumHaptics`, `LocalAnimationDuration`, `LocalMotionCurve`). It uses raw chips/buttons with hardcoded sizes, has no recording timer, and no flash control.

The codebase already has the building blocks: a shape token set (`theme/Shape.kt`), haptics (`theme/PremiumHaptics.kt`), motion composition locals, and a Hilt `MediaModule`. The fix is wiring, not new infra.

## Goals / Non-Goals

**Goals:**
- Video entries and group carousels render a decoded first-frame thumbnail.
- The capture screen looks and feels like the rest of the app: expressive shapes, haptics, motion, recording timer, and a modernized control layout.
- Tapping a video cover opens a full-screen **in-app player** (Media3), mirroring how tapping a photo cover opens the zoomable image preview.
- No data, schema, file-naming, or encryption changes. The filesystem stays the single source of truth.

**Non-Goals:**
- Editing/trimming captured video or photo.
- Persisting thumbnails to disk; decode happens on demand.
- Changing the `CameraManager` capture pipeline (CameraX Recorder) itself — only the Compose UI around it.
- Background/audio-only playback, playlists, or casting. Single-file foreground playback only.

## Decisions

### D1. Register `VideoFrameDecoder` on the app-wide singleton `ImageLoader`
Add `coil-video` and implement `SingletonImageLoader.Factory` on `MindDumpApp` (the `Application`), building an `ImageLoader` with `components { add(VideoFrameDecoder.Factory()) }`. In Coil 3, `AsyncImage` resolves its loader through `SingletonImageLoader.get(context)` — **not** the Hilt graph — so this is the one mechanism that actually reaches every `AsyncImage` call site (entry cards, carousel, zoom viewer) with zero edits. (A Hilt-`@Provides ImageLoader`, the literal task 1.3 text, would sit unused and thumbnails would stay blank — discovered at build time; task updated to reflect the correct approach.)

- *Alternative considered:* call-site-only — pass a per-frame thumbnail `ImageRequest`. Rejected: would require touching every `AsyncImage` site and re-implementing caching/decoding wiring Coil already provides.
- *Alternative considered:* generate a `.jpg` thumbnail at capture time via `MediaMetadataRetriever` and persist it. Rejected: adds files to the filesystem (breaks the "filesystem is exactly the entries" invariant and the FTS/scan model), and doubles storage.

Coil's `VideoFrameDecoder` decodes via `MediaMetadataRetriever` and frames the request by default; for entry cards the first frame is the right preview. We pin the frame to a small-ish time so dark intros aren't pure-black — pass `frameMicros = 1_000_000` (1s) via an `ImageRequest` data hint only if needed; default behavior is acceptable first.

### D2. Full M3 Expressive rebuild of `CameraScreen`
Keep the same public surface (`cameraManager`, `onClose`, `onCaptured`) so `MindDumpNavGraph` wiring is unchanged, but rebuild the interior:

- **Top bar:** close (left), flash toggle (right) — `IconButton`s sized to expressive tokens.
- **Mode toggle:** replace `FilterChip` pair with an M3 **`SegmentedButton`** row (Photo / Video).
- **Shutter:** a pill/circle button (`buttonPill`) with an animated recording ring that scales/fills while recording; red ring in video mode.
- **Recording timer:** live `mm:ss` elapsed counter, updated via a coroutine `produceState` keyed on `isRecordingVideo`. Shown only in video recording state.
- **Motion & haptics:** shutter press, mode switch, and camera flip emit `HapticPattern` ticks; transitions use `LocalAnimationDuration`/`LocalMotionCurve`.
- **Switch camera:** retained, restyled.

`isVideoMode`/`isRecordingVideo` local state stays in the composable (as today). The `CameraManager` API is unchanged.

- *Alternative considered:* lift state to the ViewModel. Rejected: capture is transient, screen-local state; no persistence or cross-screen need. Keeping it local matches the current pattern and avoids ViewModel churn.

### D3. No new permissions
`CAMERA` and `MANAGE_EXTERNAL_STORAGE`/`READ_EXTERNAL_STORAGE` are already declared. `VideoFrameDecoder` needs only `READ` of the video file, already granted. No manifest change.

### D4. In-app video playback via Media3 ExoPlayer + a full-screen dialog
Add `androidx.media3:media3-exoplayer` + `media3-ui` and build a `VideoPlayerDialog` Compose overlay (a full-screen `Dialog`, black background, `usePlatformDefaultWidth = false`). It owns one `ExoPlayer`, plays the entry's `File` via a `MediaItem.fromUri(Uri.fromFile(file))`, and **always releases the player on dismiss** (`onDismissRequest` / back) so no media resources leak. Tap a video cover → show this dialog.

- *Trigger location:* mirror the photo pattern. Photos open their viewer **inside** `ZoomableAsyncImage` (component-level), independent of the card's `onEntryClick`. Videos do the same: `VideoEntryContent` and `GroupMediaCarousel` tiles each hold local `showPlayer` state and render `VideoPlayerDialog` on tap. This keeps the player wiring out of the card-level `onEntryOpen` dispatch (which stays text→editor / else→external-open for non-video files).
- *Alternative considered:* route through `onEntryOpen` → navigate to a dedicated `VideoPlayer` nav destination. Rejected: a dialog overlay is simpler, matches the existing image-preview dialog, and avoids a nav-graph route + state plumbing for a transient view.
- *Lifecycle:* `ExoPlayer` is created in a `remember` scoped to the dialog's composition and `release()`d in a `DisposableEffect` onExit — guarantees cleanup regardless of how the dialog closes. `PlayerView` (`media3-ui`) provides transport controls; we keep `useController = true`.

### D5. Media3 version pin
`media3-exoplayer` and `media3-ui` are versioned together as `androidx.media3`. Pin a single `media3` version in `libs.versions.toml` (latest stable `1.x` line) and apply it to both artifacts so they stay in lock-step (media3 modules must share a version).

## Risks / Trade-offs

- **[coil-video pulls `MediaMetadataRetriever`, decode cost on the main list]** → Coil decodes off the main thread and caches by its memory cache; entry cards already use `AsyncImage` so this is the existing contract. Mitigation: rely on Coil defaults; if list scrolling janks on huge videos, set `size()` on the request. Out of scope unless observed.
- **[First frame may be near-black for some videos]** → acceptable for a preview; the play overlay still communicates "video." D1 notes we can pin a 1s frame if it becomes a complaint.
- **[`SegmentedButton` / ring animation are more code than chips]** → the whole point of this change; complexity is contained in one file.
- **[`ImageLoader` must be app-scoped]** → it belongs in `StorageModule` (already `SingletonComponent` + `@Singleton`), not `MediaModule` (which is `ActivityComponent`-scoped for the camera/recorder). Confirmed at design time.
- **[Media3 method/`PlayerView` API churn across versions]** → pin a single `media3` version for both `exoplayer` and `ui` (D5); keep `PlayerView` usage to the documented minimal surface. Mitigation: if the pinned version's `PlayerView` differs, adjust in `VideoPlayerDialog` only.
- **[ExoPlayer resource leak if not released]** → `DisposableEffect` `release()` on dispose + `onDismissRequest` (D4). Verified by review; the player is composition-scoped to the dialog.
- **[`media3-ui` `PlayerView` is an Android `View`, wrapped via `AndroidView`]** → standard pattern; same interop approach as the camera `PreviewView`. No added abstraction.
