# Design — Editorial, restrained card display

## Context

The feed is MindDump's primary surface. Every entry card — text, photo, video,
recording, file — currently shares an identical rhythm: circular type-icon
avatar + relative timestamp + per-type body + chip footer, on a flat
`surfaceContainer` Surface with a 1px `outlineVariant` border (`cardLarge`
28dp). The result is visually monotone: a cinematic photo and a 3-line text
note get the same weight, the type avatar competes with the media it sits
above, and the flat surface lacks depth.

The theme already ships tokens the card never uses: `LocalGradientColors`
(`cardGradient`) with `cardBrush()`, and an asymmetric `entryCard` shape
(20/20/20/6dp). This change makes the card feel content-led, media-first, and
calm-but-deep, without touching data model, storage, encryption, or navigation.

## Aesthetic direction

**Editorial · restrained depth.** One clear point of view, executed with
restraint:

- *Depth over decoration.* Layered gradient surface + two-layer elevation lift
  cards off the background; no glow, no chartjunk.
- *Media first, chrome second.* For photo/video the media is the hero; the
  header floats over it instead of sitting above it.
- *One sharp accent.* A single type-colored dot is the only loud color; tags,
  reminders, pins use tonal, desaturated treatments.
- *Restraint at the high moment.* A single staggered entrance (translateY +
  alpha) using `MotionCurve.emphasize`; reduced-motion degrades to a 120ms
  alpha fade. No per-scroll re-fire (reuse the existing `animateItem` specs).

This maps the `frontend-design` skill's design judgement (committed aesthetic
direction, dominant color + sharp accent, motion focused on high-impact
moments, layered depth) onto Jetpack Compose rather than CSS.

## Approach

### 1. Layered card surface — `EntryCard.kt`

Replace the flat `Surface(color = surfaceContainer)` with a two-layer treatment:

- `Modifier.drawBehind { drawRect(cardBrush()) }` paints the `cardGradient`
  wash under the surface content. Gradient span is small (~6% lightness delta
  in light, ~8% in dark) so it reads as depth, not color.
- `Surface` keeps its role for elevation/shape/click, but `color` becomes
  `Color.Transparent` so the gradient shows through; `tonalElevation = 2.dp`,
  `shadowElevation = 3.dp` give a tonal + ambient two-layer lift.
- Border drops from 1dp solid to a 0.5dp-equivalent `outlineVariant` at ~50%
  alpha — a hairline, not a frame.
- Shape switches from `cardLarge` (28dp, uniform) to the existing asymmetric
  `entryCard` token (20/20/20/6dp) — the small bottom-start corner gives a
  subtle "stack" rhythm down the feed.
- A faint type-tinted overlay is painted on top of the gradient via a second
  `drawRect` with the entry's type color at very low alpha (~6% light, ~4%
  dark). This is the *only* place type color reaches the surface; it lets
  heterogeneous feeds read at a glance without color blocks.

To carry the type tint without breaking the `EntryCard` signature for every
caller, `EntryCard` gains an optional `typeTint: Color? = null` parameter.
Only the entry-card call site passes a tint; the group card and other callers
pass `null` (no tint, gradient only).

### 2. Floating compact header — `EntryItem.kt` (`EntryCardHeader`)

Collapse the avatar row into a single scan-line:

- The circular 32dp type-icon avatar is **removed**. It is replaced by a 6dp
  type-colored dot (filled circle, `typeColor`) inline before the timestamp.
  The freed horizontal space goes to the timestamp.
- Layout becomes one `Row`: `[dot] [timestamp] [weight] [pin?] [status?] [lock?]`.
  Pin, status badge, lock form a right-aligned indicator cluster, unchanged in
  semantics (pin icon, localized status pill, lock icon) but now visually
  grouped rather than loosely trailing.
- For **media types** (photo/video) this header is rendered as a floating chip
  overlaid on the edge-to-edge media hero: a translucent `surface` pill with a
  top-to-bottom dark-to-transparent scrim behind it for legibility. The dot +
  timestamp sit inside the chip; the indicator cluster sits on the scrim to the
  right.
- For **non-media types** (text/recording/file) the header is a solid row on
  the card surface at the top, unchanged in position.

### 3. Media-first hero — `PhotoEntryContent` / `VideoEntryContent`

- Photo/video media expands from the current `padding(horizontal = 8dp)` +
  12dp inner clip to **edge-to-edge**: the `Box` fills the card width with
  zero horizontal padding and is clipped to the *card's* top corners (the
  `entryCard` top-start/top-end radii), so the media bleeds under the floating
  header chip.
