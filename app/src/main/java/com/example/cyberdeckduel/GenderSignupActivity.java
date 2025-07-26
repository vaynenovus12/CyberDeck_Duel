package com.example.cyberdeckduel;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class GenderSignupActivity extends AppCompatActivity {

    private String email, pwd, confirm_pwd, dob, gender;
    private RadioButton genderRadioBtn;

    private SoundPool soundPool;

    private int buttonSoundID, backbuttonSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gender_signup);

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

        RadioGroup genderRadioGroup = findViewById(R.id.genderRadioGroup);

        genderRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            genderRadioBtn = findViewById(i);
            gender = genderRadioBtn.getText().toString();
            Log.d("Gender", genderRadioBtn.getText().toString());
        });

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            email = bundle.getString("email");
            pwd = bundle.getString("pwd");
            confirm_pwd = bundle.getString("confirm_pwd");
            dob = bundle.getString("dob");
        }

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

    public void goToNameStep(View v) {
        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);

        if(gender == null) {
            Toast.makeText( GenderSignupActivity.this, "Please choose your gender", Toast.LENGTH_LONG).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            bundle.putString("pwd", pwd);
            bundle.putString("confirm_pwd", confirm_pwd);
            bundle.putString("dob", dob);
            bundle.putString("gender", gender);

            Intent i = new Intent(GenderSignupActivity.this, NameSignupActivity.class);
            i.putExtras(bundle);
            startActivity(i);
        }

    }
    public void goBackToDOB(View v) {
        soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);
        finish();
    }
}