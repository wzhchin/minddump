## Why

The file storage layer has accumulated technical debt: duplicated timestamp/metadata parsing, a monolithic `reconcileWithDisk()` that mixes sync, content extraction, and encryption concerns, and encryption logic that leaks into every save/import path. These make the code fragile and hard to extend (e.g., adding new entry types or storage backends).

## What Changes

- **Consolidate file metadata parsing** into a single `FileMetadata` value class — eliminate 3 duplicate timestamp-extraction sites and scattered `.enc` suffix handling
- **Split `reconcileWithDisk()`** into focused operations: `scanNewFiles()`, `removeOrphanedEntries()`, `updateStaleEntries()`, `refreshContentPreviews()`
- **Encapsulate encryption behind the storage layer** — `FileStorageEngine` becomes encryption-aware so callers (Repository, ViewModel) never deal with `.enc` suffixes or temp decryption files
- **Lazy content preview extraction** — stop reading full file contents during reconciliation; extract previews on-demand or via a background pass
- **Simplify `ShareItem` / `MindDumpEntry` hierarchy** — reduce the number of data classes that carry overlapping fields (type, timestamp, content preview)

## Capabilities

### New Capabilities

- `file-metadata-parsing`: Single source of truth for extracting type, timestamp, space, and encryption status from filenames
- `encryption-transparent-storage`: FileStorageEngine handles encrypt/decrypt internally; upper layers see only plaintext paths

### Modified Capabilities

_None — no existing openspec specs to modify_

## Impact

- **Files**: `FileStorageEngine.kt` (major refactor), `MindDumpRepository.kt` (simplified), `StoragePreferences.kt` (minor), `CryptoEngine.kt` (internal API only), `EntryEntity.kt` / `EntryDao.kt` (possible content-preview changes)
- **No user-visible changes** — all refactoring is internal
- **No database migration** — schema stays the same, only code paths change
- **Dependencies**: none added or removed
