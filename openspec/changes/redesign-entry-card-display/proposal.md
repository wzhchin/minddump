## Why

The feed is MindDump's primary surface, but every entry card — text note, photo,
video, recording, file — currently uses an identical "icon avatar + timestamp +
body + chip footer" rhythm. The result is visually monotonous: text notes and
cinematic photos get the same weight, the type-avatar header competes with the
media it sits above, and the quiet `surfaceContainer` card lacks the depth and
personality of a modern M3 Expressive feed. We want the feed to feel alive and
content-led — media first, chrome second — while keeping every existing
interaction (open, long-press menu, multi-select, pin/todo indicators, tags,
reminders, encryption lock) intact.

## What Changes

### Visual style upgrade
- Introduce a layered surface treatment: subtle gradient (from
  `LocalGradientColors.cardGradient`) instead of flat `surfaceContainer`, a
  softer `outlineVariant` hairline, and a refined two-layer elevation (tonal +
  ambient shadow) so cards lift off the background with depth, not just a line.
- Media-first hero region: for photo / video entries the media tile becomes the
  visual anchor and runs edge-to-edge inside the card, bleeding under a compact
  floating header chip rather than sitting below a full-width header row.
- Replace the circular type-icon avatar with a smaller, inline type indicator
  (dot or pill) so the type signal stops dominating the header; the saved space
  goes to the timestamp and status indicators.
- Tonal, type-tinted accents: text/photo/audio/video each get a faint wash of
  their theme color on the card surface or media scrim so heterogeneous feeds
  read at a glance without loud color blocks.
- Polished status/pin indicators: pin and todo badges move to a unified
  right-aligned indicator cluster on the floating header chip; DONE/CANCEL get
  a desaturated "settled" treatment.

### Information architecture adjustment
- Collapse the header into a single floating row overlaying the media (for
  media types) or sitting flush at the top (for text/file): timestamp + type
  dot on the left, pin/todo/lock cluster on the right — one scan-line instead
  of a tall avatar row.
- Promote tags + reminder into a single bottom meta bar (icon-led chips on one
  row where possible, wrapping only when necessary) instead of the current
  loose footer, so the card has a clear top (identity) / middle (content) /
  bottom (context) rhythm.
- Keep the in-card collapsed comments section and orphan-comment indicator
  behavior, but visually nest them as a distinct sub-surface inside the card.

### Non-goals
- No change to data model, storage format, encryption, or navigation.
- No change to capture flow, search, group drill-down, or the action drawer
  contents — only how the card looks and lays out.
- Group card (`group-card` capability) gets the matching surface/header/meta
  treatment for consistency, but its carousel behavior is unchanged.

## Capabilities

### New Capabilities
<!-- None. This is a presentation redesign of existing capabilities. -->

### Modified Capabilities
- `entry-card`: Visual presentation requirements (layered surface, media-first
  hero, floating compact header, unified indicator cluster, consolidated meta
  bar) replace/augment the current "calm surface + icon avatar header + chip
  footer" requirements.
- `group-card`: Header and surface presentation updated to match the new entry
  card visual language (gradient surface, compact header, consistent meta
  treatment); carousel behavior unchanged.

## Impact

- **Code**:
  - `app/src/main/java/com/chin/minddump/ui/components/EntryCard.kt` — surface
    treatment (gradient + refined elevation + border).
  - `app/src/main/java/com/chin/minddump/ui/EntryItem.kt` — header refactor
    (floating compact header), content region (media edge-to-edge), meta bar
    consolidation; group card header surface alignment.
  - Possibly `app/src/main/java/com/chin/minddump/ui/theme/Shape.kt` /
    `Theme.kt` if a new shape token or gradient helper is needed.
- **APIs / dependencies**: No new permissions. May add small Compose helpers
  (gradient modifier, scrim). No new libraries expected; `LocalGradientColors`
  already exists.
- **Specs**: `entry-card` and `group-card` requirement wording changes (delta).
  No data/storage/security contract changes — filesystem stays the source of
  truth, Room cache untouched, no migration needed.
- **i18n**: No new user-facing strings expected; existing zh-CN / en strings
  for status, expand/collapse, comments, tags remain in use.
- **Verification**: `./gradlew detekt ktlintCheck`; manual visual pass on
  API 29+ for text/photo/video/audio/file/group cards in light & dark,
  pinned/todo/encrypted states, and multi-select.
