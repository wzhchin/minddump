## Why

MindDump can *receive* content from other apps (the inbound `share-receiver` capability) but has no way to push content out. Once a thought, photo, recording, or imported file lands inside the vault, it is stuck there — the only exit is deleting it. Users who capture now and distribute later (the core brain-dump workflow) cannot forward anything to chats, email, or other apps. This change makes "share out" a first-class action for every piece of stored content.

## What Changes

- **Outbound share action on every entry**: the entry action drawer gains a "分享" (Share) action. Tapping it fires an `ACTION_SEND` intent for that single entry — text notes/comments share as text, media/files share as content-URIs via the existing `FileProvider`.
- **Encrypted entries are decrypted to cache before sharing**: Private-space `.enc` entries are transparently decrypted to the `.cache/` directory (reusing `decryptForViewing`) and the decrypted temp file's URI is shared; the plaintext never touches Public storage.
- **Multi-select bulk share**: the multi-select top bar gains a "分享" action that shares all selected entries together via `ACTION_SEND_MULTIPLE`.
- **Group share**: the long-press group action sheet gains a "分享" action that shares every member of the group together via `ACTION_SEND_MULTIPLE`.
- **Locked-session safety**: if a Private entry cannot be decrypted because the session password is no longer cached, the share is blocked with a clear message rather than sending encrypted garbage or failing silently.

Non-changes: no new permissions, no outbound format conversion (files are shared in their native type; text shares as plain text), no changes to inbound share, no cross-app deep links.

## Capabilities

### New Capabilities
- `outbound-share`: Exporting stored entries — text, comments, media, imported files — out to other Android apps via `ACTION_SEND` / `ACTION_SEND_MULTIPLE`, including transparent decryption of Private-space `.enc` entries, bulk sharing from multi-select, and whole-group sharing.

### Modified Capabilities
- `entry-actions-drawer`: The drawer gains a "Share" action for every entry type (including comments), wired to the new outbound-share capability.
- `group-card`: The group long-press action sheet gains a "Share" action that exports every member of the group.

## Impact

- **ui/components/EntryActionDrawer.kt** — add `onShare` action item; render for both file and comment entries (comments are sharable as text).
- **ui/components/GroupActionSheet** (in EntryActionDrawer.kt) — add `onShare` action item.
- **ui/components/MultiSelectTopBar** (in EntryActionDrawer.kt) — add a "分享" action button.
- **ui/MainScreen.kt** — wire `onShare` / multi-select share / group share callbacks into the ViewModel; reuses `openFile`'s FileProvider pattern at a new `shareEntries(...)` site.
- **ui/MindDumpViewModel.kt** — expose `shareEntries(entries)` and `shareGroup(groupDir)` that decrypt-to-cache as needed and surface a `ShareResult` (Success / Locked) so the UI can show a locked-session message.
- **data/MindDumpRepository.kt** — add `prepareEntriesForShare(entries): SharePayload` that resolves plaintext `File`s (decrypting `.enc` entries to `.cache/`), and group-member resolution (`getGroupMembers(groupDir)`).
- **res/xml/file_paths.xml** — no change expected (`external-path .` already covers the work root and `.cache/`), to be verified in tasks.
- **res/values/strings.xml** + **res/values-en/strings.xml** — new strings: `share`, `share_locked` (and existing share strings stay for inbound).

No schema migration: sharing is read-only over existing files; the only disk writes are decrypted temp files in `.cache/`, which is already cleared by `cleanDecryptedCache()`.
