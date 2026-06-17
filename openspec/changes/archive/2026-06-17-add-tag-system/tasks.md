## 1. Filename role and metadata parsing

- [x] 1.1 Add `META("m")` to the `EntryRole` enum in `FileMetadata.kt`
- [x] 1.2 Extend the `FILE_PATTERN` regex role char group from `([fng])` to `([fngm])`
- [x] 1.3 Ensure `m` parses with `originalName == null`, `.yaml` extension, and inherits the optional `.enc` suffix
- [x] 1.4 Update `FileMetadata` KDoc to document the `m` sidecar role and timestamp-alignment pairing
- [x] 1.5 Add unit tests: parse `2506-13-143022-m.yaml` → role=META; parse `...-m.yaml.enc` → role=META, isEncrypted=true

## 2. Sidecar data model and YAML codec

- [x] 2.1 Choose YAML approach (minimal lib vs constrained hand-rolled parser) per design OQ1
- [x] 2.2 Define `EntryMeta(tags: List<String>, events: List<EntryEvent>)` value type with `EMPTY`
- [x] 2.3 Define `EntryEvent(due: LocalDateTime, state: EventState, trigger: EventTrigger)` and the `pending|fired|snoozed` / `once` enums
- [x] 2.4 Implement sidecar serializer: `EntryMeta` ↔ YAML matching the design schema (`tags: [...]`, `events: [{due, state, trigger}]`)
- [x] 2.5 Implement sidecar parser that fails closed (malformed → `EntryMeta.EMPTY`, logs via Timber) and tolerates missing `tags`/`events`
- [x] 2.6 Add unit tests: round-trip tags+events; tags-only; events-only; malformed input returns EMPTY

## 3. Tag value validation

- [x] 3.1 Implement tag validator: allowed `[A-Za-z0-9一-鿿-]`, reject spaces/`#`/`/`
- [x] 3.2 Implement case-insensitive dedup with original-casing preservation for display
- [x] 3.3 Add unit tests for valid/invalid tags and dedup behavior

## 4. Room schema migration

- [x] 4.1 Add `tags`, `events`, `metaEncrypted` columns to `EntryEntity` (serialized `String`/`Boolean`, default empty/false)
- [x] 4.2 Bump `MindDumpDatabase` version and author a `Migration(N → N+1)` (non-destructive) that `ALTER TABLE` adds the columns
- [x] 4.3 Verify FTS table (`EntryFts`) is unaffected; no FTS migration needed
- [x] 4.4 Add a migration test that opens an old fixture DB at version N and confirms the new columns exist at N+1

## 5. Storage: scan, pair, read sidecars

- [x] 5.1 Extend `FileStorageEngine.scanEntries` to emit `GROUP` rows (currently skipped) so groups are indexed
- [x] 5.2 In `scanEntries`, collect `m.yaml`/`m.yaml.enc` sidecars into a per-directory, per-timestamp map, skipping orphans (no owner, or >1 owner at a timestamp)
- [x] 5.3 Add `MindDumpEntry` fields for `tags`, `events`, `metaEncrypted`; fold paired sidecar content into the owner entry
- [x] 5.4 For Private encrypted sidecars, do NOT decrypt during scan; set `metaEncrypted=true` and leave tags/events empty (lazy)
- [x] 5.5 Add `FileStorageEngine` methods to read/parse a sidecar at a known path (used by writeback and unlock-time decrypt)

## 6. Reconcile integration

- [x] 6.1 In `reconcileWithDisk`, write owner `tags`/`events`/`metaEncrypted` into the `EntryEntity` on insert and update
- [x] 6.2 Change stale comparison from `owner.lastModified` to `max(owner.lastModified, sidecar.lastModified)` (or `owner.lastModified` when no sidecar)
- [x] 6.3 Ensure orphan sidecars and `GROUP` rows reconcile cleanly (insert/delete/update) without affecting existing FILE/COMMENT behavior
- [x] 6.4 Verify rebuild-from-Settings still works and excludes `.trash/`

## 7. Sidecar writeback path

