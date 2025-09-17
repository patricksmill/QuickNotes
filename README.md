# QuickNotes - Android App

[![Android CI (Build Debug APK)](https://github.com/patricksmill/QuickNotes/actions/workflows/android.yml/badge.svg)](https://github.com/patricksmill/QuickNotes/actions/workflows/android.yml)

## Overview

A note-taking application for Android devices that makes taking and organizing notes simpler, leveraging the power of AI.

## Project Structure

The project follows the Model-View-Controller (MVC) architectural pattern:

- model: Contains the data models and business logic
- view: Contains the UI components and layouts
- controller: Contains the logic that connects the model and view

## Functionality

This Android application provides the following features:

- Add a new note
- Viewing all notes
- Search notes by title, content, and tags
- Filter notes based on tags
- Edit an existing note
- Auto-tag a note based on keywords, or by using an Open AI API response
- Delete a note by swiping

### Limitations and Known Issues

- OpenAI API requires an API key to be configured
- Internet connection required for AI-powered features
- BUG: User should not be able to set a previous time for a notification for the current day
- BUG: Notifications are not disabled once a user has untoggled the notification for a note
- Users should be able to launch QuickNotes from the notification, or delete the note right from the notification pane via a button on the notification


## How to Run the Application

To run the application, you need to press the run button at the top of Android Studio, or click on the Orange "QuickNotes" app icon on the virtual device.

### Usage Instructions

- When the application starts, you will see a blank menu with a FAB (Floating Action Button) in the bottom right corner. Click it to add a note, once you are done adding notes, click done.
- To delete a note, swipe from the right side of the screen to the left. There is an undo snackbar in case you delete the wrong note.
- To edit a note, tap on it.
- Use the search bar at the top to find notes by title, content, or tags
- Use the filter button to show notes with specific tags
