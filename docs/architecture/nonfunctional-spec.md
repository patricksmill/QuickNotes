# Non-Functional Requirements

## Usability

- Tag chips with color coding for visual recognition
- Swipe-to-delete gesture with undo capability
- Search functionality accessible from the main screen
- Visual feedback for user actions (toasts, snackbars)

## Reliability

- Local storage for all notes with JSON serialization
- Keyword-based tagging fallback when AI tagging is unavailable

## Security

- No transmission of user data except for OpenAI API interactions

## Supportability

- MVC architecture
- Comprehensive documentation
- Unit and Systems tests


## Implementation

- Target Android API level 35
- Minimum Android API level 30
- Java programming language
- AndroidX libraries for UI components
- Jetpack Preferences for settings management
- RecyclerView for efficient list rendering
- SharedPreferences for configuration storage
- JSON for data serialization

## Performance

- Efficient memory usage with proper view recycling
- Asynchronous API calls to prevent UI slowing down
- Efficient tag filtering algorithm
- Optimized persistence with minimal disk writes
- Responsive search functionality


## External Interfaces

- OpenAI API integration
  - Using GPT-4.1 Nano model for cost-efficient auto-tagging

- Android Notification API
  - Channel-based notification system
  - Exact alarms using AlarmManager

## Constraints

- API rate limits for OpenAI services
- Poor notification implementation
- No support for image-based or sound-based notes
- No support for Markdown language in notes
- Lack of cloud backup/sync


