# Implementation Tasks — split-entries-god-table

## 1. Domain model & tid foundation

- [x] 1.1 Add `GROUP` value to `EntryType` (`storage/MindDumpEntry.kt`); keep `EntryRole` but mark it filename-parsing-only (no DB column).
- [x] 1.2 Add `tid`/`parentId` to the `MindDumpEntry` domain model; remove `groupPath`. (Type stays; comments carry their own domain shape.)
- [x] 1.3 Create `TidCodec` helpers in `storage/` (name TBD, e.g. `Tid.kt`): `tidOf(secondResolutionTimestamp: String, collisionOffset: Int): Long` (epoch-millis of the parsed `yyMM-dd-HHmmss` padded by 000, plus offset) and the inverse display formatter. Pure functions, unit-tested.
- [x] 1.4 Add a filename collision-offset parser: given a filename stem like `2506-13-143022-f` or `2506-13-143022-f_1`, return `(timestamp, offset)`. Reuse `uniqueFile` suffix convention (`_1`,`_2`,…).
- [x] 1.5 Unit tests: same-second entries get distinct tids; `tid` round-trips for a given filename; cross-second uniqueness holds.

## 2. Room entities (v6 schema)

- [x] 2.1 Rewrite `EntryEntity` (`data/EntryEntity.kt`): `tid: Long` PK; keep `filePath`(UK), `type`, `space`, `monthFolder`, `lastModified`, `isEncrypted`, `isPinned`, `todoState`, `metaEncrypted`, `contentPreview`; add `parentId: Long?`; **remove** `id`(autogen), `role`, `timestamp`, `groupPath`, `tags`, `events`, `targetTimestamp`. Indices: `UNIQUE(filePath)`, `(space, monthFolder)`, `(space, type)`, `(parentId)`.
- [x] 2.2 Create `CommentEntity` (`data/CommentEntity.kt`): `tid: Long` PK, `targetTid: Long`, `filePath`(UK), `space`, `contentPreview`, `lastModified`, `isEncrypted`. No `monthFolder`/`timestamp`/`role`.
- [x] 2.3 Create `TagEntity` (`data/TagEntity.kt`): composite PK `(tid, tag)`; FK `tid → entries.tid` ON DELETE CASCADE.
- [x] 2.4 Create `EventEntity` (`data/EventEntity.kt`): `id: Long` autogen PK, `tid: Long` FK, `due: String`, `state: String`, `trigger: String`. FK ON DELETE CASCADE.
- [x] 2.5 Delete `EntryTagsCodec` and `EntryEventCodec` from `EntryEntity.kt`; delete/repurpose `META_TAGS_SEPARATOR` (fixes the empty-string bug). Keep sidecar encoding (`MetaYamlCodec`) — that is the on-disk authority, unchanged.
- [x] 2.6 Keep `EntryFts` content-linked to `EntryEntity` (`contentPreview` + `monthFolder`); no change to its columns.
- [x] 2.7 Update `MindDumpDatabase`: `version = 6`, `entities = [EntryEntity, CommentEntity, TagEntity, EventEntity, EntryFts]`, `exportSchema = true`.

## 3. DAO layer

- [x] 3.1 Split `EntryDao` into per-table DAOs (or keep one `EntryDao` with table-specific methods) — add `TagDao` and `EventDao`, and a `CommentDao` for comment queries.
- [x] 3.2 Replace `getByRole` with `getGroups(space)` (`WHERE type = 'GROUP'`); add `getByType`/count helpers as needed.
- [x] 3.3 `getEntriesInGroup` → `getMembers(parentId)` (`WHERE parentId = :tid`); add `getMembersSnapshot`.
- [x] 3.4 `getCommentsFor` → `comments` table, `WHERE targetTid = :tid`.
- [x] 3.5 `getByTag` → join `tags` table (`SELECT e.* FROM entries e INNER JOIN tags t ON e.tid = t.tid WHERE e.space = :space AND t.tag = :tag`); `distinctTags` → `SELECT DISTINCT tag FROM tags` joined to entries for space scoping.
- [x] 3.6 Replace single-row insert helpers with per-table inserts; add `setTags(tid, tags)` / `setEvents(tid, events)` that delete-then-insert under the tid; add `clearAll()` covering all four tables.
- [x] 3.7 Keep `rebuildFtsIndex()`; verify FTS content-sync triggers still fire on `entries` writes.
- [x] 3.8 Statistics queries (`getEntryCountByDay`, `getEntryCountByType`, `getHourlyDistribution`) — these read `timestamp`; since the column is gone, re-derive the timestamp/date from `tid` (format `tid`→`yyMM-dd`) or recompute `monthFolder` grouping. Update SQL accordingly and keep the stats UI working.

## 4. Filesystem engine

