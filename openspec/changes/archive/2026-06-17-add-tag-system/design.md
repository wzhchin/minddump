## Context

MindDump is a single-activity Compose app backed by a filesystem-as-source-of-truth
model: notes/media live on disk under a `Public/` and `Private/` root, organized as
`YYYY-MM/{ts}-f|n|g-...`. Room is explicitly an index cache that is rebuilt from disk by
`MindDumpRepository.reconcileWithDisk`. CLAUDE.md codifies two invariants this change must
not violate:

1. **Filesystem is the single source of truth**; Room is a rebuildable cache. Any new
   persisted state must live on disk first and be re-derivable.
2. The filename already carries pin + timestamp + todo-state + role + original-name +
   extension + encryption. Adding another *structural* dimension to the filename is
   discouraged.

Two gaps motivate this change: there is no cross-cutting classification (entries are only
findable by date or full-text search), and there is no way for a recorded thought to
reach back out to the user at a later moment. The todo *state* encoded in the filename
(TODO/DOING/...) describes a lifecycle stage but triggers nothing.

Current relevant code:
- `FileMetadata.fromFile` parses `f`/`n`/`g` roles; the regex group for the role char is
  `([fng])` and `.enc` suffix is already optional for all roles.
- `FileStorageEngine.scanEntries` indexes only `FILE` and `COMMENT` (it explicitly skips
  `GROUP`, which is enumerated separately by `scanChildGroups` and never gets a Room row).
- `reconcileWithDisk` does a three-way path-keyed diff (insert new / delete orphans /
  update where `dbEntity.lastModified != diskEntry.file.lastModified()`).
- `EntryEntity` is the Room row; FTS lives in a separate `EntryFts` table.

## Goals / Non-Goals

**Goals:**
- A per-entry metadata sidecar that stores tags and scheduled events, on disk, git-trackable,
  and encrypted in Private.
- Tags: flat, stackable, filterable across a space; case-insensitive dedup.
- Events: fire at a precise local time via a user-visible notification, survive reboot,
  and deep-link back to the owning entry.
- Zero migration for existing data: a sidecar is optional; its absence = empty tags, no events.
- Group directories become Room-indexed so they can own tags/events like any entry.

