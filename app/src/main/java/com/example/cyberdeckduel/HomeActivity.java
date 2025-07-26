package com.example.cyberdeckduel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseAuth authProfile;
    private FirebaseAuth.AuthStateListener authStateListener;

    private int coins;

    private List<Card> deck;

    private String name, email, dob, gender;

    private MediaPlayer mediaPlayer;

    private SoundPool soundPool;
    private int menuButtonSoundId;

    private CustomImageView playerProfileButton;
    private CustomImageView findMatchButton;
    private CustomImageView settingsButton;


    private CustomImageView cardProgressionButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);
        // Start listening for match updates
        listenForMatchUpdates();

        authProfile = FirebaseAuth.getInstance();

        // Initialize the authStateListener
        authStateListener = firebaseAuth -> {
            FirebaseUser firebaseUser = authProfile.getCurrentUser();
            if (firebaseUser == null) {
                // User is signed out, redirect to MainActivity
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close HomeActivity
            } else {
                // User is signed in, check user data
                checkUserData(firebaseUser);
            }
        };

        // Add the authStateListener to the FirebaseAuth instance
        authProfile.addAuthStateListener(authStateListener);

        // Initialize the SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        menuButtonSoundId = soundPool.load(this, R.raw.cyberdeckduel_menubutton_sfx, 1);

        // Initialize MediaPlayer and start playing background music
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music_ingame);
        mediaPlayer.setLooping(true); // Set looping
        mediaPlayer.start();

        playerProfileButton = findViewById(R.id.playerProfileBtn);
        playerProfileButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    playerProfileButton.setImageResource(R.drawable.player_profile_icon_clicked);
                    soundPool.play(menuButtonSoundId, 1, 1, 0, 0, 1);
                    AnimationUtil.animateButtonPress(playerProfileButton, () -> {
                        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        playerProfileButton.setImageResource(R.drawable.player_profile_icon_unclicked);
                    });
                    findMatchButton.setEnabled(false);
                    cardProgressionButton.setEnabled(false);
                    playerProfileButton.setEnabled(false);
                    settingsButton.setEnabled(false);
                    return true;
                case MotionEvent.ACTION_UP:
                    playerProfileButton.setImageResource(R.drawable.player_profile_icon_unclicked);
                    v.performClick(); // Ensure performClick is called
                    AnimationUtil.animateButtonPress(playerProfileButton, () -> {
                        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                        startActivity(intent);
                    });
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    playerProfileButton.setImageResource(R.drawable.player_profile_icon_unclicked);
                    return true;
            }
            return false;
        });

        findMatchButton = findViewById(R.id.findMatchBtn);
        findMatchButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    soundPool.play(menuButtonSoundId, 1, 1, 0, 0, 1);
                    AnimationUtil.animateButtonPress(findMatchButton, () -> {
                        // Show loading indicator
                        findViewById(R.id.loadingIndicator).setVisibility(View.VISIBLE);
                        findViewById(R.id.loadingText).setVisibility(View.VISIBLE);

                        DatabaseReference matchmakingRef = FirebaseDatabase.getInstance().getReference("Matchmaking");
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        matchmakingRef.child(userId).setValue(true).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Start or re-start listening for match updates
                                listenForMatchUpdates();
                                matchmakingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        ArrayList<String> playerIds = new ArrayList<>();
                                        ArrayList<String> playerNames = new ArrayList<>();
                                        for (DataSnapshot child : snapshot.getChildren()) {
                                            playerIds.add(child.getKey());
                                            // Fetch player name from the database, assuming each player has a name stored
                                            DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Players").child(child.getKey()).child("name");
                                            playerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                                    String playerName = nameSnapshot.getValue(String.class);
                                                    if (playerName != null) {
                                                        playerNames.add(playerName);
                                                    }

                                                    // Check if we have at least 2 players and proceed
                                                    if (playerIds.size() >= 2 && playerNames.size() >= 2) {
                                                        checkExistingMatchOrCreateNew(playerIds, playerNames);
                                                        matchmakingRef.removeValue(); // Clean up matchmaking queue
                                                        matchmakingRef.removeEventListener(this);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e(TAG, "Error fetching player name: ", error.toException());
                                                }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Matchmaking error: ", error.toException());
                                        findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
                                        findViewById(R.id.loadingText).setVisibility(View.GONE);
                                    }
                                });
                            } else {
                                Log.e(TAG, "Failed to join matchmaking queue.", task.getException());
                                findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
                                findViewById(R.id.loadingText).setVisibility(View.GONE);
                            }
                        });

                    });
                    findMatchButton.setEnabled(false);
                    cardProgressionButton.setEnabled(false);
                    playerProfileButton.setEnabled(false);
                    settingsButton.setEnabled(false);
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick(); // Ensure performClick is called
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        });




        cardProgressionButton = findViewById(R.id.cardProgBtn);
        cardProgressionButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    soundPool.play(menuButtonSoundId, 1, 1, 0, 0, 1);
                    AnimationUtil.animateButtonPress(cardProgressionButton, () -> {
                        Intent intent = new Intent(HomeActivity.this, CardProgressionActivity.class);
                        startActivity(intent);
                    });
                    findMatchButton.setEnabled(false);
                    cardProgressionButton.setEnabled(false);
                    playerProfileButton.setEnabled(false);
                    settingsButton.setEnabled(false);
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick(); // Ensure performClick is called
                    AnimationUtil.animateButtonPress(cardProgressionButton, () -> {
                        Intent intent = new Intent(HomeActivity.this, CardProgressionActivity.class);
                        startActivity(intent);
                    });
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        });


        settingsButton = findViewById(R.id.settingsBtn);
        settingsButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    settingsButton.setImageResource(R.drawable.settings_icon_clicked);
                    soundPool.play(menuButtonSoundId, 1, 1, 0, 0, 1);
                    AnimationUtil.animateButtonPress(settingsButton, () -> {
                        settingsButton.setEnabled(false);
                        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        settingsButton.setImageResource(R.drawable.settings_icon_unclicked);
                    });
                    findMatchButton.setEnabled(false);
                    cardProgressionButton.setEnabled(false);
                    playerProfileButton.setEnabled(false);
                    settingsButton.setEnabled(false);
                    return true;
                case MotionEvent.ACTION_UP:
                    settingsButton.setImageResource(R.drawable.settings_icon_unclicked);
                    v.performClick(); // Ensure performClick is called
                    AnimationUtil.animateButtonPress(settingsButton, () -> {
                        settingsButton.setEnabled(false);
                        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    });
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    settingsButton.setImageResource(R.drawable.settings_icon_unclicked);
                    return true;
            }
            return false;
        });

        checkUserData(authProfile.getCurrentUser());


    }


    private void checkExistingMatchOrCreateNew(ArrayList<String> playerIds, ArrayList<String> playerNames) {
        DatabaseReference matchesRef = FirebaseDatabase.getInstance().getReference("Matches");
        String player1Id = playerIds.get(0);
        String player2Id = playerIds.get(1);

        matchesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean matchExists = false;
                String existingMatchId = null;

                // Check if any of the players is already in a match
                for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                    if (matchSnapshot.child("players").hasChild(player1Id) || matchSnapshot.child("players").hasChild(player2Id)) {
                        matchExists = true;
                        existingMatchId = matchSnapshot.getKey();
                        break;
                    }
                }

                if (matchExists) {
                    // Player is already in a match, join the existing match
                    startGameActivity(existingMatchId);
                } else {
                    // No match exists, create a new match
                    createMatch(playerIds, playerNames);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking existing matches: ", error.toException());
            }
        });
    }



    private void createMatch(ArrayList<String> playerIds, ArrayList<String> playerNames) {
        if (playerIds.size() < 2) {
            Log.e(TAG, "Not enough players to create a match.");
            return;
        }

        String player1Id = playerIds.get(0);
        String player2Id = playerIds.get(1);

        String player1Name;
        String player2Name;

        // If you want to ensure the correct assignment based on the current player, you can use:
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (player1Id.equals(currentUserId)) {
            player1Name = playerNames.get(0);
            player2Name = playerNames.get(1);
        } else {
            player1Name = playerNames.get(1);
            player2Name = playerNames.get(0);
        }

        DatabaseReference matchesRef = FirebaseDatabase.getInstance().getReference("Matches");

        // Start a transaction
        matchesRef.runTransaction(new Transaction.Handler() {
            final boolean[] isPlayer1Turn = new boolean[1];

            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                // Check if a match already exists for these players
                boolean matchExists = false;
                for (MutableData matchData : currentData.getChildren()) {
                    if (matchData.child("players").hasChild(player1Id) || matchData.child("players").hasChild(player2Id)) {
                        matchExists = true;
                        break;
                    }
                }

                if (matchExists) {
                    return Transaction.abort();
                } else {
                    // No match exists, create a new match
                    String matchId = matchesRef.push().getKey();
                    if (matchId == null) {
                        Log.e(TAG, "Failed to generate match ID.");
                        return Transaction.abort();
                    }

                    MutableData matchRefData = currentData.child(matchId);
                    matchRefData.child("status").setValue("ready");

                    // Randomly determine which player goes first
                    isPlayer1Turn[0] = Math.random() < 0.5;

                    matchRefData.child("players").child(player1Id).child("isTurn").setValue(isPlayer1Turn[0]);
                    matchRefData.child("players").child(player2Id).child("isTurn").setValue(!isPlayer1Turn[0]);

                    return Transaction.success(currentData);
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Log.d(TAG, "Match created successfully.");

                    String matchId = null;
                    if (currentData != null) {
                        for (DataSnapshot matchSnapshot : currentData.getChildren()) {
                            if (matchSnapshot.hasChild("players") &&
                                    (matchSnapshot.child("players").hasChild(player1Id) ||
                                            matchSnapshot.child("players").hasChild(player2Id))) {
                                matchId = matchSnapshot.getKey();
                                break;
                            }
                        }
                    }

                    if (matchId != null) {
                        DatabaseReference matchRef = matchesRef.child(matchId);

                        // Select random cards for both players
                        selectRandomCards(player1Id, player1Name, matchRef, isPlayer1Turn[0]);
                        selectRandomCards(player2Id, player2Name, matchRef, !isPlayer1Turn[0]);

                        // Notify both players by updating a node under their user data
                        notifyPlayer(player1Id, matchId);
                        notifyPlayer(player2Id, matchId);
                    }
                } else {
                    Log.d(TAG, "Match creation aborted or failed.");
                    if (error != null) {
                        Log.e(TAG, "Transaction error: ", error.toException());
                    }
                }
            }
        });
    }


    private void selectRandomCards(String playerId, String playerName, DatabaseReference matchRef, boolean isTurn) {
        DatabaseReference playersRef = FirebaseDatabase.getInstance().getReference("Players");
        playersRef.child(playerId).child("deck").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Card> deck = new ArrayList<>();
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    Card card = cardSnapshot.getValue(Card.class);
                    if (card != null) {
                        deck.add(card);
                    }
                }

                // Shuffle and update hand - ensure UI updates happen on the main thread
                runOnUiThread(() -> {
                    Collections.shuffle(deck);
                    HashMap<String, Card> hand = new HashMap<>();
                    for (int i = 0; i < Math.min(deck.size(), 10); i++) {
                        Card card = deck.get(i);
                        if (card != null && card.getId() != null) {
                            hand.put(card.getId(), card);
                        }
                    }

                    // Now set the totalCardsLeft based on the hand size (i.e., hand.size())
                    int totalCardsLeft = hand.size();

                    matchRef.child("players").child(playerId).setValue(new PlayerData(playerName, hand, isTurn, totalCardsLeft))
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Player data updated for player: " + playerId + " with turn: " + isTurn);
                                } else {
                                    Log.e(TAG, "Error creating player data: ", task.getException());
                                }
                            });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error selecting cards: ", error.toException());
            }
        });
    }

    private void notifyPlayer(String playerId, String matchId) {
        DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Players").child(playerId).child("currentMatch");
        playerRef.setValue(matchId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Player " + playerId + " notified of match: " + matchId);
            } else {
                Log.e(TAG, "Failed to notify player " + playerId, task.getException());
            }
        });
    }

    private void listenForMatchUpdates() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference playerMatchRef = FirebaseDatabase.getInstance().getReference("Players").child(userId).child("currentMatch");

        playerMatchRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String matchId = snapshot.getValue(String.class);
                    if (matchId != null && !matchId.isEmpty()) {
                        startGameActivity(matchId);
                        // Optionally remove the listener if no longer needed
                        playerMatchRef.removeEventListener(this);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen for match updates: ", error.toException());
            }
        });
    }


    private void startGameActivity(String matchId) {
        Intent intent = new Intent(HomeActivity.this, GameActivity.class);
        intent.putExtra("matchId", matchId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
        findViewById(R.id.loadingText).setVisibility(View.GONE);
    }



    private void checkUserData(FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
            String userID = firebaseUser.getUid();
            DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Players");
            referenceProfile.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    // If user data exists, continue with existing logic
                    else {
                        showUserDetails(firebaseUser); // Or other operations
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking user data existence.", error.toException());
                }
            });

            // Force refresh user info to get updated verification status
            firebaseUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (!firebaseUser.isEmailVerified()) {
                        showAlertDialog();
                    }
                } else {
                    Log.e(TAG, "Error reloading user info.", task.getException());
                }
            });

        } else {
            // No user, redirect to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Close HomeActivity
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return ev.getPointerCount() == 1 && super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }

        // Re-enable buttons when returning to the activity
        if (playerProfileButton != null) playerProfileButton.setEnabled(true);
        if (findMatchButton != null) findMatchButton.setEnabled(true);
        if (settingsButton != null) settingsButton.setEnabled(true);
        if (cardProgressionButton != null) cardProgressionButton.setEnabled(true);

        checkUserData(authProfile.getCurrentUser());
        // Start or re-start listening for match updates
        listenForMatchUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (authProfile != null) {
            authProfile.removeAuthStateListener(authStateListener);
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void showAlertDialog() {
        if (isFinishing() || isDestroyed()) {
            Log.e(TAG, "Attempted to show dialog on a destroyed or finishing activity.");
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(HomeActivity.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Please verify your email first. You can not login without email verification next time.");

        builder.setPositiveButton("Continue", (dialogInterface, i) -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUserDetails(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();

        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Players");
        referenceProfile.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    email = firebaseUser.getEmail();
                    name = firebaseUser.getDisplayName();
                    dob = readUserDetails.dob;
                    gender = readUserDetails.gender;
                    coins = readUserDetails.coins;
                    deck = readUserDetails.deck;
                    coins = readUserDetails.coins;
                    int level = readUserDetails.level;
                    int xp = readUserDetails.xp;


                    TextView playerCoins = findViewById(R.id.playerCoins);
                    int currentCoins = Integer.parseInt(playerCoins.getText().toString()); // Get current coin value
                    animateCoinChange(currentCoins, coins); // Call the animation with current and new coin values

                    TextView playerName = findViewById(R.id.playerName);
                    playerName.setText(name);


                    TextView playerLevelTextView = findViewById(R.id.playerLevel);
                    TextView playerXPTextView = findViewById(R.id.playerXPText);
                    ProgressBar xpProgressBar = findViewById(R.id.playerXPProgressBar);

                    playerLevelTextView.setText("Level: " + level);
                    playerXPTextView.setText("XP: " + xp + "/" + (level * 100));
                    xpProgressBar.setMax(level * 100);
                    xpProgressBar.setProgress(xp);
                    animateProgressBar(xpProgressBar, xp);

                    // Handle level-up logic
                    handleLevelUp(referenceProfile, userID, xp, level);

                    // Ensure progression cards are added to the deck based on current level
                    checkAndAddCardsToDeck(referenceProfile, userID, level);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error appropriately
            }
        });
    }



    private void handleLevelUp(DatabaseReference referenceProfile, String userID, int xp, int level) {
        int xpToNextLevel = level * 100; // XP required for the next level
        boolean leveledUp = false; // Flag to track level-up

        // Check if the XP is enough to level up
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel; // Deduct XP required for the current level
            level++; // Increment level
            xpToNextLevel = level * 100; // Update the XP threshold for the next level
            leveledUp = true; // Mark that a level-up has occurred
        }

        if (leveledUp) {
            // Only update Firebase when there's a level-up
            referenceProfile.child(userID).child("level").setValue(level);
            referenceProfile.child(userID).child("xp").setValue(xp);

            checkAndAddCardsToDeck(referenceProfile, userID, level);

            // Show a level-up message
            Snackbar.make(findViewById(android.R.id.content), "Congratulations! You've leveled up!", Snackbar.LENGTH_LONG).show();
        }

        // Update the UI
        TextView playerLevel = findViewById(R.id.playerLevel);
        playerLevel.setText(String.format("Level: %d", level));

        TextView playerXPText = findViewById(R.id.playerXPText);
        playerXPText.setText(String.format("XP: %d/%d", xp, xpToNextLevel));

        ProgressBar playerXPProgressBar = findViewById(R.id.playerXPProgressBar);
        playerXPProgressBar.setProgress(0);
        playerXPProgressBar.setMax(xpToNextLevel);
        animateProgressBar(playerXPProgressBar, xp);
    }


    private void checkAndAddCardsToDeck(DatabaseReference referenceProfile, String userID, int playerLevel) {
        // Get a reference to the progression cards
        DatabaseReference progressionCardsRef = FirebaseDatabase.getInstance().getReference("ProgressionCards");
        DatabaseReference playerDeckRef = referenceProfile.child(userID).child("deck");

        // Retrieve the progression cards
        progressionCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get the current size of the player's deck
                playerDeckRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot deckSnapshot) {
                        // Create a HashSet to avoid adding duplicate cards
                        HashSet<String> existingCardIds = new HashSet<>();
                        for (DataSnapshot cardSnapshot : deckSnapshot.getChildren()) {
                            Card card = cardSnapshot.getValue(Card.class);
                            if (card != null) {
                                existingCardIds.add(card.getId()); // Track existing card IDs
                            }
                        }

                        long nextIndex = deckSnapshot.getChildrenCount(); // Determine the next index in the deck

                        for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                            Card card = cardSnapshot.getValue(Card.class);

                            if (card != null && card.getUnlockLevel() <= playerLevel && !existingCardIds.contains(card.getId())) {
                                // Add the card at the next numeric index if not already in the deck
                                playerDeckRef.child(String.valueOf(nextIndex)).setValue(card);
                                nextIndex++; // Increment the index for the next card
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle the error appropriately
                        Log.e("DeckListener", "Error retrieving player deck: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error appropriately
                Log.e("AddCardsToDeck", "Error retrieving progression cards: " + error.getMessage());
            }
        });
    }



    private void animateProgressBar(ProgressBar progressBar, int targetValue) {
        if (targetValue < 0 || targetValue > progressBar.getMax()) {
            Log.e("ProgressBar", "Invalid target value: " + targetValue);
            return; // Prevent invalid progress updates
        }

        progressBar.clearAnimation(); // Clear any previous animations
        ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), targetValue);
        animator.setDuration(500); // Smooth animation duration
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> progressBar.setProgress((int) animation.getAnimatedValue()));
        animator.start();

        Log.d("AnimateProgressBar", "Animation started: Current=" + progressBar.getProgress() + ", Target=" + targetValue);
    }


    private void animateCoinChange(int oldValue, int newValue) {
        // Get reference to TextView displaying the coins
        TextView playerCoins = findViewById(R.id.playerCoins);

        // Animate the coin value change if there's a change
        if (oldValue != newValue) {
            ValueAnimator coinAnimator = ValueAnimator.ofInt(oldValue, newValue);
            coinAnimator.setDuration(1000); // Set the duration of the animation (1 second)
            coinAnimator.setInterpolator(new DecelerateInterpolator()); // Smooth deceleration

            // Update listener for updating the TextView with the animated coin value
            coinAnimator.addUpdateListener(animation -> {
                int animatedValue = (int) animation.getAnimatedValue();
                playerCoins.setText(String.valueOf(animatedValue)); // Set the animated value to the TextView
            });

            // Start the animation
            coinAnimator.start();
        }
    }

}
