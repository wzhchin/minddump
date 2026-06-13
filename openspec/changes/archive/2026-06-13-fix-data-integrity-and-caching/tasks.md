## 1. Room Schema Migration

- [x] 1.1 Add `@Index(value = ["filePath"], unique = true)` to `EntryEntity`, add composite indexes `(space, dateFolder)` and `(space, type)` via `@Entity(indices = [...])`
- [x] 1.2 Bump `MindDumpDatabase` version to 2
- [x] 1.3 Write `Migration(1, 2)`: deduplicate by `filePath` (keep `MIN(id)`), then `CREATE UNIQUE INDEX` + `CREATE INDEX` for composite indexes
- [x] 1.4 Add migration to `Room.databaseBuilder()` via `.addMigrations(MIGRATION_1_2)`
- [x] 1.5 Verify migration compiles and runs on an existing DB (schema export check)

## 2. Atomic Encryption

- [x] 2.1 Rewrite `MindDumpRepository.encryptFile()`: add `check(encrypted.exists() && encrypted.length() > 0)` before `source.delete()`
- [x] 2.2 In `scanEntries()` (FileStorageEngine), strip `.enc` suffix before `EntryType.fromFileName()` so encrypted files are correctly typed
- [x] 2.3 Verify `registerMediaFile()` already uses pre-encryption file name for type inference (confirm current code is correct)

## 3. File Naming — Millisecond Timestamps + Collision Guard

- [x] 3.1 Change `TIME_FORMAT` in `FileStorageEngine` from `HHmmss` to `HHmmssSSS`
- [x] 3.2 Extract a `uniqueFile(dir, baseName, ext)` helper that checks `exists()` and appends `_1`, `_2`, ... suffix on collision
- [x] 3.3 Refactor `saveTextEntry`, `getRecordingFile`, `getPhotoFile`, `getVideoFile`, `importFile` to use `uniqueFile`
- [x] 3.4 Verify `extractTimestamp` still works with `HHmmssSSS` format (`split("_")[1]` returns the timestamp)

## 4. Import File Validation

- [x] 4.1 In `FileStorageEngine.importFile()`: throw `IOException` when `openInputStream` returns null
- [x] 4.2 In `FileStorageEngine.importFile()`: add `check(destFile.exists() && destFile.length() > 0)` after copy, delete empty file on failure
- [x] 4.3 In calling ViewModel/repository code: add try-catch around `importFile` to surface error to user (if not already handled)

## 5. Bidirectional Disk Reconciliation

- [x] 5.1 Add `suspend fun getAllSnapshot(space: Space): List<EntryEntity>` to `EntryDao` (returns `List`, not `Flow`)
- [x] 5.2 Add `suspend fun deleteByPaths(paths: List<String>)` and `suspend fun updateAll(entities: List<EntryEntity>)` to `EntryDao`
- [x] 5.3 Implement `MindDumpRepository.reconcileWithDisk(space)`: full diff of disk vs DB, insert new / delete orphans / update stale
- [x] 5.4 Replace old `refreshFromDisk(space)` calls with `reconcileWithDisk(space)`
- [x] 5.5 Call `reconcileWithDisk` on app startup (in ViewModel init or `Application.onCreate` via Hilt-injected worker)

## 6. Verification

- [x] 6.1 Build and run the app, verify no crash on fresh install and on migration from v1 DB
- [x] 6.2 Test: save text → encrypt → verify `.enc` file exists, plaintext deleted
- [x] 6.3 Test: rapid-fire save 10 text entries, verify no filename collision
- [x] 6.4 Test: manually delete a file from disk, run reconcile, verify Room entry removed
- [x] 6.5 Test: import from Share sheet with invalid URI, verify no ghost entry created
- [x] 6.6 Run Detekt + ktlint, fix any violations
