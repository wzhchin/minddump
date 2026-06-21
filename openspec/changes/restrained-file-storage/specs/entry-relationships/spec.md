## MODIFIED Requirements

### Requirement: Group directories
The system SHALL support grouping files into directories named `{ts}-f[-{name}]/` (role token `f`, indistinguishable in filename from a file note — the directory itself is what marks it as a group). A group directory SHALL contain only member files; groups SHALL NOT be nested inside other groups (single level only). Group name is optional (anonymous group).

#### Scenario: Named group
- **WHEN** files are grouped under "travel"
- **THEN** a directory `2506-13-143912-f-travel/` is created containing the member files

#### Scenario: Anonymous group
- **WHEN** files are grouped without a name
- **THEN** a directory `2506-13-143912-f/` is created

#### Scenario: Files in group directory belong to that group
- **WHEN** a file `2506-13-144100-f.jpg` exists inside `2506-13-143912-f-travel/`
- **THEN** the file belongs to the "travel" group — no additional metadata needed

#### Scenario: Groups are single-level
- **WHEN** a group directory `2506-13-143912-f-travel/` exists
- **THEN** no directory inside it SHALL be treated as a sub-group; the scanner SHALL enter a group directory once, collect only its files, and SHALL NOT recurse looking for nested groups

### Requirement: One group per file
A file SHALL belong to at most one group. Moving a file to a group physically moves it into that group's directory. Because groups are limited to a single level, a file's group is always a top-level group directory directly under the month bucket.

#### Scenario: Move file to group
- **WHEN** `2506-13-143645-f.jpg` is moved to group `2506-13-143912-f-travel/`
- **THEN** the file is physically relocated to `{workDir}/{space}/2025-06/2506-13-143912-f-travel/2506-13-143645-f.jpg`

#### Scenario: File cannot be in two groups
- **WHEN** a file is already in group A and moved to group B
- **THEN** the file is removed from group A's directory and placed in group B's directory

### Requirement: Room indexes relationships from disk
Room SHALL store a `groupPath` field (the owning group directory's path, or null for a file/group at the month-bucket root) for query performance, but all relationship data SHALL be derivable from the filesystem structure. Reconciliation rebuilds this field from disk. There is no `role` field for comments (comments are removed) and no `targetTimestamp`; entry identity is the file path. `groupPath` SHALL use the same path form everywhere it is stored or compared (absolute path, consistent with `filePath`), so a reconciliation re-stamp never changes a member's `groupPath` form and breaks the open-group view.

#### Scenario: Group membership from directory
- **WHEN** reconciliation scans `2506-13-143912-f-travel/2506-13-144100-f.jpg`
- **THEN** the Room row has `groupPath` pointing to the group directory and `filePath` equal to the file's path relative to `workDir`

#### Scenario: Ungrouped file at month bucket root
- **WHEN** reconciliation scans `2506-13-143022-f.md` directly in a month bucket directory
- **THEN** the Room row has `groupPath == null`

## REMOVED Requirements

### Requirement: Comment files
**Reason**: Comments are removed entirely. There is no `n` role and no comment filename grammar.
**Migration**: Existing `{targetTs}-n-{commentTs}.md[.enc]` files on disk are no longer parsed. They are ignored (invisible) and may be deleted manually.

### Requirement: Comment association by timestamp
**Reason**: With no comments, there is no comment-to-target association to perform.
**Migration**: As above; existing comment files are ignored.

### Requirement: Group can have comments
**Reason**: Comments are removed; groups carry only files and an optional metadata sidecar.
**Migration**: Existing comment files inside group directories are ignored.

### Requirement: UI displays comments with parent entry
**Reason**: The comment UI is removed. There are no comment bubbles to render.
**Migration**: The indented-comment rendering is deleted from the UI; existing comment files on disk are ignored.
