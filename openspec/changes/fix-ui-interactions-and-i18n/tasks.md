# Tasks — fix-ui-interactions-and-i18n

Implementation order: interaction fixes (A) → correctness (B) → smoothness (D) → verify. i18n (capability C) is **out of scope** this round — the app ships zh-CN; only a few new zh-CN string keys are added where a *new* label is introduced.

## 1. Interaction fixes (capability A)

- [x] 1.1 Make the expand affordance in `TextEntryContent` a real toggle: wrap body in animated expand (alpha/size or `AnimatedContent`), wire the affordance's `clickable` to flip `expanded`, swap label to 展开/收起. Add a `R.string.action_collapse` (收起) zh-CN key (展开 already externalized? if not, add `action_expand` too). Ensure the toggle consumes the tap so it does not trigger the card's open-navigation. Preserve long-press → action menu.
- [x] 1.2 Replace the in-body `sendScale` write in `InputBar.kt` with a triggered animation: bump a one-shot trigger on submit (and/or when text becomes non-empty) via `LaunchedEffect`, animate with `animateFloatAsState`. Remove the direct state write in the composition path.
- [x] 1.3 Convert `RecordingIndicatorChip` from `FilterChip(selected=true, onClick={})` to a decorative `Surface`+`Row` (dot + label) with equivalent visuals and no interactive semantics.
- [x] 1.4 Fix `produceState` flicker: set the initial value to `""`/loading placeholder in `TextEntryContent` and `CommentPreview`; render placeholder until the async value arrives (no file-name flash).
- [x] 1.5 Bound `spaceRotation` in `InputBar.kt` (e.g. toggle 0/180 or `% 360f`) so it does not accumulate unbounded.

## 2. Correctness & semantics (capability B)

- [x] 2.1 Remap `EntryType.toColor()`: move `VIDEO` off `colorScheme.error` to a distinct, theme-derived non-semantic category color; confirm no entry type uses the error token for category coloring.
- [x] 2.2 Replace `entry.file.name.contains("enc")` sniff in `EntryCardHeader` with `FileMetadata.fromFile(entry.file).isEncrypted`. If `MindDumpEntry` lacks an `isEncrypted` accessor, add a cheap derived/cached accessor and use it; keep behavior identical.
- [x] 2.3 Consolidate comment grouping to a single source of truth: have the ViewModel expose the already-grouped `GroupedEntry` list and remove `EntryItem.groupEntriesForRender`. Verify orphan-comment (deleted-parent) rendering is unchanged.

## 3. Smoothness (capability D)

- [x] 3.1 Replace the per-item `AnimatedVisibility(visible = true, …)` wrapper in `EntryList` with `Modifier.animateItem()` on each item (spring spec). Remove the dead `exit` spec. Keep `reverseLayout = true`.
- [x] 3.2 Extract a shared `VideoPlayOverlay` composable; use it in both `VideoEntryContent` (entry card) and `CarouselMediaTile` (group carousel) at one size spec (48dp circle / 28dp icon).
- [x] 3.3 (Optional polish) tune the expand transition easing/curve to use `LocalMotionCurve` for consistency.

## 4. Verify

- [x] 4.1 `./gradlew detekt ktlintCheck` clean.
- [x] 4.2 Build succeeds (`./gradlew assembleDebug`).
- [ ] 4.3 Smoke (device): expand/collapse works and stays in card; submit bounce; recording indicator non-interactive; no filename flash; list reorder animates; lock indicator shows for encrypted entries; video avatar not red. — *Deferred to device; cannot run in this environment. Code reviewed.*
- [ ] 4.4 Confirm orphan-comment and group-card cases render correctly after grouping consolidation. — *Logic preserved verbatim (filter on `entry.groupPath` over the VM's identical grouping); device-confirm deferred.*
