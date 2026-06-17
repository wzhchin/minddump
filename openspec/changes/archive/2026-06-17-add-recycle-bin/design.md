# Design — add-recycle-bin

## Goal

Convert irreversible `file.delete()` into recoverable soft delete, with a retention
window and manual purge, keeping the filesystem as the single source of truth and
adding no Room schema.

## Non-goals (this batch)

- Scheduled background purge via `WorkManager`/`AlarmManager` (deferred to the
  notifications batch, ROADMAP #7/#8). This batch purges opportunistically on the
  existing startup/reconcile path.
- A side database of trashed-item metadata. The original path is reconstructable
  from the trashed file's preserved location; age from its last-modified time.
- Decrypting trashed entries for display. The trash list shows filename metadata
  only; encrypted items remain encrypted at rest in `.trash/`.

## Trash directory layout

```
<rootDir>/.trash/<Public|Private>/<originalMonth>/<optionalGroupNesting>/<filename>
```

The trashed file keeps the **exact relative path it had under its space root**.
Example:

- Live: `<root>/Public/2026-06/2506-13-143022-f.md`
- Trashed: `<root>/.trash/Public/2026-06/2506-13-143022-f.md`

- Live: `<root>/Public/2026-06/2506-13-120000-g-trip/2506-13-121000-f.jpg`
- Trashed: `<root>/.trash/Public/2026-06/2506-13-120000-g-trip/2506-13-121000-f.jpg`

Group directories are trashed as whole trees (their member files travel with them),
mirroring how they live on disk. This makes restore trivial: move the trashed node
back under its space root at the preserved relative path.

### Why preserve the full relative path

1. **Restore is path-invertible.** `restore = move(trashPath, rootDir/space/relPath)`
   — no metadata record needed, no "where did this come from?" guesswork.
2. **Group integrity is free.** A trashed group directory and its members keep their
   nesting, so a group restore is a single tree move, not a member-by-member rebuild.
3. **No collision ambiguity at trash time.** Two entries can never collide in trash
   because they could never have shared the same live path.

### Collision at restore time

The original live path may be occupied again (user created a new entry, or the
timestamp collided after a rename). On restore, if the target exists, append a
`_1`, `_2`, … suffix to the filename portion (reuse the existing `renameEntryFile`
collision convention) so restore never overwrites. Restore always succeeds.

## Scanning must skip `.trash`

`scanEntries`/`scanChildGroups` currently walk every child of the space root /
month dir. Two places to guard:

1. The top-level month-dir enumeration under `<root>/<space>/` is unaffected
   (`.trash` lives under `<root>/`, a sibling of `Public`/`Private`, so it is
   never a child of a space dir). **Decision: place `.trash/` under the root, not
   under each space.** This keeps the per-space scans untouched — the strongest
   guarantee that trash never leaks into the live feed.
2. `countFiles` / `walkTopDown` style helpers that traverse the whole root must
   exclude `.trash` if their counts are user-facing as "live files". Inspect each.

`.cache/` is already a sibling hidden dir under root and is already excluded from
normal flows by convention; `.trash/` follows the same pattern.

## Retention and purge

- **Retention window:** 30 days (a constant, easy to tune later; expose nothing to
  settings UI in this batch to stay in scope).
- **Age source:** a trashed file's `lastModified()` is **refreshed at trash time**
  (achieved by `renameTo`, which updates mtime, or an explicit `setLastModified`
  if the platform zeroes it). Purge compares now − mtime > 30d.
- **When purge runs:** opportunistically, at the start of `reconcileWithDisk` for
  each space and once at app startup (piggyback on the existing startup reconcile
  in `MindDumpViewModel` init / `onCreate`). This guarantees eventual cleanup
  without a scheduler, and bounds the worst case to "next time the user opens the
  app". Documented limitation in the spec.
- **Group purge:** a trashed group directory older than the window is deleted
  recursively (tree and all members).

## Entry identity and Room

- **Trash → remove from Room now.** Trashing deletes the Room row immediately
  (the entry leaves the feed). Restore re-indexes it via the normal scan/reconcile
  on next refresh (it is back on disk at a live path). This keeps Room an exact
  index of live files, consistent with the disk-is-truth principle.
- **No schema change.** No `isTrashed` column; trashed state is implicit in the
  file living under `.trash/`.

## Interaction with comments and groups

- **Comment under a trashed file:** comments live in the same directory as their
  target. Trashing a single file entry does **not** move its sibling comments
  (they may belong to other entries or be orphans). This matches current hard-delete
  behavior, which also left sibling comments behind. No change in semantics.
- **Trashing a group directory:** the whole directory tree is moved to trash, so
  its member files (and nested sub-groups/comments) travel with it and restore
  together. This is strictly better than today, where dissolving/deleting could
  orphan members.
- **Multi-select trash:** each selected entry is trashed independently; a selected
  group is trashed as a tree.

## UI

- **Entry actions drawer / delete dialog:** keep the existing "删除" action and
  confirmation dialog; the only change is the verb's effect (trash, not destroy).
  The dialog copy moves from "确定要删除" irreversibility to soft-delete framing,
  with a hint that items can be restored from trash.
- **Settings → 回收站 (Trash):** opens a list of trashed items grouped by space,
  each row showing type icon, filename, and "trashed N days ago". Row actions:
  恢复 (Restore), 永久删除 (Delete forever, with confirm). Screen action: 清空回收站
  (Empty trash, with confirm). Empty state string when nothing is trashed.
- **Trash list data:** comes straight from `listTrashed()` over the filesystem —
  no Room involvement. Sorted by trashed date, newest first.

## Edge cases

- **`.trash` under a user-changed work directory:** trash is always relative to the
  *current* root, so if the user changes the work dir, old trash travels with the
  old root (the `migrateTo` copy already moves the whole root tree, including any
  `.trash`). No special handling.
- **Purge while an entry is mid-restore:** restore is a synchronous rename on the IO
  dispatcher; purge runs on the same dispatcher before reconcile, so they do not
  overlap within a single reconcile call. No locking needed.
- **Encrypted entries:** stay encrypted in `.trash/`; restore returns them still
  encrypted, and the normal decrypt-on-view path handles them once restored. Trash
  list never decrypts.

## Verification hooks

- Unit test `FileStorageEngine`: trash moves file to `.trash/<space>/relPath`,
  `scanEntries` no longer lists it, restore brings it back, purge removes
  >30d-old items, empty-trash clears all, delete-forever removes one.
- ktlint/detekt clean; `assembleRelease` builds.
