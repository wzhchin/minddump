## Context

The current persistence layer models four entity kinds (`f` notes, `g` groups, `n` comments, `m` sidecars) and overlays a synthesized identity — `tid` = epoch-millis of a second-resolution timestamp + a per-directory collision offset — on top of the filesystem. Three things are wrong with that:

1. **`tid` is a redundant entity.** The OS already gives every file a globally unique, human-readable, `grep`-able identity: its path. `tid` re-invents uniqueness in a Long that must be decoded back out of the filename, adding a whole parsing layer (`Tid.kt`) and a foreign-key target that exists only to support group nesting.
2. **`tid` is not actually safe.** The offset is counted *per directory*. Two notes created at the same second in different month buckets both get offset 0, hence identical `tid`, hence a primary-key collision on `entries` (one silently overwrites the other). `Tid.kt`'s own docstring admits this.
3. **Comments and multi-level groups add complexity the product does not need.** The `n` comment role brings its own filename grammar (`{targetTs}-n-{nowTs}`), a `comments` table, a fragile `targetTid` resolution in reconcile, and UI. Multi-level group nesting needs a `parentId` chain and recursive dissolve.

The product's promise is unchanged and already holds: **the filesystem is the source of truth; Room is a rebuildable index cache.** This change does not add anything; it removes the synthetic identity layer and two entity kinds, leaning fully on a principle the architecture already commits to.

## Goals / Non-Goals

**Goals:**
- Make the file path the identity of every note and group — no synthesized `tid`, no offset decoding.
- Distinguish note vs group by physical form (file vs directory), collapsing the role token to `f`.
- Pair metadata sidecars statelessly by the owner's full name (`owner.name + ".meta"`), so the pairing is conflict-free and cold-rebuildable.
- Remove comments entirely (`n` role, `comments` table, comment UI).
- Constrain groups to a single nesting level.
- Keep the filesystem layout, encryption, `MetaYamlCodec`, tags/events relations, FTS, and the destructive-rebuild strategy exactly as-is.

**Non-Goals:**
- No on-disk format migration. Existing notes/groups are read as-is by the new parser. Existing `n` files are simply not parsed or shown (user may delete them).
- No new capabilities. This is a reduction.
- No change to the encryption scheme, password store, or biometric flow.
- No fix (separate) to *which* filename tokens encode pin/status — those stay in the filename as today.

## Decisions

### Decision 1: Identity = file path (stored as an absolute path)

`entries.filePath` becomes the primary key and holds the **absolute** path of the file (e.g. `/storage/.../MindDump/Public/2026-06/2506-15-100000-f.md`). `tags`/`events` foreign keys reference `filePath`. **One path form everywhere:** `filePath` and `groupPath` are absolute everywhere they are stored or compared — `toEntity`, `moveToGroup`/`groupPathOf`/`indexGroupDir`, `scanEntries`, the DAO keys, and the UI's `currentDir.absolutePath` scope filter. The canonical form is absolute because the rest of the app (UI, FileProvider, scheduler owner path) works in absolute paths; an earlier draft stored `groupPath` relative-to-`workDir` in `scanEntries` only, which a post-merge `reconcileWithDisk` re-stamped and silently emptied the open-group view.

**Why path over `tid`:** the path is already unique (filesystem guarantees it), is already on disk, and needs no decoding. `tid` duplicated that guarantee with a synthesized Long and introduced a real collision bug.

**Why absolute, despite `workDir` moves:** a `workDir` migration already triggers a full rebuild-from-disk (see `database-rebuild`), so every `filePath`/`groupPath` is re-rooted at rebuild time. Absolute paths cost nothing here and avoid the cross-cutting form mismatch a relative form introduced (above). The identity is still intrinsic to the note's *position* in the user's tree; the prefix is rebuilt, not remembered.

**Alternative considered:** keep `tid` but fix the collision by making offset space-global. Rejected — it preserves the synthetic layer and still needs a "never reuse an offset after deletion" discipline that is *unobservable on a cold rebuild* (a scanner seeing `…_1` with `…_0` gone cannot know whether `0` was "deleted-retired" or "never assigned"). Path identity has no such discipline.

**Consequence — rename = new identity:** pinning (adding/removing `9999-`), toggling todo status, or editing a slug changes the filename, hence the path, hence the DB identity. The row for the old path is deleted and a new row for the new path is inserted. This is acceptable because nothing references a note across its lifetime anymore (comments are gone; group membership is positional via `groupPath`, not via a long-lived FK to the note). See Decision 4 for why metadata survives renames.

### Decision 2: note vs group by physical form; role token = `f`

A file is a note; a directory is a group. The filename role token collapses from `f`/`g` to `f` everywhere. The scanner distinguishes notes from groups by `File.isDirectory`.

**Why:** the `g` token duplicated information the filesystem already carries. Removing it removes a parse case and removes the *same-second note-vs-group shared-sidecar* ambiguity at its root (see Decision 3).

### Decision 3: Sidecar pairing by owner full name + `.meta`

The metadata sidecar for an owner `O` is named `O.name + ".meta"` (e.g. `2506-15-100000-f.md` → `2506-15-100000-f.md.meta`; a group `2506-15-100000-f-旅行/` → `2506-15-100000-f-旅行.meta`). Encrypted: append `.enc`.

This replaces the current `{ts}-m.yaml` pairing, which keys only on the timestamp and therefore makes a same-second note and group *share* one sidecar (tags/pin bleed across them).

