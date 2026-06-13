## Context

Today every entry tap ends in `openFile(context, entry.file)` → `Intent.ACTION_VIEW` (see `MainScreen.kt:openFile`, reused by the main list, group detail, and comment bubbles). For text entries (`.md`/`.txt`) and comments this hands the user to whatever external text editor the system picks — a context switch out of MindDump and a loss of the in-app capture framing. This change keeps text reading/editing in-app.

Relevant current state:
- `FullscreenEditScreen` exists but is **new-entry only**: it is bound to `uiState.inputText` and its submit calls `viewModel.submitText()` (creates a fresh file). It must not be reused verbatim for editing, or it would clobber the new-entry flow.
- Encryption: text in Private space is stored as `{ts}-f.md.enc`. The repository already has a decrypt→mutate→re-encrypt→`deleteByPath`/`insert` pattern in `moveEntryToSpace` and `renameEntry`. `decryptForViewing(encryptedFile)` produces a cache temp file. Save-back must follow the same pattern.
- Feed ordering: `EntryList` sorts by `file.lastModified()`. Overwriting a file bumps `lastModified`, so an edited entry naturally floats to the top — no extra sort logic needed.
- The three tap call-sites are: `MainScreen` `onEntryClick`, `GroupDetailScreen` member `onClick`, and `EntryItem` `CommentBubble` `onCommentClick` (which already routes through the main list's `onEntryClick`).

## Goals / Non-Goals

**Goals:**
- In-app fullscreen edit for `EntryType.TEXT` entries and `EntryRole.COMMENT` entries, all three tap sites.
- Keep non-text entries on the external viewer.
- A single dispatch point so the text-vs-other branching can't drift between call sites.
- Preserve the encryption invariant: editing a `.enc` entry saves it back encrypted.
- Preserve the new-entry flow untouched.
- Avoid persistent draft storage — a one-shot unsaved-changes confirmation instead.

**Non-Goals:**
- No persistent / cross-process draft saving (deliberately rejected during exploration for weight).
- No change to feed sort algorithm (rely on `lastModified`).
- No editing of photos/audio/video content (still external).
- No change to long-press entry actions (`entry-actions-drawer`) — this is only the tap-to-open path.
- No group nesting / drill-down (that's a separate follow-up change).

## Decisions

### Decision 1: One dispatch function `onEntryOpen(entry)`
A single function routes a tap: `TEXT` or `COMMENT` → open in fullscreen editor; anything else → `openFile()`. All three call sites call it instead of `openFile` directly.

- *Why over per-site branching:* the same branch lives in three places today; a dispatch function makes the text-vs-other rule live in one spot and prevents drift.
- *Alternative considered:* leave branching in each call site. Rejected — three copies of the same `if`.

### Decision 2: Edit mode is a separate route with a path argument, not a flag on the existing route
Navigation route: `fullscreen_edit?entryPath={encodedAbsolutePath}`. When `entryPath` is present, `FullscreenEditScreen` runs in **edit mode**; when absent, **new-entry mode** (current behavior).

- *Why over a UI flag:* Compose Navigation encodes screen identity + args in the back stack, surviving config changes and process death; a flag held in `uiState` would need manual restoration.
- *Why one screen, two modes vs two screens:* ~95% of the layout is identical (top bar + giant text field + send). Splitting would duplicate it. A sealed `mode` parameter keeps the shared layout and only swaps the close/save wiring.
- *The file path is the entry identity:* the entry's file path is stable across edits (we overwrite in place; rename isn't part of this change), so it's a valid nav argument. Uri-encode it.

### Decision 3: Edit state is screen-local, not in `uiState`
Edit text lives in `remember { mutableStateOf(initialText) }` inside the edit-mode composable. The new-entry `uiState.inputText` is untouched.

- *Why:* the two flows have different sources (existing file content vs. blank) and different save targets (overwrite vs. create). Coupling them risks the editor clobbering a half-typed new entry if the user switches between them.
- *Initial text load:* read once via `LaunchedEffect`/`produceState` on `Dispatchers.IO` — decrypt to cache first if the entry is `.enc` (reuse `repository.decryptForViewing`), then `readText()`. Loading must not block the main thread.

### Decision 4: Save = overwrite original file, keep the same path
`repository.saveEntryEdit(entry, newText)`:
1. If encrypted: decrypt is already done for display; write plaintext to the (cache) working file, then re-encrypt to the **original `.enc` path**, delete the working file. Follow `moveEntryToSpace`'s encrypt pattern.
2. If plaintext: `writeText()` directly to `entry.file`.
3. Update the Room row: the path is unchanged, so `deleteByPath(entry.file.absolutePath)` + `insert(updatedEntry.toEntity(...))` (same pattern as `renameEntry`), refreshing `lastModified`/FTS content.
4. Crypto runs on `Dispatchers.IO` (off main thread, per project rule).

- *Why overwrite-in-place vs. delete+create new:* keeps the entry's identity (same path, same targetTimestamp for comments anchored to it), and lets `lastModified` bump reorder it — which is the desired "edited → floats to top" behavior.
- *Alternative considered:* save as a brand-new entry. Rejected — would orphan anchored comments and lose the "edit, not append" intent.

### Decision 5: Unsaved-changes drawer, not persistent drafts
On back-press / close-tap:
- If text is unchanged (dirty == false): close immediately.
- If changed: show a `ModalBottomSheet` "保留这次编辑吗？" with three actions:
  - **保存** → `onSave` (write back, close).
  - **丢弃** → close without saving.
  - **继续编辑** → dismiss sheet, stay on editor.
- *Why over persistent drafts (DB table / `.draft` shadow file / in-memory map):* all three add storage complexity for a "do you want to keep this?" question. A one-shot confirmation captures the same intent with zero storage surface. Matches existing drawer/dialog style (`EntryActionDrawer`, `DeleteConfirmDialog`).
- *Trade-off:* closing-and-discarding is final (no recovery). Accepted — the drawer makes the choice explicit.

### Decision 6: System back button interception
The edit-mode screen uses `BackHandler` to trigger the same dirty-check + drawer as the on-screen close button, so hardware back can't silently discard edits.

## Risks / Trade-offs

- **[Risk] Editing an encrypted entry fails if the session password is gone** (e.g. session locked between open and save). → *Mitigation:* save-back re-reads `sessionPassword`; if null, surface a toast "需要解锁才能保存" and keep the editor open (user can copy text out). Encrypt path already errors loudly; we catch and toast instead of crashing.
- **[Risk] Concurrent edit / reconcile race** — `reconcileWithDisk` or a background scan could run while save writes. → *Mitigation:* save does file-write then `deleteByPath`/`insert` atomically per entry; reconcile reconciles whole-space and is idempotent. Window is tiny; acceptable.
- **[Risk] Reading a large text file on open janks UI.** → *Mitigation:* `Dispatchers.IO` load + `produceState` with the filename as placeholder; show the field once loaded.
- **[Trade-off] Discarded edits are unrecoverable.** → Accepted; the explicit drawer makes the discard a deliberate choice.
- **[Trade-off] Edited entry moves in the feed.** → Desired ("edit = re-express"), and noted in the spec so it isn't filed as a bug.

## Migration Plan

- Additive only — no data format, schema, or migration changes. Existing entries edit in place.
- Rollout: single release. No feature flag needed (behavior is contained to the tap path and one screen mode).
- Rollback: revert the route/dispatch change; entries saved via this path are normal files, so a rolled-back build reads them fine.
