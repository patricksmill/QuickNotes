package com.example.quicknotes.view;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.quicknotes.controller.ControllerActivity;
import com.example.quicknotes.model.ManageTags;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TagColorSettingsFragment extends PreferenceFragmentCompat {
    private ManageTags mgr;
    private int[] colorResIds;
    private String[] colorNames;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof ControllerActivity) {
            NoteLibrary noteLibrary = ((ControllerActivity) context).getNoteLibrary();
            mgr = noteLibrary.getManageTags();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context ctx = requireContext();
        List<ManageTags.ColorOption> colorOptions = mgr.getAvailableColors();
        Set<Tag> allTags = mgr.getAllTags();

        // Initialize color arrays for the dialog
        colorResIds = new int[colorOptions.size()];
        colorNames = new String[colorOptions.size()];
        for (int i = 0; i < colorOptions.size(); i++) {
            ManageTags.ColorOption option = colorOptions.get(i);
            colorResIds[i] = option.resId;
            colorNames[i] = option.name;
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

    @NonNull
    private Preference editTagColor(Tag tag, Context ctx, List<ManageTags.ColorOption> colorOptions) {
        String tagName = tag.getName();
        Preference pref = new Preference(ctx);
        pref.setKey("tag_color_" + tagName);
        pref.setTitle(tagName);
        int currentColorRes = tag.getColorResId();
        String currentColorName = "Default";
        for (ManageTags.ColorOption opt : colorOptions) {
            if (opt.resId == currentColorRes) {
                currentColorName = opt.name;
                break;
            }
        }
        pref.setSummary(currentColorName);
        pref.setOnPreferenceClickListener(p -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("Select color for '" + tagName + "'")
                    .setItems(colorNames, (d, which) -> {
                        int chosen = colorResIds[which];
                        mgr.setTagColor(tagName, chosen);
                        pref.setSummary(colorNames[which]);
                        Toast.makeText(ctx, "Color updated", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            return true;
        });
        return pref;
    }
}