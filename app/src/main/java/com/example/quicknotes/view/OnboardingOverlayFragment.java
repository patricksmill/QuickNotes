package com.example.quicknotes.model;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.quicknotes.R;

/**
 * OnboardingOverlay creates a spotlight effect that highlights specific views
 * and displays tutorial information to guide users through the app.
 */
public class OnboardingOverlay {
    private final FragmentActivity activity;
    private final ViewGroup rootView;
    private final OnboardingManager.OnboardingStep step;
    private final OnboardingManager onboardingManager;
    
    private OverlayView overlayView;
    private View tutorialCard;
    private View targetView;

    public OnboardingOverlay(@NonNull FragmentActivity activity, 
                           @NonNull ViewGroup rootView, 
                           @NonNull OnboardingManager.OnboardingStep step,
                           @NonNull OnboardingManager onboardingManager) {
        this.activity = activity;
        this.rootView = rootView;
        this.step = step;
        this.onboardingManager = onboardingManager;
    }

    /**
     * Shows the onboarding overlay
     */
    public void show() {
        if (step.targetViewId() != -1) {
            targetView = rootView.findViewById(step.targetViewId());
        }

        createOverlay();
        createTutorialCard();
        
        // Add overlay to root view
        rootView.addView(overlayView);
        
        // Animate in
        animateIn();
    }

    /**
     * Hides the onboarding overlay
     */
    public void hide() {
        if (overlayView != null && overlayView.getParent() != null) {
            animateOut(() -> {
                ((ViewGroup) overlayView.getParent()).removeView(overlayView);
                overlayView = null;
                tutorialCard = null;
                targetView = null;
            });
        }
    }

    /**
     * Creates the overlay view with spotlight effect
     */
    private void createOverlay() {
        overlayView = new OverlayView(activity);
        overlayView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayView.setClickable(true);
        overlayView.setFocusable(true);
    }

    /**
     * Creates the tutorial card with step information
     */
    private void createTutorialCard() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        tutorialCard = inflater.inflate(R.layout.onboarding_card, overlayView, false);
        
        TextView titleText = tutorialCard.findViewById(R.id.onboarding_title);
        TextView descriptionText = tutorialCard.findViewById(R.id.onboarding_description);
        Button nextButton = tutorialCard.findViewById(R.id.onboarding_next);
        Button skipButton = tutorialCard.findViewById(R.id.onboarding_skip);
        
        titleText.setText(step.title());
        descriptionText.setText(step.description());
        
        // Set button text based on step
        if (step.requiresUserAction()) {
            nextButton.setText(R.string.try_it);
            nextButton.setOnClickListener(v -> {
                onboardingManager.executeStepAction(step.action());
                // Don't advance automatically for user action steps
            });
        } else {
            nextButton.setText(R.string.next);
            nextButton.setOnClickListener(v -> {
                onboardingManager.executeStepAction(step.action());
                onboardingManager.nextStep(activity, rootView);
            });
        }
        
        skipButton.setOnClickListener(v -> onboardingManager.skipOnboarding());
        
        // Position the tutorial card
        positionTutorialCard();
        
        overlayView.addView(tutorialCard);
    }

    /**
     * Positions the tutorial card relative to the target view
     */
    private void positionTutorialCard() {
        if (tutorialCard == null) return;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );

        if (targetView != null) {
            // Position card relative to target view
            int[] targetLocation = new int[2];
            targetView.getLocationInWindow(targetLocation);
            
            int[] rootLocation = new int[2];
            rootView.getLocationInWindow(rootLocation);

            int targetY = targetLocation[1] - rootLocation[1];
            
            // Position card below target if there's space, otherwise above
            if (targetY + targetView.getHeight() + 200 < rootView.getHeight()) {
                params.topMargin = targetY + targetView.getHeight() + 32;
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            } else {
                params.bottomMargin = rootView.getHeight() - targetY + 32;
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            }
        } else {
            // Center the card for welcome step
            params.gravity = Gravity.CENTER;
        }

        params.leftMargin = 32;
        params.rightMargin = 32;
        tutorialCard.setLayoutParams(params);
    }

    /**
     * Animates the overlay in
     */
    private void animateIn() {
        overlayView.setAlpha(0f);
        overlayView.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
    }

    /**
     * Animates the overlay out
     */
    private void animateOut(Runnable onComplete) {
        overlayView.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            })
            .start();
    }

    /**
     * Custom view that creates the spotlight overlay effect
     */
    private class OverlayView extends FrameLayout {
        private final Paint backgroundPaint;
        private final Paint clearPaint;
        private RectF spotlightRect;

        public OverlayView(Context context) {
            super(context);
            setWillNotDraw(false);
            
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(180); // Semi-transparent
            
            clearPaint = new Paint();
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            clearPaint.setAntiAlias(true);
            
            calculateSpotlight();
        }

        private void calculateSpotlight() {
            if (targetView == null) {
                spotlightRect = null;
                return;
            }

            // Wait for layout to complete
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    int[] targetLocation = new int[2];
                    targetView.getLocationInWindow(targetLocation);
                    
                    int[] overlayLocation = new int[2];
                    OnboardingOverlay.this.rootView.getLocationInWindow(overlayLocation);
                    
                    float targetX = targetLocation[0] - overlayLocation[0];
                    float targetY = targetLocation[1] - overlayLocation[1];
                    
                    // Add padding around the target view
                    float padding = 24f;
                    spotlightRect = new RectF(
                        targetX - padding,
                        targetY - padding,
                        targetX + targetView.getWidth() + padding,
                        targetY + targetView.getHeight() + padding
                    );
                    
                    invalidate();
                }
            });
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            
            // Draw semi-transparent background
            canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
            
            // Clear spotlight area
            if (spotlightRect != null) {
                float cornerRadius = 16f;
                canvas.drawRoundRect(spotlightRect, cornerRadius, cornerRadius, clearPaint);
            }
        }
    }
} 