package com.example.cyberdeckduel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth authProfile;

    private SoundPool soundPool;

    private int buttonSoundID;

    CredentialManager credentialManager;

    private ArrayList<Card> deck = new ArrayList<>(); // Initialize deck

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        try {
            storeCardsFromJson();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading cards from JSON", e);
        }

        credentialManager = CredentialManager.create(this);

        authProfile = FirebaseAuth.getInstance();
        setTitle("Main");

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

        checkAndStoreCards();

        findViewById(R.id.google_button).setOnClickListener(view -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        // Configure Google ID option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getResources().getString(R.string.default_web_client_id))
                .build();

        // Create the GetCredentialRequest in Java
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();


        // Obtain the current activity context for the async call
        Activity activity = this;

        // Use a cancellation signal if you want to cancel the operation at any point
        CancellationSignal cancellationSignal = new CancellationSignal();

        // Define an executor to run the callback on the appropriate thread
        Executor executor = Executors.newSingleThreadExecutor();

        // Call getCredentialAsync for asynchronous operation
        credentialManager.getCredentialAsync(
                activity,                // Pass activity context here
                request,                 // Pass the credential request
                cancellationSignal,      // Optional cancellation signal
                executor,                // Executor to handle the callback on background thread
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Handle the successful result
                        handleSignIn(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // Handle the error if something goes wrong
                        Log.e(TAG, "Error retrieving credentials: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

    }




    private void handleSignIn(GetCredentialResponse result) {
        Credential credential = result.getCredential();

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    // Create a GoogleIdTokenCredential object from the received data
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
                    String idTokenString = googleIdTokenCredential.getIdToken();

                    // Authenticate with Firebase using this ID token
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idTokenString, null);
                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                    if (firebaseUser != null) {
                                        if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                            // Load cards into the deck for a new user
                                            try {
                                                loadCardsFromJson();  // Load the cards into the deck
                                            } catch (IOException | JSONException e) {
                                                Log.e(TAG, "Error loading cards from JSON", e);
                                            }

                                            // Register new user data in Firebase Realtime Database
                                            String name = firebaseUser.getDisplayName();
                                            String dob = "01/01/2000"; // Default value or custom logic
                                            String gender = "Not Specified"; // Default value or custom logic
                                            int coins = 0; // Default coins
                                            int level = 1; // Default level
                                            int xp = 0; // Default XP
                                            int wins = 0; // Default Wins
                                            registerUser(name, dob, gender, coins, level, xp, wins, deck, firebaseUser.getUid());
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Firebase authentication failed", task.getException());
                                }
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Google ID token", e);
                }
            } else {
                Log.e(TAG, "Unexpected credential type");
            }
        }
    }

    private void registerUser(String name, String dob, String gender, int coins, int level, int xp, int wins, ArrayList<Card> deck, String uid) {
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Players");
        DatabaseReference cardsReference = FirebaseDatabase.getInstance().getReference("ProgressionCards");

        // Create the user details object with initial cards
        ReadWriteUserDetails writeUserDetails = new ReadWriteUserDetails(name, dob, gender, coins, level, xp, wins, deck);

        // Save the user to the database
        referenceProfile.child(uid).setValue(writeUserDetails).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    firebaseUser.sendEmailVerification().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            // After successful registration, assign progression cards
                            cardsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                                            String cardId = cardSnapshot.getKey();
                                            int unlockLevel = cardSnapshot.child("unlockLevel").getValue(Integer.class);

                                            if (unlockLevel <= level) {
                                                // Add the unlocked progression card to the user's deck
                                                Card card = cardSnapshot.getValue(Card.class);
                                                if (card != null) {
                                                    referenceProfile.child(uid).child("deck").child(cardId).setValue(card);
                                                }
                                            }
                                        }
                                        Toast.makeText(MainActivity.this, "Progression cards assigned based on level.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Error fetching progression cards: ", error.toException());
                                }
                            });

                            Toast.makeText(this, "Account created successfully. Please verify your email", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e(TAG, "Email verification sending failed: " + task2.getException());
                            Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Database update failed: " + task.getException());
                Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
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






    private void checkAndStoreCards() {
        DatabaseReference cardsReference = FirebaseDatabase.getInstance().getReference("Cards");

        // Check if the cards exist
        cardsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // If cards don't exist, load and store them
                    try {
                        storeCardsFromJson();
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error loading cards from JSON", e);
                    }
                } else {
                    Log.i(TAG, "Cards already exist in Firebase.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking Firebase for existing cards", error.toException());
            }
        });
    }

    private void storeCardsFromJson() throws IOException, JSONException {
        String json = loadJSONFromAsset(this);
        if (json == null) {
            Log.e(TAG, "Failed to load JSON file.");
            return;
        }

        JSONObject jsonObject = new JSONObject(json);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Cards");

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
            databaseReference.child(card.getId()).setValue(card);
        }

        Log.i(TAG, "Cards successfully stored in Firebase.");
    }

    private String loadJSONFromAsset(Context context) {
        StringBuilder json = new StringBuilder();
        try {
            InputStream is = context.getAssets().open("cardsList.json");
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

    public void viewSignupPage(View v) {
        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);
        Intent i = new Intent(MainActivity.this, EmailSignupActivity.class);
        startActivity(i);
    }

    //Check if user is already logged in.
    @Override
    protected void onStart() {
        super.onStart();
        if(authProfile.getCurrentUser() != null) {
            Toast.makeText(MainActivity.this, "Already Logged In!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }
    }

    public void viewLoginPage(View v) {
        soundPool.play(buttonSoundID, 1, 1, 0, 0, 1);

        // Display Login Activity
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);
    }




}