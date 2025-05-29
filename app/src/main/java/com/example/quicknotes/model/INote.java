package com.example.quicknotes.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface INote {
    String getContent();

    void setContent(String content);

    String getTitle();

    void setTitle(String title);

    List<String> getTagNames();

    void setTag(Tag tag);

    Set<Tag> getTags();
    
    Date getLastModified();
    
    void setLastModified(Date lastModified);
}
