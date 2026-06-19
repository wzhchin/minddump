## Why

The previous card iteration (`redesign-entry-card-display`) leaned on layered
gradients, a media-bleeding floating-header overlay, and an audio "document
chip", which made the feed feel busy and inconsistent: photo/video cards used a
different header treatment than text/recording/file cards, the multi-select
check floated over content (overlapping the header on media cards), and the
recording card showed a file chip instead of an audio affordance. We want the
feed to read as one calm, faithful **Material 3 Expressive** surface — tonal
containers, one accent, hairlines, and a single unified layout applied
identically to every card type.

## What Changes

### Unified three-zone scaffold (all card types identical)
- Every entry card and group card SHALL use the same **head · body · foot**
  skeleton: one header row at a fixed inset, a body region, and a footer region
  at the same inset. Type differences is expressed only via the type accent,
  not via different header placement or chrome.
- The media (photo/video) header SHALL stop floating over the media and SHALL
  sit as the same solid top row every other card uses — media renders below the
  header, edge-to-edge inside the body zone, clipped to the body's corners.
- The multi-select affordance SHALL move into the header dateline as its first
  slot (replacing the type dot while multi-select is active), so it can never
  overlap body or footer content and stays left-aligned across all card types.

### Calmer Material 3 Expressive presentation
- The card surface SHALL use a single calm tonal container (e.g.
  `surfaceContainerLow`) with a faint type-tint wash, a thin `outlineVariant`
  hairline, and a light two-layer elevation — no gradient wash, no heavy
  shadows. The asymmetric `entryCard` shape (20/20/20/6) is retained.
- Header SHALL show a small type-colored dot, a compact type label, a relative
  timestamp, and a right-aligned cluster (pin, todo status, lock) — all using
  M3 tonal containers (`primaryContainer`, `secondaryContainer`,
  `tertiaryContainer`) instead of ad-hoc colors.
- Pin indicator SHALL be a `primaryContainer` rounded square (tonal), not a
  free-floating icon.

### Recording card becomes an audio player affordance
- The recording card body SHALL render a waveform visualization plus a circular
  play button and the recording's name/duration (a real audio control), instead
  of a generic file/document chip.

### Group card parity
- The group card SHALL adopt the same unified scaffold (dot + label + time, a
  bold title, a byline, a media strip, and type-count chips in the foot),
  matching the entry card's head and foot placement exactly.

### Non-goals
- No change to data model, storage format, encryption, capture flow, search,
  navigation, or the action drawer contents.
- No new color tokens or shape tokens beyond reusing existing
  `MaterialTheme.colorScheme` containers and `ExpressiveShapes.entryCard`.
- Carousel tap/browse behavior on the group card, and the tag/reminder/comment
  footer semantics, are unchanged — only their visual treatment and placement
  move to the unified scaffold.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `entry-card`: Presentation requirements rewritten to the unified three-zone
  scaffold (header no longer floats over media; media renders below the header
  in the body zone; multi-select check lives in the header dateline), the calmer
  M3 tonal surface, and the recording-as-audio-player body.
- `group-card`: Presentation requirements rewritten so the group card shares the
  entry card's unified head/foot scaffold and tonal surface; the member-count
  info moves into the unified header dateline/byline and type counts into the
  foot.

## Impact

- **Code**:
  - `app/src/main/java/com/chin/minddump/ui/EntryItem.kt` — largest change:
    restructure `EntryItem`/`EntryCardHeader`/`IndicatorCluster`/content
    composables around the unified scaffold; replace floating header + media
    overlay with header-above-media; move `MultiSelectBadge` into the dateline;
    replace `AudioEntryContent` (document chip) with a waveform + play control;
    rebuild `GroupSummaryCard` on the same scaffold.
  - `app/src/main/java/com/chin/minddump/ui/components/EntryCard.kt` — surface
    treatment simplified to a single tonal container + faint type-tint wash +
    hairline + light elevation (drop the gradient `cardBrush` wash).
  - Possibly small additions in `app/src/main/java/com/chin/minddump/ui/components/`
    for a reusable waveform composable and an audio-play affordance.
- **APIs / dependencies**: No new permissions. The waveform needs the audio's
  duration/amplitude; if amplitude extraction is costly, render a deterministic
  envelope from duration as a first cut (no new media library). No new Gradle
  dependencies expected.
- **Specs**: `entry-card` and `group-card` requirement deltas. No
  data/storage/security contract changes — filesystem stays the source of truth,
  Room cache untouched, no migration needed.
- **i18n**: No new user-facing strings expected; existing zh-CN / en strings for
  status, expand/collapse, comments, tags, group member count remain in use.
- **Verification**: `./gradlew detekt ktlintCheck`; manual visual pass on API 29+
  for text/photo/video/audio/file/group cards in light & dark, pinned/todo/
  encrypted states, and multi-select (verifying no overlap).
