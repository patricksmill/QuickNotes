```mermaid
graph TB
    subgraph "Notification Flow"
        A[User sets reminder]
        B[ManageNoteFragment]
        X[ControllerActivity]
        E[Notifier]
        F[AlarmManager]
        G[NotificationReceiver]
        NL[NoteLibrary]
    end

    A --> B
    B --> X
    X --> E
    X --> NL
    NL --> Save[(save)]
    E --> F
    F --> G
    G --> X
