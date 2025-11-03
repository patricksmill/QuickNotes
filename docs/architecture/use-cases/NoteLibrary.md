# NoteLibrary

## 1. Primary actor and goals
Who is the main interested party and what goal(s) this use case is designed to help them achieve.

__User__: Able to navigate the through the app with ease and search through their previous notes to find the one they want.


## 2. Other stakeholders and their goals

No other stakeholders 


## 2. Preconditions

What must be true prior to the start of the use case.

User provides search criteria before pressing search.


## 4. Postconditions

What must be true upon successful completion of the use case.

* User is returned to this menu
* If the user has added a note, this is reflected at the top of the notes section
* User was able to successfully browse and search for a desired note.


## 4. Workflow

The sequence of steps involved in the execution of the use case, in the form of one or more activity diagrams (please feel free to decompose into multiple diagrams for readability).

The workflow can be specified at different levels of detail:

* __Brief__: main success scenario only;
* __Casual__: most common scenarios and variations;
* __Fully-dressed__: all scenarios and variations.

Please be sure indicate what level of detail the workflow you include represents.

```plantuml
@startuml

skin rose

title browseNotes (casual level)

'define the lanes
|#application|User|
|#technology|System|



start
|System|
:Displays all notes;
|User| 
if (Users presses search) is (yes) then
|System|
    :Clear list of viewable Notes;

|System|
    while (Search fits criteria) is (yes)
      :Display model.Note;
    endwhile (no)
else (yes) 




endif
|User|
:Selects desired model.Note;

if (User wants to delete note) is (no) then
    |System|
    :executes __manageNote__ ;
stop

else (yes)
|User|
:User swipes left to delete note;

|System|

:Deletes note from System;
stop
@enduml
```


## Sequence Diagram

```plantuml
@startuml
hide footbox
skin rose
actor User as user
participant "SearchNotesFragment" as view
participant "ControllerActivity" as controller
participant "NoteLibrary" as noteLibrary
participant "TagManager" as tagManager
participant "Persistence" as persistence

== Initialization ==
user -> view: opens app
view -> controller: onGetNotes()
controller -> noteLibrary: getNotes()
note right of noteLibrary: Returns a copy of the\nnote list for safety
noteLibrary --> controller: ArrayList<Note>
controller --> view: List<Note>
view -> view: displayNotes()
view --> user: shows notes list

== Searching Notes ==
user -> view: enters search query
view -> controller: onSearchNotes(query, title, content, tag)
controller -> noteLibrary: searchNotes(query, title, content, tag)
note right of noteLibrary: Performs search based on specified criteria
noteLibrary --> controller: filteredNotes
controller -> view: updateView(filteredNotes)
view -> view: displayNotes()
view --> user: shows filtered notes

== Filtering by Tag ==
user -> view: selects tag filter
view -> view: updates activeTagFilters
view -> controller: onManageTags()
controller -> noteLibrary: getManageTags()
noteLibrary --> controller: tagManager
controller --> view: tagManager
view -> noteLibrary: getNotes()
noteLibrary --> view: list of notes
view -> view: filter notes locally by tag names
view --> user: filtered notes
view -> view: displayNotes()
view --> user: shows tag-filtered notes

== Deleting a Note ==
user -> view: swipes note left
view -> controller: onDeleteNote(note)
controller -> noteLibrary: deleteNote(note)
note right of noteLibrary: Stores deleted note for possible undo operation
noteLibrary -> persistence: saveNotes(ctx, notes)
persistence --> noteLibrary: success
noteLibrary --> controller: deleted note
controller -> view: updateView()
view -> view: displayNotes()
view --> user: shows updated list
view --> user: shows undo snackbar
user -> view: clicks undo
view -> controller: onUndoDelete()
controller -> noteLibrary: undoDelete()
note right of noteLibrary: Restores the recently deleted note
noteLibrary -> persistence: saveNotes(ctx, notes)
persistence --> noteLibrary: success
noteLibrary --> controller: success
controller -> view: updateView()
view -> view: displayNotes()
view --> user: shows restored note

== Toggling Note Pin ==
user -> view: clicks pin icon
view -> controller: onTogglePin(note)
controller -> noteLibrary: togglePin(note)
note right of noteLibrary: Inverts the pin status\nand persists the change
noteLibrary -> persistence: saveNotes(ctx, notes)
persistence --> noteLibrary: success
noteLibrary --> controller: success
controller -> view: updateView()
view -> view: displayNotes()
view --> user: shows updated note list with pinned note at top
@enduml
```



