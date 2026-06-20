## Context

The MindDump index is a Room3 database whose **single source of truth is the filesystem** — Room is a rebuildable cache. Room3 has no automatic migration path; schema evolution happens by dropping and rebuilding from disk. The current schema (v5) stores everything in one `entries` table:

- Four `EntryRole` kinds share one relation (FILE, COMMENT, GROUP, META — though META never gets a row; its sidecar is folded into the owner's `tags`/`events` columns).
- `tags` and `events` are crammed into two `TEXT` columns joined by `META_TAGS_SEPARATOR` — which currently **evaluates to the empty string** (`storage/EntryMeta.kt:54`), so the join/split codec is effectively broken for multi-tag entries.
- Group membership uses an absolute-path string column `groupPath`. Renaming a group rewrites every member row (`MindDumpRepository.renameGroup`, ~lines 612–631).
- The primary key is an autogen `id` that **changes on every full rebuild**, so it cannot be used as a stable foreign key.

This change splits the god-table into four relations, replaces the autogen `id` with a rebuild-stable `tid`, and replaces the `groupPath` string with an integer `parentId` self-reference. The filesystem format is **not** changed.

See `proposal.md` for motivation and the capability deltas.

## Goals / Non-Goals

**Goals:**
- Four relations (`entries`, `comments`, `tags`, `events`) with clear responsibility separation.
- A primary key `tid` that is **identical before and after a full rebuild-from-disk**, so it is a valid FK target and `parentId` references survive rebuilds.
- `parentId` (integer self-FK) replaces `groupPath` (string), so group rename/dissolve is O(1) on the group row instead of O(members).
- Retire `EntryRole` from the DB layer; entry kind is expressed by `EntryType` (gaining a `GROUP` value). `EntryRole` survives only in filename parsing.
- Tags and events become first-class relation rows (fixing the empty-separator codec bug as a side effect).
- All of the above within the fs-as-truth contract: no filename format change, destructive rebuild migration only.

**Non-Goals:**
- Changing the on-disk filename format. Filenames stay second-resolution `[9999-][yymm-dd-HHMMSS]-[STATUS]-f[-{name}].{ext}[.enc]`. `tid` is derived, not stored in the name.
- A normalized `tag` entity table (`tags(id, name)` + `entry_tags(tagId, tid)`). Tags stay as a flat `(tid, tag)` relation — there is no tag-management surface today to justify the extra join. (Trivial to upgrade later.)
- Preserving the autogen `id`. It is intentionally dropped; `tid` replaces it everywhere.
- Manual row-by-row data migration. The schema migrates by destructive rebuild.
- FTS redesign. `entries_fts` stays content-linked to `entries` via `contentPreview` + `monthFolder`.

## Decisions

### Decision 1: `tid` = second-resolution epoch-millis + same-second offset

**Choice.** `tid: Long` is `epochMillisOf(secondResolutionTimestamp) + sameSecondOffset`, where the timestamp is the file's `yyMM-dd-HHmmss` value (padded to millis with `000`), and the offset is the integer encoded in the `uniqueFile` collision suffix (`_1`, `_2`, …; no suffix → `0`).

**Why.** A PK must be rebuild-stable to serve as an FK target. The only rebuild-stable input is what is recoverable from the filename, and the filename is second-resolution. A pure "millis of `now()`" PK is **not** rebuild-stable (the millis are never stored), so it was rejected. Padding the second to millis and adding a per-second offset yields a PK that:
- Is deterministic from the filename alone (timestamp + collision suffix).
- Distinguishes entries created within the same second (the offset comes from `uniqueFile`'s existing `_1`/`_2` logic at `storage/FileStorageEngine.kt:96–104`).
- Survives a full rebuild because both inputs come from the name, not from `now()`.

**Alternatives considered.**
- *Pure epoch-millis of `now()` as PK, not stored in filename.* Rejected: not rebuild-stable; the millis are lost the moment the file is named. Rebuild would re-derive from the second-resolution name and collide for same-second entries. This would put `tid`'s stability outside the fs-as-truth contract (it would be DB-only state).
- *Millisecond-resolution filenames (`HHmmssSSS`).* Rejected: changes the `file-naming-format` capability, requires renaming every existing file, and conflicts with the git-backed history. Cost dwarfs benefit.
- *Keep autogen `id` as PK, store parent's `tid` as a plain column (logical FK).* Rejected: two key schemes coexisting (`id` for some FKs, `tid` for `parentId`) is confusing, and `parentId` pointing at a non-PK column weakens referential integrity. One key (`tid`) everywhere is cleaner.

**Edge: comment `tid`.** A comment is named `{targetTs}-n-{nowTs}.md`. Its `tid` derives from its own `nowTs` (plus its own collision suffix), exactly like a file. `targetTid` derives from the owner's `targetTs`. Same-second comments are distinguished by their suffix offset.

### Decision 2: `parentId` is a self-reference on `entries`; one field, two roles

**Choice.** `parentId: Long?` on `entries` references another row's `tid` (or is null at root). For a `type == GROUP` row it points at the **parent group's** `tid` (nesting); for a member file it points at its **owning group's** `tid` (membership).

**Why.** This unifies the two relationships the old `groupPath` conflated (membership and nesting) into one integer mechanism. `groupPath` as an absolute path forced O(members) rewrites on rename; `parentId` makes rename O(1) (only the group's own `filePath`/`tid` row changes; members keep their `parentId`).

**Why one field and not two (`parentGroupId` + `owningGroupId`).** The relationships are mutually exclusive by `type`: a GROUP row's parent is always a group (or root); a file row's parent is its owning group (or root). Splitting would require a CHECK constraint and two nullable columns for what is a single containment edge. One nullable FK is the natural model.

**Rebuild derivation.** Because `tid` is derived purely from a filename (`Tid.tidOfStem`), the filesystem scan can resolve `parentId` **during** the depth-first walk: a group's tid comes from its directory name, and that tid becomes the `parentTid` passed to its descendants. This means `reconcileWithDisk` is **single-pass** — no two-pass topological insert is needed, because every `parentId` is computable from ancestor directory names without any DB insert ordering. (The original design assumed a two-pass insert; the filename-derivable `tid` made it unnecessary.)

Comments (`comments` table) are inserted independently — their `targetTid` is derived from the owner's `targetTs`, no ordering dependency on `entries` insert order beyond the owner row existing for display join (the FK is to `entries.tid`, but a dangling `targetTid` degrades gracefully to the existing "orphan comment" presentation rather than failing the rebuild).

### Decision 3: `EntryType` gains `GROUP`; `EntryRole` leaves the DB

**Choice.** `EntryType` adds a `GROUP` value. The DB no longer has a `role` column; `type == GROUP` distinguishes container rows from content rows. `EntryRole` (FILE/COMMENT/GROUP/META) is retained in `storage/` **only** for filename parsing (`FileMetadata`), since the `f`/`n`/`g`/`m` role chars are still in the name.

**Why.** With COMMENT moved to its own table and META never stored, `entries` holds only two kinds (content vs group). One discriminator (`type`) suffices. Keeping `EntryRole` as a DB column would be a redundant second discriminator. `EntryType.fromExtension` is unaffected (it keys off extension and never produces `GROUP`).

### Decision 4: Tags and events become relation tables

**Choice.**
- `tags(tid PK/FK, tag PK)` — composite primary key, flat strings, no tag entity table.
- `events(id PK autogen, tid FK, due, state, trigger)` — one row per scheduled event, addressed by `id`.

**Why.** The joined-string columns are the source of the empty-`META_TAGS_SEPARATOR` bug and make tag/event queries contortion (`GLOB '*<sep>tag<sep>*'`). Relation rows give ordinary equality/join queries and per-event addressing.

**Why `events` keeps an autogen `id` while `entries` uses `tid`.** An event's identity is **not** its timestamp (multiple events can share a due time; events get cancelled and re-added). The `EntryEvent.key()` `due#state` derivation is fragile (state transitions change the key). An autogen `id` gives stable per-event addressing for the `EventScheduler`'s cancel-then-set logic. This `id` is **not** rebuild-stable — but events are re-derived from sidecars on every rebuild and re-registered with the scheduler from scratch, so rebuild instability is acceptable for events (unlike `entries`, where `parentId` needs stability).

**Sidecar is still the source of truth for tags/events.** Tags and events are written to the `m` sidecar on disk (unchanged), and the `tags`/`events` tables are rebuilt from it. The tables are an index, not an authority — consistent with the fs-as-truth contract.

### Decision 5: Destructive rebuild migration (v5 → v6)

**Choice.** `MindDumpDatabase` version bumps to 6 with **no migration objects**. On first launch after upgrade, Room3 cannot open the v5 DB against the v6 schema, so the index is dropped and rebuilt from both spaces via the existing `rebuildDatabase()` path (`clearAll` → per-space `reconcileWithDisk` → `rebuildFtsIndex`).

**Why.** Room3 removed the imperative migration machinery (no `Migration` callbacks / `SupportSQLiteDatabase`). The fs-as-truth contract means the index is disposable. A destructive rebuild is the only schema-evolution mechanism the project has, and it is already the user-facing "重建数据库" feature — this change simply triggers it on version mismatch.

**Alternatives considered.**
- *Hand-written data move (`INSERT … SELECT` into new tables).* Rejected: Room3 has no migration callback surface, and the data is trivially rebuildable from disk. Writing migration SQL would be throwaway work that fights the project's direction.

## Risks / Trade-offs

- **[Two-pass rebuild ordering bug] → wrong `parentId`.** If groups are not inserted ancestor-first, a child group's `parentId` cannot resolve. **Mitigation:** walk directories depth-first; insert each group row before descending into it. Add a repository test that rebuilds a nested `A → B → member` tree and asserts `parentId` chains.
- **[Same-second offset collision across months] → duplicate `tid`.** Two files with the same second-resolution timestamp in different month folders get the same base epoch-millis; offsets are per-directory (`uniqueFile` checks within one dir). Across months they could collide. **Mitigation:** `tid` uniqueness is enforced within a **space** (the natural rebuild unit). A `UNIQUE(tid)` index on `entries` scoped appropriately, plus reconcile resolving a genuine cross-month collision by bumping the offset. Document that `tid` is unique within a space, not globally.
- **[Comment whose owner was deleted before rebuild] → dangling `targetTid`.** **Mitigation:** `comments.targetTid` is a logical FK (no `FOREIGN KEY` constraint with cascade); a dangling target renders as the existing "orphan comment" card. This preserves the `comment-presentation` orphan behavior.
- **[Destructive migration loses pending event state] → `state: fired` events look pending again.** The `events` table is rebuilt from sidecars, and sidecars store `state`. As long as fired-state is persisted to the sidecar (current behavior), it survives. **Mit:** verify `markEventFired` writes state to the sidecar, not only the DB column (it does — events are sidecar-sourced).
- **[Large library, slow first-launch rebuild] → user waits.** **Mitigation:** the rebuild is already a known, non-cancellable operation with a progress dialog (see `database-rebuild` spec). No new UX needed; just ensure the version-mismatch trigger reuses that flow.
- **[Autogen event `id` not rebuild-stable] → scheduler re-arms all events on every rebuild.** **Mitigation:** acceptable and already the model — `BOOT_COMPLETED` and unlock both re-register from scratch. Document that rebuild re-registers events (no behavior change vs. today's reboot path).

## Open Questions

- Should `tags` carry an index on `tag` (for `distinctTags` aggregation performance) in addition to the `(tid, tag)` primary key? **Leaning yes** — cheap, and distinct-tag queries are common. Decide at implementation time based on a measured query cost.
- The route `group_detail?groupPath={path}` carries a directory **path** (not a DB column). After this change, opening a group requires resolving that path to a group row's `tid` to scope `parentId` queries. Is a `filePath → tid` lookup (already `UNIQUE(filePath)`) sufficient, or should navigation switch to carrying `tid`? **Leaning: keep the path** (it survives rebuild and is what the back stack already encodes); resolve to `tid` on entry. Defer a `tid`-based route to a future change.
