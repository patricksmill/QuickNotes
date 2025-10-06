# QuickNotes - Android App

[![Android CI (Build Debug APK)](https://github.com/patricksmill/QuickNotes/actions/workflows/android.yml/badge.svg)](https://github.com/patricksmill/QuickNotes/actions/workflows/android.yml)

## Overview

A note-taking application for Android devices that makes taking and organizing notes simpler, leveraging the power of AI.

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

## Roadmap

This roadmap outlines the planned improvements and future direction for QuickNotes.

### Short-Term Goals
*   **Resolve Existing Bugs:**
    *   Address notification bugs (cannot set previous time for current day, notifications not disabled after untoggling).
    *   Enhance notification interactions (launch app, delete note from notification).
*   **Complete MVVM Adoption & Refinement:** Ensure all UI controllers (Fragments) consistently use ViewModels, fully abstracting logic from views. Continue refining the separation of concerns between Activity, Fragments, ViewModels, and Repositories.
*   **Dependency Injection with Hilt:** Integrate Hilt for robust dependency management across the application, improving testability and code structure.
*   **UI/UX Polish:**
    *   Improve empty states for notes, tags, and search results.
    *   Review and enhance basic accessibility (content descriptions, touch targets, focus order).
*   **Increase Unit Test Coverage:** Expand unit tests for ViewModels, Repositories, and core logic.

### Medium-Term Goals
*   **Database Persistence:** Migrate from JSON file storage to a Room database for improved performance, querying capabilities, and data integrity.
- **Kotlin Migration (Incremental):** Begin strategically migrating key components from Java to Kotlin to leverage modern language features like coroutines for background tasks and improved null safety.
-   **Rich Text Editing:** Implement basic rich text editing features for note content (e.g., bold, italics, checklists).
*   **Advanced Search & Filtering:** Introduce more granular search options (e.g., by creation/modification date, notes without tags).
*   **UI Test Automation:** Add more Espresso UI tests for critical user flows.

### Long-Term Goals
-   **Cloud Sync & Backup:** Explore options for syncing notes across devices using a cloud service (e.g., Google Drive API or a dedicated backend).
*   **Customizable Themes:** Offer users options for different app themes (e.g., various dark and light modes, accent colors).
*   **Tablet/Foldable UI Optimization:** Enhance layouts and user experience for larger screens and foldable devices.
*   **AI Feature Expansion:**
    *   Investigate AI-powered note summarization for long notes.
    *   Explore possibilities for natural language search queries.
*   **Home Screen Widgets:** Develop widgets for quick note creation or displaying specific notes.
*   **CI/CD Pipeline:** Set up a Continuous Integration/Continuous Deployment pipeline for automated builds and testing.