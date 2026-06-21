## REMOVED Requirements

### Requirement: Comments fold into the parent card
**Reason**: The comment feature is removed entirely (no `n` role, no `comments` table). There are no comments to fold or expand.
**Migration**: Existing on-disk comment files (`{targetTs}-n-{commentTs}.md`) are no longer parsed or displayed. They remain on disk and may be deleted manually by the user; their content is intentionally discarded from the app.

### Requirement: Orphan comment indication
**Reason**: With comments removed, there is no orphan-comment concept to indicate.
**Migration**: Same as above — orphan comment files on disk are ignored.