- [x] 7.1 Implement `saveSidecar(owner, meta)`: read-or-empty → mutate → write YAML → encrypt in Private → place as sibling `{ts}-m.yaml[.enc]`
- [x] 7.2 Delete the sidecar file when `meta` becomes empty (no tags AND no events)
- [x] 7.3 Route authoring writes through existing storage write paths (off main thread); trigger reconcile refresh
- [x] 7.4 Reuse `CryptoEngine` for Private encryption so keys/IV match the existing scheme

## 8. Private lazy decrypt on unlock

- [x] 8.1 On Private unlock, batch-decrypt all Private owner sidecars and backfill `tags`/`events`/`metaEncrypted=false` in Room
- [x] 8.2 Keep Public tags unaffected by the Private unlock flow
- [x] 8.3 Add tests (or instrumentation) verifying locked Private shows no tags and unlock populates them

## 9. Notification channels and permissions

- [x] 9.1 Add `USE_EXACT_ALARM` to `AndroidManifest.xml` (normal permission)
- [x] 9.2 Declare `<uses-permission>` for `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED`
- [x] 9.3 Create a `NotificationChannels` helper creating the high-importance "提醒 / Reminders" and low-importance "汇总 / Digest" channels at app startup
- [x] 9.4 Create channels from `Application.onCreate` (or Hilt entry) so they exist before any notification posts
- [x] 9.5 Add zh-CN + en strings for both channel names, descriptions, and event UI/notification text

## 10. Event scheduler (AlarmManager)

- [x] 10.1 Implement `EventScheduler` wrapping `AlarmManager.setExactAndAllowWhileIdle` keyed by event id (owner filePath + event index)
- [x] 10.2 Implement `EventAlarmReceiver` (BroadcastReceiver): on fire → post notification (Reminders channel), write back `state=fired` via the sidecar writeback path, cancel the alarm
- [x] 10.3 Build the notification `PendingIntent` deep link that routes to the owning entry's detail screen (space + filePath)
- [x] 10.4 Add a `setExactAlarm` permission guard: if exact alarms are unavailable, log and still transition the event to `fired`
- [x] 10.5 Register all `pending` Public events on app start / after reconcile

## 11. Boot + unlock re-registration

- [x] 11.1 Implement `BootCompletedReceiver` listening for `BOOT_COMPLETED`; re-register all `pending` Public events from Room
- [x] 11.2 In the Private unlock flow (after step 8 backfill), register every Private event whose `due` is in the future; skip past-dues (no retroactive fire)
- [x] 11.3 Ensure registration is idempotent (cancel-then-set) so re-register doesn't stack duplicate alarms
- [x] 11.4 Add the receiver to the manifest with `RECEIVE_BOOT_COMPLETED` intent filter

## 12. UI — tag authoring and filtering

- [x] 12.1 Add a tag chip row + add-tag input with autocomplete (from existing distinct tags in the current space) on the entry detail/edit screen
- [x] 12.2 Validate tag input against the rules from task 3; show error on invalid input
- [x] 12.3 Display tags with a leading `#` and case-insensitive dedup
- [x] 12.4 Add a tag filter chip/dimension to the list/search surface; filter the feed by selected tag scoped to the current space
- [x] 12.5 Add zh-CN + en strings for tag UI

## 13. UI — event authoring

- [x] 13.1 Add an "add event" affordance on the entry detail screen: pick a local date/time → creates a `once` pending event
- [x] 13.2 Request `POST_NOTIFICATIONS` at runtime when the user first schedules an event (if not granted)
- [x] 13.3 Show the event's `due` and `state` on the entry; surface that Private events are silent while locked (a short hint)
- [x] 13.4 Add zh-CN + en strings for event UI

## 14. Verification

- [x] 14.1 Run `./gradlew detekt ktlintCheck` and resolve findings
- [x] 14.2 Run unit tests: filename parsing, sidecar codec, tag validation, Room migration
- [x] 14.3 Manually verify on a device/emulator: add tag, filter by tag, schedule event fires at due time, reboot re-registers, Private lock silences events
- [x] 14.4 Verify no empty `m.yaml` files are left behind after removing the last tag/event
