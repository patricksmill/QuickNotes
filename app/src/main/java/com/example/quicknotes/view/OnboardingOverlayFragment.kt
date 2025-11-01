package com.example.quicknotes.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.quicknotes.R
import com.example.quicknotes.model.OnboardingManager

class OnboardingOverlayFragment : Fragment() {

    private lateinit var step: OnboardingManager.OnboardingStep
    private var callbacks: Callbacks? = null
    private var overlayView: OverlayView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("DEPRECATION")
            step = it.getParcelable(ARG_STEP)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val targetView = if (step.targetViewId != -1) requireActivity().findViewById(step.targetViewId) else null
        overlayView = OverlayView(requireContext(), targetView)

        val tutorialCard = inflater.inflate(R.layout.onboarding_card, overlayView, false)
        setupTutorialCard(tutorialCard)

        overlayView!!.addView(tutorialCard)
        overlayView!!.setOnClickListener { /* consume clicks */ }

        return overlayView!!
    }

    private fun setupTutorialCard(card: View) {
        val titleText = card.findViewById<TextView>(R.id.onboarding_title)
        val descriptionText = card.findViewById<TextView>(R.id.onboarding_description)
        val nextButton = card.findViewById<Button>(R.id.onboarding_next)
        val skipButton = card.findViewById<Button>(R.id.onboarding_skip)

        titleText.text = step.title
        descriptionText.text = step.description

        if (step.requiresUserAction) {
            nextButton.setText(R.string.try_it)
            nextButton.setOnClickListener { callbacks?.onAction(step.action) }
        } else {
            nextButton.setText(R.string.next)
            nextButton.setOnClickListener {
                callbacks?.onAction(step.action)
                callbacks?.onNext()
            }
        }

        skipButton.setOnClickListener { callbacks?.onSkip() }
    }

    fun animateOut(onEnd: () -> Unit) {
        view?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })?.start()
    }

    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    private class OverlayView(context: Context, private val targetView: View?) : FrameLayout(context) {
        private val backgroundPaint: Paint
        private val clearPaint: Paint
        private var spotlightRect: RectF? = null

        init {
            setWillNotDraw(false)
            backgroundPaint = Paint().apply {
                color = Color.BLACK
                alpha = 180 // Semi-transparent
            }
            clearPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                isAntiAlias = true
            }

            if (targetView != null) {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        calculateSpotlight()
                        invalidate()
                    }
                })
            }
        }

        private fun calculateSpotlight() {
            val target = targetView ?: return
            val targetLocation = IntArray(2)
            target.getLocationInWindow(targetLocation)

            val rootLocation = IntArray(2)
            (parent as? View)?.getLocationInWindow(rootLocation)

            val targetX = (targetLocation[0] - rootLocation[0]).toFloat()
            val targetY = (targetLocation[1] - rootLocation[1]).toFloat()

            val padding = 24f
            spotlightRect = RectF(
                targetX - padding,
                targetY - padding,
                targetX + target.width + padding,
                targetY + target.height + padding
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
            spotlightRect?.let {
                val cornerRadius = 16f
                canvas.drawRoundRect(it, cornerRadius, cornerRadius, clearPaint)
            }
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            // Position the tutorial card
            val card = getChildAt(0) ?: return
            val params = card.layoutParams as LayoutParams

            if (targetView != null) {
                val targetLocation = IntArray(2)
                targetView.getLocationOnScreen(targetLocation)
                val rootLocation = IntArray(2)
                (this.parent as View).getLocationOnScreen(rootLocation)

                val targetY = targetLocation[1] - rootLocation[1]

                if (targetY + targetView.height + card.height < height) {
                    params.topMargin = targetY + targetView.height + 32
                    params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                } else {
                    params.bottomMargin = height - targetY + 32
                    params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
            } else {
                params.gravity = Gravity.CENTER
            }

            params.leftMargin = 32
            params.rightMargin = 32
            card.layoutParams = params
        }
    }

    companion object {
        private const val ARG_STEP = "step"

        fun newInstance(step: OnboardingManager.OnboardingStep): OnboardingOverlayFragment {
            return OnboardingOverlayFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_STEP, step)
                }
            }
        }
    }

    interface Callbacks {
        fun onAction(action: OnboardingManager.OnboardingStep.StepAction)
        fun onNext()
        fun onSkip()
    }
}