**Non-Goals:**
- Hierarchical/nested tags (e.g. `project/sub`).
- Tag inheritance from a group to its children.
- Recurring events (`trigger: recurring`) — reserved field, not implemented in v1.
- Snooze UX (`state: snoozed` is defined but not produced by v1 UI).
- Inline `#tag` parsing from note bodies (a possible v2 enhancement layered on top).
- Digest (#7) and Watch Folder (#4) job logic — only the *shared notification channel*
  foundation is built here; their WorkManager jobs come later.
- Tag/event authoring on comments (`-n-`): supported by storage, but v1 UI exposes it on
  the detail page of owner entries.

## Decisions

### D1. Metadata lives in a sidecar `m` role, paired by timestamp alignment

A new filename role `m` denotes a YAML sidecar. Pairing is **timestamp alignment**: the
sidecar shares its owner's timestamp and sits as a sibling in the same directory.

```
Public/2026-06/
  2506-13-143022-n-x.md        ← owner (text)
  2506-13-143022-f-photo.jpg   ← owner (file)  [a different timestamp]
  2506-13-150000-g-项目A/       ← owner (group, a directory)
  2506-13-150000-m.yaml         ← the group's meta (sibling, same ts)
```

- **Why sibling, not nested**: `reconcile` pairs by `(directory, timestamp)`; a flat
  sibling is the simplest pairing and works identically for files and directories.
  Nesting a group's meta inside the group would require special-case recursion.
- **Why timestamp alignment (A) over a filename-target scheme (B)**: the chosen rule is
  "one timestamp in one directory = exactly one owner + at most one `m.yaml`". This
  forbids two owners sharing a timestamp in the same directory, which is already the de
  facto invariant for `f`/`g` (comments `-n-` disambiguate via their `-n-{targetTs}`
  suffix and do not collide). Alignment needs zero extra filename fields.
- **Not inherited**: a tag/event on a group applies to the group entry only, never to its
  children. Keeps filtering semantics simple and avoids double-counting.

Alternatives rejected:
- *Room-only tags*: violates the "filesystem is source of truth" invariant; lost on
  rebuild / new device.
- *Tags in the filename*: tags are one-to-many and variable-length; the filename is a
  fixed-shape token sequence already near capacity.
- *Inline `#tag` in note bodies*: only works for text entries, not media/groups.

### D2. Sidecar schema (YAML), with `events` as a list

```yaml
tags: [idea, 项目A]
events:
  - due: 2026-06-20T09:00      # local time, no offset
    state: pending             # pending | fired | snoozed
    trigger: once              # v1: only "once"
```

- `tags`: flat list. Allowed chars `[A-Za-z0-9一-鿿-]` (letters/digits, CJK, hyphen);
  no spaces, no `#`, no `/`. Stored without `#`; UI prepends it on display. Dedup is
  case-insensitive (`项目A` and `项目a` are one tag; original casing preserved for display).
- `events`: a **list** even though v1 authors one at a time. Choosing a list now avoids a
  future object→list schema migration when multi-event authoring lands. `state` lives
  per-event so each is tracked independently.
- `due` is local wall-clock time without a timezone offset; it fires by device timezone,
  matching a personal-reminder mental model.
- Empty/missing `tags` and/or `events` are both valid; a sidecar may carry only one.
- Parsing: a small YAML reader. To avoid pulling a heavyweight YAML dependency, the
  schema is constrained to the two shapes above; a hand-rolled parser (or a minimal
  `kaml`/`snakeyaml` dep) reads it. Decision deferred to implementation; default is a
  minimal dependency if a maintained Kotlin YAML lib is available and small.

### D3. Reconcile treats the sidecar as owner fields, not as an entry (A1)

`m.yaml` is **not** its own Room row. `scanEntries` parses the paired sidecar and folds
`tags`/`events` into the owner's `MindDumpEntry`. Consequences:

- `EntryEntity` gains `tags: String` (serialized, e.g. newline- or JSON-joined) and
  `events: String` (serialized) columns, plus `metaEncrypted: Boolean`.
- The stale-detection comparison in `reconcileWithDisk` changes from
  `dbEntity.lastModified != owner.lastModified` to
  `dbEntity.lastModified != max(owner.lastModified, sidecar.lastModified)`, so editing
  only the sidecar still refreshes Room.
- Orphan sidecars (no owner at that timestamp in that directory) are dropped during scan,
  the same way unrecognized files are skipped today.

### D4. Groups become Room rows (G1)

`scanEntries` is extended to emit a `GROUP` row per `g` directory (currently skipped at
`:419`). The list/group UIs that read via `scanChildGroups` keep working (they read the
filesystem directly), and additionally gain the ability to filter/group by the group's
own tags. `EntryType`/`EntryRole.GROUP` already exist; only indexing changes.

### D5. Private sidecars are lazily decrypted (B2)

Private space content is AES-256-GCM encrypted at rest; a sidecar there is
`{ts}-m.yaml.enc`. Decrypting requires the in-memory password/key. Policy:

- **Tags**: not decrypted until Private is unlocked. While locked, a Private owner shows
  empty tags with `metaEncrypted = true`; tag filtering/search does not surface Private
  matches. This is the security-correct behavior: locked content must not be inferable.
- **Events**: likewise not decrypted while locked. **Locked = silent**: a Private event
  does not fire while the space is locked. On unlock, all Private sidecars are decrypted
  and all `pending` events with a future `due` are registered with the scheduler; any
  whose `due` already passed while locked do **not** retroactively fire (consistent with
  "locked = silent" — see Risk R3 for the boundary).

This is a deliberate product trade-off: full encryption purity wins over "always fires".
An always-fires Private event would require storing `due` outside the encrypted sidecar,
which would re-introduce a structural filename dimension and leak timing metadata.

### D6. Scheduling: exact alarms for events, WorkManager reserved for batch/imprecise jobs

- Events need a **precise** moment ("9:00am reminder"). `AlarmManager.setExactAndAllowWhileIdle`
  survives Doze for the moment of firing; `WorkManager` would defer firing into Doze
  maintenance windows (minutes-to-tens-of-minutes late), which breaks a reminder.
- `WorkManager` is reserved for future imprecise/batch jobs (Digest, Watch Folder). It is
  **not** wired up in this change beyond reserving the concept — only the **shared
  notification channels** (D7) are built now so both subsystems use them.
- **Restart survival**: `AlarmManager` alarms are cleared on reboot. A `BOOT_COMPLETED`
  receiver re-registers all `pending` Public events from Room immediately on boot, and
  registers Private events at the next Private unlock (which already triggers decrypt +
  register). This composes with the unlock flow from D5.
- **On fire**: the receiver cancels its alarm, posts the notification (Reminders channel),
  and writes back `state: fired` to the sidecar (and to Room) so a restart or re-reconcile
  does not re-fire it. Crypto/write is off the main thread (per project rules).

### D7. Two notification channels + permissions

- Channel **提醒 / Reminders** (high importance — sound + heads-up) for event firings.
- Channel **汇总 / Digest** (low importance — silent/quiet) reserved for Digest/Watch
  Folder. Created at app startup alongside the Reminders channel.
- Permissions: `USE_EXACT_ALARM` (normal permission, granted at install, suitable for
  reminder-class apps; no runtime prompt) + `POST_NOTIFICATIONS` (runtime prompt on
  Android 13+, requested before the user first schedules an event).
- **Deep link**: the notification's `PendingIntent` carries enough to route to the owning
  entry's detail screen (`space` + `filePath`/timestamp). For Private entries, the event
  only fires while unlocked, so the target is reachable — no encrypted-deep-link problem.

### D8. Sidecar writeback path

Authoring tags and authoring/scheduling events both write the sidecar via the existing
storage write path (mirroring how text is saved). Sequence: read existing sidecar (or
empty) → mutate → write back (encrypt in Private) → update owner `lastModified` impact is
automatic via the sidecar's own mtime → reconcile refreshes Room. Event `state` writeback
on fire uses the same path.

## Risks / Trade-offs

- **R1 — Two owners colliding on one timestamp in one directory.** The "one owner per
  timestamp" invariant is enforced by the naming grammar for `f`/`g`; comments disambiguate
  via `-n-{targetTs}`. If a sidecar finds two candidate owners, scan treats it as an orphan
  (dropped) and logs. → *Mitigation*: scan counts owners per timestamp; >1 ⇒ orphan.
- **R2 — Sidecar bloat / extra files.** Every tagged entry adds one file. Acceptable
  (git-diffable, small); mitigate by only writing a sidecar when there is non-empty
  content. An entry with no tags and no events has no `m.yaml`.
- **R3 — Private event missed because the space was locked at `due`.** This is intended
  ("locked = silent") but a user who sets a reminder and never unlocks will never be
  reminded. → *Mitigation*: document this; the unlocked-time registration still catches
  all *future* dues. (Retroactively firing missed Private events is explicitly out of scope.)
- **R4 — Doze / exact-alarm revocation.** Users can disable exact alarms in system
  settings (with `SCHEDULE_EXACT_ALARM` we'd be subject to this; `USE_EXACT_ALARM` is
  harder to revoke and fits a reminder app). → *Mitigation*: use `USE_EXACT_ALARM`; if the
  user denies `POST_NOTIFICATIONS`, the alarm still fires and the event transitions to
  `fired` (no visible notification) — surface a one-time hint to enable notifications.
- **R5 — YAML parsing robustness.** Hand-edited sidecars may be malformed. → *Mitigation*:
  parser fails closed (treat malformed as empty meta, never crash the app/log); `reconcile`
  logs and skips.
- **R6 — Schema migration correctness.** Adding columns + indexing groups bumps the Room
  version; the project uses destructive migration (Room3 has no Migration API, and disk is
  source of truth). → *Mitigation*: the post-upgrade `reconcileWithDisk` repopulates
  `tags`/`events`/`metaEncrypted` and `GROUP` rows from sidecars on disk, so no data is
  lost (it lives on disk, not in the dropped DB). Covered by the existing rebuild path +
  scan unit tests.

## Migration Plan

- **No on-disk data migration.** Existing entries have no sidecar; they read as empty tags
  and no events. Sidecars are created lazily on first tag/event authoring.
- **Room migration N→N+1**: bump the `@Database` version. Room3 has no Migration API and
  the project already uses `fallbackToDestructiveMigration(dropAllTables = true)`; since
  the filesystem is the source of truth, a destructive rebuild is repopulated from disk
  (sidecars + groups) on next launch via the existing `reconcileWithDisk` flow. No
  hand-written `ALTER TABLE`. FTS table unchanged.
- **Rollback**: revert the code; existing sidecars on disk become inert unrecognized
  `m.yaml` files (ignored like any non-conforming filename today). No data loss.

## Open Questions

- OQ1: Which YAML library? Preferred: a small maintained Kotlin YAML lib; fallback is a
  constrained hand-rolled parser for the two known shapes. Resolved at implementation start.
- OQ2: Tag color/label metadata in v1? Decision: **no** — plain text tags only.
- OQ3: Event list editing UI for multiple events? v1 UI authors one event; the list schema
  supports more, surfaced later.
