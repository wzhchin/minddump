## Why

MindDump has no lightweight, cross-cutting way to classify entries, and no way for a
timestamp to come back and nudge the user. Entries can only be found by date or by
full-text search; a thought recorded today has no path back to the user a week later.
This change introduces a per-entry **metadata sidecar** (`m` role) that carries two
orthogonal concerns — **tags** (flat, stackable classification) and **scheduled events**
(time-based reminders that fire regardless of the entry's todo status). Together they
turn a flat chronological dump into something that can be filtered forward in time and
that reaches back out when a moment arrives.

## What Changes

- **New filename role `m` (meta)** — a sidecar `{ts}-m.yaml` (or `{ts}-m.yaml.enc` in
  Private) that is paired to its owner entry by **timestamp alignment**. One timestamp
  in one directory = one owner (`f`/`n`/`g`) + at most one `m.yaml`, stored **sibling
  to** the owner (flat, not nested), and **not inherited** by children.
- **New sidecar schema** carrying `tags` (flat list, `[A-Za-z0-9一-鿿-]`,
  dedup case-insensitive) and `events` (a list of `{due, state, trigger}` where
  `due` is local-time, `state ∈ {pending, fired, snoozed}`, `trigger` v1 is `once` only).
- **Groups become Room-indexed entries** — `g` directories currently only appear via
  live `scanChildGroups`; they now get a `GROUP` row so a group can own tags/events
  like any file entry.
- **Reconcile reads sidecars** into owner fields (tags/events), and the stale-entry
  comparison switches from the owner file's `lastModified` to `max(owner.mtime, meta.mtime)`
  so editing only the sidecar still refreshes Room.
- **Private sidecars are lazy-decrypted** — unencrypted-in-memory only after unlock;
  until then Private tags/events are not queryable or schedulable (locked = silent).
- **Scheduled events fire via exact alarms** — `AlarmManager` (`setExactAndAllowWhileIdle`)
  for precise-time reminders; a separate `WorkManager` track is reserved for future
  imprecise/batch jobs (Digest, Watch Folder). Two notification channels: high-priority
  "提醒 / Reminders" and low-priority "汇总 / Digest".
- **Permissions**: `USE_EXACT_ALARM` (declared) + runtime `POST_NOTIFICATIONS`.
- **`BOOT_COMPLETED` re-registration** of all pending events, combined with the existing
  "unlock-then-register" flow for Private events.
- **Tag UX**: detail-page tag chips with autocomplete; tag as a composable filter
  dimension in the list/search surface.
- **BREAKING** (storage format, not on-disk history): the `m` role is a new filename
  token; existing files are unaffected and require no migration (sidecar is optional).

## Capabilities

### New Capabilities
- `entry-metadata-sidecar`: the `m` role — timestamp-aligned YAML sidecar (optionally
  encrypted), its pairing rule, its disk layout, and the contract that it is optional,
  not inherited, and lazily decrypted in Private.
- `entry-tags`: flat tag classification stored in the sidecar — authoring, dedup
  semantics, and cross-entry filtering.
- `scheduled-events`: time-based reminders stored in the sidecar — scheduling via exact
  alarms, the "locked = silent" rule for Private, restart survival, and the deep link
  back to the owning entry.

### Modified Capabilities
- `file-naming-format`: add the `m` role token to the naming grammar.
- `file-metadata-parsing`: parse the `m` role into `FileMetadata`.
- `database-rebuild`: index `GROUP` directories as rows; read sidecars into owner
  tags/events columns; compare via `max(owner.mtime, meta.mtime)`.
- `entry-search`: support filtering by tag as a dimension (Private tags unavailable
  until unlock).

## Impact

- **Storage layer** (`FileStorageEngine`, `FileMetadata`): new role, sidecar scanning
  and pairing, group indexing.
- **Data layer** (`MindDumpDatabase`, `EntryEntity`, `EntryDao`): schema migration adding
  `tags`, `events` (JSON/serialized) columns and `GROUP` rows; FTS unaffected.
- **Repository** (`MindDumpRepository.reconcileWithDisk`): pairing + lazy decrypt +
  mtime comparison change.
- **Security** (`CryptoEngine`): sidecar encrypt/decrypt path alongside existing entry
  encryption.
- **New scheduling subsystem**: `AlarmManager`-based scheduler, `BOOT_COMPLETED`
  receiver, two notification channels, notification tap deep link.
- **Manifest**: new permissions, new receiver, notification channels created at startup.
- **UI**: tag chip row + autocomplete in detail/edit; tag filter chip in list/search;
  event authoring + fired-state writeback.
- **Strings**: zh-CN (`res/values/strings.xml`) and en (`res/values-en/strings.xml`) for
  channel names, event UI, tag UI.
- **No on-disk migration**: existing entries keep working; sidecars are created on
  demand and their absence means empty tags/no events.
