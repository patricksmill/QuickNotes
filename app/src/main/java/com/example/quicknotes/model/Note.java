package com.example.quicknotes.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Note implements INote {
    private String title;
    private String content;
    private Set<Tag> tags;
    private Date lastModified;
    private boolean notificationsEnabled;
    private Date notificationDate;


    private boolean pinned;

    public Note(String title, String content, Set<Tag> tags) {
        this.title = title;
        this.content = content;
        this.tags = tags != null
            ? new LinkedHashSet<>(tags)
            : new LinkedHashSet<>();
        this.lastModified = new Date();
        this.pinned = false;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
        this.lastModified = new Date();
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
        this.lastModified = new Date();
    }

    @NonNull
    @Override
    public String toString() {
        return title + ": " + content;
    }

    @Override
    public Set<Tag> getTags() {
        return tags;
    }


    @Override
    public void setTag(Tag tag) {
        tags.add(tag);
        this.lastModified = new Date();
    }

    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public List<String> getTagNames() {
        Set<Tag> tags = getTags();
        List<String> tagNames = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagNames.add(tag.getName());
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

}
    



