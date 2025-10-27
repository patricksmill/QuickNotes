package com.example.quicknotes.view

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.quicknotes.R
import com.example.quicknotes.databinding.MainBinding

/**
 * Class to manage components shared among all screens and the fragments being displayed.
 */
class MainUI(factivity: FragmentActivity) {
    private val binding: MainBinding = MainBinding.inflate(LayoutInflater.from(factivity))
    private val fmanager: FragmentManager = factivity.supportFragmentManager

    init {
        // Set up window insets handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Replaces the contents of the screen's fragment container with the one passed in as an argument.
     *
     * @param frag The fragment to be displayed.
     * @param addToBackStack Whether to add this transaction to the back stack
     */
    fun displayFragment(frag: Fragment, addToBackStack: Boolean) {
        val ftrans = fmanager.beginTransaction()
        ftrans.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        ftrans.replace(binding.fragmentContainerView.id, frag)
        if (addToBackStack) {
            ftrans.addToBackStack(null)
        }
        ftrans.commit()
    }

    /**
     * Retrieve the graphical widget (android view) at the root of the screen hierarchy.
     *
     * @return the screen's root android view (widget)
     */
    fun getRootView(): View = binding.root
}
