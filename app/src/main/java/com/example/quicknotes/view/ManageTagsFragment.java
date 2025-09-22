package com.example.quicknotes.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.quicknotes.controller.ControllerActivity;
import com.example.quicknotes.model.Tag;
import com.example.quicknotes.model.TagColorManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ManageTagsFragment provides a dedicated screen to view and manage all tags:
 * rename, delete, merge, and change colors.
 */
public class ManageTagsFragment extends PreferenceFragmentCompat implements NotesUI {
    private NotesUI.Listener listener;
    private int[] colorResIds;
    private String[] colorNames;

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
            tagList.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            for (Tag tag : tagList) {
                screen.addPreference(createTagPreference(ctx, tag));
            }
        }

        Preference mergePref = new Preference(ctx);
        mergePref.setTitle("Merge Tags");
        mergePref.setSummary("Combine multiple tags into one");
        mergePref.setOnPreferenceClickListener(p -> {
            showMergeDialog();
            return true;
        });
        screen.addPreference(mergePref);
    }

    private Preference createTagPreference(Context ctx, Tag tag) {
        Preference pref = new Preference(ctx);
        String tagName = tag.name();
        pref.setKey("manage_tag_" + tagName);
        pref.setTitle(tagName);
        int currentColorRes = tag.colorResId();
        int currentColor = ContextCompat.getColor(ctx, currentColorRes);
        pref.setIcon(new ColorDrawable(currentColor));
        pref.setIconSpaceReserved(true);
        pref.setSummary("Tap to rename, delete, or change color");
        pref.setOnPreferenceClickListener(p -> {
            showTagOptionsDialog(tagName, currentColorRes, pref);
            return true;
        });
        return pref;
    }

    private void showTagOptionsDialog(String tagName, int currentColorRes, Preference pref) {
        String[] options = new String[]{"Rename", "Delete", "Change Color"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage '" + tagName + "'")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0 -> showRenameDialog(tagName);
                        case 1 -> showDeleteConfirm(tagName);
                        case 2 -> showColorPicker(tagName, pref);
                    }
                })
                .show();
    }

    private void showRenameDialog(String oldName) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(oldName);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename Tag")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!newName.isEmpty() && listener != null) {
                        listener.onRenameTag(oldName, newName);
                        refreshScreen();
                    }
                })
                .show();
    }

    private void showDeleteConfirm(String tagName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Tag")
                .setMessage("Remove tag '" + tagName + "' from all notes?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (listener != null) {
                        listener.onDeleteTag(tagName);
                        refreshScreen();
                    }
                })
                .show();
    }

    private void showColorPicker(String tagName, Preference pref) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select color for '" + tagName + "'")
                .setItems(colorNames, (d, which) -> {
                    int chosen = colorResIds[which];
                    if (listener != null) {
                        listener.onSetTagColor(tagName, chosen);
                        int color = ContextCompat.getColor(requireContext(), chosen);
                        pref.setIcon(new ColorDrawable(color));
                    }
                })
                .show();
    }

    private void showMergeDialog() {
        if (listener == null) return;
        Set<Tag> allTags = listener.onGetAllTags();
        if (allTags.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (Tag t : allTags) names.add(t.name());
        names.sort(String::compareToIgnoreCase);

        String[] items = names.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select tags to merge")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Next", (d, w) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < items.length; i++) if (checked[i]) selected.add(items[i]);
                    if (selected.isEmpty()) return;
                    showMergeTargetDialog(selected);
                })
                .show();
    }

    private void showMergeTargetDialog(List<String> sources) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Merge into tag")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Merge", (d, w) -> {
                    String target = input.getText() != null ? input.getText().toString().trim() : "";
                    if (target.isEmpty() || listener == null) return;
                    // De-duplicate sources and remove target if present
                    Set<String> unique = new LinkedHashSet<>(sources);
                    unique.removeIf(s -> s.equalsIgnoreCase(target));
                    if (unique.isEmpty()) return;
                    listener.onMergeTags(new ArrayList<>(unique), target);
                    refreshScreen();
                })
                .show();
    }

    private void refreshScreen() {
        // Rebuild preferences to reflect latest tag set/colors
        onCreatePreferences(null, null);
    }

    private void setListener(NotesUI.Listener listener) {
        this.listener = listener;
    }
}


