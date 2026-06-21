## Why

The persistence layer carries a synthesized identity (`tid` = epoch-millis + per-directory offset) layered on top of the filesystem, plus two extra entity kinds (`n` comments, multi-level group nesting). These add concepts, parsing, and a latent cross-directory tid-collision bug — none of which the product needs. The filesystem already gives every note and group a globally unique, human-readable, `grep`-able identity: its path. This change removes the synthetic layer and the extra entities, leaving a restrained design where **file content + metadata sidecar + directory = the entire data model**, fully legible to plain Unix tools.

## What Changes

- **BREAKING**: Identity becomes the file path (stored relative to `workDir`). The `tid` concept, `Tid.kt`, and all stem/offset decoding are deleted. The `entries` primary key changes from `tid` to `filePath`.
- **BREAKING**: `n` comments are removed entirely — the role, the `{targetTs}-n-{nowTs}` filename mechanism, `parseCommentStem`, the `comments` table, `resolveCommentTargetTid`, and all comment UI/saving. Existing `n` files on disk are not migrated (discarded).
- **BREAKING**: Groups are constrained to a single level. `parentId` becomes `groupPath` (single-level), `dissolveGroup` drops recursion, and the scanner enters a group once and collects only notes — no sub-group recursion.
- note vs group is distinguished purely by physical form: a file is a note, a directory is a group. The role token collapses to `f` (the `g` token is removed).
- Metadata sidecar pairing changes from `{ts}-m.yaml` to **owner's full name + `.meta`** (e.g. `2506-15-100000-f.md` → `2506-15-100000-f.md.meta`). Pairing becomes a stateless pure function `owner.name + ".meta"`, which is structurally conflict-free (a same-second note and group have different full names, so their sidecars never collide — fixing the current same-second shared-sidecar ambiguity). Encrypted sidecars become `….meta.enc`.
- All entry renames (pin `9999-`, todo status, slug edits) funnel through a single `renameEntry()` storage-engine function that renames owner and its `.meta` together. Pairing is derived (not recorded); a crash mid-rename leaves at worst an orphaned sidecar, which the next reconcile treats as garbage and clears. Owner content is never lost.

Preserved unchanged: `YYYY-MM/` month buckets, the transparent encryption layer, filename-encoded pin/status/role tokens, `MetaYamlCodec` (tags + events/reminders parsing, fail-closed), `fallbackToDestructiveMigration(dropAllTables=true)` + rebuild-from-disk, the `tags` table, the `events` table, and `entries_fts`.

## Capabilities

### New Capabilities
<!-- None. This is a reduction, not an addition. -->

### Modified Capabilities
- `database-rebuild`: rebuild/reconcile operates on `filePath` identity (no `tid`), drops comment reconciliation, and treats same-second entries without synthetic offset.
- `entry-metadata-sidecar`: sidecar is paired by owner full name + `.meta` (stateless, conflict-free) instead of `{ts}-m.yaml`.
- `file-naming-format`: role token collapses to `f` (groups are directories, not a `g` role); sidecar suffix becomes `.meta`.
- `file-metadata-parsing`: parsing derives identity from path rather than from a synthesized `tid`; no comment-stem parsing.
- `comment-presentation`: **REMOVED** — the comment capability is deleted (delta expresses removal).
- `nested-group-drill-down`: nesting collapses to one level; `groupPath` replaces `parentId` chains; sub-group cards/creation removed.
- `entry-relationships`: membership/nesting expressed by `groupPath`, single-level only; comment relationships removed.

> Note: `entry-tags` and `scheduled-events` requirements are unchanged at the product-behavior level (tags/events still live in the sidecar; filtering/scheduling work as before). The `tid`→`filePath` re-keying of their Room relations is an implementation detail covered by `database-rebuild` and `design.md`, not a spec-level requirement change.

## Impact

- **Code**: delete `Tid.kt`; rewrite `EntryEntity`/`CommentEntity`→removed/`TagEntity`/`EventEntity` (`EntryEntity.kt`); rewrite `FileMetadata` parsing; rewrite `FileStorageEngine` sidecar pairing + add `renameEntry()`; rewrite `MindDumpRepository` reconcile (drop comments, drop tid resolution, drop multi-level group walk); remove comment screens/components; adjust `EntryDao` queries; remove `commentTargetTs`/`tid` from `MindDumpEntry`.
- **DB schema**: Room version bump. `entries` PK `tid`→`filePath` (drop `tid` column); `comments` table dropped; `entries.parentId`→`groupPath`; `tags` PK `(tid,tag)`→`(filePath,tag)`, FK `tid`→`filePath`; `events` FK `tid`→`filePath`. Handled by `fallbackToDestructiveMigration` + rebuild-from-disk (no hand-written migration).
- **On-disk files**: no format migration. Same-second note/group sidecars gain the `-{role}`/full-name pairing implicitly (filename collision rules already hold). Old `n` comment files remain on disk but are no longer parsed or shown — user may delete manually.
- **External behavior**: app rebuilt from disk on next launch (existing destructive path); no user data loss for notes/groups. Scheduler re-registers events by new path identity.
