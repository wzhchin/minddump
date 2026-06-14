# Design — add-pin-and-todo

## Goal

Add two organizing levers to the MindDump feed — **pin** (keep on top) and **todo status** (track open work) — encoded entirely in filenames, consistent with the project's "filesystem is the source of truth, Room is an index cache" principle. No side store. No disk migration.

## Filename schema

```
[9999-][yymm-dd-HHMMSS]-[STATUS]-[f|n|g]-<name>.<ext>[.enc]
```

Position assignments are chosen so the three free-text-ish fields never collide:

| Slot | Position | Domain | Notes |
|------|----------|--------|-------|
| Pin prefix | before timestamp | literal `9999-` or absent | sort sentinel |
| Timestamp | fixed `yyMM-dd-HHmmss` | digits + dashes | unchanged |
| Status | after timestamp, before role | `TODO\|DOING\|WAIT\|DONE\|CANCEL` (uppercase, multi-char) or absent | optional |
| Role | single char | `f\|n\|g` (lowercase) | never collides with uppercase status |
| Name | after role | free text (may contain `-`, `.`, uppercase words) | only consumed when present |
| Extension | after name | `\w+` | unchanged |
| Encryption | trailing | `.enc` or absent | unchanged |

**Non-collision argument (the crux of why this schema is safe):**
- Status tokens are uppercase multi-char; the role char is lowercase single-char. The regex `(?:([A-Z]+)-)?` is anchored immediately before `[fng]`, so a status token is only recognized when followed by `-` then a role char.
- The free `<name>` sits *after* the role char. So an imported file literally named `TODO-list.pdf` becomes `2506-13-143022-f-TODO-list.pdf` — the regex consumes the role `f`, then `TODO-list` falls into the name slot. The status group reads nothing. Verified by the spec scenario "Status-like text in the original name is not misread."

## Regexes

`FileMetadata.FILE_PATTERN` (replaces current):
```
^(9999-)?(\d{4}-\d{2}-\d{6})-(?:(TODO|DOING|WAIT|DONE|CANCEL)-)?([fng])(?:-(.+?))?\.(\w+)(\.enc)?$
```
Groups: 1=pin, 2=timestamp, 3=status, 4=role, 5=name, 6=ext, 7=enc.

`FileMetadata.DIR_PATTERN` (replaces current; groups now support pin + status):
```
^(9999-)?(\d{4}-\d{2}-\d{6})-(?:(TODO|DOING|WAIT|DONE|CANCEL)-)?g(?:-(.+))?$
```
Groups: 1=pin, 2=timestamp, 3=status, 4=name.

A comment filename `{ts}-n-{ts}.md` matches FILE_PATTERN with role `n`; the status group is absent, pin absent — comments parse as `isPinned=false, todoState=NONE`, which is exactly what we want.

## Data model

New enum in `storage` package:
```kotlin
enum class TodoState(val code: String?) {
    NONE(null),
    TODO("TODO"),
    DOING("DOING"),
    WAIT("WAIT"),
    DONE("DONE"),
    CANCEL("CANCEL");
}
```
`NONE.code == null` because no token is written for plain entries.

`FileMetadata` gains: `isPinned: Boolean`, `todoState: TodoState`.

`MindDumpEntry` gains: `isPinned: Boolean = false`, `todoState: TodoState = TodoState.NONE`.

`EntryEntity` gains: `isPinned: Boolean = false`, `todoState: TodoState = TodoState.NONE`. Bumps `MindDumpDatabase` version 3 → 4. Existing Room3 pattern (`StorageModule.kt:46-49`: `fallbackToDestructiveMigration(dropAllTables = true)`) handles the schema change — the DB is dropped and rebuilt from disk via the `database-rebuild` capability, repopulating `isPinned`/`todoState` by re-parsing filenames. No hand-written migration (consistent with `minddump-room3-dropped-legacy-sqlite` memory: Room3 has no `SupportSQLiteDatabase`/Migration callback).

`toEntity`/`toEntry` mappers carry the two new fields.

## File operations (FileStorageEngine)

The existing `saveTextEntry`/`getRecordingFile`/`getPhotoFile`/`getVideoFile`/`importFile`/`createGroup`/`saveComment` builders **keep emitting pin-less, status-less names by default** — a brand-new entry is ordinary until the user explicitly pins/statuses it. This preserves the common case and avoids touching every builder.

