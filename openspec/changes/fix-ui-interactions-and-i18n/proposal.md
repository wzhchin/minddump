## Why

The app's UI polish outpaces its interaction reliability. Several affordances are *visible but non-functional* (a "展开" hint that is never clickable and a state that is never set true; a `sendScale` written directly in the composable body instead of triggered as a real animation; a `FilterChip` misused as a static badge; a per-item `AnimatedVisibility(visible=true)` whose exit is dead and whose enter re-fires on scroll). A category color is wired to the error token, and encryption is sniffed from a filename substring despite a real `isEncrypted` property existing. These erode trust in what the UI "promises" and undermine the otherwise strong micro-interaction work.

**Scope note:** This change is deliberately scoped to interaction correctness, semantic correctness, and fluidity. Internationalization (making the `values-en` locale effective) is **out of scope** this round — the app ships as zh-CN, and hardcoded Chinese labels stay as-is (Chinese). Only new minimal zh-CN string keys are added where a new label is required; no English values are introduced.

## What Changes

**Interaction correctness (A):**
- Wire the "展开" (expand) affordance in the text-entry card so it actually toggles the long-text body between collapsed and expanded, with an animated transition and a "收起" (collapse) label when expanded. The collapsed/expanded state is driven by the body, not a dead hint.
- Replace the direct-in-body `sendScale` write in `InputBar` with a one-shot, triggered scale animation (a genuine bounce on submit / on becoming non-empty), using a `LaunchedEffect` or snapshot-driven target — no state writes in the composition path.
- Stop using `FilterChip(selected = true, onClick = {})` as the recording indicator; render it as a plain `Surface`/`Row` visual container with correct semantics (decorative, not an interactive chip).
- Fix `produceState` flicker: initialize text/comment previews to an empty/loading placeholder rather than `entry.file.name`, so the file name does not flash before content loads.
- Bound `spaceRotation` (modulo) instead of letting it accumulate without limit on every toggle.

**Correctness & semantics (B):**
- **BREAKING (internal only):** Remap `EntryType.toColor()` so `VIDEO` no longer maps to `colorScheme.error`; give video a non-semantic category color (e.g. a fixed accent) while keeping the type→color mapping token-driven where possible.
- Replace `entry.file.name.contains("enc")` sniff with the existing `FileMetadata.isEncrypted` (or an equivalent real property) so lock indicators reflect a parsed property, not a substring guess.
- Eliminate the duplicated comment-grouping logic: `EntryItem.groupEntriesForRender` mirrors the ViewModel's grouping. Consolidate to a single source of truth so the UI renders what the ViewModel already computed.

**Smoothness (D):**
- Replace the per-item `AnimatedVisibility(visible = true, …)` wrapper in `EntryList` (whose `exit` spec is dead and whose `enter` re-fires on scroll) with `LazyColumn` `animateItem()` for genuine, efficient insert/move/reorder animation.
- Unify the video play-button affordance size (currently 48dp circle/28dp icon in the entry card vs 44dp/26dp in the group carousel) to a single shared component/spec.

**Out of scope (C — i18n):** The app ships zh-CN. Hardcoded Chinese labels (relative timestamps, empty states, chart labels, heatmap weekday labels) remain hardcoded Chinese this round. Only new minimal zh-CN string keys are added where a *new* label is introduced (e.g. 收起). Making the en locale effective is deferred.

## Capabilities

### New Capabilities
<!-- None. The localization concern is folded into the existing entry-card / statistics behavior rather than introduced as a standalone capability, because the app's locale story is a property of every surface, not a new module. -->

### Modified Capabilities
- `entry-card`: long-text expand affordance becomes a real, animated toggle (with a 收起 collapse label); lock indicator driven by parsed encryption property; `VIDEO` category color no longer uses the error token; unified video play-button spec; list item animation strategy (animateItem) and stable previews.
- `text-entry-editing`: the in-card long-text expand/collapse is now an interactive, animated contract (touches how text bodies behave, distinct from the fullscreen editor but within this capability's text-rendering scope).
- `expressive-theme`: clarify that category/type colors are derived from theme tokens and SHALL NOT reuse the error/semantic token for non-error categories (video).
- `group-card`: unify the carousel video play overlay with the entry-card play overlay spec (shared size/styling).

> Note: i18n is explicitly out of scope this round (see "Out of scope (C — i18n)" above), so no capability's localization requirement is modified.

## Impact

- **Code:** `ui/EntryItem.kt`, `ui/InputBar.kt`, `ui/statistics` is untouched (i18n deferred), `ui/MainScreen.kt` / `EntryList` (animateItem adoption), `ui/components/*` (shared video play overlay). Possible consolidation point for comment grouping between `MindDumpViewModel.kt` and `EntryItem.kt`.
- **Resources:** `res/values/strings.xml` only — a few new zh-CN keys (e.g. 收起) where a new label is introduced. No `values-en` changes this round.
- **Data:** No on-disk schema/filename change. `FileMetadata.isEncrypted` already exists; consumption moves from name-sniff to property. No Room migration.
- **Build/verify:** `./gradlew detekt ktlintCheck` after implementation; manual smoke of expand/collapse, submit bounce, recording indicator, and list reorder animation.
- **Risk:** Low. Behavior contracts change (expand is now interactive; video color changes). No storage/encryption behavior changes; no locale change.
