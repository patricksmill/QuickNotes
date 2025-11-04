# Component Interaction Diagram

This diagram shows how components interact across the application layers.

```mermaid
flowchart TB
 subgraph View["View"]
    direction TB
        SF["SearchNotesFragment"]
        MF["ManageNoteFragment"]
        SETF["SettingsFragment"]
        TF["ManageTagsFragment"]
        TOF["TutorialOverlayFragment"]
  end
 subgraph Controller["Controller"]
        CA["ControllerActivity"]
        NR["NotificationReceiver"]
  end
        TM["TagManager"]
        UM["TutorialManager"]
        NOT["Notifier"]

        AM["AlarmManager"]

        ATS["AutoTaggingService"]
        TSM["TagSettingsManager"]

        NL["NoteLibrary"]
        PERS["Persistence"]
  
    View --> CA
    CA --> NR & TM & UM & NOT & NL
    TM --> ATS & TSM
    NL --> PERS
    ATS -- calls --> OAI["OpenAI API"]
    NOT -- schedules --> AM
    AM -. triggers .-> NR
    NR -. calls .-> NL

```