- Height stays 200dp (current value) — only horizontal extent and corner
  treatment change.
- The floating header + scrim render above the media (z-order: media, then
  scrim `Box`, then header chip).
- Tap/zoom/player behaviors are unchanged.

### 4. Consolidated bottom meta bar — `EntryCardMetaFooter`

Already a `FlowRow` of tonal chips. Behavior change is small per Q6 (wrap):

- The footer already wraps; this is formalized (no max-lines clamp, no "+N"
  truncation). Tags + reminder chip share one wrapping row with consistent
  8dp chip radius and 6dp spacing.
- Treatment is unchanged (tags → `secondaryContainer` `#tag` chips, reminder
  → `primaryContainer` bell chip, fired → reduced alpha) — the editorial
  direction keeps these tonal, not loud.

### 5. Comments as a nested sub-surface — `CommentListSection`

Per Q7, the collapsed comments block becomes a visually distinct nested
sub-surface inside the card:

- Wrap the existing clipped `Column` in a surface treatment: `cardSmall`
  (16dp) shape, `surfaceContainerHighest` fill, inset 12dp from the card edges
  (replaces the current `start=16/end=16/bottom=12` padding with a symmetric
  12dp inset around the sub-surface).
- The expand affordance + expanded previews keep their current behavior; only
  the container gets the nested-surface look so the card reads as
  top (identity) / middle (content) / nested (context).

### 6. Group card parity — `GroupSummaryCard`

Per Q8, `GroupSummaryCard` gets the matching surface/header/meta treatment:

- Renders on the same gradient + hairline + `entryCard` shape `EntryCard`
  (passes `typeTint = null`).
- Its header row collapses the same way: the 32dp folder-icon avatar is
  replaced by a primary-colored dot before the group name; pin/status/member
  count form the right cluster.
- Carousel behavior, type-chip row, empty-state wording — **unchanged**.

### 7. Dark mode + reduced motion parameters

- **Dark mode:** `cardGradient` span reduced to ~8% lightness delta (already
  the case in `Theme.kt`'s dark `GradientColors`); type-tint overlay alpha
  drops from ~6% (light) to ~4% (dark) to avoid noise. These are the only
  dark-specific tweaks; everything else is theme-token-driven.
- **Reduced motion:** the new staggered entrance (section 8) degrades via
  `rememberAnimationDuration(reduceMotion = true)` / `rememberMotionCurve`:
  translateY collapses to 0, leaving a 120ms alpha fade. Existing
  `animateItem` placement/fade specs are reused untouched.

### 8. Staggered entrance (the single high-impact moment)

- Each card's first composition animates in with a short `MotionCurve.emphasize`
  tween: `translateY(8dp → 0)` + `alpha(0 → 1)`, duration
  `LocalAnimationDuration.medium`. This is layered on top of the existing
  `LazyColumn` `animateItem` (which already handles insert/move/remove) — it
  fires only on genuine new inserts, not on scroll-into-view, by keying off
  a `remember { mutableStateOf(false) → true }` first-composition flag inside
  the card content.
- One high-impact moment, not scattered micro-interactions — the editorial
  discipline from `frontend-design`.

## Non-goals

- No data model / storage / encryption / Room / migration changes.
- No new permissions, libraries, strings, or navigation routes.
- No change to capture, search, group drill-down, or action-drawer contents.
- Group carousel behavior is unchanged.

## Risks

- **Z-order / clip on the floating header** over media: must verify the scrim
  + chip don't eat the image's tap target. Mitigation: scrim and chip are
  `pointerInput`-pass-through (decorative); the media `Box` keeps its
  `combinedClickable`/zoom.
- **Type-tint noise in dark mode:** kept at ~4% alpha; if it still reads as
  noise, the fallback is to drop the surface tint in dark and keep only the
  type dot (the dot alone still signals type).
- **Asymmetric `entryCard` shape clipping media corners:** the edge-to-edge
  media must be clipped to the card's *top* corners only (media sits at the
  top of the card under the header), not all four — verified in section 3.

## Verification

- `./gradlew detekt ktlintCheck` clean.
- Manual matrix (light + dark): text, photo, video, audio, file, group cards ×
  {plain, pinned, TODO, DOING, DONE, encrypted, with-tags, with-reminder,
  multi-select}. Confirm: gradient depth visible, type dot present, media
  edge-to-edge with legible floating header, comments nested, group parity,
  reduced-motion fade-only entrance.
