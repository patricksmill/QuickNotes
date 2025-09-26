package com.example.quicknotes.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Note implements java.io.Serializable {
    private String id;
    private String title;
    private String content;
    private final Set<Tag> tags;
    private Date lastModified;
    private boolean notificationsEnabled;
    private Date notificationDate;


    private boolean pinned;

    public Note(String title, String content, Set<Tag> tags) {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = title;
        this.content = content;
        this.tags = tags != null
            ? new LinkedHashSet<>(tags)
            : new LinkedHashSet<>();
        this.lastModified = new Date();
        this.pinned = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.lastModified = new Date();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.lastModified = new Date();
    }

    @NonNull
    @Override
    public String toString() {
        return title + ": " + content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Tag> getTags() {
        return tags;
    }


    public void setTag(Tag tag) {
        tags.add(tag);
        this.lastModified = new Date();
    }

    public Date getLastModified() {
        return lastModified;
    }

    public List<String> getTagNames() {
        Set<Tag> tags = getTags();
        List<String> tagNames = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagNames.add(tag.name());
            }
        }
        return tagNames;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    public Date getNotificationDate() {
        return notificationDate;
    }

    public void setNotificationDate(Date notificationDate) {
        this.notificationDate = notificationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return java.util.Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
    



