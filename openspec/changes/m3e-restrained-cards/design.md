# Design — m3e-restrained-cards

Reference implementation (visual contract): `design/cards-redesign-m3e.html`.
The Kotlin result should match that prototype one-to-one within Material 3
Compose's tonal system. This doc records the non-obvious decisions.

## Context & scope

This supersedes the presentation portion of `redesign-entry-card-display`. That
change shipped a gradient surface, a media-bleeding floating header, and a
document-chip recording card. The user reviewed both the "Field Journal" and
"Material 3 Expressive (restrained)" HTML directions and chose the latter, with
the added requirement that all card types share one identical layout. The
data/interaction contracts (open, long-press menu, multi-select, pin/todo,
tags, reminders, comments, encryption lock, group carousel) are untouched —
only presentation and placement move.

## Goals / non-goals

- Goals: one unified scaffold across all six card types; calmer M3 tonal look;
  audio card as a real player affordance; multi-select that never overlaps.
- Non-goals: new color/shape tokens; changing capture/search/nav/drawer;
  re-deriving the seed palette; amplitude-accurate waveforms on day one.

## The unified scaffold (single source of truth)

Every card is a `Column` with three zones, identical insets:

```
HEAD  (height ≈ 48dp, horizontal inset 16dp, top inset 14dp)
  [check] · type-dot · KIND · • · relative-time    →→→   cluster(pin · status · lock)
BODY  (horizontal inset 16dp; media body = 0 inset, media clipped to body corners)
  type-specific content
FOOT  (horizontal inset 16dp, bottom inset 16dp; top inset 2dp / 12dp after media)
  chips: #tag · reminder · comments · counts
```

- `HEAD` is rendered by **one** `EntryCardHeader`-style composable for all types
  (including photo/video). The `floating: Boolean` concept from the previous
  iteration is **removed**; media no longer takes an overlay header.
- `BODY` for media is the only case that drops the horizontal inset so the media
  bleeds edge-to-edge *inside the body zone* (not under the header). The media
  is clipped to the top corners of the body region.
- `FOOT` is one wrapping `FlowRow` of tonal chips — identical composable for
  every card (the group card just feeds it type-count chips).

## Key decisions

### D1. Header above media, not floating
The previous spec required a floating chip over media with a legibility scrim.
That created a second header treatment and pushed the multi-select check into a
corner where it overlapped. Unifying on header-above-media trades a small amount
of media "hero" immersion for layout consistency across all types — the explicit
goal of this change. **This is the breaking behavioral change vs the prior spec.**

### D2. Multi-select check in the dateline
The check becomes the first slot of the header dateline; when present
(`isMultiSelectMode`), the type dot is hidden and the check occupies its place.
Guarantees: zero overlap (it is in normal flow), and left-edge alignment holds on
every card type because every card has a header row. State: selected →
`primary` fill with a check glyph; unselected → `outline` ring.

### D3. Surface = one tonal container, not a gradient
`EntryCard` drops the `LocalGradientColors.cardBrush()` wash. The surface is
`surfaceContainerLow` (calm), with: a faint type-tint wash at ~4–6% alpha
(kept — it lets a heterogeneous feed read at a glance), a 1dp `outlineVariant`
hairline at ~55% alpha, and a light elevation (`tonalElevation` 2dp +
`shadowElevation` 3dp). This is "克制": depth from tonal elevation, not paint.

### D4. Pin / status / lock use M3 containers
- Pin: a 28dp rounded square filled with `primaryContainer`, icon in
  `onPrimaryContainer` (replaces the free-floating outline pin).
- Status: a tonal pill whose container/fill comes from the status's M3 tone
  (TODO→`tertiaryContainer`, DOING→`primaryContainer`, WAIT→`secondaryContainer`,
  DONE/CANCEL→`surfaceContainerHighest`-style settled tone); DONE/CANCEL render
  the label struck-through.
- Lock: a 24dp `surfaceContainerHigh` tile with the lock icon in
  `onSurfaceVariant`.
All three live in the single right-aligned `cluster`.

### D5. Recording = waveform + play affordance, not a file chip
`AudioEntryContent` is replaced by: a `Waveform` composable (row of rounded bars)
+ a circular `play` button (`secondaryContainer`) + name + duration/size. There
is **no in-app audio player** in MindDump today (tapping a recording calls
`openFile()`, handing it to an external player via `ACTION_VIEW`). So the play
button is a visual affordance whose tap reuses the existing `openFile()` external
playback path — building a true in-app player is a separate capability and is
explicitly out of scope here. Waveform data: render a deterministic envelope
derived from duration as the first cut (cheap, no decode); an amplitude-
extraction pass is a later enhancement and out of scope here. The bar tint uses
the audio type accent (`secondary`).

### D6. File card stays a document tile
`FileEntryContent` keeps a document tile (icon + name + size) but re-skinned to
the tonal container, inside the unified body/foot.

### D7. Group card reuses entry-card scaffolding
`GroupSummaryCard` uses the same header (dot + `Collection` label + time, cluster
with pin/status), a bold title + byline ("12 项 · 最近更新 昨天") in the body, the
existing media **strip** preview (keep `HorizontalMultiBrowseCarousel` — tap/
browse unchanged), and type-count chips in the unified foot. Member count moves
into the header dateline/byline (it is already "12 项"); the separate
right-side member-count text is removed in favor of the byline.

## Risks / trade-offs

- **Waveform fidelity**: a duration-derived envelope looks plausible but is not
  the real signal. Acceptable as a visual affordance; document it and defer true
  amplitude extraction. No correctness contract depends on it.
- **Header-above-media reduces media immersion**: intentional; consistency wins
  per the user's direction. Media still gets the full body width.
- **Removing the gradient** is a visual regression only if the user later wants
  the depth back; the surface treatment is centralized in `EntryCard`, so it is
  a one-line revert.

## Migration / rollout

Pure presentation; no data migration. Room cache and filesystem untouched. Build
verification: `./gradlew detekt ktlintCheck` on the host (sandbox build is
network-blocked per the prior change's notes). Manual matrix: each card type ×
{light, dark} × {plain, pinned, TODO, DOING, DONE, encrypted, with-tags,
with-reminder, multi-select}.

## Open questions

- Q: Real amplitude waveform now or later? **Decision**: later (duration-derived
  envelope this change) — keeps scope bounded and avoids a media-decode
  dependency.
- Q: Keep `LocalGradientColors` for the card at all? **Decision**: no — drop the
  card gradient wash; the gradient helpers stay available for other surfaces
  (input/primary) and are not deleted.