**Why full name:** the filename is already unique within a directory (filesystem-enforced), so `name + ".meta"` is necessarily unique too. Pairing is a pure function `metaFile(owner) = File(owner.parentFile, owner.name + ".meta")` — no state, no offset bookkeeping, fully cold-rebuildable. A note and a group in the same second have different full names (`…-f.md` vs `…-f-旅行`), so their sidecars never collide. This is conflict prevention by construction, not by discipline.

**Why `.meta` (single suffix) over a `ts+role` or `ts+seq` anchor:** any `ts`-derived anchor reintroduces either the collision problem (ts alone) or the unobservable-reuse problem (ts+seq). The full name borrows the filesystem's own uniqueness guarantee and adds nothing.

`MetaYamlCodec` is untouched — it governs *format* (tags + events/reminders, fail-closed), independent of *naming*. Only `sidecarFileFor()` changes.

### Decision 4: Atomic-ish rename via a single `renameEntry()` entry point; self-heal on the gap

Every entry rename funnels through one storage-engine function:

```
renameEntry(old: File, newName: String): File
  1. validate target uniqueness in old.parentFile
  2. owner.renameTo(newOwner)
  3. if metaFile(old).exists(): metaFile(old).renameTo(metaFile(newOwner))
```

(Group renames use a temp-directory dance to move members atomically first, then rename the meta. Same two-phase shape.)

**OS reality:** `renameTo` moves one file at a time, so steps 2–3 are not instantaneously atomic. A kill between them leaves `newOwner` present but its sidecar still under the *old* name.

**Why that's tolerable:** pairing is *derived*, not *recorded*. On reconcile the owner is always truth. For any `X`: if `X.name` ends with `.meta`, it is a sidecar and its owner is `X` minus `.meta`; otherwise it is an owner and its sidecar is `X.name + ".meta"`. So the crash gap yields two deterministic facts: the new owner is found, its (new) sidecar is absent → the owner reads as a bare note (tags/reminders temporarily empty); the old sidecar is an orphan whose owner no longer exists → reconcile deletes it. **Owner content is never lost; at worst a sidecar (metadata) is dropped and re-created.** This is strictly safer than the current `tid` collision, which can overwrite a note row.

**Alternative considered:** write a journal/lock file to make the rename transactional. Rejected — it adds an entity to guard an entity, against the "do not multiply entities" goal, for a window that self-heals anyway.

### Decision 5: Remove comments entirely

The `n` role, `{targetTs}-n-{nowTs}` grammar, `parseCommentStem`, `comments` table, `resolveCommentTargetTid`, comment UI, and comment saving are all deleted. Existing `n` files on disk are left untouched but un-parsed.

**Why:** comments are the one entity kind that *references another note across its lifetime* — precisely what makes "rename = new identity" (Decision 1) costly. Removing comments removes that long-lived reference, which is what makes path identity free. Per user direction, existing comments are discarded, not migrated.

### Decision 6: Single-level groups

`parentId` becomes `groupPath`: `null` for a note/group at the month-bucket root, else the absolute path of the containing (single-level) group directory (same form as `filePath` — see Decision 1). `dissolveGroup` moves members out one level and deletes the directory — no recursion. The scanner enters a group directory once, collects only notes, and never recurses for sub-groups.

**Why:** multi-level nesting was the only justification for the `parentId` chain and for `tid` as a stable FK target. One level needs neither: membership is positional (a note is "in" a group because it sits in that directory). Enforcing "one level" structurally (scanner doesn't recurse for groups) is cheaper than enforcing it by validation.

## Risks / Trade-offs

- **[Rename changes DB identity]** → Mitigated by Decision 4 (sidecar travels with owner) and Decision 5 (no cross-note references). Cost is a delete+insert per rename instead of an update; reconcile and rebuild already handle this. Acceptable.
- **[Existing encrypted `n` files linger on disk]** → Mitigated by documenting that they are orphaned and user-deletable. No data the user wants is lost (comments are explicitly discarded). They are simply invisible to the app.
- **[Existing same-second sidecars may already have been shared]** → With the `{ts}-m.yaml` scheme retired, a pre-existing shared sidecar is reparsed under the new full-name rule: each same-second owner gets its own `.meta`; if only the legacy shared file exists, both owners read it (the legacy behavior) until one is edited, at which point its own `.meta` is written. No loss; converges.
- **[Cold-rebuild orphan-meta cleanup could delete a sidecar whose owner is mid-rename at scan time]** → Extremely narrow window; outcome is a temporarily bare note that re-gains metadata on next edit/save. Self-heals. Acceptable.
- **[Relative-path identity assumes `workDir` is stable within a session]** → True today; `workDir` change already forces a full rebuild, which re-roots all relative paths consistently.

## Migration Plan

There is **no on-disk format migration** — by design.

1. Ship the new schema + parser. Room version bumps; `fallbackToDestructiveMigration(dropAllTables=true)` drops and recreates all tables on first launch.
2. On next launch, `reconcileWithDisk(PUBLIC)` + `(PRIVATE)` re-scan under the new rules: identity = relative path, note/group by physical form, sidecar by full-name `.meta`, no comments, single-level groups.
3. Existing `n` files are not scanned into `entries` (no `n` role exists); they remain on disk, invisible.
4. Scheduler re-arms Public events by path identity; Private events wait for unlock, as today.
5. **Rollback**: revert the code. The destructive-rebuild path means a downgrade is just another rebuild-from-disk under the old rules; no irreversible on-disk change has occurred (we never rewrote existing files — the only filesystem writes are new notes' own files and sidecars).

## Open Questions

- Should the scanner emit a one-time log/Snackbar count of orphaned `n` files discovered on disk, to nudge the user to clean them up? (Cosmetic; default: silent log.)
