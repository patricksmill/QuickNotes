package com.example.quicknotes.model;
import androidx.annotation.ColorRes;

public record Tag(String name, @ColorRes int colorResId) {
    @Override
    public int colorResId() {
        return colorResId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag other)) return false;
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

}

