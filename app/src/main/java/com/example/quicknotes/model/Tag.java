package com.example.quicknotes.model;
import androidx.annotation.ColorRes;

public class Tag {
    private final String name;
    @ColorRes private final int colorResId;

    public Tag(String name, @ColorRes int colorResId) {
        this.name = name;
        this.colorResId = colorResId;
    }

    public String getName() {
        return name;
    }
    public int getColorResId() {
        return colorResId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

}

