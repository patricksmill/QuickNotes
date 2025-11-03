# Manage Tags

## 1. Primary actor and goals
Who is the main interested party and what goal(s) this use case is designed to help them achieve.

__User__: Wants to input a tag into the system for their thoughts


## 2. Other stakeholders and their goals

* __System__: wants to easily manage the tags in the system


## 2. Preconditions

What must be true prior to the start of the use case.

* Manage tags menu is opened
* Note to be tagged is identified/tag to be edited is identified

## 4. Postconditions

What must be true upon successful completion of the use case.

* Tag is saved
* note is tagged if that is what user wants
* Tag is updated to the tag list if that is what user wants
* tag edited/deleted if that is what user wants


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

title manageTags (casual level)

'define the lanes
|#application|User|
|#technology|System|

|User|
start
:manage tags is executed;
:decides whether to edit, add, or delete a tag;

if (add tag OR edit tag)

|System|
:Prompts user for a tag name;
|User|
:inputs desired tag;

|System|
if (tag exists) is (false)then;
:saves tag to tag list;

else (true);

endif;


else(deleteTag)
|System| 
:prompts user for a tag name;
|User|
:inputs desired tag;
|System|
:deletes tag from the system;
endif
stop
@enduml
```

## Sequence Diagram

```plantuml
@startuml
hide footbox
skin rose
actor User as user
participant "ManageNoteFragment" as noteUI
participant "ControllerActivity" as controller
participant "ManageTags" as manageTags
participant "Note" as note
participant "Persistence" as persistence
participant "KeywordTagDictionary" as tagDict
participant "OpenAIClient" as aiClient

== Manual Tagging ==
user -> noteUI: enters tag in tag field
noteUI -> controller: onSetTag(note, tagName)
controller -> manageTags: setTag(note, tagName)
manageTags -> manageTags: getTagColorRes(tagName)
note right of manageTags: Assigns a color to the tag,\nreusing existing color if available
manageTags -> note: setTag(new Tag(tagName, colorRes))
manageTags -> persistence: saveNotes(ctx, notes)
persistence --> manageTags: success
manageTags --> controller: success
controller --> noteUI: success
noteUI --> user: tag appears in note's tag list

== Simple Auto-Tagging ==
user -> noteUI: edits and saves note
noteUI -> controller: onSaveNote(note, isNewNote)
controller -> manageTags: isAiMode()
manageTags --> controller: false
controller -> manageTags: simpleAutoTag(note, limit)
manageTags -> tagDict: loadTagMap(ctx)
tagDict --> manageTags: keyword to tag mapping
note right of manageTags: Scans note content for\nkeywords that match dictionary
loop for each matching keyword
  manageTags -> manageTags: setTag(note, tagName)
  manageTags -> note: setTag(new Tag(tagName, colorRes))
end
manageTags -> persistence: saveNotes(ctx, notes)
persistence --> manageTags: success
manageTags --> controller: success
controller --> noteUI: success
noteUI --> user: auto-tags appear in note

== AI Auto-Tagging ==
user -> noteUI: edits and saves note
noteUI -> controller: onSaveNote(note, isNewNote)
controller -> manageTags: isAiMode()
manageTags --> controller: true
controller -> manageTags: aiAutoTag(note, limit)
manageTags -> manageTags: getAllTags()
manageTags -> aiClient: create OpenAI request with context
aiClient --> manageTags: tag suggestions
loop for each suggested tag
  manageTags -> manageTags: setTag(note, tagName)
  manageTags -> note: setTag(new Tag(tagName, colorRes))
end
manageTags -> persistence: saveNotes(ctx, notes)
persistence --> manageTags: success
manageTags --> controller: success
controller --> noteUI: success
noteUI --> user: AI-generated tags appear in note

== Tag Filtering ==
user -> noteUI: selects tag filter
noteUI -> controller: onManageTags()
controller -> manageTags: reference
controller --> noteUI: manageTags reference
noteUI -> manageTags: filterNotesByTags(activeTagNames)
manageTags -> controller: getNotes()
controller --> manageTags: list of all notes
note right of manageTags: Filters notes that have\nany of the selected tags
manageTags --> noteUI: filtered notes
noteUI --> user: displays filtered notes

== Tag Cleanup ==
controller -> manageTags: cleanupUnusedTags()
manageTags -> manageTags: getAllTags()
note right of manageTags: Identifies tags no longer\nused in any notes
manageTags -> persistence: saveTagMap(ctx, colorMap)
persistence --> manageTags: success
@enduml
```


