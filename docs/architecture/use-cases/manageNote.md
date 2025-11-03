# ManageNote

## 1. Primary actor and goals
Who is the main interested party and what goal(s) this use case is designed to help them achieve.

__User__: Wants to add a Note to the app quickly and in as little steps as possible, or to manage a previously made note.

## 2. Other stakeholders and their goals

No other stakeholders.


## 2. Preconditions

What must be true prior to the start of the use case.


* User has picked a note to manage or selected new note

## 4. Postconditions

What must be true upon successful completion of the use case.


Note is saved and tagged.
If requested by User, Note is deleted.
User is indicated that changes are saved.



## 4. Workflow

The sequence of steps involved in the execution of the use case, in the form of one or more activity diagrams (please feel free to decompose into multiple diagrams for readability).

The workflow can be specified at different levels of detail:

* __Brief__: main success scenario only;
* __Casual__: most common scenarios and variations;
* __Fully-dressed__: all scenarios and variations.



```plantuml
@startuml

skin rose
title noteLibrary (casual level)

'define the lanes
|#application|User|
|#implementation|System|


|User|
start
if (Note type) is (New) then
|System|

:Prompt user for note;

|User|
:Type in note and press submit;

|System| 
:Note is tagged by ChatGPT or fallback system;
   
else (Existing)
|System|
:User presented with options to edit note body, tag, or notification; or to delete the note;
    if (edit) is yes then
        if (tag) is (yes) then
            :execute ManageTags;
            else if (reminder) is (yes) then
                :execute ManageReminders;
        
            else (body)
                :prompt user to change text;
                |User|
                :changes text, presses submit;
                |System|
        
            :Show save confirmation;
            endif;
    
       
  
   
        
   
   
   
   
   :Return to userSession menu;
   
 @enduml

```


## Sequence Diagram

```plantuml
@startuml
hide footbox
skin rose
actor User as user
participant "SearchNotesFragment" as listUI
participant "ManageNoteFragment" as noteUI
participant "ControllerActivity" as controller
participant "NoteLibrary" as noteLibrary
participant "ManageTags" as manageTags
participant "Note" as note
participant "Persistence" as persistence

== Create New Note ==
user -> listUI: clicks FAB to add note
listUI -> controller: onNewNote()
controller -> note: new Note("", "", new LinkedHashSet<>())
controller -> noteUI: displays ManageNoteFragment with empty note
note right of noteUI: Shows bottom sheet dialog with empty form fields
noteUI --> user: shows note creation interface
user -> noteUI: enters title and content
user -> noteUI: enters tags (comma separated)
user -> noteUI: configures notification (optional)
user -> noteUI: clicks Save button
noteUI -> noteUI: validates required fields
note right of noteUI: Ensures title and content aren't empty
noteUI -> noteUI: processes tags from text input
noteUI -> controller: onSaveNote(note, true)
controller -> noteLibrary: addNote(note)
note right of noteLibrary: Checks title uniqueness and updates date
noteLibrary -> manageTags: isAiMode()
alt AI Tagging Mode
  manageTags --> noteLibrary: true
  noteLibrary -> manageTags: aiAutoTag(note, limit)
  note right of manageTags: Uses OpenAI API to generate relevant tags
else Simple Tagging Mode
  manageTags --> noteLibrary: false
  noteLibrary -> manageTags: simpleAutoTag(note, limit)
  note right of manageTags: Scans note content for keywords that match dictionary
end
noteLibrary -> persistence: saveNotes(ctx, notes)
persistence --> noteLibrary: success
noteLibrary --> controller: success
controller -> listUI: updateNotesView()
listUI --> user: shows updated note list with new note
noteUI --> user: dismisses edit interface

== Edit Existing Note ==
user -> listUI: taps on existing note
listUI -> controller: onManageNotes(note)
controller -> noteUI: displays ManageNoteFragment with note
noteUI -> noteUI: bindNoteFields()
note right of noteUI: Populates form with existing note data
noteUI --> user: shows note edit interface
user -> noteUI: modifies note content
user -> noteUI: updates tags
user -> noteUI: clicks Save button
noteUI -> controller: onSaveNote(note, false)
controller -> noteLibrary: getNotes()
note right of noteLibrary: Note is already in the collection, so no need to add it
noteLibrary --> controller: notes collection
controller -> persistence: saveNotes(ctx, notes)
persistence --> controller: success
controller -> listUI: updateNotesView()
listUI --> user: shows updated note list
noteUI --> user: dismisses edit interface


== Delete Note (Alternative Flow) ==
user -> noteUI: clicks Delete button
noteUI -> noteUI: shows confirmation dialog
user -> noteUI: confirms deletion
noteUI -> controller: onDeleteNote(note)
controller -> noteLibrary: deleteNote(note)
noteLibrary -> persistence: saveNotes(ctx, notes)
persistence --> noteLibrary: success
noteLibrary --> controller: deleted note
controller -> listUI: updateView()
listUI --> user: shows updated list
noteUI --> user: dismisses edit interface
@enduml
```


