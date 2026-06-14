## Context

The comment feature is implemented end-to-end *except* for the user-facing creation path:

- `FileStorageEngine.saveComment(targetDir, targetTimestamp, content)` (`FileStorageEngine.kt:365`) — writes `{targetTs}-n-{nowTs}.md` via `uniqueFile`, so collisions are avoided.
- `MindDumpRepository.saveComment(space, targetEntry, content)` (`MindDumpRepository.kt:189`) — writes the file, applies encryption for Private space, parses metadata, and inserts a Room row with `role = EntryRole.COMMENT` and `targetTimestamp = targetEntry.timestamp`. It also stores a `contentPreview = content.take(500)`.
- `MindDumpViewModel.groupEntriesWithComments()` (`MindDumpViewModel.kt:808`) — matches comments to parents by `targetTimestamp == file.timestamp`, nesting matched comments and surfacing orphans as standalone cards.
- `CommentListSection` / `CommentPreview` (`EntryItem.kt:455`) — renders the folded comment list inside the parent card; tapping a comment opens it in the editor.

The missing piece is a `ViewModel.addComment()` method and a drawer action that calls it.

The `EntryActionDrawer` already follows an established pattern for new optional actions: `onTogglePin: (() -> Unit)? = null` and `onSetStatus: ((TodoState) -> Unit)? = null` are both nullable, hidden on comment entries (`isComment = entry.role == EntryRole.COMMENT`), and gated with `if (!isComment && onX != null) { ActionItem(...) }`. The "Add comment" action will follow the same pattern.

## Goals / Non-Goals

**Goals:**
- Let the user create a comment on a file entry from the action drawer, with the comment immediately appearing in the parent card's folded comment list.
- Reuse the existing `saveComment` storage/repository path — no new filename conventions or Room schema changes.
- Hide the action on comment entries (a comment cannot target another comment) and on the encrypted-without-password edge case (handled by the repository).

**Non-Goals:**
- Inline editing of a comment's text from the drawer (tapping the comment preview already opens the fullscreen editor — reuse that).
- Threading/replies on comments (flat comment list only).
- Comments on group directories (groups aggregate member entries; comments target individual files).
- A dedicated comment composer screen (an inline dialog is sufficient).

## Decisions

### 1. Expose `addComment` on the ViewModel, not a new UI route

**Decision**: Add `addComment(targetEntry, content)` to `MindDumpViewModel` that calls `repository.saveComment(currentSpace, targetEntry, content)` on `Dispatchers.IO`, then `refreshForCurrentScope()` so the new comment reflows into the parent card.

**Rationale**: `repository.saveComment` already does the right thing (writes file, indexes Room, sets `targetTimestamp`). The ViewModel only needs to (a) pass the current space and (b) refresh. No new nav route or screen is warranted for a single-line input.

**Alternative considered**: Open the fullscreen editor in a "new comment for entry X" mode. Rejected — it's heavier than the feature needs, and the editor's save path writes a top-level entry, not a `-n-` comment. Reusing `saveComment` keeps the filename contract intact.

### 2. Trigger via the action drawer with an inline input dialog

**Decision**: Add an "添加评论" `ActionItem` to `EntryActionDrawer` (icon `Icons.Filled.Comment` or `EditNote`), gated by `if (!isComment && onAddComment != null)`. Tapping it sets `showCommentDialog = true`, which renders an `AlertDialog` with a multi-line `OutlinedTextField`. Confirm validates non-blank content, calls `onAddComment(content.trim())`, then dismisses; cancel/dismiss leaves the entry untouched.

**Rationale**: The drawer is already the established entry for all per-entry operations (rename, delete, pin, status, move). Adding comment creation here is consistent and discoverable. An `AlertDialog` (not a bottom sheet) matches the existing rename dialog's weight for short text input.

**Alternative considered**: A bottom-sheet composer with send button (chat-style). Rejected for now — the comment model is note-like (folded under the card), not chat-like; a dialog matches that mental model. Can be upgraded later.

### 3. Place the action between "status" and "rename"

**Decision**: Order the new item after the todo-status action and before "重命名" (Rename), grouping the "meta" actions (pin, status, comment) together above the file-management actions (rename, multiselect, move, delete).

**Rationale**: Keeps destructive/structural operations (rename, move, delete) visually separated from lightweight metadata actions.

### 4. Comment targeting a grouped entry inherits the parent's `groupPath`

**Decision**: No new logic needed. `repository.saveComment` already sets `groupPath = targetEntry.groupPath` (`MindDumpRepository.kt:208`), so a comment on a grouped entry lands in the same group directory and nests correctly.

**Rationale**: Verified against existing implementation — the contract is already correct; we only need to exercise it.

## Risks / Trade-offs

- **[Empty/whitespace content]** → The dialog confirm is disabled (or ignored) when the text is blank, so no zero-length comment file is written. Matches how the main input bar treats empty submits.
- **[Large comment text]** → The dialog's text field wraps and scrolls; no hard limit is enforced. The repository already caps the Room preview at 500 chars (`content.take(500)`); the file itself stores the full text. Acceptable — comments are expected to be short.
- **[Refresh timing]** → `addComment` calls `refreshForCurrentScope()` after the insert completes (sequential `await`, not fire-and-forget), so the comment appears immediately. If the user is viewing a different scope than the target entry's, the refresh targets the current scope; the comment still lands on disk and surfaces on next refresh of that scope. This matches how `setEntryStatus`/`toggleEntryPinned` already behave.
- **[Private space encryption without session password]** → `repository.saveComment` calls `encryptIfNeeded`, which for Private space without a password leaves the file unencrypted (existing behavior for normal entries). No new edge case introduced.
