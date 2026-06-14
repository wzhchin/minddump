## 1. ViewModel — expose comment creation

- [x] 1.1 Add `addComment(targetEntry: MindDumpEntry, content: String)` to `MindDumpViewModel` — launch on `Dispatchers.IO`, call `repository.saveComment(_uiState.value.currentSpace, targetEntry, content)`, then `refreshForCurrentScope()`. Ignore blank/whitespace-only `content`.

## 2. EntryActionDrawer — add comment action + input dialog

- [x] 2.1 Add optional callback `onAddComment: ((String) -> Unit)? = null` to the `EntryActionDrawer` signature (alongside `onTogglePin` / `onSetStatus`).
- [x] 2.2 Add an `ActionItem` "添加评论", placed after the todo-status action and before "重命名". Gate it with `if (!isComment && onAddComment != null)` — comments cannot have comments. *(Icon: `Icons.AutoMirrored.Filled.Comment` — the non-mirrored `Filled.Comment` is deprecated in material3 1.5; matches the project's existing AutoMirrored convention.)*
- [x] 2.3 Add a `showCommentDialog` state; tapping the action sets it true and renders a `CommentDialog` (an `AlertDialog` with a multi-line `OutlinedTextField`, 3–6 lines, placeholder "写评论…"). The confirm button is `enabled = content.isNotBlank()`; on confirm → `onAddComment(content.trim())` + dismiss; on cancel/dismiss → no-op.

## 3. UI wiring — MainScreen

- [x] 3.1 In `MainScreen.kt`, pass `onAddComment = { content -> viewModel.addComment(entry, content) }` to the `EntryActionDrawer` invocation.

## 4. Strings

- [x] 4.1 Add `add_comment` ("添加评论") to `res/values/strings.xml` (zh-CN).
- [x] 4.2 Add `add_comment` ("Add comment") to `res/values-en/strings.xml` (en).
- [x] 4.3 Add `comment_dialog_placeholder` ("写评论…" / "Write a comment…") in both locales. The dialog title reuses `add_comment`; confirm/cancel reuse existing `confirm` / `cancel` strings.

## 5. Spec delta

- [x] 5.1 Add an "Add comment action" requirement (with scenarios for: add from drawer, blank content rejected, comment inherits the parent's group, comment entry has no add-comment action) to `specs/entry-actions-drawer/spec.md` under `## ADDED Requirements`. *(Written during propose.)*

## 6. Verification

- [x] 6.1 Build: `./gradlew :app:assembleDebug` succeeds. *(Verified — BUILD SUCCESSFUL.)*
- [x] 6.2 Lint: `./gradlew :app:detekt :app:ktlintCheck` passes. *(Verified — BUILD SUCCESSFUL; resolved a detekt `LongMethod` (threshold 185, drawer now aggregates the new action too) by extending the existing `@Suppress` that already documents the drawer's aggregation-by-design, and a `MaxLineLength` by moving the rationale to its own comment line.)*
- [ ] 6.3 Manual: create a text entry, long-press → drawer → "添加评论" → type text → confirm. A `{targetTs}-n-{nowTs}.md` file appears in the entry's month dir, and the parent card's folded comment list now shows "1 条评论"; expanding reveals the new comment preview; tapping opens it in the editor. *(Pending on-device confirmation.)*
- [ ] 6.4 Manual: opening the drawer on an existing comment (`-n-`) entry shows no "添加评论" action. *(Pending on-device confirmation.)*
- [ ] 6.5 Manual: confirming with an empty text field is blocked (confirm disabled or no-op). *(Pending on-device confirmation.)*

## Notes

- **No storage/repository/schema changes** — `repository.saveComment` and `storageEngine.saveComment` already implement the `{targetTs}-n-{nowTs}.md` contract, encryption, `targetTimestamp` indexing, and `groupPath` inheritance. This change only wires the ViewModel method + drawer UI to them.
- **Lint debt surfaced (pre-existing):** the `EntryActionDrawer` composable was already at the 185-line `LongMethod` ceiling; any new action trips it. Resolved via the existing `@Suppress` pattern rather than an artificial split, matching the drawer's documented "aggregates many callbacks by design" stance.
