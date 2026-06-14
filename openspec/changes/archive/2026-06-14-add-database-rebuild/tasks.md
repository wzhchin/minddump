## 1. Data Layer

- [x] 1.1 Add `EntryDao.clearAll()` — `@Query("DELETE FROM entries")`
- [x] 1.2 Add `EntryDao.rebuildFtsIndex()` — `@Query("INSERT INTO entries_fts(entries_fts) VALUES('rebuild')")`
- [x] 1.3 Add `MindDumpRepository.rebuildDatabase()` — `clearAll()` → `reconcileWithDisk(PUBLIC)` → `reconcileWithDisk(PRIVATE)` → `rebuildFtsIndex()`, on `Dispatchers.IO`
- [x] 1.4 Add `MindDumpRepository.countAllTotal()` delegating to `dao.countAll()`

## 2. ViewModel

- [x] 2.1 Add `MindDumpUiState.showRebuildDatabaseDialog` and `isRebuildingDatabase` fields
- [x] 2.2 Add `showRebuildDatabaseDialog()` / `dismissRebuildDatabaseDialog()` accessors
- [x] 2.3 Add `suspend fun rebuildDatabase(): Int` — sets loading flag, calls repo rebuild, `refreshForCurrentScope()`, returns total count, clears flags in `finally`

## 3. UI

- [x] 3.1 Add "重建数据库" `OutlinedButton` + description to `SettingsDialog`, with `onRebuildDatabase` callback
- [x] 3.2 Add `RebuildDatabaseDialog` composable — confirmation view + indeterminate-progress view driven by `running`
- [x] 3.3 Wire `onRebuildDatabase` → `viewModel.showRebuildDatabaseDialog()` in `MainScreen`
- [x] 3.4 Render `RebuildDatabaseDialog` when `showRebuildDatabaseDialog`; on confirm `scope.launch { count = viewModel.rebuildDatabase(); snackbar }`; on dismiss `dismissRebuildDatabaseDialog()`
- [x] 3.5 Snackbar shows "已重建数据库，共 N 条记录" with `SnackbarDuration.Short`

## 4. Strings (zh-CN + en)

- [x] 4.1 `rebuild_database` / `rebuild_database_desc` / `rebuild_database_confirm_title` / `rebuild_database_confirm_message` / `rebuild_database_running` / `rebuild_database_success` / `rebuild` in `values/strings.xml`
- [x] 4.2 Same keys in `values-en/strings.xml`

## 5. Verification

- [x] 5.1 `./gradlew :app:compileDebugKotlin` passes
- [x] 5.2 New/edited files are ktlint-clean (`SettingsDialog.kt`, `EntryDao.kt`)
- [ ] 5.3 Manual run on device: rebuild from Settings, confirm count Snackbar, verify list refreshes (pending device test)
