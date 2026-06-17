## Context

MindDump's UI layer carries a high density of micro-interactions (haptics, shimmer skeletons, expressive shapes, animated previews) but several affordances are *visually present yet non-functional or semantically wrong*. At the same time, user-visible copy is hardcoded in Kotlin, so the English `values-en/strings.xml` is not actually effective — switching locale leaves Chinese strings on screen. These are correctness/trust and i18n-discipline problems, not feature gaps.

Key current-state facts established from the code:
- `EntryItem.kt` `TextEntryContent` declares `var expanded` and renders an "展开" hint for bodies >120 chars, but **nothing ever sets `expanded = true`** and the hint `Text` has no click handler — it is a dead state.
- `InputBar.kt` sets `sendScale` directly inside the composable body (`if (inputText.isNotBlank()) sendScale = 1.05f else sendScale = 1f`), which is a recomposition anti-pattern and is not a triggered "bounce" despite the comment.
- `RecordingIndicatorChip` renders a `FilterChip(selected = true, onClick = {})` purely as a visual container.
- `produceState` initial values are `entry.file.name`, so text/comment previews flash the filename before content loads.
- `EntryType.toColor()` maps `VIDEO` → `colorScheme.error` (error red used as a category color).
- Encryption is detected via `entry.file.name.contains("enc")` even though `FileMetadata.isEncrypted` already exists (`file-metadata-parsing` capability).
- `EntryItem.groupEntriesForRender` duplicates the ViewModel's comment-grouping (comment self-documents: "Mirrors the ViewModel logic").
- `EntryList` wraps each item in `AnimatedVisibility(visible = true, …)`; the `exit` spec is dead and `enter` re-fires as items scroll into view.
- The video play overlay is sized 48dp/28dp in the entry card and 44dp/26dp in the group carousel.

## Goals / Non-Goals

**Goals:**
- Make every visible affordance actually functional (expand, submit feedback, recording indicator).
- Stop the two recomposition anti-patterns (in-body state write; per-item `AnimatedVisibility(visible=true)`).
- Make the en locale fully effective by externalizing every hardcoded user-visible string.
- Drive visual state from parsed properties/tokens, not substring guesses or semantic-token misuse.
- Improve perceived fluidity with idiomatic Compose list animation (`animateItem`) and animated expand.

**Non-Goals:**
- No new features. No new screens.
- No on-disk filename format change, no Room schema change, no migration.
- No encryption/storage behavior change (lock is only *displayed* more correctly).
- Not a full i18n audit of every screen — scoped to the identified hardcoded strings (timestamps, expand/collapse, empty states, stats labels, heatmap weekday labels). Camera/Settings/etc. strings that are already externalized stay as-is.
- Not restyling the whole color system — only the video category token.

## Decisions

### D1: Expand as a real, animated, in-card toggle
Drive collapse/expand from a `remember` boolean, make the whole hint (and the body area) toggle it, and animate line-count via `AnimatedContent`/`Crossfade` or an alpha+size transition. When expanded, the affordance reads the localized "收起". The card-level tap-to-open is preserved by making the expand affordance its own clickable region that **does not** propagate to the card (stop the open-navigation when the user is toggling expand).

*Why over "always expand":* feed scannability benefits from collapse; the bug is that collapse was a dead end, not that it existed.
*Alternative considered:* remove collapse entirely and always show full text — rejected; long entries would wreck the feed.

### D2: Genuine submit/empty bounce via triggered animation
Replace the in-body `sendScale` write with `animateFloatAsState` whose target is set from a `LaunchedEffect` keyed on a one-shot trigger (e.g. a counter that bumps on submit, and/or a transition when the field becomes non-empty). State is never written directly in the composition path.

*Alternative:* use a `Animatable` with `animateTo` in a coroutine — heavier; the key-triggered `animateFloatAsState` is sufficient and idiomatic.

### D3: Recording indicator as a decorative Surface, not an interactive chip
`FilterChip(selected = true, onClick = {})` is semantically an interactive control. Render the indicator as a `Surface` + `Row` (icon dot + label) matching the existing visual, but with no click affordance and correct semantics. Visually equivalent; semantically honest.

