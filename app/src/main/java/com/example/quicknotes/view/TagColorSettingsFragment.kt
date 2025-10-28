package com.example.quicknotes.view;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.quicknotes.controller.ControllerActivity;
import com.example.quicknotes.model.Tag;
import com.example.quicknotes.model.TagColorManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fragment for managing tag color settings.
 * Allows users to customize the colors associated with tags.
 * Follows MVC pattern by delegating actions to the controller.
 */
public class TagColorSettingsFragment extends PreferenceFragmentCompat implements NotesUI {
    private NotesUI.Listener listener;
    private int[] colorResIds;
    private String[] colorNames;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up the listener if we're attached to a ControllerActivity
        if (getActivity() instanceof ControllerActivity) {
            setListener((NotesUI.Listener) getActivity());
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context ctx = requireContext();
        if (listener == null && getActivity() instanceof ControllerActivity) {
            setListener((NotesUI.Listener) getActivity());
        }

        List<TagColorManager.ColorOption> colorOptions =
                (listener != null) ? listener.onGetAvailableColors() : new TagColorManager(ctx).getAvailableColors();
        Set<Tag> allTags = (listener != null) ? listener.onGetAllTags() : java.util.Collections.emptySet();

        // Initialize color arrays for the dialog
        colorResIds = new int[colorOptions.size()];
        colorNames = new String[colorOptions.size()];
        for (int i = 0; i < colorOptions.size(); i++) {
            TagColorManager.ColorOption option = colorOptions.get(i);
            colorResIds[i] = option.resId();
            colorNames[i] = option.name();
        }

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(ctx);
        setPreferenceScreen(screen);

        if (allTags.isEmpty()) {
            Preference noTagsPref = new Preference(ctx);
            noTagsPref.setTitle("No tags available");
            noTagsPref.setSummary("Create tags by editing notes");
            noTagsPref.setSelectable(false);
            screen.addPreference(noTagsPref);
        } else {
            List<Tag> tagList = new ArrayList<>(allTags);
            for (Tag tag : tagList) {
                Preference pref = editTagColor(tag, ctx, colorOptions);
                screen.addPreference(pref);
            }
        }
    }

    /**
     * Creates a preference for editing a tag's color.
     *
     * @param tag The tag to create a preference for
     * @param ctx The context
     * @param colorOptions Available color options
     * @return The configured preference
     */
    @NonNull
    private Preference editTagColor(Tag tag, Context ctx, List<TagColorManager.ColorOption> colorOptions) {
        String tagName = tag.name();
        Preference pref = new Preference(ctx);
        pref.setKey("tag_color_" + tagName);
        pref.setTitle(tagName);
        int currentColorRes = tag.colorResId();
        int currentColor = ContextCompat.getColor(ctx, currentColorRes);
        pref.setIcon(new ColorDrawable(currentColor));
        pref.setIconSpaceReserved(true);
        String currentColorName = "Default";
        for (TagColorManager.ColorOption opt : colorOptions) {
            if (opt.resId() == currentColorRes) {
                currentColorName = opt.name();
                break;
            }
        }
        pref.setSummary(currentColorName);
        pref.setOnPreferenceClickListener(p -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("Select color for '" + tagName + "'")
                    .setItems(colorNames, (d, which) -> {
                        int chosen = colorResIds[which];
                        if (listener != null) {
                            listener.onSetTagColor(tagName, chosen);
                            pref.setSummary(colorNames[which]);
                            pref.setIcon(new ColorDrawable(ContextCompat.getColor(ctx, chosen)));
                            showSuccessMessage();
                        }
                    })
                    .show();
            return true;
        });
        return pref;
    }

    /**
     * Shows a success message using Snackbar for consistency.
     */
    private void showSuccessMessage() {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, "Color updated", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setListener(NotesUI.Listener listener) {
        this.listener = listener;
    }

}