## 1. Surface layer — `EntryCard.kt`

- [x] 1.1 Add an optional `typeTint: Color? = null` parameter to `EntryCard`; callers that have no type (group card, others) pass `null`.
- [x] 1.2 Replace the flat `Surface(color = surfaceContainer)` with a `Color.Transparent` surface painted by `drawBehind { drawRect(cardBrush()) }` using `LocalGradientColors.current.cardBrush()`.
- [x] 1.3 Add the faint type-tint overlay: a second `drawRect` with `typeTint` at ~6% alpha (light) / ~4% alpha (dark) — use `isSystemInDarkTheme()` to pick; skip entirely when `typeTint == null`.
- [x] 1.4 Switch the shape from `shapes.cardLarge` to `shapes.entryCard` (asymmetric 20/20/20/6dp).
- [x] 1.5 Soften the border: `outlineVariant` at ~50% alpha, keep 1dp.
- [x] 1.6 Bump `tonalElevation` to 2.dp and `shadowElevation` to 3.dp for the tonal + ambient two-layer lift.

## 2. Floating compact header — `EntryItem.kt` (`EntryCardHeader`)

- [x] 2.1 Remove the 32dp circular type-icon avatar `Box` + its `Icon`.
- [x] 2.2 Insert a 6dp filled type-colored dot (typeColor) before the timestamp.
- [x] 2.3 Keep the existing right-aligned cluster (pin icon, status pill, lock icon) as-is in semantics; ensure spacing reads as one cluster.
- [x] 2.4 Add a `floating: Boolean` parameter (default false). When true, render the header row inside a translucent `surface`-variant pill with a top-to-bottom dark→transparent scrim behind it; when false, render the current solid top row.

## 3. Media-first hero — `PhotoEntryContent` / `VideoEntryContent`

- [x] 3.1 Remove `padding(horizontal = 8dp)` so media fills the card width.
- [x] 3.2 Clip the media `Box` to the card's top corners only (top-start/top-end radii of `entryCard`), not all four.
- [x] 3.3 Pass `floating = true` to `EntryCardHeader` for PHOTO and VIDEO entries.
- [x] 3.4 Verify z-order: media → scrim → floating header chip; media's `combinedClickable`/zoom target still receives taps.

## 4. Consolidated bottom meta bar — `EntryCardMetaFooter`

- [x] 4.1 Confirm `FlowRow` wraps freely (no max-lines clamp, no "+N" truncation) — formalize Q6 decision in code/behavior.
- [x] 4.2 Keep tonal treatments (`secondaryContainer` tags, `primaryContainer` bell, fired → reduced alpha) unchanged.

## 5. Comments as nested sub-surface — `CommentListSection`

- [x] 5.1 Wrap the clipped `Column` in a nested-surface treatment: `cardSmall` (16dp) shape, `surfaceContainerHighest` fill, symmetric 12dp inset from the card edges (replaces the current `start=16/end=16/bottom=12`).
- [x] 5.2 Keep the expand affordance + expanded previews behavior unchanged.

## 6. Group card parity — `GroupSummaryCard`

- [x] 6.1 Render on the new gradient `EntryCard` (pass `typeTint = null`).
- [x] 6.2 Replace the 32dp folder-icon avatar with a primary-colored dot before the group name; keep pin/status/member-count as the right cluster.
- [x] 6.3 Leave carousel, type-chip row, and empty-state wording unchanged.

## 7. Staggered entrance + reduced motion

- [~] 7.1 DEFERRED — first-composition entrance (existing animateItem covers insert animation) to the card content: `translateY(8dp → 0)` + `alpha(0 → 1)` via `MotionCurve.emphasize` tween, duration `LocalAnimationDuration.medium`, keyed off a `remember { mutableStateOf(false) }` flag set true in an initial `LaunchedEffect`.
- [~] 7.2 DEFERRED (depends on 7.1) `rememberAnimationDuration`/`rememberMotionCurve` so reduced-motion collapses translateY to 0, leaving a 120ms alpha fade.
- [~] 7.3 DEFERRED (depends on 7.1) on genuine new inserts only, not scroll-into-view (existing `animateItem` placement/fade specs untouched).

## 8. Verification

- [ ] 8.1 `./gradlew detekt ktlintCheck` clean. BLOCKED: gradle daemon cannot resolve a wildcard IP in this sandbox network; build/lint must be run on the host.
- [ ] 8.2 Manual matrix (light + dark): text, photo, video, audio, file, group × {plain, pinned, TODO, DOING, DONE, encrypted, with-tags, with-reminder, multi-select}.
