package com.example.cyberdeckduel;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateOfBirthActivity extends AppCompatActivity {
    private String email, pwd, confirm_pwd;

    private SoundPool soundPool;

    private int buttonSoundID, backbuttonSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_date_of_birth);

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

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            email = bundle.getString("email");
            pwd = bundle.getString("pwd");
            confirm_pwd = bundle.getString("confirm_pwd");
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

    public void goToGenderStep(View v) {

        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);

        DatePicker dob = findViewById(R.id.dob_picker);
        int day = dob.getDayOfMonth();
        int month = dob.getMonth();
        int year = dob.getYear();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);


        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String formatedDateDOB =  sdf.format(calendar.getTime());


        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        bundle.putString("pwd", pwd);
        bundle.putString("confirm_pwd", confirm_pwd);
        bundle.putString("dob", formatedDateDOB);

        Intent i = new Intent(DateOfBirthActivity.this, GenderSignupActivity.class);
        i.putExtras(bundle);

        startActivity(i);
    }
    public void goBacktoPassword(View v) {
        soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);
        finish();
    }
}