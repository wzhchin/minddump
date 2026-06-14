## Why

Comments are fully implemented at the storage and repository level — `FileStorageEngine.saveComment()` writes `{targetTs}-n-{nowTs}.md` into the target entry's directory, and `MindDumpRepository.saveComment()` indexes it into Room with `role = EntryRole.COMMENT`. The presentation layer also works: `groupEntriesWithComments()` nests comments under their parent, and `CommentListSection` renders them folded inside the parent card.

But there is **no way to create a comment from the UI**. `MindDumpViewModel` exposes no `addComment` method, and the `EntryActionDrawer` has no "add comment" action. As a result, the comment list for any entry is permanently empty (no historical comments exist), and the entire comment feature is unreachable by the user.

## What Changes

- Add `addComment(targetEntry: MindDumpEntry, content: String)` to `MindDumpViewModel` — delegates to `repository.saveComment(currentSpace, targetEntry, content)` then refreshes the current scope so the new comment appears in the parent card's folded comment list.
- Add an "添加评论" (Add comment) action to `EntryActionDrawer`, shown only for non-comment file entries. Tapping it opens an inline text input dialog; confirming calls `onAddComment(content)`.
- Wire the new drawer callback in `MainScreen.kt` to `viewModel.addComment(entry, content)`.
- Comments cannot target other comments: the action is hidden on `-n-` entries (mirrors the existing "comment has no pin / no status" rule).

## Capabilities

### New Capabilities
<!-- None — comments already exist as a storage/presentation capability. -->

### Modified Capabilities
- `entry-actions-drawer`: add an "Add comment" action that creates a comment targeting the selected entry. This closes the gap between the already-implemented storage layer and the UI.

## Impact

- `MindDumpViewModel.kt` — new `addComment(targetEntry, content)` method.
- `EntryActionDrawer.kt` — new optional `onAddComment` callback parameter, new `ActionItem`, and an input dialog (text field + confirm/cancel). Shown only when `entry.role != COMMENT`.
- `MainScreen.kt` — wire `onAddComment = { content -> viewModel.addComment(entry, content) }`.
- `strings.xml` (zh-CN) / `strings-en.xml` (en) — new string `add_comment` (+ optional dialog title/confirm labels).
- `MindDumpRepository.kt` — **no change** (`saveComment` already exists).
- `FileStorageEngine.kt` — **no change** (`saveComment` already exists).
