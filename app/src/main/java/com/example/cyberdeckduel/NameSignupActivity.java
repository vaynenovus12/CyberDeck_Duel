package com.example.cyberdeckduel;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class NameSignupActivity extends AppCompatActivity {
    private String email, pwd, dob, gender;
    private int coins = 0; // Initialize coins
    private int level = 1; // Default level
    private int xp = 0; // Default XP

    private int wins = 0;
    private ArrayList<Card> deck = new ArrayList<>(); // Initialize deck
    private static final String TAG = "NameSignupActivity";

    private SoundPool soundPool;

    private int buttonSoundID, backbuttonSoundID;

    public NameSignupActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_name_signup);

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

        Button privacyPolicyBtn = findViewById(R.id.privacy_policy_btn);
        privacyPolicyBtn.setOnClickListener(view -> {
            soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);
            // Open the Privacy Policy activity
            Intent intent = new Intent(NameSignupActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            email = bundle.getString("email");
            pwd = bundle.getString("pwd");
            String confirm_pwd = bundle.getString("confirm_pwd");
            dob = bundle.getString("dob");
            gender = bundle.getString("gender");
        }

        TextInputEditText nameInput = findViewById(R.id.name_field);

        Button createAccountBtn = findViewById(R.id.create_account_btn);
        createAccountBtn.setOnClickListener(view -> {
            soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);

            String name = Objects.requireNonNull(nameInput.getEditableText()).toString();
            if (name.trim().isEmpty()) {
                Toast.makeText(NameSignupActivity.this, "Please enter your name", Toast.LENGTH_LONG).show();
                nameInput.setError("Name is required");
                nameInput.requestFocus();
            } else {
                try {
                    loadCardsFromJson(); // Load cards before registering user
                    registerUser(name, email, pwd, dob, gender, coins, level, xp, wins,  deck);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error loading cards from JSON", e);
                    Toast.makeText(NameSignupActivity.this, "Failed to load card data.", Toast.LENGTH_LONG).show();
                }
            }
        });

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


    private void loadCardsFromJson() throws IOException, JSONException {
        String json = loadJSONFromAsset();
        if (json == null) {
            Log.e(TAG, "Failed to load JSON file.");
            return;
        }

        JSONObject jsonObject = new JSONObject(json);

        for (int i = 0; i < jsonObject.length(); i++) {
            String cardId = Objects.requireNonNull(jsonObject.names()).getString(i);
            JSONObject cardJson = jsonObject.getJSONObject(cardId);

            Card card = new Card();
            card.setId(cardJson.getString("id"));
            card.setName(cardJson.getString("name"));
            card.setCategory(cardJson.getString("category"));
            card.setType(cardJson.getString("type"));
            card.setDescription(cardJson.getString("description"));
            card.setPower(cardJson.getInt("power"));

            // Add each card to the user's deck
            deck.add(card);
        }
    }

    private String loadJSONFromAsset() {
        StringBuilder json = new StringBuilder();
        try {
            InputStream is = getAssets().open("cardsList.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file.", e);
            return null;
        }
        return json.toString();
    }

    private void registerUser(String name, String email, String pwd, String dob, String gender, int coins, int level, int xp, int wins, ArrayList<Card> deck) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(NameSignupActivity.this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();

                UserProfileChangeRequest profileChangeReq = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

                assert firebaseUser != null;
                firebaseUser.updateProfile(profileChangeReq).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        // Write user details to Firebase Database
                        ReadWriteUserDetails writeUserDetails = new ReadWriteUserDetails(name, dob, gender, coins, level, xp, wins, deck);

                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Players");
                        DatabaseReference progressionCardsRef = FirebaseDatabase.getInstance().getReference("ProgressionCards");

                        referenceProfile.child(firebaseUser.getUid()).setValue(writeUserDetails).addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                // Assign progression cards based on initial level
                                progressionCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                                                String cardId = cardSnapshot.getKey();
                                                int unlockLevel = cardSnapshot.child("unlockLevel").getValue(Integer.class);

                                                if (unlockLevel <= level) {
                                                    // Add unlocked progression card to the user's deck
                                                    Card card = cardSnapshot.getValue(Card.class);
                                                    if (card != null) {
                                                        referenceProfile.child(firebaseUser.getUid()).child("deck").child(cardId).setValue(card);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error fetching progression cards: ", error.toException());
                                    }
                                });

                                // Send email verification
                                firebaseUser.sendEmailVerification().addOnCompleteListener(task3 -> {
                                    if (task3.isSuccessful()) {
                                        Toast.makeText(NameSignupActivity.this, "Account created successfully. Please verify your email", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(NameSignupActivity.this, HomeActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e(TAG, "Email verification sending failed: " + task3.getException());
                                        Toast.makeText(NameSignupActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                Log.e(TAG, "Database update failed: " + task2.getException());
                                Toast.makeText(NameSignupActivity.this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "User profile update failed: " + task1.getException());
                        Toast.makeText(NameSignupActivity.this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                try {
                    throw Objects.requireNonNull(task.getException());
                } catch (FirebaseAuthUserCollisionException e) {
                    Toast.makeText(NameSignupActivity.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Registration error: " + e.getMessage());
                    Toast.makeText(NameSignupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    public void goBackToGender(View v) {
        soundPool.play(backbuttonSoundID, 1, 1, 0, 0, 1);
        finish();
    }
}