Two new rename helpers, mirroring the existing `renameEntry`/`renameGroupDir` pattern (parse → reassemble → `renameTo` → `check`):

```kotlin
fun setPinned(file: File, pinned: Boolean): File   // toggles 9999- prefix
fun setStatus(file: File, state: TodoState): File   // swaps/clears status token
fun setGroupPinned(dir: File, pinned: Boolean): File
fun setGroupStatus(dir: File, state: TodoState): File
```

Both rebuild the name from the parsed `FileMetadata` (`timestamp`, `role`, `originalName`, `extension`, `isEncrypted`) rather than string surgery, so pin + status + encryption + original-name compose correctly. They reuse `uniqueFile`-style collision guarding where relevant.

**Comment cascade**: none. `saveComment` writes `targetTs` = the parent's *real* timestamp (already the case). When a parent is pinned, only the parent is renamed; the comment's `targetTs` still matches the parent's real timestamp (the regex strips `9999-` before extracting the timestamp used for association). Confirmed by the reconcile logic, which joins on the real timestamp, not the pinned filename.

## Sorting

Current: `scanEntries` ends with `sortedByDescending { it.file.lastModified() }` (`FileStorageEngine.kt:348`).

New: sort by the **full filename string descending**, which encodes "pinned block (newest→oldest) then normal block (newest→oldest)" in one pass:
- `9999-…` prefixes sort after every real `yyMM` prefix (`9999` > `9912`, the max real date, because at the 3rd char `9` > `1`).
- Within a block, the timestamp portion is lexicographically time-ordered.

```kotlin
.sortedByDescending { it.file.name }
```

Group lists (`scanChildGroups`) get the same `sortedByDescending { it.name }`.

**Trade-off acknowledged**: this changes ordering semantics from "last-edited floats up" to "stable creation order." The user explicitly chose filename ordering. Editing an entry no longer bumps it; only pinning does. This is documented in the spec scenarios and is the intended behavior.

Comment association is unaffected because it is by `targetTimestamp` field, not sort adjacency.

## UI

Minimal surface, reusing the existing M3 Expressive components:

1. **Entry/group card headers**: add a pin icon (when `isPinned`) and a status badge (when `todoState != NONE`) next to the type icon/timestamp. `DONE`/`CANCEL` use a de-emphasized treatment (e.g. strikethrough or muted chip) to read as closed.
2. **Entry actions drawer**: add two actions — "置顶 / 取消置顶" (toggle) and "待办状态" (opens a chooser). Comments hide both. Groups get the same two via their own action affordance.
3. **Status chooser**: a simple list/`ModalBottomSheet` of the 6 options (5 states + clear), current one marked.
4. **Strings**: add to both `res/values/strings.xml` (zh-CN) and `res/values-en/strings.xml` (en): `pin`, `unpin`, `todo_status`, `status_todo`, `status_doing`, `status_wait`, `status_done`, `status_cancel`, `status_clear`.

## Backward compatibility

No disk migration. Both new slots are optional in the regexes, so:
- `2506-13-143022-f.md` → unpinned, NONE (existing files unchanged in behavior).
- The destructive Room rebuild re-derives fields from filenames on next launch.

## Risks / alternatives considered

- **`P-` marker in the role slot (option A from discussion)**: rejected — only distinguishes within the same second, breaks the role regex, doesn't actually float.
- **`9999-{ts}` prefix (chosen)**: accepted — sentinel sort, zero special-case code, survives rebuild.
- **Status as suffix `.todo`/`.done`**: rejected — user preferred an in-band status token at a fixed slot; multiple states (5) read better as a token than as competing suffixes.
- **Status as a DB-only flag**: rejected — violates filesystem-as-truth; wouldn't survive rebuild or survive a user copying files out.
- **Cascading pin to comments**: rejected — unnecessary; `targetTs` already decouples association from the parent's current name.

## Out of scope

- Status *filtering* UI (a "show only TODO" toggle) — can be added later; the data model supports it.
- Reminders/deadlines on todos.
- Per-status theming beyond the done/cancel de-emphasis.
