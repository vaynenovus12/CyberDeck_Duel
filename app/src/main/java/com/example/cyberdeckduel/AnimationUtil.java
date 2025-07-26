package com.example.cyberdeckduel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class AnimationUtil {

    public static void animateButtonPress(View view, Runnable onAnimationEnd) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(150)  // Adjust duration as needed
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)  // Adjust duration as needed
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        onAnimationEnd.run();
                                    }
                                });
                    }
                });
    }
}