### D4: Fix preview flicker with a loading placeholder initial value
Set `produceState` initial value to `""` (empty) and show a localized "加载中…" (or render nothing/skeleton) until the async value arrives. Do not seed with `entry.file.name`.

### D5: VIDEO category color — non-semantic token
Map `VIDEO` to a theme-derived but **non-semantic** color. Options: a fixed accent like `colorScheme.tertiary` is already taken by PHOTO; use a distinct approach — introduce a small, theme-aware "video" tint (e.g. blend of secondary/tertiary) or pick `colorScheme.error`'s sibling. **Decision:** give video a dedicated, clearly non-error category color by reusing a fixed brand-ish accent derived from the seed is over-engineering; simplest correct option is to assign VIDEO a distinct container/outline-derived tone that is not `error`. Concretely: keep TEXT=primary, PHOTO=tertiary, RECORDING=secondary, and move VIDEO to `colorScheme.outline`/a neutral-but-distinct accent OR a 5th derived tone. Final pick documented in tasks; the contract is only "VIDEO SHALL NOT use the error token."

### D6: Lock indicator from `FileMetadata.isEncrypted`
Stop the `name.contains("enc")` sniff. Compute encryption from the parsed property already available via `FileMetadata.fromFile(file).isEncrypted` (the file-metadata-parsing capability guarantees this). If `MindDumpEntry` does not already expose it, add a thin derived accessor rather than re-parsing at every render (cache or expose via the model).

### D7: Single source of truth for comment grouping
`groupEntriesForRender` duplicates the ViewModel. Decision: have the **ViewModel** own the `GroupedEntry` list and pass it down already grouped; `EntryItem` renders what it is given and drops its local `groupEntriesForRender`. If the ViewModel already produces a grouped structure, consume it directly; otherwise move the function up and expose it. Eliminates the second source of truth.

*Migration note:* ensure the groupings stay byte-identical in behavior during the move (same orphan-comment handling).

### D8: `animateItem()` over per-item `AnimatedVisibility(visible = true)`
Drop the wrapping `AnimatedVisibility`. Apply `Modifier.animateItem()` (Compose Foundation `LazyItemScope`) to each item for genuine insert/move/remove animation that does not re-fire on scroll. Keep a tasteful spring. This also removes the dead `exit` spec.

*Alternative:* keep enter animation via `AnimatedVisibility` only on truly new insertions — not distinguishable from animateItem and more code. animateItem is the idiomatic 2025 answer.

### D9: Shared video play overlay component
Extract one `VideoPlayOverlay` composable used by both `VideoEntryContent` and `CarouselMediaTile`, with a single size spec (decide 48dp circle / 28dp icon, the larger of the two, for consistency with the entry card which is the primary surface).

### Out of scope: i18n
The app ships zh-CN. Making `values-en` effective is deferred to a later change. Only new minimal zh-CN string keys are added where a *new* label is introduced (e.g. 收起). Hardcoded Chinese labels (relative timestamps, empty states, chart labels, heatmap weekday labels) stay as-is this round.

## Risks / Trade-offs

- **[Expand affordance vs. card open-navigation conflict]** The whole card currently opens the entry on tap. A clickable expand region inside it must consume the tap so it does not also navigate. → Make the expand region its own `clickable` with no ripple leak; verify long-press still reaches the action menu (expand region should not capture long-press).
- **[animateItem on reverseLayout feed]** `reverseLayout = true` LazyColumn + animateItem can occasionally animate in the wrong direction on first layout. → Verify on device; if janky, scope animation to move/insert only and accept default for initial.
- **[VIDEO color change is visible]** Users may notice video cards no longer red. → Intended; red was wrong. Communicate via the fact that red is now reserved for actual errors.
- **[Comment-grouping consolidation could reorder orphans]** → Keep behavior identical; test orphan-comment (deleted-parent) case explicitly.

## Open Questions

- Exact VIDEO replacement color token (D5) — decided during implementation against the live palette; the spec only constrains "not error".
- Whether `MindDumpEntry` should gain an `isEncrypted` field (cleaner) vs. a render-time `FileMetadata.fromFile` lookup (cheaper to ship). Lean toward exposing it on the model if cheap; otherwise cache.
