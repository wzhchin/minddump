## ADDED Requirements

### Requirement: Comments fold into the parent card
The system SHALL present comments as a collapsed, expandable list inside the parent entry's card, rather than as nested standalone bubbles below the entry. This gives entries a note/card-app structure instead of a chat-thread structure.

#### Scenario: Entry with collapsed comments
- **WHEN** an entry card has one or more comments and is in its default state
- **THEN** the card shows an expandable affordance summarizing the comment count (e.g. "▸ N 条评论")
- **AND** the individual comment contents are hidden

#### Scenario: Expanding comments
- **WHEN** the user activates the comment affordance
- **THEN** the comments expand in place within the card, each showing its timestamp and a preview of its content
- **AND** activating the affordance again collapses them

#### Scenario: Tapping a comment opens it
- **WHEN** the user taps an expanded comment
- **THEN** that comment opens for viewing/editing, as before

### Requirement: Orphan comment indication
The system SHALL indicate when a comment's original parent file has been deleted, so the user understands why the comment stands alone.

#### Scenario: Comment whose parent was deleted
- **WHEN** an entry card represents a comment whose target file no longer exists
- **THEN** the card shows a localized indicator explaining the original file was deleted (e.g. "原始文件已删除")
