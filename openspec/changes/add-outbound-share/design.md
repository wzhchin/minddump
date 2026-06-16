## Context

MindDump stores every entry as a file under the user's work root (`Public/` or `Private/`, organized into `YYYY-MM` month folders and optional group directories). Private-space files are AES-256-GCM encrypted to a `.enc` variant; their plaintext only ever exists transiently in `.cache/` during viewing. The app already registers as an inbound share target (`share-receiver`) and already grants content-URIs via a `FileProvider` authority `${applicationId}.fileprovider` with `file_paths.xml` exposing `<external-path name="minddump" path="." />`. `MainScreen.openFile` is the working precedent: it builds a FileProvider URI and launches `ACTION_VIEW`.

What is missing is the reverse direction: there is no `ACTION_SEND` path, no share affordance anywhere in the UI, and no code that resolves a stored entry into a shareable plaintext form.

## Goals / Non-Goals

**Goals:**
- Share any single entry (text, comment, photo, recording, video, imported file) out via `ACTION_SEND`.
- Share multiple entries together via `ACTION_SEND_MULTIPLE`, from both multi-select and a whole-group action.
- Transparently decrypt Private `.enc` entries so they share as usable content, never as ciphertext.
- Surface a clear "session locked" failure when a Private entry can't be decrypted, instead of sharing broken data.
- Reuse the existing FileProvider, `.cache/` directory, and `cleanDecryptedCache()` — no new permissions, no schema change.

**Non-Goals:**
- No format conversion or transcoding (a photo stays a photo, audio stays audio).
- No outbound deep links or "share back into MindDump" round-trips.
- No change to inbound share behavior.
- No encryption of the *outgoing* temp URI beyond what the OS share sheet already provides.
- No per-file share-target customization, sharing of group directories as ZIPs, or renaming on export.

## Decisions

### Decision 1: Build the outbound share intent in the ViewModel, not the composable
The composable receives an `ApplicationContext` and a resolved list of shareable plaintext `File`s, but assembling the `Intent` and calling `Intent.createChooser` + `startActivity` is a UI-layer concern. However, the *resolution* of entries to plaintext files (decrypting `.enc` to `.cache/`) is a suspend, IO-bound operation that belongs in the repository.

**Shape:** `Repository.prepareEntriesForShare(entries): ShareResult` returns either `ShareResult.Payload(List<File>, mimeTypes)` or `ShareResult.Locked`. The ViewModel surfaces the result; `MainScreen` builds and launches the chooser intent for the success case and shows a toast/snackbar for the locked case. This mirrors the existing `decryptForViewing` split.

*Alternative considered:* do everything in the composable. Rejected — decryption is IO + needs the session password, which the repository owns; putting it in the view layer couples UI to crypto state.

### Decision 2: Decrypt-to-cache for Private `.enc` entries, share the temp URI
For an encrypted entry, write the plaintext to `.cache/<nameWithoutEnc>` (same convention as `decryptForViewing`) and share the temp file's URI. For a plaintext Public entry, share the original file directly — no copy. Mixed batches may contain both; each entry resolves independently to a `File`.

Cache cleanup reuses `cleanDecryptedCache()`, called on app exit / screen leave as it already is. We do NOT delete temp files immediately after `startActivity`, because the receiving app reads the URI asynchronously; the existing lifecycle-driven cleanup is the safe model.

*Alternative considered:* copy Public files to cache too for uniformity. Rejected — wasteful for large videos, and Public files are already world-shareable via FileProvider.

### Decision 3: Text/comment entries share as `text/plain`, everything else as file URIs
- `EntryType.TEXT` and `EntryRole.COMMENT` (always `.md`): put the file's text content into `EXTRA_TEXT` and send `ACTION_SEND` with type `text/plain`. This is the most compatible form for chat apps.
- All other types: put the FileProvider URI into `EXTRA_STREAM` with the MIME derived from extension (reuse `MainScreen.getMimeType`).

For `ACTION_SEND_MULTIPLE` with a text-only batch, use `EXTRA_TEXT` concatenated? **No** — concatenating notes silently merges distinct thoughts. Mixed/single-text batches with non-text content use `EXTRA_STREAM` URIs for all, treating the `.md` as a text/markdown file attachment. A pure-text multi-select batch is a niche case; treating text as file attachments keeps behavior predictable. (Captured as an open question below.)

### Decision 4: Single intent with chooser, not a custom target picker
Always wrap in `Intent.createChooser(...)`. We do not build a custom Material sheet of target apps — the system chooser is the platform convention, handles per-app availability, and the inbound side already trusts the system sheet.

### Decision 5: Locked-session share is a soft failure with a message, not a throw
If `prepareEntriesForShare` hits an encrypted entry while `sessionPassword == null`, it returns `ShareResult.Locked`. The ViewModel exposes a transient UI message ("私密内容已锁定，请先解锁后再分享"); no exception, no partial send. This matches `saveEntryEdit`'s `EditSaveResult.Locked` pattern, which the codebase already established.

## Risks / Trade-offs

- **[Temp plaintext lives in `.cache/` until cleanup]** → Reuses the existing viewing-decryption threat model (plaintext-in-cache is already accepted for viewing). No new exposure surface; same `cleanDecryptedCache()` lifecycle. Document in code comment that share temp files share this contract.
- **[Receiver app keeps a copy of Private content]** → Inherent to sharing; nothing the app can do. Mitigation: the user explicitly tapped share. No code fix; just expected behavior.
- **[Large multi-share may exceed chooser intent limits or be slow]** → Bound is the OS, not us; building the intent is cheap (URIs only, no copying for Public). Accept.
- **[`.cache/` must be under the FileProvider-exposed root]** → `file_paths.xml` exposes `<external-path path=".">`, and `.cache/` lives under `getRootDir()` which is on external storage. To be **verified** in tasks that the resolved root is an `external-path`, not app-private storage; if it is app-private, `<cache-path>` must be added.
- **[Group with 0 members shares an empty intent]** → Guard: disable/no-op the group share action (or show a message) when the group has no members.

## Migration Plan

No data migration — sharing is read-only over existing files. Rollback is trivial: remove the new UI affordances; no persisted state is introduced.

Deployment order:
1. Repository `prepareEntriesForShare` + group-member resolution + `ShareResult`.
2. ViewModel `shareEntries` / `shareGroup`.
3. UI wiring (drawer, multi-select bar, group sheet) + chooser launch.
4. Strings (zh + en) + `cleanDecryptedCache()` already in place.
5. Verify `.cache/` FileProvider coverage; detekt/ktlint.

## Open Questions

- **Pure-text multi-select batch:** share concatenated text in `EXTRA_TEXT`, or attach `.md` files as streams? Design assumes file-attachment for predictability; revisit if early testing shows chat apps drop `.md` attachments.
- **Comment share content:** share only the comment text, or prefix it with a quote of the parent entry? Design assumes comment text only (parent context is app-internal).
