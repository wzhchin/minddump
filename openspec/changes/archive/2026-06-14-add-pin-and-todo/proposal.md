## Why

MindDump currently captures everything into one flat, time-ordered feed. As the dump grows, the entries that actually matter right now (a thing to follow up on, a note to keep at hand) sink to the bottom and become indistinguishable from background noise. Users have no way to mark "this is important, keep it on top" or "this is something I still have to do." Adding **pin** (keep on top) and **todo status** (track open work) — encoded entirely in the filename, consistent with the project's filesystem-is-source-of-truth principle — gives the feed two lightweight, durable organizing levers without introducing a separate database of flags.

## What Changes

- **Pin (置顶)**: a `9999-` prefix on a file/group's filename acts as a sort sentinel. Pinned entries float above all real-date entries with zero special-case sort code, because `9999` sorts after every real `yyMM`.
- **Todo status**: an optional status slot — `TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL` — placed between the timestamp and the role char in the filename. Plain notes omit it; only entries explicitly marked carry a status.
- **New filename schema**: `[9999-][yymm-dd-HHMMSS]-[STATUS]-[f|n|g]-<name>.<ext>[.enc]`. The pin prefix, status slot, and free `<name>` occupy disjoint positions (uppercase status word before the lowercase role char; `<name>` after it) so they never collide.
- **Comments (`n`) are excluded**: comments keep `{targetTs}-n-{nowTs}.md` and are never pinned or statused. Their `targetTs` stores the parent's **real** timestamp, so pinning/unpinning the parent renames only the parent — zero cascade to its comments.
- **Sort basis change**: the main/group feeds switch from last-modified to filename-timestamp ordering. Pinned block (newest→oldest) then normal block (newest→oldest) falls out of one descending filename sort.
- **Backward compatible**: the pin prefix and status slot are both optional, so every existing filename on disk still parses (parsed as unpinned, no status). No disk migration required.
- **UI**: pin toggle + status selector surfaced in the entry actions drawer (and group equivalents); entry/group cards show pin and status badges; the feed and group list render pinned-first.

## Capabilities

### New Capabilities
- `pin-and-todo`: marking entries and groups as pinned and/or carrying a todo status, the filename encoding that persists it, and the feed ordering it drives.

### Modified Capabilities
- `file-naming-format`: filename schema gains optional `9999-` pin prefix and optional status slot; the canonical format string and the scenarios update accordingly.
- `file-metadata-parsing`: `FileMetadata` gains `isPinned` and `todoState` parsed from the new regex groups.
- `entry-card`: card surfaces a pin indicator and a status badge.
- `group-card`: group card surfaces a pin indicator and a status badge.
- `entry-actions-drawer`: drawer gains pin/unpin and set-status actions.
- `group-list-loading`: group list ordering becomes pinned-first by filename timestamp.

## Impact

- **Parsing core**: `FileMetadata.kt` — `FILE_PATTERN` and `DIR_PATTERN` rewritten to the new schema; new `isPinned` / `todoState` fields.
- **Storage**: `FileStorageEngine.kt` — new `togglePin` / `setStatus` rename helpers (mirror existing `renameEntry`/`renameGroupDir`); `scanEntries` sort switches to filename timestamp; existing save/get builders keep emitting pin-less, status-less names by default.
- **Data model**: `MindDumpEntry.kt` adds `isPinned`, `todoState`; new `TodoState` enum. `EntryEntity.kt` adds matching columns; `MindDumpDatabase` version 3 → 4 with destructive-migration-rebuild (existing Room3 pattern — rebuild repopulates from disk, no hand-written migration).
- **Repository**: `MindDumpRepository.kt` scan/reconcile wiring carries the new fields.
- **UI**: entry & group cards, actions drawer, main & group feed ordering; new strings in `res/values/strings.xml` (zh-CN) and `res/values-en/strings.xml` (en).
- **Comment association**: unaffected — `targetTs` already uses the parent's real timestamp, so parent pin rename does not cascade.
- **Existing data**: no migration; old filenames parse as unpinned/no-status.