- [x] 4.1 `scanEntries` (`storage/FileStorageEngine.kt`): emit `type == GROUP` for group directories (not `role == GROUP`); keep META sidecars paired-and-folded (do NOT create a row for them).
- [x] 4.2 Ensure each scanned entry carries the collision-offset needed for `tid` (parse from the `_N` suffix; 0 when absent).
- [x] 4.3 Add/adjust a `tid`-derivation call site in scan output so the repository can compute `tid` from `(timestamp, offset)`.

## 5. Repository — rebuild & insert paths

- [x] 5.1 Rewrite `reconcileWithDisk` / `rebuildDatabase` for two-pass topological insert per space: (a) ancestor-first group rows, (b) member rows resolving `parentId` to the group tid whose `filePath` == member parent dir; (c) comment rows into `comments` with `targetTid`; (d) tags/events into their tables from sidecars.
- [x] 5.2 `clearAll` must wipe `entries`, `comments`, `tags`, `events` (order respects FK).
- [x] 5.3 Rewrite insert paths — `saveTextEntry`, `saveComment`, `registerMediaFile`, `importFile` — to compute `tid` and write the correct table(s); tag/event authoring writes sidecar + refreshes the `tags`/`events` rows.
- [x] 5.4 `onPrivateUnlocked` backfill: decrypt Private sidecars and populate `tags`/`events` rows + clear `metaEncrypted` for those owners.
- [x] 5.5 `distinctTags`, `getEntriesByTag`, `searchEntries` (content GLOB on `contentPreview`) re-pointed at new schema.

## 6. Repository — group operations (parentId replaces groupPath)

- [x] 6.1 `createAndMoveToGroup`: create dir + move file; insert group row (compute tid) and set member `parentId`.
- [x] 6.2 `moveToGroup` / `moveOutOfGroup`: update member `parentId` (not `groupPath`); move file on disk.
- [x] 6.3 `renameGroup`: rename dir on disk + update the group row's `filePath` only (O(1)); members' `parentId` unchanged.
- [x] 6.4 `dissolveGroup`: set members' `parentId` to the dissolved group's own `parentId` (parent-aware: parent group tid, or null for month-top); delete the group row + dir; relocate nested sub-groups preserving their internal nesting.
- [x] 6.5 Resolve the navigation `currentDir` (a directory path) to the group's `tid` on entry (filePath → tid lookup) for membership queries.

## 7. Scheduler (events addressed by id)

- [x] 7.1 `EventScheduler`: register/cancel alarms by `event.id` (the `events` row id) instead of `EntryEvent.key()` (`due#state`).
- [x] 7.2 `markEventFired`: update `events.state` row and persist to sidecar (sidecar remains authority).
- [x] 7.3 Re-registration paths (BOOT_COMPLETED, Private unlock) read `events` table pending rows and register by id.

## 8. UI / ViewModel

- [x] 8.1 `MindDumpViewModel.buildSummaries`: derive group cards from `entries` where `type == GROUP`; scope by `parentId`.
- [x] 8.2 `groupEntriesWithComments`: comments now from `comments` table by `targetTid`; group under owner `tid`.
- [x] 8.3 Tag editor (`openTagEditor`/`addEntryTag`/`removeEntryTag`) + Event editor (`openEventEditor`/`addEntryEvent`/`removeEntryEvent`): operate via repository methods that touch the `tags`/`events` tables + sidecar.
- [x] 8.4 `MainScreen` membership predicate: `grouped.entry.parentId == currentGroupTid` (root → `parentId == null`), replacing `groupPath == currentDir?.absolutePath`.
- [x] 8.5 `EntryItem` tag/event rendering (`:571`, `:1278`): source from the new model (tags as a list — unchanged at the UI layer).
- [x] 8.6 Any UI consuming entry `timestamp` for display: derive from `tid` (format back to display string) since the column is gone.

## 9. Schema export & migration wiring

- [x] 9.1 Run a build to export `app/schemas/.../6.json`; confirm it matches the entity set.
- [x] 9.2 Wire destructive rebuild-on-version-mismatch: ensure a v5 install upgrades by dropping + `rebuildDatabase()` (reuse the existing rebuild flow + progress dialog). Verify no `fallbackToDestructiveMigration` misuse under Room3.
- [ ] 9.3 Manual verify on a populated v5 DB: upgrade → app launches → destructive rebuild → entries/groups/comments/tags/events all repopulate; data unchanged on disk.

## 10. Verification

- [x] 10.1 `./gradlew detekt ktlintCheck` clean.
- [x] 10.2 Unit tests: `TidCodec` (Decision 1 edge cases); repository rebuild of a nested `A → B → member` tree asserting `parentId` chains; same-second `tid` uniqueness within a space.
- [ ] 10.3 Regression sweep: capture text/photo/audio/import; tag + event authoring; group create/rename/move/dissolve/nest; comment authoring; search; rebuild-from-settings; Private lock/unlock event backfill.
- [ ] 10.4 Min-SDK (API 29) smoke test if touching lifecycle/permissions paths (not expected here).
