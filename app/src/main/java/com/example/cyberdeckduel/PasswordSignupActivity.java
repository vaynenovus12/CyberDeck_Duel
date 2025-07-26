package com.example.cyberdeckduel;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;


public class PasswordSignupActivity extends AppCompatActivity {
    private String email;

    private SoundPool soundPool;

    private int buttonSoundID, backbuttonSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_password_signup);

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

        Intent i = getIntent();
        email = i.getStringExtra("email");

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
    public void goToDOBStep(View v) {
        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);

        TextInputEditText pwdInput = findViewById(R.id.password_field_input);
        TextInputEditText confirm_pwdInput = findViewById(R.id.confirm_password_input);
        String pwd = pwdInput.getEditableText().toString();
        String confirm_pwd = confirm_pwdInput.getEditableText().toString();

        if(pwd.trim().isEmpty()) {
            Toast.makeText( PasswordSignupActivity.this, "Please enter your password", Toast.LENGTH_LONG).show();
            pwdInput.setError("Password is required");
            pwdInput.requestFocus();
        } else if(pwd.trim().length() < 8) {
            Toast.makeText( PasswordSignupActivity.this, "Please should be at least 8 characters", Toast.LENGTH_LONG).show();
            pwdInput.setError("Password too weak");
            pwdInput.requestFocus();
        } else if (!pwd.matches(".*[!@#$%^&*()\\-_=+{};:,<.>].*")) {
            Toast.makeText(PasswordSignupActivity.this, "Password must contain at least one symbol", Toast.LENGTH_LONG).show();
            pwdInput.setError("Symbol required");
            pwdInput.requestFocus();
        } else if(confirm_pwd.trim().isEmpty()) {
            Toast.makeText( PasswordSignupActivity.this, "Please confirm your password", Toast.LENGTH_LONG).show();
            confirm_pwdInput.setError("Password confirmation is required");
            confirm_pwdInput.requestFocus();
        } else if(!pwd.equals(confirm_pwd)) {
            Toast.makeText( PasswordSignupActivity.this, "Passwords do not match", Toast.LENGTH_LONG).show();
            confirm_pwdInput.setError("Passwords do not match");
            confirm_pwdInput.requestFocus();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            bundle.putString("pwd", pwd);
            bundle.putString("confirm_pwd", confirm_pwd);

            Intent i = new Intent(PasswordSignupActivity.this, DateOfBirthActivity.class);
            i.putExtras(bundle);

            startActivity(i);
        }

    }

    public void goBacktoEmail(View v) {
        soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);

        finish();
    }
}