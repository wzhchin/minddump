## Context

`nested-group-drill-down` shipped nesting as two screens — `MainScreen` (root feed) and `GroupDetailScreen` (a group) — bridged by a shared mutable `currentGroupDir: File?` in `uiState`. `GroupDetailScreen` drives that field from its compose lifecycle:

```kotlin
LaunchedEffect(groupDir) { viewModel.setCurrentGroupDir(groupDir) }
DisposableEffect(groupDir) { onDispose { viewModel.setCurrentGroupDir(null) } }
```

That bridge is the root cause of two bugs:

1. **In-group capture lands at month-top.** The `Camera` route is a *sibling* destination, not a child of the group screen. Navigating to it pops `GroupDetailScreen` out of composition, firing `onDispose` → `setCurrentGroupDir(null)`. Then `MindDumpNavGraph.kt` computes `getPhotoFile()/getVideoFile()` against a null `currentGroupDir` → the capture file resolves to the month dir. The photo appears in the root feed, not the group. Audio recording started before navigation escapes this only because `getRecordingFile()` is called *before* the navigate; the camera path has no such luck.
2. **Drill/back race.** Tapping a sub-group calls `setCurrentGroupDir(childDir)` then `navigate()`. The new `GroupDetailScreen`'s `LaunchedEffect(B)` and the old one's `onDispose { setCurrentGroupDir(null) }` race; if dispose wins, the field is wrong (clobbered to null or the parent), yielding empty sub-group cards or a mis-scoped write.

The deeper realization: **the root month directory is itself a group** (the root group). Root and any nested group differ only in which directory is "current." There is no genuine second screen — there is one screen whose current directory moves. Modeling it as two screens invented a bridge that must not exist.

The storage/repository layer is already nesting-correct from `nested-group-drill-down` (`createGroup(parentDir)`, `scanChildGroups`, parent-aware `dissolveGroup`/`moveOutOfGroup`, write-target params on save/get-file). This change reworks only the UI/navigation layer on top and removes the racy bridge.

## Goals / Non-Goals

**Goals:**
- One `MainScreen` composable serves the root feed and every group level, parametrized by `currentDir: File?`.
- Fix the in-group-capture bug: a photo/video/audio captured from a group lands in that group, even though the capture route sits on the back stack above the group.
- Fix the drill/back race: `currentGroupDir` is set exactly once per route entry, never cleared by composition disposal.
- Hide the Public/Private toggle inside a group; keep it at root. Encryption behavior unchanged.
- Preserve back-stack/process-death correctness by keeping the route as the carrier of `currentDir`.

**Non-Goals:**
- No storage, repository, or DB changes.
- No change to group naming, picker, search, or per-entry long-press actions.
- No cross-month/cross-space nesting.
- No re-parenting or drag-to-reorder UI.

## Decisions

### Decision 1: Single `MainScreen(currentDir)`; route is the carrier, not a second screen
`MainScreen` gains `currentDir: File?` (null = root). `Screen.GroupDetail`'s composable body calls the same `MainScreen` with `currentDir = File(Uri.decode(groupPath))` and an `onBack = popBackStack`. `GroupDetailScreen.kt` is deleted.

- *Why unify rather than fix the dispose:* the dispose is not the bug — it's a *symptom* of two screens sharing one scope field. Any two-screen design needs a bridge; any bridge is racy across the camera sibling route and across push/pop ordering. Unifying removes the bridge entirely.
- *Why keep the route at all:* the back stack must record each open level so system-back pops one level and process death restores the exact level. The encoded `groupPath` is the persistent carrier of `currentDir`. The route and the single screen are not in tension: the route carries *which* dir, the composable is *how* it renders.

### Decision 2: `currentGroupDir` set once on enter, never cleared by composition
`MainScreen` holds exactly:

```kotlin
LaunchedEffect(currentDir) { viewModel.setCurrentGroupDir(currentDir) }
```

No `DisposableEffect`/`onDispose`. Returning to root is achieved by navigating back to `Screen.Main`, whose composable calls `MainScreen(currentDir = null)`, and that `LaunchedEffect(null)` sets the field to null.

