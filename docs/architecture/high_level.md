```mermaid
graph TB
    subgraph "View Layer"
        A[ControllerActivity]
        B[SearchNotesScreen]
        C[ManageNoteBottomSheet]
        D[SettingsScreen]
        E[ManageTagsBottomSheet]
        F[TutorialOverlay]
        G[QuickNotesNavHost]
    end

    subgraph "Model/Domain Layer"
        H[NoteLibrary]
        I[TagManager]
        I1[TagOperationsManager]
        I2[TagColorManager]
        I3[TagSettingsManager]
        J[AutoTaggingService]
        K[Notifier]
        L[Persistence]
        O[TutorialManager]
    end

    subgraph "Data/External Layer"
        M[Note]
        N[Tag]
        P[JSON Files]
        Q[OpenAI API]
        R[Android AlarmManager]
        S[NotificationReceiver]
    end

    G --> B
    G --> D
    A --> G
    A --> C
    A --> E
    A --> F
    F --> O
    A --> H
    A --> I
    A --> K
    A --> O

    H --> L
    H --> I
    I --> I1
    I --> I2
    I --> I3
    I --> J
    J --> Q
    K --> R
    S --> A

    L --> P
    H --> M
    I --> N
```
