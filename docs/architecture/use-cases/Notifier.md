# Notifier

## 1. Primary actor and goals
Who is the main interested party and what goal(s) this use case is designed to help them achieve. For example, for _process sale_:

__Note Taker__: Wants to have their previously inputted notes notified back to them, they want to change the notification settings to exactly what they prefer.


## 2. Other stakeholders and their goals

No other stakeholders


## 2. Preconditions

What must be true prior to the start of the use case.


* User has notifications enabled globally and a note has metadata to notify the user.

## 4. Postconditions

What must be true upon successful completion of the use case.

* Notification data is updated
* Alarm is scheduled with the system (if applicable)
* User receives visual confirmation of scheduled notification


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

title Notify user (casual level)

'define the lanes
|#application|User|
|#technology|System|

|User|
start
:manageNotification is executed;


|System|
while (More notifications?) is (active)
:Prompts for a timeline for notifications (hours, day, week, etc.) 
This will show the current notification settings and the user can update them;
 
|User|
:Selects desired notification time;

|System|
:Updates notification settings, feeds that to deliverNotification();

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
participant "Note" as note
participant "Notifier" as notifier
participant "AlarmManager" as alarmMgr
participant "System Settings" as settings
participant "NotificationReceiver" as receiver

== Configure Notification ==
user -> noteUI: toggles notification switch
noteUI --> user: shows date/time pickers
user -> noteUI: selects notification date and time
user -> noteUI: saves note
noteUI -> controller: onSetNotification(note, enabled, date)
controller -> note: setNotificationsEnabled(enabled)
controller -> note: setNotificationDate(date)
controller -> controller: Persistence.saveNotes()
controller -> notifier: scheduleNotification(note)

== Schedule Notification ==
notifier -> notifier: globalNotificationsAllowed()
note right of notifier: Checks if notifications\nare globally enabled
notifier -> note: isNotificationsEnabled()
note --> notifier: true
notifier -> note: getNotificationDate()
note --> notifier: future date
notifier -> notifier: canScheduleExactAlarms()
note right of notifier: Checks for SCHEDULE_EXACT_ALARM\npermission on Android 12+
notifier --> notifier: true
notifier -> alarmMgr: setExactAndAllowWhileIdle()
note right of alarmMgr: Schedules the alarm to trigger\nat the exact specified time
alarmMgr --> notifier: success
notifier --> user: shows "Notification scheduled" toast

@enduml
```