- *Why this fixes bug 1:* the `Camera` route is navigated *to* (pushed on top), not replacing the group screen. The group composable stays in the back stack and is **not** disposed when the camera screen is shown (Navigation Compose keeps back-stack destinations in saved state, and a pushed destination does not dispose the one beneath). So `currentGroupDir` is never cleared during capture, and `getPhotoFile()`/`getVideoFile()` resolve against the right group. (Note: even where compose may dispose a backgrounded destination to save memory, restoring it re-runs `LaunchedEffect(currentDir)` and re-sets the field before the user can capture — there is no window where it's wrong and observable.)
- *Why this fixes bug 2:* there is one setter call per route entry, ordered by the nav lifecycle, not two competing effects across two instances. Push A→B: A's effect already ran long ago; B's effect runs on its entry. Back B→A: A's restored composable re-runs its `LaunchedEffect(A)`. No dispose ever fires to clobber.

### Decision 3: Scope derivation is local to `MainScreen`
`MainScreen` derives its visible data from `currentDir` rather than reading a second scope flag:

- members = `uiState.entries.filter { it.groupPath == currentDir?.absolutePath }` (null path → root loose entries; the existing `EntryList` already filters `groupPath == null` for the root case, so this is one expression).
- group cards = `if (currentDir != null) uiState.childGroups else uiState.groups`.

- *Why keep both `groups` and `childGroups` in state:* `groups` (month-top) feeds the move-picker and merge-picker which must always list root-level groups regardless of which screen is open; `childGroups` is the per-page sub-group set. They have different lifetimes and consumers; collapsing them would force the picker to recompute. Leave the dual fields; only the *rendering* unifies.

### Decision 4: Top bar branches on `currentDir`
Root (`currentDir == null`) renders the existing `CenterAlignedTopAppBar` (search/stats/settings). An open group renders a `TopAppBar` with a back `IconButton` (arrow-back) and the group name from `FileMetadata.fromFile(currentDir)?.originalName ?: currentDir.name` (falling back to "未命名分组"). This is the old `GroupDetailScreen` top bar, moved into the `currentDir != null` branch.

- *Why not a shared slot abstraction:* one `if (currentDir != null) ... else ...` is simpler and matches the existing two top-bar styles. Extracting a shared component is premature.

### Decision 5: `InputBar.showSpaceToggle` gates the toggle
Add `showSpaceToggle: Boolean = true` to `InputBar` (and `InputBarActions` if cleaner). `MainScreen` passes `currentDir == null`. Inside `InputBar`, wrap the `SpaceSwitchButton` (currently `InputBar.kt:267`) in the existing `AnimatedVisibility` pattern, visible when `showSpaceToggle`.

- *Why hide rather than disable:* a disabled toggle implies a state the user could reach; hidden correctly signals "space switching isn't a thing here." Capture/import stay enabled — they are space-bound to the current space, just not toggleable in-place.

## Risks / Trade-offs

- **[Risk] A backgrounded group composable disposed under memory pressure could momentarily null the field if the dispose path were still wired.** → *Mitigation:* the dispose path is removed entirely (Decision 2). Re-composition on restore re-runs `LaunchedEffect(currentDir)`. No observable wrong window.
- **[Risk] `setCurrentGroupDir` is still a public VM method; future callers could re-introduce a lifecycle-driven write.** → *Mitigation:* its docstring states "called once per route entry from MainScreen's LaunchedEffect only." Code review gate.
- **[Trade-off] Root and group now share one large `MainScreen` composable, increasing its branching.** The `@Suppress("LongMethod")` already present absorbs the modest growth; the `currentDir`-keyed `if/else` for the top bar is the only new branch.
- **[Trade-off] Two list-state fields (`groups`, `childGroups`) persist.** Justified by their distinct consumers (picker vs page); collapsing would regress picker correctness.

## Migration Plan

- Pure UI/navigation refactor; no data, schema, or storage change. Existing on-disk groups (month-top or nested) render identically.
- Rollback: revert the `MainScreen`/NavGraph/InputBar edits and restore `GroupDetailScreen.kt`. No on-disk state depends on the unification.
- Reconciliation with `nested-group-drill-down`: that change's design/tasks describe the two-screen model that this change supersedes. On archive, the merged `nested-group-drill-down` spec reflects the unified model (this change's delta is the source of truth for the screen/navigation requirements).

## Open Questions

- None blocking. (Decisions A–D confirmed with the user: single-screen + route carrier; hide-only space toggle; default back; in-place doc update of the prior change's artifacts.)
