## 1. Spec + scaffolding
- [x] 1.1 Scaffold change via `openspec new change m3e-restrained-cards`.
- [x] 1.2 Write proposal.md, design.md, tasks.md, and spec deltas.
- [x] 1.3 `openspec validate m3e-restrained-cards` passes (after sign-off).

## 2. Surface — `EntryCard.kt`
- [x] 2.1 Replace the gradient `cardBrush()` wash with a calm tonal container
      (`colorScheme.surfaceContainerLow`).
- [x] 2.2 Keep the faint `typeTint` wash (~6% light / ~4% dark); skip when
      `typeTint == null`.
- [x] 2.3 Keep 1dp `outlineVariant` hairline (~55% alpha) and the asymmetric
      `entryCard` shape.
- [x] 2.4 Keep light elevation: `tonalElevation = 2.dp`, `shadowElevation = 3.dp`.
- [x] 2.5 Remove the now-unused gradient import/use from `EntryCard` (leave
      `LocalGradientColors` available elsewhere).

## 3. Unified header — `EntryItem.kt` (`EntryCardHeader` / `IndicatorCluster`)
- [x] 3.1 Remove the `floating: Boolean` branch entirely; there is one header
      layout for all types (solid row on the card surface, at the top).
- [x] 3.2 Header dateline: `[check?] · type-dot · KIND label · dot-sep · time`,
      all left-aligned; right-aligned `cluster` with pin/status/lock.
- [x] 3.3 Add a compact type `KIND` label (uppercase, type-colored, ~11sp) between
      the dot and the timestamp.
- [x] 3.4 Pin → `primaryContainer` rounded square (icon `onPrimaryContainer`).
- [x] 3.5 Status pill → M3 tonal container per status; DONE/CANCEL struck-through.
- [x] 3.6 Lock → `surfaceContainerHigh` tile, `onSurfaceVariant` icon.
- [x] 3.7 Media (photo/video) uses the same header; no overlay, no scrim.

## 4. Multi-select check in the dateline
- [x] 4.1 Move `MultiSelectBadge` out of the absolute/corner placement into the
      header dateline first slot; in multi-select the type dot is replaced by it.
- [x] 4.2 Selected state → `primary` fill + check glyph; unselected → `outline`
      ring.
- [x] 4.3 For media cards, render the check in the same header dateline (proves
      no-overlap across all types).
- [x] 4.4 Card-wide selected outline = `primary` 2dp.

## 5. Body per type — `EntryItem.kt`
- [x] 5.1 Wrap all bodies in a `card__body`-equivalent `Column` at 16dp inset;
      media body uses 0 horizontal inset.
- [x] 5.2 Photo/video: render media below the header, edge-to-edge in the body
      zone, clipped to the body's top corners (remove the "bleed under header"
      clip). Keep zoom (photo) / player-open (video) behavior.
- [x] 5.3 Text: keep collapse/expand; re-skin body to the unified inset.
- [x] 5.4 Replace `AudioEntryContent` document chip with: `Waveform` bars + round
      play button (`secondaryContainer`) + name + duration/size.
- [x] 5.5 File: keep document tile, re-skin to tonal container, unified inset.

## 6. Recording player affordance — new `Waveform`/play composable
- [x] 6.1 Add `Waveform` composable (row of rounded bars, audio-tinted) sized off
      a deterministic duration-derived envelope (no media decode).
- [x] 6.2 Round play button reuses the existing external-playback path
      (`openFile()` / `ACTION_VIEW`) — no new in-app player.
- [x] 6.3 Show recording file name + duration/size in mono `labelSmall`.

## 7. Foot — unified `EntryCardMetaFooter`
- [x] 7.1 One wrapping `FlowRow` of tonal chips: `#tag` (`secondaryContainer`),
      reminder bell (`primaryContainer`), comments chip, fired→de-emphasized.
- [x] 7.2 Same foot composable used by the group card (counts) and entry cards.
- [x] 7.3 Omit foot when empty and during multi-select (existing behavior).

## 8. Group card parity — `GroupSummaryCard`
- [x] 8.1 Render on the unified `EntryCard` (`typeTint = null`).
- [x] 8.2 Header: dot + `Collection` label + time, cluster (pin/status); member
      count moves into the body byline.
- [x] 8.3 Body: bold title + byline ("N 项 · 最近更新 …") + existing media strip
      (`HorizontalMultiBrowseCarousel`, tap/browse unchanged).
- [x] 8.4 Foot: type-count chips (`×N` per type, type-tinted) in the unified foot.
- [x] 8.5 Empty-group wording unchanged.

## 9. Entrance + reduced motion
- [x] 9.1 Per-card insert/scroll animation continues to use `animateItem` (untouched).
- [x] 9.2 No new first-composition entrance in this change (deferred, as before).

## 10. Cleanup
- [x] 10.1 Remove dead `floating`/scrim/overlay header code and the
       `MediaHeroClip` top-corner-bleed helper if no longer used.
- [x] 10.2 Remove unused imports (Icons/gradient) flagged by detekt/ktlint.

## 11. Verification
- [x] 11.1 `./gradlew detekt ktlintCheck` clean on touched files (EntryItem.kt,
      EntryCard.kt, AudioRecordingContent.kt); `assembleDebug` green.
      NOTE: 3 unrelated files (MainScreen/FileStorageEngine/EntryActionDrawer)
      carry pre-existing ktlint violations at HEAD and were reformatted by
      `ktlintFormat` as collateral — left in place (net reduction of violations).
- [ ] 11.2 Manual matrix (light + dark): text, photo, video, audio, file, group ×
       {plain, pinned, TODO, DOING, DONE, encrypted, with-tags, with-reminder,
       multi-select} — confirm no overlap, left edges aligned. (Run on device.)
