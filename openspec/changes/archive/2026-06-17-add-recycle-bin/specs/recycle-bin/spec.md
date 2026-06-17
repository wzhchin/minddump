## ADDED Requirements

### Requirement: Deleting an entry or group moves it to the trash instead of destroying it

The system SHALL NOT permanently delete a file entry, a group directory, or a batch of selected entries on the user's first delete action. Instead it SHALL move the item into a `.trash/` holding directory under the work root, preserving the item's exact relative path beneath its space (month folder, group nesting, filename). The item SHALL disappear from the live feed and be removed from the index immediately. The deletion SHALL remain reversible until the item is explicitly purged or expires.

#### Scenario: Delete a text entry
- **WHEN** the user deletes `2506-13-143022-f.md` located at `<root>/Public/2026-06/`
- **THEN** the file is moved to `<root>/.trash/Public/2026-06/2506-13-143022-f.md`
- **AND** the entry no longer appears in the Public feed
- **AND** the original file location is no longer occupied

#### Scenario: Delete an entry inside a group
- **WHEN** the user deletes `2506-13-121000-f.jpg` located inside group `2506-13-120000-g-trip`
- **THEN** the file is moved to `<root>/.trash/Public/2026-06/2506-13-120000-g-trip/2506-13-121000-f.jpg`
- **AND** its group nesting is preserved in the trash path

#### Scenario: Delete a group directory
- **WHEN** the user deletes group directory `2506-13-120000-g-trip`
- **THEN** the whole directory tree, including all member files and nested sub-groups, is moved to `<root>/.trash/Public/2026-06/2506-13-120000-g-trip/`

#### Scenario: Batch delete in multi-select trashes every selected item
- **WHEN** the user selects three entries and taps Delete
- **THEN** each entry is moved into `.trash/` and removed from the feed

#### Scenario: Encrypted entries stay encrypted in the trash
- **WHEN** the user deletes an encrypted Private entry `2506-13-143022-f.md.enc`
- **THEN** the file is moved to the trash still encrypted (`2506-13-143022-f.md.enc`)
- **AND** no decryption occurs at trash time

### Requirement: The trash is invisible to the live feed and to the index

The system SHALL exclude the `.trash/` directory from every scan, reconcile, and rebuild of live entries. Trashed items SHALL never appear in the main feed, group lists, search, or statistics. A rebuild of the index from disk SHALL not re-introduce trashed items.

#### Scenario: Trashed entry does not appear after refresh
- **WHEN** an entry has been moved to `.trash/`
- **THEN** a refresh and re-scan of its space does not list it

#### Scenario: Rebuild does not re-index trashed items
- **WHEN** the index is rebuilt from disk while `.trash/` contains entries
- **THEN** none of the trashed entries are re-indexed or shown in the feed

### Requirement: A trashed item can be restored to its original location

The system SHALL allow the user to restore a trashed entry or group, moving it back under its space root at the preserved relative path so it reappears in the feed. If the original path is again occupied, the system SHALL append a numeric suffix to the filename rather than overwrite.

#### Scenario: Restore an entry
- **WHEN** the user restores `<root>/.trash/Public/2026-06/2506-13-143022-f.md`
- **THEN** the file is moved back to `<root>/Public/2026-06/2506-13-143022-f.md`
- **AND** the entry reappears in the Public feed

#### Scenario: Restore a group restores its whole tree
- **WHEN** the user restores a trashed group directory
- **THEN** the directory and all its members return to their original live path

#### Scenario: Restore avoids overwriting a colliding new entry
- **WHEN** the user restores a trashed entry whose original path is now occupied by a newer entry
- **THEN** the restored file is written with a `_1` (then `_2`, …) suffix on the filename portion
- **AND** no existing file is overwritten

### Requirement: Trashed items are purged after a retention window

The system SHALL permanently delete trashed items older than a fixed retention window of 30 days, measured from the time the item was trashed. Purging SHALL run opportunistically when the app starts and during disk reconciliation; the system SHALL NOT require a scheduled background job to enforce retention.

#### Scenario: Expired entry is purged on startup
- **WHEN** the app starts and `.trash/` contains an entry trashed 31 days ago
- **THEN** that entry is permanently deleted

#### Scenario: Recent entry survives purge
- **WHEN** a purge runs and `.trash/` contains an entry trashed 5 days ago
- **THEN** that entry remains in the trash

#### Scenario: Expired group is purged with its tree
- **WHEN** a trashed group directory is older than the retention window
- **THEN** the directory and all its members are permanently deleted

### Requirement: The user can manually empty the trash or delete a single item forever

The system SHALL provide a "清空回收站" (Empty trash) action that permanently deletes every trashed item, and a per-item "永久删除" (Delete forever) action. Both SHALL require confirmation, because these operations are irreversible.

#### Scenario: Empty trash with confirmation
- **WHEN** the user taps "清空回收站" and confirms
- **THEN** the entire `.trash/` tree is deleted
- **AND** the trash list becomes empty

#### Scenario: Delete one item forever with confirmation
- **WHEN** the user taps "永久删除" on a trashed item and confirms
- **THEN** only that item is permanently deleted
- **AND** the other trashed items remain

### Requirement: A trash list is reachable from Settings

The system SHALL expose a "回收站" (Trash) entry in Settings that opens a list of all trashed items. Each row SHALL show the item's type, filename, and how long ago it was trashed, and SHALL offer Restore and Delete-forever actions. The list SHALL show an empty state when nothing is trashed. The list SHALL display filename metadata only and SHALL NOT decrypt encrypted items for display.

#### Scenario: Open trash from Settings
- **WHEN** the user taps "回收站" in Settings
- **THEN** a list of trashed items is shown, sorted by trashed time newest-first

#### Scenario: Empty trash state
- **WHEN** the trash is opened and `.trash/` is empty
- **THEN** an empty-state message is shown

#### Scenario: Encrypted item shows metadata only
- **WHEN** the trash list contains an encrypted Private entry
- **THEN** its row shows the filename and type without decrypting its content
