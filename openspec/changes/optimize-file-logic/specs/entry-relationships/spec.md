## ADDED Requirements

### Requirement: Comment files
The system SHALL support comment files using the naming pattern `{targetTs}-n-{commentTs}.md[.enc]`. Comments are always Markdown text files. Multiple comments may target the same file or group.

#### Scenario: Comment on a file
- **WHEN** a user adds a comment to `2506-13-143022-f.m4a`
- **THEN** a file `2506-13-143022-n-2506-13-143100.md` is created in the same directory

#### Scenario: Multiple comments on same file
- **WHEN** two comments are added to `2506-13-143022-f.m4a`
- **THEN** two files are created: `2506-13-143022-n-2506-13-143100.md` and `2506-13-143022-n-2506-13-143200.md`

#### Scenario: Comments are text-only
- **WHEN** a user adds a comment
- **THEN** the file extension SHALL be `.md` — media comments are not supported

### Requirement: Comment association by timestamp
The system SHALL associate comments to their target by matching the timestamp prefix. A comment's target is the file or group in the same directory whose timestamp matches the comment's prefix (the part before `-n-`).

#### Scenario: Comment matches a file
- **WHEN** the directory contains `2506-13-143022-f.m4a` and `2506-13-143022-n-2506-13-143100.md`
- **THEN** the comment is associated with the `.m4a` file (matching timestamp `2506-13-143022`)

#### Scenario: Comment matches a group
- **WHEN** the directory contains `2506-13-143912-g-travel/` and `2506-13-143912-n-2506-13-144000.md`
- **THEN** the comment is associated with the group (matching timestamp `2506-13-143912`)

#### Scenario: Orphan comment — target deleted
- **WHEN** a comment `2506-13-143022-n-2506-13-143100.md` exists but no matching `2506-13-143022-f.*` or `2506-13-143022-g*` entry exists
- **THEN** the comment is an orphan — it SHALL still be indexed in Room and displayed in the UI as a standalone comment

### Requirement: Group directories
The system SHALL support grouping files into directories named `{ts}-g[-{name}]/`. A group directory contains member files. Group name is optional (anonymous group).

#### Scenario: Named group
- **WHEN** files are grouped under "travel"
- **THEN** a directory `2506-13-143912-g-travel/` is created containing the member files

#### Scenario: Anonymous group
- **WHEN** files are grouped without a name
- **THEN** a directory `2506-13-143912-g/` is created

#### Scenario: Files in group directory belong to that group
- **WHEN** a file `2506-13-144100-f.jpg` exists inside `2506-13-143912-g-travel/`
- **THEN** the file belongs to the "travel" group — no additional metadata needed

### Requirement: One group per file
A file SHALL belong to at most one group. Moving a file to a group physically moves it into that group's directory.

#### Scenario: Move file to group
- **WHEN** `2506-13-143645-f.jpg` is moved to group `2506-13-143912-g-travel/`
- **THEN** the file is physically relocated to `{workDir}/{space}/2025-06/2506-13-143912-g-travel/2506-13-143645-f.jpg`

#### Scenario: File cannot be in two groups
- **WHEN** a file is already in group A and moved to group B
- **THEN** the file is removed from group A's directory and placed in group B's directory

### Requirement: Group can have comments
Groups SHALL support comments, associated by matching the group directory's timestamp prefix.

#### Scenario: Comment on a group
- **WHEN** directory `2506-13-143912-g-travel/` exists and file `2506-13-143912-n-2506-13-144000.md` is created inside it
- **THEN** the comment is associated with the "travel" group

### Requirement: Room indexes relationships from disk
Room SHALL store `role` and `targetTimestamp` fields for query performance, but all relationship data SHALL be derivable from the filesystem structure. Reconciliation rebuilds these fields from disk.

#### Scenario: Reconcile populates targetTimestamp
- **WHEN** reconciliation scans `2506-13-143022-n-2506-13-143100.md`
- **THEN** the Room row has `role="n"`, `targetTimestamp="2506-13-143022"`

#### Scenario: Group membership from directory
- **WHEN** reconciliation scans `2506-13-143912-g-travel/2506-13-144100-f.jpg`
- **THEN** the Room row has `groupPath` pointing to the group directory

### Requirement: UI displays comments with parent entry
The system SHALL render comments as nested items within their parent entry's bubble. Comments are shown with a distinct visual style (e.g., indented, comment icon).

#### Scenario: Entry with comments in list
- **WHEN** an entry `2506-13-143022-f.m4a` has two comments
- **THEN** the bubble shows the audio entry at top, followed by indented comment bubbles below

#### Scenario: Orphan comment in list
- **WHEN** a comment has no matching parent entry
- **THEN** it is displayed as a standalone comment bubble with a visual indicator that the original target is missing
