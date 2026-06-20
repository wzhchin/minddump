## Why

The `entries` table is a god-table: a single relation carries four `EntryRole` kinds (FILE / COMMENT / GROUP / META), and tags + events are crammed into two separator-joined `TEXT` columns. That shape makes tag/event queries brittle (the `META_TAGS_SEPARATOR` ended up an empty string — the join/split codec is effectively broken), forces `O(members)` rewrites of every member's `groupPath` string whenever a group is renamed, and gives the index an unstable autogen `id` that breaks foreign-key references whenever the DB is rebuilt. Splitting by responsibility — with a rebuild-stable `tid` primary key and a `parentId` self-reference — fixes all three at once and stays within the project's "filesystem is the single source of truth; Room is a rebuildable index" contract.

## What Changes

- **BREAKING** — Replace the single `entries` table with four relations: `entries` (FILE + GROUP rows), `comments` (COMMENT rows), `tags`, and `events`.
- **BREAKING** — Introduce `tid` (epoch-millis, second-resolution with a same-second offset) as the primary key of `entries` and `comments`, and as the foreign key of `tags` / `events`. `tid` is derived deterministically from the file's second-resolution timestamp plus its collision-suffix offset, so it survives a full rebuild-from-disk unchanged.
- **BREAKING** — Remove the `role` column; entry kind is now expressed by `type` (`EntryType` gains a `GROUP` value). `EntryRole` is retired from the DB layer (it survives only in filename parsing).
- **BREAKING** — Replace the `groupPath` absolute-path string with an integer `parentId` FK (→ `entries.tid`). A single field expresses both group membership (member file → its group) and group nesting (child group → parent group).
- **BREAKING** — Remove the per-row `tags` / `events` separator-joined columns and the `EntryTagsCodec` / `EntryEventCodec`. Tags become `(tid, tag)` rows; events become per-event rows addressed by an autogen `id` (the `EntryEvent.key()` due+state addressing is retired).
- **BREAKING** — Migrate the Room schema to version 6 via destructive rebuild-from-disk (Room3 has no automatic migration; the filesystem is the source of truth). On first launch after upgrade the old DB is dropped and repopulated from both spaces.

## Capabilities

### New Capabilities
<!-- None: this refactor splits an existing capability's storage model. No new product behavior is introduced. -->

### Modified Capabilities
Only capabilities whose **requirement text** changes (not just the implementation behind it):
- `database-rebuild`: the rebuild now populates four tables from disk; entry identity is the rebuild-stable `tid`; `parentId` is derived from directory containment during a two-pass (groups-then-members) topological insert; the old `role` / `targetTimestamp` / `groupPath` columns are gone, and tags/events are read into their own tables.
- `nested-group-drill-down`: the membership predicate (`groupPath == dir path`) becomes `parentId == open group's tid`, since the `groupPath` column no longer exists. The route carrier (`group_detail?groupPath=`) is unaffected — it carries a directory path, not a DB column.
- `group-list-loading`: "the database record SHALL be updated with the new file path and `groupPath`" becomes updating `parentId`, since `groupPath` is removed.

### Implementation-only changes (no spec delta; recorded here for traceability)
The following storage-layer changes alter **no product behavior** described in their specs, so they carry no spec delta — they are covered in `design.md`:
- `entry-tags`: tags move from a joined-string column to a `tags` relation table; filtering, dedup, and display behavior are unchanged.
- `scheduled-events`: events move to an `events` relation table addressed by an autogen `id`; firing, reboot re-registration, and Private-lock behavior are unchanged.
- `group-card`: membership stats are computed via `parentId` instead of `groupPath`; card rendering is unchanged.
- `comment-presentation`: comments live in a `comments` table linked by `targetTid`; collapsed/expanded and orphan behavior are unchanged.

## Impact

- **Storage / DB** (`data/`): `MindDumpDatabase` version 5 → 6; `EntryEntity` restructured (drop `role`, `timestamp`, `groupPath`, `tags`, `events`, `targetTimestamp`, autogen `id`; add `tid`, `parentId`); new `CommentEntity`, `TagEntity`, `EventEntity`; `EntryDao` split into per-table queries; `EntryFts` content-entity stays on `entries` (keeps `contentPreview`); new schema export `6.json`. Destructive migration on first launch.
- **Repository** (`data/MindDumpRepository.kt`): `reconcileWithDisk` / `rebuildDatabase` rewritten for two-pass topological insert and `tid` derivation; insert paths (`saveTextEntry`, `saveComment`, `registerMediaFile`, `importFile`) write the right table; group ops (`renameGroup`, `dissolveGroup`, `moveToGroup`, `createAndMoveToGroup`) flip from rewriting `groupPath` to rewriting `parentId`; `distinctTags` and `onPrivateUnlocked` backfill rewritten for the `tags` / `events` tables.
- **Filesystem engine** (`storage/FileStorageEngine.kt`): `scanEntries` emits `type == GROUP` for directories; `tid` parse/format helpers (second-resolution timestamp ↔ epoch-millis, plus same-second offset from the `uniqueFile` `_1`/`_2` suffix).
- **Domain model** (`storage/`): `EntryType` gains `GROUP`; `EntryRole` demoted to filename-parsing only; `MindDumpEntry` carries `tid` / `parentId` instead of `groupPath`.
- **Scheduler** (`scheduling/EventScheduler.kt`): alarm addressing switches to the event-row `id`.
- **UI** (`ui/`): `MindDumpViewModel` (`buildSummaries`, `groupEntriesWithComments`, tag/event editors), `MainScreen` (`groupPath == currentDir` → `parentId` membership), `EntryItem` (tag rendering) adapt to the new model.
- **No filesystem format change**: filenames stay second-resolution `[9999-][yymm-dd-HHMMSS]-[STATUS]-f…`; `tid` is derived, not stored in the name.
