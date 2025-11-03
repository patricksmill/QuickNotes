# Quick Notes Glossary

- **Note**: The core content that users will create. It consists of user inputted titles and content, tags that can be user generated or automatically added, and a modification date.

- **Tag**: A label or category assigned to notes for organization and easier retrieval.

- **Auto-Tagging**: The automatic process of assigning relevant tags to notes based on their content using keywords or an AI model.

## Domain Classes

- **NoteLibrary**: Repository and use-case layer for notes (add, edit, delete, search, pin, undo delete). Exposes `manageTags: TagManager` for tag-related flows.

- **TagManager**: Facade for all tag-related operations. Orchestrates auto-tagging, settings, and repository operations.

- **TagRepository**: Central store for tag data. Performs tag assignment/rename/merge/delete across all notes and manages persistent tag color mappings.

- **TagSettingsManager**: Manages tag-related preferences (AI mode, tag limit, confirmation) and securely stores the OpenAI API key via Android Keystore.

- **AutoTaggingService**: Service that performs keyword-based tagging and AI-powered tag suggestions/assignments.

- **Persistence**: JSON-based read/write of notes and the tag color map to local storage.

- **ControllerActivity**: App controller that mediates between UI fragments and the model layer.

- **NotificationReceiver**: `BroadcastReceiver` that displays scheduled reminders and handles actions (view, delete, dismiss).

- **Notifier**: Schedules/cancels alarms for note reminders and surfaces user feedback.

- **SearchNotesFragment / ManageNoteFragment / SettingsFragment / ManageTagsFragment / OnboardingOverlayFragment / TagColorSettingsFragment**: UI fragments that render screens and forward user actions to `ControllerActivity`.




