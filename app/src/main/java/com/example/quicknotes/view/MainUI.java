package com.example.quicknotes.view;

import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.quicknotes.databinding.MainBinding;

/**
 * Class to manage components shared among all screens and the fragments being displayed.
 */
public class MainUI {
    private final MainBinding binding;

    /**
     * Constructor method.
     * @param factivity The android activity the screen is associated with.
     */
    public MainUI(@NonNull FragmentActivity factivity) {
        this.binding = MainBinding.inflate(LayoutInflater.from(factivity));

        // eliminates colored bar at top of screen
        EdgeToEdge.enable(factivity);
        ViewCompat.setOnApplyWindowInsetsListener(this.binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Retrieve the graphical widget (android view) at the root of the screen hierarchy.
     *
     * @return the screen's root android view (widget)
     */
    @NonNull
    public View getRootView() { return this.binding.getRoot(); }
}
