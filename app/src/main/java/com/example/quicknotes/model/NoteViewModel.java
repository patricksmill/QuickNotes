package com.example.quicknotes.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quicknotes.util.Event;

import java.util.ArrayList; // Added
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NoteViewModel extends AndroidViewModel {
    private final NoteLibrary noteLibrary;
    private final NotificationRepository notificationRepository;
    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> snackbarMessage = new MutableLiveData<>();
    private final MutableLiveData<Event<UiAction>> uiEvents = new MutableLiveData<>();
    private final MutableLiveData<Event<Note>> _navigateToManageNoteFromIntent = new MutableLiveData<>();
    public LiveData<Event<Note>> navigateToManageNoteFromIntent = _navigateToManageNoteFromIntent;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteLibrary = new NoteLibrary(application);
        notificationRepository = new NotificationRepository(application);
        refreshNotesLiveData(); // Initial load
    }

    private void refreshNotesLiveData() {
        List<Note> currentNotes = noteLibrary.getNotes();
        notesLiveData.setValue(currentNotes != null ? new ArrayList<>(currentNotes) : new ArrayList<>());
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<String> getSnackbarMessage() {
        return snackbarMessage;
    }

    public LiveData<Event<UiAction>> getUiEvents() {
        return uiEvents;
    }

    public void addNote(Note note) {
        noteLibrary.addNote(note);
        refreshNotesLiveData();
    }

    public void deleteNote(Note note) {
        noteLibrary.deleteNote(note);
        refreshNotesLiveData();
    }

    public void searchNotes(String query, boolean title, boolean content, boolean tag) {
        List<Note> filteredNotes = noteLibrary.searchNotes(query, title, content, tag);
        notesLiveData.setValue(filteredNotes != null ? new ArrayList<>(filteredNotes) : new ArrayList<>());
    }

    public void undoDelete() {
        if (noteLibrary.undoDelete()) {
            refreshNotesLiveData();
        }
    }

    public void togglePin(Note note) {
        noteLibrary.togglePin(note);
        refreshNotesLiveData();
    }

    public void deleteAllNotes() {
        noteLibrary.deleteAllNotes();
        refreshNotesLiveData();
        snackbarMessage.setValue("All notes deleted");
    }

    public TagManager getTagManager() {
        return noteLibrary.getManageTags();
    }

    public void setTags(@NonNull Note note, @NonNull List<String> tags) {
        noteLibrary.getManageTags().setTags(note, tags);
        refreshNotesLiveData(); 
    }

    public Set<Tag> getAllTags() {
        return noteLibrary.getManageTags().getAllTags();
    }

    public List<Note> filterNotesByTags(Set<String> tagNames) {
        return noteLibrary.getManageTags().filterNotesByTags(tagNames);
    }

    public boolean isValidNotificationDate(Date date) {
        return notificationRepository.isValidNotificationDate(date);
    }

    public void scheduleNotification(Note note, boolean enabled, Date date) {
        noteLibrary.updateNoteNotificationSettings(note, enabled, date);
        refreshNotesLiveData();

        if (enabled && date != null) {
            notificationRepository.scheduleNotification(note); 
        } else {
            notificationRepository.cancelNotification(note);
        }
    }

    public void addDemoNotes() {
        String[] titles = {"Meeting", "Shopping List", "Ideas for Presentation", "Reminder", "Workout routine"};
        String[] topics = {
                "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
                "groceries, household items, and birthday gifts.",
                "emphasize key points, statistics, and visual aids.",
                "call the doctor's office to schedule the a physical",
                "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        };

        for (int i = 0; i < topics.length; i++) {
            // Directly use noteLibrary.addNote to avoid multiple LiveData updates in a loop
            noteLibrary.addNote(new Note(titles[i], topics[i], new LinkedHashSet<>()));
        }
        refreshNotesLiveData(); // Single refresh after loop
        snackbarMessage.setValue("Demo notes added");
    }

    public void saveNote(Note note, boolean isNewNote) {
        if (isNewNote) {
            noteLibrary.addNote(note); 
        } else {
            noteLibrary.updateNote(note); // Added call to updateNote in NoteLibrary
        }
        refreshNotesLiveData(); 
    }


    public void requestBrowseNotes() {
        uiEvents.setValue(new Event<>(UiAction.BROWSE_NOTES));
    }

    public void requestOpenSettings() {
        uiEvents.setValue(new Event<>(UiAction.OPEN_SETTINGS));
    }

    public boolean isAiTaggingConfigured() {
        return getTagManager().isAiMode() && new TagSettingsManager(getApplication()).hasValidApiKey();
    }

    public boolean shouldConfirmAiSuggestions() {
        return new TagSettingsManager(getApplication()).isAiConfirmationEnabled();
    }

    public void aiSuggestTags(Note note, int limit, Consumer<List<String>> onSuggestions, Consumer<String> onError) {
        getTagManager().aiSuggestTags(note, limit, onSuggestions, onError);
    }

    public void processViewNoteIntent(String noteId) {
        if (noteId == null) return;
        List<Note> currentNotes = noteLibrary.getNotes(); // Use a local copy for iteration
        if (currentNotes == null) return;

        for (Note note : currentNotes) {
            if (noteId.equals(note.getId())) {
                _navigateToManageNoteFromIntent.setValue(new Event<>(note));
                return;
            }
        }
    }

    public enum UiAction {
        BROWSE_NOTES,
        OPEN_SETTINGS
    }
}
