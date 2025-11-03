# NotificationReceiver

## 1. Primary actor and goals

__System (Android Alarm/Work)__: Delivers a scheduled trigger. The app should surface a user-facing notification for the related `Note`, and handle user actions from that notification (e.g., open note, dismiss, delete).

## 2. Other stakeholders and their goals

- __Note Taker__: Receives the reminder at the correct time and can act quickly.

## 3. Preconditions

- A `Note` has notifications enabled and a valid future `notificationDate`.
- Global notifications are enabled and, where required, exact alarm permissions are granted.
- A pending intent has been scheduled by `Notifier`.

## 4. Postconditions

- A system notification is posted for the `Note` at the scheduled time, or the trigger is ignored if stale or disabled.
- Optional cleanup actions are performed (e.g., reschedule or cancel).

## 5. Activity (casual level)

```plantuml
@startuml
skin rose
title Handle alarm trigger (casual level)

|#technology|Android|
start
:Alarm fires pending intent;
-> NotificationReceiver;

|#application|App|
:Extract noteId and metadata from intent;
if (Note data valid & enabled?) then (yes)
  :Build and post notification;
else (no)
  :Ignore or cancel any stale schedule;
endif
stop
@enduml
```

## 6. Sequence Diagram

```plantuml
@startuml
hide footbox
skin rose
actor "Android OS" as os
participant "NotificationReceiver" as receiver
participant "Notifier" as notifier
participant "NoteLibrary" as notes
participant "NotificationManager" as nm

== Trigger ==
os -> receiver: onReceive(context, intent)
receiver -> notifier: parseNoteFromIntent(intent)
notifier --> receiver: noteId, enabled, date

== Validate ==
receiver -> notes: getNotes()
notes --> receiver: List<Note>
receiver -> receiver: find note by id
receiver -> receiver: validate enabled & date >= now

== Notify ==
receiver -> nm: notify(notificationId, notification)
nm --> receiver: posted

@enduml
```


