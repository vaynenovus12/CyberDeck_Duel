package com.example.cyberdeckduel;


import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;


public class EmailSignupActivity extends AppCompatActivity {

    private SoundPool soundPool;

    private int buttonSoundID, backbuttonSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_email_signup);

        // Initialize the SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        buttonSoundID = soundPool.load(this, R.raw.cyberdeckduel_menubutton_sfx, 1);
        backbuttonSoundID = soundPool.load(this, R.raw.cyberdeckduel_backbutton_sfx,1);

        // Override back button behavior
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Play the back button sound
                soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);

                // Finish the activity
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void goToPasswordStep(View v) {
        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);
        Intent i = new Intent(EmailSignupActivity.this, PasswordSignupActivity.class);
        TextInputEditText emailInput = findViewById(R.id.email_field);
        String email = emailInput.getEditableText().toString();


        if (email.isEmpty()) {
            Toast.makeText(EmailSignupActivity.this, "Please enter your email", Toast.LENGTH_LONG).show();
            emailInput.setError("Email is required");
            emailInput.requestFocus();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(EmailSignupActivity.this, "Please enter valid email", Toast.LENGTH_LONG).show();
            emailInput.setError("Valid email is required");
            emailInput.requestFocus();
        } else {

            i.putExtra("email", email);
            startActivity(i);
        }

    }
    public void goBackToMain(View v) {
        soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);
        finish();
    }
}