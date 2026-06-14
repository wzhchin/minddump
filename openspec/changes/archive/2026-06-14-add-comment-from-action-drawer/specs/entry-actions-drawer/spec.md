## ADDED Requirements

### Requirement: Add comment action
The entry actions drawer SHALL include an "Add comment" action for file entries (role `f`), shown only when the selected entry is not itself a comment. Selecting it SHALL open a text input dialog; confirming with non-blank text SHALL create a comment targeting that entry.

#### Scenario: Add a comment from the drawer
- **WHEN** the user long-presses a file entry and taps "Add comment"
- **THEN** a dialog with a multi-line text field appears
- **WHEN** the user enters "follow up on this tomorrow" and confirms
- **THEN** a comment file named `{targetTs}-n-{nowTs}.md` is written into the target entry's directory
- **AND** it is indexed with the comment role and the target entry's timestamp as its `targetTimestamp`
- **AND** the parent entry's card now shows the comment folded inside it (e.g. "1 条评论")

#### Scenario: Blank comment is rejected
- **WHEN** the comment dialog is open with an empty or whitespace-only text field
- **THEN** the confirm action is unavailable, and no comment file is written

#### Scenario: Comment inherits the parent's group
- **WHEN** the user adds a comment to an entry that lives inside a group directory
- **THEN** the comment file is written into that same group directory
- **AND** the comment nests under the entry within the group's detail view

#### Scenario: Comment entry has no add-comment action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no "Add comment" action is shown (comments cannot target other comments)
