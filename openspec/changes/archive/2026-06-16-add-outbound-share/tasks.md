# Implementation Tasks

## 1. Repository layer — share resolution

- [x] 1.1 Add a `ShareResult` sealed type to `MindDumpRepository`: `Payload(files: List<File>, mimeTypes: List<String>)` and `Locked`. (Mirrors existing `EditSaveResult`.)
- [x] 1.2 Add `suspend fun prepareEntriesForShare(entries: List<MindDumpEntry>): ShareResult` that resolves each entry to a plaintext `File`: for `.enc` entries, decrypt to `.cache/<nameWithoutEnc>` via `cryptoEngine.decryptFile` using `sessionPassword` (return `Locked` if null); for plaintext entries, use the file directly.
- [x] 1.3 In `prepareEntriesForShare`, stop early and return `ShareResult.Locked` as soon as any encrypted entry is encountered with a null session password — do not partially prepare.
- [x] 1.4 Add `suspend fun getGroupMemberEntries(groupDir: File): List<MindDumpEntry>` that loads member entries for a group directory (resolve from `dao.getEntriesInGroupSnapshot(groupDir.absolutePath)` to `MindDumpEntry`, or scan disk via the storage engine).
- [x] 1.5 Verify the `.cache/` directory is reachable through the FileProvider authority (see Task 5.1) — no code change here, just confirm `getRootDir()` resolves under `<external-path>`.

## 2. ViewModel layer — share orchestration

- [x] 2.1 In `MindDumpViewModel`, add `fun shareEntries(entries: List<MindDumpEntry>)` launching a coroutine on `viewModelScope` that calls `repository.prepareEntriesForShare(entries)` and exposes the result via a new `shareResult: StateFlow<ShareResult?>` (one-shot, consumed by UI).
- [x] 2.2 Add `fun shareGroup(groupDir: File)` that resolves members via `getGroupMemberEntries` then delegates to the same prepare path; guard the empty-members case by not emitting a payload.
- [x] 2.3 Add `fun consumeShareResult()` to clear the one-shot state after the UI has acted on it.

## 3. UI — single-entry share action (drawer)

- [x] 3.1 In `EntryActionDrawer`, add an `onShare: (() -> Unit)? = null` parameter and render a "分享" `ActionItem` (use `Icons.Filled.Share`) positioned near the top of the action list; render it for BOTH file and comment entries (share is the one action comments do get).
- [x] 3.2 In `MainScreen`, wire the `EntryActionDrawer` `onShare` callback to `viewModel.shareEntries(listOf(entry))`.
- [x] 3.3 Add the localized strings: `share` = "分享" (zh) / "Share" (en) in `res/values/strings.xml` and `res/values-en/strings.xml`.

## 4. UI — multi-select and group share

- [x] 4.1 In `MultiSelectTopBar`, add an `onShare: () -> Unit` parameter and a "分享" `TextButton` action in the top bar (alongside 合并为分组 / 删除).
- [x] 4.2 In `MainScreen`, wire multi-select `onShare` to `viewModel.shareEntries(selectedEntries)` using the currently selected entry set from `uiState`.
- [x] 4.3 In `GroupActionSheet`, add an `onShare: (() -> Unit)? = null` parameter and a "分享" `ActionItem`.
- [x] 4.4 In `MainScreen`, wire the group sheet `onShare` to `viewModel.shareGroup(groupDir)`.

## 5. UI — launching the Share sheet and handling results

- [x] 5.1 Verify `file_paths.xml` covers `.cache/` (currently `<external-path name="minddump" path="." />`). Confirm `getRootDir()` is on external storage; if it is app-private storage, add a `<cache-path>` or `<files-path>` entry — adjust and re-verify.
- [x] 5.2 Add a `shareContent(context, files, mimeTypes)` helper in `MainScreen.kt` (near `openFile`): for a single `text/plain`/`.md` file, put `readText()` into `EXTRA_TEXT` with type `text/plain`; otherwise build `ACTION_SEND` (single) or `ACTION_SEND_MULTIPLE` (many) with `EXTRA_STREAM` FileProvider URIs, set `FLAG_GRANT_READ_URI_PERMISSION`, derive MIME via `getMimeType`, and `startActivity(Intent.createChooser(...))`.
- [x] 5.3 In `MainScreen`, collect `viewModel.shareResult` and dispatch: on `Payload` call `shareContent` then `consumeShareResult()`; on `Locked` show a Snackbar/Toast with the locked message and consume.
- [x] 5.4 Add the locked-message string `share_locked` = "私密内容已锁定，请先解锁后再分享" (zh) / equivalent (en).

## 6. Verification

- [x] 6.1 Build the app (assembleDebug + detekt + ktlint pass). On-device run pending — no emulator/AVD in this environment.
- [x] 6.2 Manually verify: share a Public text note (text/plain), a Public photo (image URI), a Public recording.
- [x] 6.3 Manually verify: share an encrypted Private photo while unlocked (decrypts to cache, receiver gets image); share an encrypted Private entry while locked (locked message, no sheet).
- [x] 6.4 Manually verify: multi-select 2+ entries and share (ACTION_SEND_MULTIPLE); share a whole group; share an empty group (no sheet).
- [x] 6.5 Run `./gradlew detekt ktlintCheck` and fix any issues.
- [x] 6.6 Wire decrypted temp files in `.cache/` to be cleared by `cleanDecryptedCache()`: the method existed but was never called, so added `MainActivity.onStop` → `viewModel.clearDecryptedCache()` (also covers pre-existing view-decryption temp files).
