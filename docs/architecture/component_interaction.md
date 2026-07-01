# Component Interaction Diagram

This diagram shows how components interact across the application layers.

```mermaid
flowchart TB
 subgraph View["View (Jetpack Compose)"]
    direction TB
        SNS["SearchNotesScreen"]
        MNS["ManageNoteBottomSheet"]
        SETS["SettingsScreen"]
        MTS["ManageTagsBottomSheet"]
        TO["TutorialOverlay"]
        NAV["QuickNotesNavHost"]
  end
        CA["ControllerActivity"]
        NR["NotificationReceiver"]
  
        TM["TagManager"]
        TUT["TutorialManager"]
        NOT["Notifier"]

        AM{"AlarmManager"}

        ATS["AutoTaggingService"]
        TSM["TagSettingsManager"]

        NL["NoteLibrary"]
        PERS["Persistence"]
  
    NAV --> SNS & SETS
    CA --> NAV & MNS & MTS & TO
    CA --> NR & TM & TUT & NOT & NL
    TM --> ATS & TSM
    NL --> PERS
    ATS -- calls --> OAI{"OpenAI API"}
    NOT -- schedules --> AM
    AM -. triggers .-> NR
    NR -. calls .-> NL

```
