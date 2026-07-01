```mermaid
sequenceDiagram
    participant User
    participant ComposeUI as Compose Screen/Sheet
    participant Controller as ControllerActivity
    participant NoteLibrary
    participant TagManager
    participant AutoTaggingService
    participant Persistence
    participant OpenAI

    User->>ComposeUI: Create Note
    ComposeUI->>Controller: onSaveNote(note, isNew)
    Controller->>NoteLibrary: addNote(note)
    NoteLibrary->>Persistence: saveNotes()
    Persistence->>Persistence: Write JSON
    Controller-->>ComposeUI: refreshNotes (recomposition)

    User->>ComposeUI: AI Tag Request
    ComposeUI->>Controller: onAiSuggestTags(note, limit)
    Controller->>TagManager: aiSuggestTags(note, limit)
    TagManager->>AutoTaggingService: performAiSuggest()
    AutoTaggingService->>OpenAI: API request
    OpenAI-->>AutoTaggingService: tag list
    AutoTaggingService-->>TagManager: suggestions
    TagManager-->>Controller: suggestions
    Controller-->>ComposeUI: MultiSelectBottomSheet

    Note over OpenAI: External Service
```
