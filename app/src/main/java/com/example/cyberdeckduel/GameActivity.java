package com.example.cyberdeckduel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GameActivity";
    private DatabaseReference matchRef;
    private ValueEventListener matchValueEventListener;

    private Map<String, ValueEventListener> handListeners = new HashMap<>();

    private ValueEventListener attackListener;
    private ValueEventListener defenseListener;
    private ValueEventListener turnStartTimeListener;



    private String matchId;
    private String playerId;


    private TextView player1Name;
    private TextView player2Name;
    private TextView player1Score;
    private TextView player2Score;
    private TextView player1CardsLeft;
    private TextView player2CardsLeft;

    private LinearLayout opponentCardContainer;

    private LinearLayout playerCardContainer;
    private TextView matchID;
    private MediaPlayer cardPlaceSfx;
    private MediaPlayer defenseSuccessSfx;

    private MediaPlayer mediaPlayer;
    private CountDownTimer localTimer;

    private ProgressBar timerBar;
    private TextView timerText;

    private int maxTime = 120000; // 20 seconds

    private boolean matchEnded = false;  // Flag to check if the match has ended
    private AlertDialog postDialog;
    private AlertDialog rewardsDialog;

    private int successfulDefenseAttempts = 0;
    private int failedDefenseAttempts = 0;

    private List<String> successfulDefenseDescriptions = new ArrayList<>();

    // A list of pairs of successful attack and defense cards
    private final List<Pair<Card, Card>> successfulDefensePairs = new ArrayList<>();


    private final Map<String, Map<String, String>> defenseAttackMap = new HashMap<String, Map<String, String>>() {{
        put("M1047", new HashMap<String, String>() {{
            put("T1548", "Check for common UAC bypass weaknesses on Windows systems to be aware of the risk posture and address issues where appropriate");
            put("T1543", "Use auditing tools capable of detecting privilege and service abuse opportunities on systems within an enterprise and correct them.");
            put("T1484", " Identify and correct GPO permissions abuse opportunities (ex: GPO modification privileges) using auditing tools such as BloodHound (version 1.5.1 and later).");
        }});

        put("M1051", new HashMap<String, String>() {{
            put("T1548", "Perform regular software updates to mitigate exploitation risk.");
            put("T1546", "Perform regular software updates to mitigate exploitation risk.");
        }});

        put("M1028", new HashMap<String, String>() {{
            put("T1548", "Applications with known vulnerabilities or known shell escapes should not have the setuid or setgid bits set to reduce potential damage if an application is compromised. Additionally, the number of programs with setuid or setgid bits set should be minimized across a system. Ensuring that the sudo tty_tickets setting is enabled will prevent this leakage across tty sessions.");
            put("T1098", "Protect domain controllers by ensuring proper security configuration for critical servers to limit access by potentially unnecessary protocols and services, such as SMB file sharing.");
            put("T1543", "Ensure that Driver Signature Enforcement is enabled to restrict unsigned drivers from being installed.");
        }});

        put("M1026", new HashMap<String, String>() {{
            put("T1548", "Remove users from the local administrator group on systems. By requiring a password, even if an adversary can get terminal access, they must know the password to run anything in the sudoers file. Setting the timestamp_timeout to 0 will require the user to input their password every time sudo is executed");
            put("T1134", "Limit permissions so that users and user groups cannot create tokens. This setting should be defined for the local system account only. GPO: Computer Configuration > [Policies] > Windows Settings > Security Settings > Local Policies > User Rights Assignment: Create a token object. Also define who can create a process level token to only the local and network service through GPO: Computer Configuration > [Policies] > Windows Settings > Security Settings > Local Policies > User Rights Assignment: Replace a process level token. Administrators should log in as a standard user but run their tools with administrator privileges using the built-in access token manipulation command runas.\n");
            put("T1098", "Do not allow domain administrator accounts to be used for day-to-day operations that may expose them to potential adversaries on unprivileged systems.");
            put("T1543", "Manage the creation, modification, use, and permissions associated to privileged accounts, including SYSTEM and root.");
            put("T1484", "Use least privilege and protect administrative access to the Domain Controller and Active Directory Federation Services (AD FS) server. Do not create service accounts with administrative privileges.");
            put("T1611", "Ensure containers are not running as root by default and do not use unnecessary privileges or mounted components. In Kubernetes environments, consider defining Pod Security Standards that prevent pods from running privileged containers.");
            put("T1546", "Manage the creation, modification, use, and permissions associated to privileged accounts, including SYSTEM and root.");
            put("T1055", "Utilize Yama (ex: /proc/sys/kernel/yama/ptrace_scope) to mitigate ptrace based process injection by restricting the use of ptrace to privileged users only. Other mitigation controls involve the deployment of security kernel modules that provide advanced access control and process restrictions such as SELinux, grsecurity, and AppArmor.");
        }});

        put("M1032", new HashMap<String, String>() {{
            put("T1098", " Use multi-factor authentication for user and privileged accounts.");
        }});

        put("M1022", new HashMap<String, String>() {{
            put("T1037", "Restrict write access to logon scripts to specific administrators.");
            put("T1543", "Restrict read/write access to system-level process files to only select privileged users who have a legitimate need to manage system services.\n");
        }});

        put("M1048", new HashMap<String, String>() {{
            put("T1611", "Consider utilizing seccomp, seccomp-bpf, or a similar solution that restricts certain system calls such as mount. In Kubernetes environments, consider defining Pod Security Standards that limit container access to host process namespaces, the host network, and the host file system.\n");
        }});

    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);

        // Initialize MediaPlayer for the card placement sound effect
        cardPlaceSfx = MediaPlayer.create(this, R.raw.placing_card_sfx);

        defenseSuccessSfx = MediaPlayer.create(this, R.raw.defense_success_sfx);


        opponentCardContainer = findViewById(R.id.opponent_card_container);

        playerCardContainer = findViewById(R.id.player_card_container);

        ConstraintLayout gameBackground = findViewById(R.id.gameBackground);

        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            final Bitmap backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.game_background, options);

            runOnUiThread(() -> gameBackground.setBackground(new BitmapDrawable(getResources(), backgroundBitmap)));
        }).start();

        player1Name = findViewById(R.id.player1Name);
        player2Name = findViewById(R.id.player2Name);
        player1Score = findViewById(R.id.player1Score);
        player2Score = findViewById(R.id.player2Score);
        player1CardsLeft = findViewById(R.id.player1CardsLeft);
        player2CardsLeft = findViewById(R.id.player2CardsLeft);

        matchId = getIntent().getStringExtra("matchId");
        matchRef = FirebaseDatabase.getInstance().getReference("Matches").child(matchId);

        matchID = findViewById(R.id.matchID);
        matchID.setText(matchId);

        // Initialize MediaPlayer and start playing background music
        mediaPlayer = MediaPlayer.create(this, R.raw.battte_music);
        mediaPlayer.setLooping(true); // Set looping
        mediaPlayer.start();

        timerBar = findViewById(R.id.timer_bar);
        timerText = findViewById(R.id.timer_text);

        timerBar.setMax(maxTime);

        setupGame();

        observeTurnTimer();

        // Check match end condition
        checkMatchEndCondition();
    }


    private void setupGame() {
        // Remove any existing listener before adding a new one
        if (matchValueEventListener != null) {
            matchRef.removeEventListener(matchValueEventListener);
        }

        // Define the new listener
        matchValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot playersSnapshot = snapshot.child("players");
                    for (DataSnapshot playerSnapshot : playersSnapshot.getChildren()) {
                        String playerId = playerSnapshot.getKey();
                        PlayerData playerData = playerSnapshot.getValue(PlayerData.class);

                        if (playerData != null) {
                            assert playerId != null;
                            if (playerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                // This is the current player
                                updatePlayerInfo(playerData, true);
                                setupHandListeners(playerId); // Add listener for current player's hand
                            } else {
                                // This is the opponent
                                updatePlayerInfo(playerData, false);
                                setupHandListeners(playerId); // Add listener for opponent's hand
                            }
                        }
                    }
                }

                // Start observing the turn timer
                observeTurnTimer();

                // Check match end condition
                checkMatchEndCondition();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading game data: ", error.toException());
            }
        };
        // Add the listener to the match reference
        matchRef.addValueEventListener(matchValueEventListener);

    }


    private void setupHandListeners(String playerId) {
        this.playerId = playerId; // Store playerId in the class-level variable

        // Remove any existing listener for this player's hand
        if (handListeners.containsKey(playerId)) {
            matchRef.child("players").child(playerId).child("hand")
                    .removeEventListener(Objects.requireNonNull(handListeners.get(playerId)));
        }
        // Define the new listener
        ValueEventListener handListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the current hand
                HashMap<String, Card> newHand = new HashMap<>();

                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    Card card = cardSnapshot.getValue(Card.class);
                    if (card != null) {
                        newHand.put(cardSnapshot.getKey(), card);  // Add each valid card to the list
                    }
                }

                // Update UI with the new hand for the opponent or current player
                if (playerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    // Update UI for current player (You can add specific methods for current player if needed)
                    updatePlayerHandUI(newHand);
                } else {
                    // Update UI for opponent
                    updateOpponentCards(newHand.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading hand data: ", error.toException());
            }
        };

        // Add the listener
        matchRef.child("players").child(playerId).child("hand")
                .addValueEventListener(handListener);

        // Store the listener reference for later removal
        handListeners.put(playerId, handListener);

        // Listen for attack row updates
        attackListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Card> attackCards = new ArrayList<>();
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    Card card = cardSnapshot.getValue(Card.class);
                    attackCards.add(card);
                }
                updateRowUI(attackCards, playerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()), "attack");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading attack row data: ", error.toException());
            }
        };

        // Add the listener for attack row
        DatabaseReference attackRef = matchRef.child("players").child(playerId).child("attack");
        attackRef.addValueEventListener(attackListener);

        // Listen for defense row updates
        defenseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Card> defenseCards = new ArrayList<>();
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    Card card = cardSnapshot.getValue(Card.class);
                    defenseCards.add(card);
                }
                updateRowUI(defenseCards, playerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()), "defense");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading defense row data: ", error.toException());
            }
        };

        // Add the listener for defense row
        DatabaseReference defenseRef = matchRef.child("players").child(playerId).child("defense");
        defenseRef.addValueEventListener(defenseListener);

    }

    private void updateRowUI(List<Card> cards, boolean isCurrentPlayer, String rowType) {
        if (isDestroyed() || isFinishing()) {
            Log.w(TAG, "Activity is destroyed, skipping image loading.");
            return;
        }

        LinearLayout row;
        if (rowType.equals("attack")) {
            row = isCurrentPlayer ? findViewById(R.id.attack_row) : findViewById(R.id.opponent_attack_row);
        } else {
            row = isCurrentPlayer ? findViewById(R.id.defense_row) : findViewById(R.id.opponent_defense_row);
        }

        row.removeAllViews(); // Clear the row before updating

        for (Card card : cards) {
            ImageView cardView = new ImageView(this);
            cardView.setLayoutParams(new LinearLayout.LayoutParams(160, 200)); // Set card size

            // Ensure the activity is not destroyed before loading the image
            if (!isDestroyed() && !isFinishing()) {
                // Load the card image using Glide
                Glide.with(getApplicationContext())
                        .load(getCardImageResource(card.getId()))  // Load the appropriate card image
                        .into(cardView);
            } else {
                Log.w(TAG, "Activity is destroyed, skipping image loading for card.");
            }

            // Handle card long press for details
            cardView.setOnLongClickListener(v -> {
                showCardDetails(card); // Show detailed card info
                return true; // Consume the long press event
            });
            
            row.addView(cardView);
        }
    }



    private void updateOpponentCards(int cardCount) {
        opponentCardContainer.removeAllViews(); // Clear previous views

        for (int i = 0; i < cardCount; i++) {
            ImageView cardBackView = new ImageView(this);
            cardBackView.setLayoutParams(new LinearLayout.LayoutParams(150, 190)); // Adjust size as needed
            cardBackView.setImageResource(R.drawable.back_of_card); // Use card back drawable
            cardBackView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Add the card back view to the opponent's card container
            opponentCardContainer.addView(cardBackView);
        }
    }

    private void updatePlayerHandUI(HashMap<String, Card> hand) {
        // Avoid updates if the activity is no longer valid
        if (isDestroyed() || isFinishing()) {
            Log.w(TAG, "Activity is destroyed or finishing, skipping updatePlayerHandUI.");
            return;
        }
        playerCardContainer.removeAllViews(); // Clear the container before adding new cards

        for (Card card : hand.values()) {
            if (card != null && card.getId() != null) {  // Add null check here
                ImageView cardView = new ImageView(this);
                cardView.setLayoutParams(new LinearLayout.LayoutParams(160, 200)); // Set card size

                // Load the card image using Glide
                Glide.with(this)
                        .load(getCardImageResource(card.getId()))  // Load the appropriate card image
                        .into(cardView);  // Load into the ImageView

                cardView.setOnClickListener(v -> {
                    // Handle card click (optional)
                    onCardClicked(card);
                });

                // Handle card long press for details
                cardView.setOnLongClickListener(v -> {
                    showCardDetails(card); // Show detailed card info
                    return true; // Consume the long press event
                });

                // Add the card view to the player card container
                playerCardContainer.addView(cardView);
            } else {
                Log.e(TAG, "Null card found in player's hand.");  // Log null card issues
            }
        }
    }

    private void showCardDetails(Card card) {
        // Create a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate a custom layout for detailed view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_card_details, null);
        builder.setView(dialogView);

        // Populate the card details
        ImageView cardImage = dialogView.findViewById(R.id.cardImage);
        TextView cardName = dialogView.findViewById(R.id.cardName);
        TextView cardDescription = dialogView.findViewById(R.id.cardDescription);
        TextView cardType = dialogView.findViewById(R.id.cardType);
        TextView cardPower = dialogView.findViewById(R.id.cardPower);

        // Load the card image
        Glide.with(this)
                .load(getCardImageResource(card.getId()))
                .into(cardImage);

        cardName.setText(card.getName());
        cardDescription.setText(card.getDescription());
        cardType.setText("Type: " + card.getType());
        cardPower.setText("Power: " + card.getPower());

        // Show the dialog
        builder.create().show();
    }

    int getCardImageResource(String cardId) {
        // Create a list of CardImage objects
        List<CardImage> cardImageList = new ArrayList<>();
        cardImageList.add(new CardImage("T1037", R.drawable.t1037));
        cardImageList.add(new CardImage("T1055", R.drawable.t1055));
        cardImageList.add(new CardImage("T1098", R.drawable.t1098));
        cardImageList.add(new CardImage("T1134", R.drawable.t1134));
        cardImageList.add(new CardImage("T1484", R.drawable.t1484));
        cardImageList.add(new CardImage("T1543", R.drawable.t1543));
        cardImageList.add(new CardImage("T1546", R.drawable.t1546));
        cardImageList.add(new CardImage("T1547", R.drawable.t1547));
        cardImageList.add(new CardImage("T1548", R.drawable.t1548));
        cardImageList.add(new CardImage("T1611", R.drawable.t1611));
        cardImageList.add(new CardImage("T1574", R.drawable.t1574));
        cardImageList.add(new CardImage("T1053", R.drawable.t1053));
        cardImageList.add(new CardImage("T1078", R.drawable.t1078));
        cardImageList.add(new CardImage("M1022", R.drawable.m1022));
        cardImageList.add(new CardImage("M1026", R.drawable.m1026));
        cardImageList.add(new CardImage("M1028", R.drawable.m1028));
        cardImageList.add(new CardImage("M1032", R.drawable.m1032));
        cardImageList.add(new CardImage("M1047", R.drawable.m1047));
        cardImageList.add(new CardImage("M1048", R.drawable.m1048));
        cardImageList.add(new CardImage("M1051", R.drawable.m1051));
        // Add other mappings here...

        // Find the matching CardImage
        for (CardImage cardImage : cardImageList) {
            if (cardImage.getCardId().equals(cardId)) {
                return cardImage.getResourceId();
            }
        }

        // Return the default image if not found
        return R.drawable.back_of_card;
    }


    private void onCardClicked(Card card) {
        // Check if it’s the player’s turn before proceeding
        String playerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        matchRef.child("players").child(playerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PlayerData playerData = snapshot.getValue(PlayerData.class);
                if (playerData != null && playerData.isTurn) {
                    // Only allow card placement if it's the player's turn
                    if (card.getType().equalsIgnoreCase("attack")) {
                        placeCardInRow(card, "attack");
                    } else if (card.getType().equalsIgnoreCase("defense")) {
                        placeCardInRow(card, "defense");
                    }

                    // Remove the card from the player's hand
                    removeCardFromHand(card);

                    switchTurn();
                    // Update timer in Firebase after card placement
                    matchRef.child("turnTimer").setValue(120000);
                } else {
                    Log.d(TAG, "It's not the player's turn.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check turn: ", error.toException());
            }
        });
    }


    private void switchTurn() {
        // Reference both players' data
        DatabaseReference playersRef = matchRef.child("players");
        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentPlayerId = null; // Track the current player ID
                String nextPlayerId = null; // Track the next player ID

                // Find the current player (whose turn it is) and the next player
                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    String playerId = playerSnapshot.getKey();
                    PlayerData playerData = playerSnapshot.getValue(PlayerData.class);

                    if (playerData != null) {
                        if (playerData.isTurn) {
                            currentPlayerId = playerId;  // Player whose turn it is
                        } else {
                            nextPlayerId = playerId;  // Player whose turn is next
                        }
                    }
                }

                // Ensure that only the next player gets the turn, and the current player's turn is set to false
                if (currentPlayerId != null && nextPlayerId != null) {
                    playersRef.child(currentPlayerId).child("isTurn").setValue(false);  // End current player's turn
                    playersRef.child(nextPlayerId).child("isTurn").setValue(true);     // Set next player's turn

                    // Reset the turn start time only for the next player
                    matchRef.child("turnStartTime").setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error switching turn: ", error.toException());
            }
        });
    }









    private void observeTurnTimer() {
        DatabaseReference turnStartTimeRef = matchRef.child("turnStartTime");

        // Define the listener and store it in the class-level variable
        turnStartTimeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long turnStartTime = snapshot.getValue(Long.class);
                    if (turnStartTime != null) {
                        calculateRemainingTime(turnStartTime);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error observing turn start time: ", error.toException());
            }
        };

        // Add the listener
        turnStartTimeRef.addValueEventListener(turnStartTimeListener);
    }



    private void calculateRemainingTime(long turnStartTime) {
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long offset = snapshot.getValue(Long.class);
                        if (offset != null) {
                            // Get the current server time
                            long currentTime = System.currentTimeMillis() + offset;

                            // Calculate remaining time
                            long elapsedTime = currentTime - turnStartTime;
                            long remainingTime = maxTime - elapsedTime;

                            if (remainingTime > 0) {
                                startLocalCountdown(remainingTime); // Start countdown locally
                            } else {
                                // Timer expired; automatically switch turn
                                switchTurn();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching server time offset: ", error.toException());
                    }
                });
    }



    private void startLocalCountdown(long remainingTime) {
        if (localTimer != null) {
            localTimer.cancel(); // Stop any existing timer
        }

        localTimer = new CountDownTimer(remainingTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText((millisUntilFinished / 1000) + "s");
                timerBar.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // Timer expired; switch turn
                switchTurn();
            }
        }.start();
    }












    private void removeCardFromHand(Card card) {
        String playerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference handRef = matchRef.child("players").child(playerId).child("hand");

        handRef.orderByChild("id").equalTo(card.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                        cardSnapshot.getRef().removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Card removed successfully.");

                                // Update total cards left in real-time
                                updateCardsLeft(playerId);
                            } else {
                                Log.e(TAG, "Failed to remove card: ", task.getException());
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Card not found in hand.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error removing card: ", error.toException());
            }
        });
    }

    // Method to update the total cards left in real-time
    private void updateCardsLeft(String playerId) {
        DatabaseReference handRef = matchRef.child("players").child(playerId).child("hand");

        handRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Count the number of cards left in the hand
                int cardsLeft = (int) snapshot.getChildrenCount();

                // Update the 'totalCardsLeft' in Firebase in real-time
                matchRef.child("players").child(playerId).child("totalCardsLeft").setValue(cardsLeft);

                // Optionally, update the UI here too
                player1CardsLeft.setText(String.format(Locale.getDefault(), "%d", cardsLeft));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error tracking cards left: ", error.toException());
            }
        });
    }


    private void placeCardInRow(Card card, String rowType) {
        // Find the appropriate row (attack or defense)
        LinearLayout row;
        if (rowType.equals("attack")) {
            row = findViewById(R.id.attack_row); // Player's attack row
        } else {
            row = findViewById(R.id.defense_row); // Player's defense row
        }

        // Create an ImageView for the card
        ImageView cardView = new ImageView(this);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(160, 200)); // Set card size

        // Load the card image using Glide
        Glide.with(this)
                .load(getCardImageResource(card.getId()))  // Load the appropriate card image
                .into(cardView);  // Load into the ImageView

        // Add the card to the specified row
        row.addView(cardView);

        // Remove the card from the player's hand
        playerCardContainer.removeView(cardView);

        // Play card placement sound effect
        if (cardPlaceSfx != null) {
            cardPlaceSfx.start();
        }

        // Update player score based on card type
        String playerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference scoreRef = matchRef.child("players").child(playerId).child("score");

        scoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentScore = snapshot.getValue(Integer.class);
                int newScore = (currentScore != null) ? currentScore : 0;

                if (rowType.equals("attack")) {
                    // Add power points for attack card
                    newScore += card.getPower();
                } else if (rowType.equals("defense")) {
                    // Retrieve opponent ID using callback
                    getOpponentId(playerId, opponentId -> {
                        if (opponentId == null) {
                            Log.e(TAG, "Opponent ID is null. Unable to update opponent's score.");
                            failedDefenseAttempts++; // Increment failed attempts when no opponent is found
                            return; // Exit if no opponent ID found
                        }

                        // Check for corresponding attack cards in opponent's attack row
                        DatabaseReference opponentAttackRowRef = matchRef.child("players").child(opponentId).child("attack");

                        opponentAttackRowRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot attackSnapshot) {
                                boolean success = false;
                                for (DataSnapshot attackCardSnapshot : attackSnapshot.getChildren()) {
                                    Card attackCard = attackCardSnapshot.getValue(Card.class);
                                    if (isCorrespondingAttackCard(card, attackCard)) {
                                        success = true;

                                        // Retrieve the defense description
                                        String defenseDescription = defenseAttackMap.get(card.getId()).get(attackCard.getId());
                                        Log.d(TAG, "Defense Success Description: " + defenseDescription);

                                        successfulDefensePairs.add(new Pair<>(attackCard, card)); // Add the pair

                                        // Optionally, store this description in a list for post-match analysis
                                        successfulDefenseDescriptions.add(defenseDescription);


                                        // Subtract opponent's score based on defense power points
                                        DatabaseReference opponentScoreRef = matchRef.child("players").child(opponentId).child("score");
                                        opponentScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot opponentScoreSnapshot) {
                                                Integer opponentCurrentScore = opponentScoreSnapshot.getValue(Integer.class);
                                                int newOpponentScore = (opponentCurrentScore != null) ? opponentCurrentScore : 0;
                                                newOpponentScore -= card.getPower(); // Subtract defense card power from opponent's score
                                                opponentScoreRef.setValue(newOpponentScore);

                                                // Play defense success sound effect
                                                if (defenseSuccessSfx != null) {
                                                    defenseSuccessSfx.start();
                                                }

                                                // Trigger animation on opponent's score text
                                                TextView opponentScoreTextView = findViewById(R.id.player2Score);
                                                animateOpponentScore(opponentScoreTextView, newOpponentScore);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e(TAG, "Error updating opponent's score: ", error.toException());
                                            }
                                        });
                                        break; // No need to continue checking other attack cards
                                    }
                                }

                                if (success) {
                                    successfulDefenseAttempts++; // Increment successful attempts
                                } else {
                                    failedDefenseAttempts++; // Increment failed attempts
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error checking opponent's attack cards: ", error.toException());
                            }
                        });
                    });
                }

                // Update score in Firebase for the current player
                updatePlayerScore(newScore, playerId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error updating score: ", error.toException());
            }
        });
        // Update Firebase to reflect the card placement
        updateCardPlacementInFirebase(card, rowType);
    }

    private void updatePlayerScore(int newScore, String playerId) {
        DatabaseReference scoreRef = matchRef.child("players").child(playerId).child("score");

        // Animate score change
        animatePlayerScore(player1Score, newScore);

        // Set the updated score in Firebase
        scoreRef.setValue(newScore);
    }

    private void animatePlayerScore(TextView scoreTextView, int newScore) {
        int currentScore = Integer.parseInt(scoreTextView.getText().toString());
        ValueAnimator animator = ValueAnimator.ofInt(currentScore, newScore);
        animator.setDuration(1000);  // Duration of the animation (adjust as needed)
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scoreTextView.setText(String.valueOf(animation.getAnimatedValue()));
            }
        });
        animator.start();
    }

    private void animateOpponentScore(TextView scoreTextView, int newScore) {
        int currentScore = Integer.parseInt(scoreTextView.getText().toString());
        ValueAnimator animator = ValueAnimator.ofInt(currentScore, newScore);
        animator.setDuration(1000);  // Duration of the animation (adjust as needed)
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scoreTextView.setText(String.valueOf(animation.getAnimatedValue()));
            }
        });
        animator.start();
    }


    private boolean isCorrespondingAttackCard(Card defenseCard, Card attackCard) {
        // Get the map of attack IDs for the given defense card ID
        Map<String, String> correspondingAttacks = defenseAttackMap.get(defenseCard.getId());

        // Check if the attackCard ID exists in the map of corresponding attacks
        return correspondingAttacks != null && correspondingAttacks.containsKey(attackCard.getId());
    }



    // Callback interface to handle asynchronous retrieval of opponent ID
    // Callback interface to handle asynchronous retrieval of opponent ID
    private interface OpponentIdCallback {
        void onOpponentIdReceived(String opponentId);
    }

    // Method to get opponent's ID with a callback
    private void getOpponentId(String playerId, OpponentIdCallback callback) {
        DatabaseReference playersRef = matchRef.child("players");

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String opponentId = null;

                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    String id = playerSnapshot.getKey(); // Get the player ID
                    Log.d(TAG, "Found player ID: " + id);

                    // Find the ID that does NOT match the current player ID
                    if (id != null && !id.equals(playerId)) {
                        opponentId = id; // This is the opponent ID
                        break;
                    }
                }

                if (opponentId != null) {
                    Log.d(TAG, "Opponent ID found: " + opponentId);
                    callback.onOpponentIdReceived(opponentId);
                } else {
                    Log.e(TAG, "Opponent ID not found for player: " + playerId);
                    callback.onOpponentIdReceived(null); // No opponent found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error retrieving players: ", error.toException());
                callback.onOpponentIdReceived(null); // Handle cancellation or error
            }
        });
    }


    private void updateCardPlacementInFirebase(Card card, String rowType) {
        String playerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cardPlacementRef = matchRef.child("players").child(playerId).child(rowType);

        // Push the card to the correct row in the database (attack or defense)
        cardPlacementRef.push().setValue(card);
    }


    private void updatePlayerInfo(PlayerData playerData, boolean isCurrentPlayer) {
        if (isCurrentPlayer) {
            player1Name.setText(playerData.name != null ? playerData.name : "You");
            player1Score.setText(String.format(Locale.getDefault(), "%d", playerData.score));
            player1CardsLeft.setText(String.format(Locale.getDefault(), "%d", playerData.totalCardsLeft));

            // Update UI for the current player's hand (optional)
            updatePlayerHandUI(playerData.hand);

            // Handle turn indication (optional UI feedback)
            if (playerData.isTurn) {
                // Highlight that it's the current player's turn
                player1Name.setTextColor(getColor(R.color.green));
            } else {
                player1Name.setTextColor(getColor(R.color.white));
            }
        } else {
            player2Name.setText(playerData.name != null ? playerData.name : "Opponent");
            player2Score.setText(String.format(Locale.getDefault(), "%d", playerData.score));
            player2CardsLeft.setText(String.format(Locale.getDefault(), "%d", playerData.totalCardsLeft));


            // Update UI for the opponent's cards (card backs only)
            updateOpponentCards(playerData.hand.size());

            // Handle turn indication for opponent
            if (playerData.isTurn) {
                player2Name.setTextColor(getColor(R.color.green));
            } else {
                player2Name.setTextColor(getColor(R.color.white));
            }
        }
    }














    private void checkMatchEndCondition() {
        // If match has already ended, return early
        if (matchEnded) {
            return;
        }

        String playerId1 = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Use getOpponentId to retrieve the opponent's ID
        getOpponentId(playerId1, playerId2 -> {
            if (playerId2 != null) {
                // Both player IDs are available, proceed with checking the cards left
                DatabaseReference player1CardsLeftRef = matchRef.child("players").child(playerId1).child("totalCardsLeft");
                DatabaseReference player2CardsLeftRef = matchRef.child("players").child(playerId2).child("totalCardsLeft");

                player1CardsLeftRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Check if the value exists and is not null, if null, set it to 0
                        Integer player1CardsLeft = snapshot.getValue(Integer.class);
                        if (player1CardsLeft == null) {
                            player1CardsLeft = 10; // Default to 0 if null
                        }
                        Log.d(TAG, "Player 1 cards left:" + player1CardsLeft);

                        Integer finalPlayer1CardsLeft = player1CardsLeft;
                        player2CardsLeftRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                // Check if the value exists and is not null, if null, set it to 0
                                Integer player2CardsLeft = snapshot.getValue(Integer.class);
                                if (player2CardsLeft == null) {
                                    player2CardsLeft = 10; // Default to 0 if null
                                }
                                Log.d(TAG, "Player 2 cards left:" + player2CardsLeft);

                                // Check if both players have no cards left
                                if (finalPlayer1CardsLeft == 0 && player2CardsLeft == 0) {
                                    endMatch(playerId1, playerId2);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error checking player 2's cards left: ", error.toException());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking player 1's cards left: ", error.toException());
                    }
                });
            } else {
                Log.e(TAG, "Opponent ID not found.");
            }
        });
    }

    private void endMatch(String playerId1, String playerId2) {
        // Check if the match has already ended
        if (matchEnded) {
            return; // Prevent multiple executions
        }

        matchEnded = true;

        Log.d(TAG, "End Match executed");

        if (localTimer != null) {
            localTimer.cancel(); // Stop the timer
        }

        // Check if coins have already been updated
        matchRef.child("coinsUpdated").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean coinsUpdated = snapshot.getValue(Boolean.class);

                // Retrieve both players' scores
                DatabaseReference player1ScoreRef = matchRef.child("players").child(playerId1).child("score");
                DatabaseReference player2ScoreRef = matchRef.child("players").child(playerId2).child("score");
                DatabaseReference player1NameRef = matchRef.child("players").child(playerId1).child("name");
                DatabaseReference player2NameRef = matchRef.child("players").child(playerId2).child("name");

                player1ScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Integer player1Score = snapshot.getValue(Integer.class);
                        if (player1Score == null) {
                            player1Score = 0; // Default to 0 if null
                        }
                        Integer finalPlayer1Score = player1Score;

                        Integer finalPlayer1Score1 = player1Score;
                        player2ScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Integer player2Score = snapshot.getValue(Integer.class);
                                if (player2Score == null) {
                                    player2Score = 0; // Default to 0 if null
                                }

                                Integer finalPlayer2Score = player2Score;
                                Integer finalPlayer2Score1 = player2Score;
                                Integer finalPlayer2Score2 = player2Score;
                                player1NameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String player1Name = snapshot.getValue(String.class);
                                        if (player1Name == null) {
                                            player1Name = "Player 1";
                                        }

                                        String finalPlayer1Name = player1Name;
                                        String finalPlayer1Name1 = player1Name;
                                        String finalPlayer1Name2 = player1Name;
                                        player2NameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                String player2Name = snapshot.getValue(String.class);
                                                if (player2Name == null) {
                                                    player2Name = "Player 2";
                                                }

                                                Log.d(TAG, "Player 1 Score: " + finalPlayer1Score + ", Player 2 Score: " + finalPlayer2Score);

                                                // Determine winner and coins (common for both players)
                                                String winnerId;
                                                int player1Coins, player2Coins, xpPlayer1, xpPlayer2;

                                                if (finalPlayer1Score > finalPlayer2Score) {
                                                    winnerId = playerId1;
                                                    xpPlayer1 = 200; // Winner gets more XP
                                                    xpPlayer2 = 100; // Loser gets less XP
                                                    player1Coins = 250;
                                                    player2Coins = 50;

                                                    // Increment wins for Player 1
                                                    updatePlayerWins(playerId1);
                                                } else if (finalPlayer2Score > finalPlayer1Score) {
                                                    winnerId = playerId2;
                                                    xpPlayer1 = 100;
                                                    xpPlayer2 = 200;
                                                    player2Coins = 250;
                                                    player1Coins = 50;

                                                    // Increment wins for Player 2
                                                    updatePlayerWins(playerId2);
                                                } else {
                                                    winnerId = "tie";
                                                    xpPlayer1 = 150;
                                                    xpPlayer2 = 150;
                                                    player1Coins = 0;
                                                    player2Coins = 0;
                                                }

                                                // Update coins only once
                                                if (coinsUpdated == null || !coinsUpdated) {
                                                    matchRef.child("winner").setValue(winnerId);
                                                    matchRef.child("matchStatus").setValue("ended");
                                                    matchRef.child("coinsUpdated").setValue(true);

                                                    // Call the method to update both players' coins
                                                    Map<String, Integer> playerCoins = new HashMap<>();
                                                    playerCoins.put(playerId1, player1Coins);
                                                    playerCoins.put(playerId2, player2Coins);

                                                    updatePlayerCoins(playerCoins); // Update both players' coins in one method
                                                    updatePlayerXP(playerId1, xpPlayer1);
                                                    updatePlayerXP(playerId2, xpPlayer2);
                                                }

                                                // Show  -match dialogs
                                                String matchResult = determineMatchResult(playerId1, winnerId); // Win, lose, or tie
                                                showPostMatchDialogs(matchResult, player1Coins, player2Coins, finalPlayer1Name2, player2Name, finalPlayer1Score1, finalPlayer2Score2, successfulDefenseAttempts, failedDefenseAttempts);


                                                // Remove the currentMatch field from both players after the match ends
                                                removeCurrentMatchField(playerId1);
                                                removeCurrentMatchField(playerId2);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e(TAG, "Error retrieving player 2's name: ", error.toException());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error retrieving player 1's name: ", error.toException());
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error checking player 2's score: ", error.toException());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking player 1's score: ", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking coinsUpdated flag: ", error.toException());
            }
        });
    }



    private void updatePlayerWins(String playerId) {
        DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Players").child(playerId);
        playerRef.child("wins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentWins = snapshot.getValue(Integer.class);
                if (currentWins == null) {
                    currentWins = 0; // Default to 0 if null
                }

                // Increment the win count
                currentWins++;

                // Update the wins field in the database
                playerRef.child("wins").setValue(currentWins)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Successfully updated wins for player " + playerId);
                            } else {
                                Log.e(TAG, "Failed to update wins for player " + playerId, task.getException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error reading player's wins: ", error.toException());
            }
        });
    }



    private void removeCurrentMatchField(String playerId) {
        DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Players").child(playerId);
        playerRef.child("currentMatch").removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Removed currentMatch for player " + playerId);
                    } else {
                        Log.e(TAG, "Failed to remove currentMatch for player " + playerId, task.getException());
                    }
                });
    }



    // Helper function to determine match result (win/lose/tie) for the current player
    private String determineMatchResult(String currentPlayerId, String winnerId) {
        if (winnerId.equals("tie")) {
            return "tie";
        } else if (winnerId.equals(currentPlayerId)) {
            return "win";
        } else {
            return "lose";
        }
    }

    private void showPostMatchDialogs(String matchResult, int coinsEarned, int opponentCoins, String player1Name, String player2Name, int player1Score, int player2Score, int successfulDefense, int failedDefense) {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_result_match, null);

        // Create the dialog
        Dialog postDialog = new Dialog(this, R.style.FullscreenDialog);

        // Set the custom layout as the content of the dialog
        postDialog.setContentView(dialogView);

        // Ensure fullscreen by adjusting window attributes
        if (postDialog.getWindow() != null) {
            postDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            postDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Set transparent background
            postDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }



        TextView tvPlayer1Name = dialogView.findViewById(R.id.tv_player1_name);
        TextView tvPlayer2Name = dialogView.findViewById(R.id.tv_player2_name);
        TextView tvPlayer1Score = dialogView.findViewById(R.id.tv_player1_score);
        TextView tvPlayer2Score = dialogView.findViewById(R.id.tv_player2_score);
        TextView tvMatchResult = dialogView.findViewById(R.id.tv_match_result);
        TextView tvCoinsEarned = dialogView.findViewById(R.id.tv_coins_earned);
        TextView tvOpponentCoins = dialogView.findViewById(R.id.tv_opponent_coins);
        TextView tvSuccessfulDefense = dialogView.findViewById(R.id.tv_successful_defense);
        TextView tvFailedDefense = dialogView.findViewById(R.id.tv_failed_defense);
        CustomImageView btnExitMatch = dialogView.findViewById(R.id.btn_exit_match);

        // Set dynamic data
        tvPlayer1Name.setText(player1Name);
        tvPlayer2Name.setText(player2Name);
        tvPlayer1Score.setText(String.valueOf(player1Score)); // Correct
        tvPlayer2Score.setText(String.valueOf(player2Score)); // Correct
        tvMatchResult.setText(getMatchResultMessage(matchResult));
        tvCoinsEarned.setText("You: " + coinsEarned + " coins");
        tvOpponentCoins.setText("Opponent: " + opponentCoins + " coins");
        tvSuccessfulDefense.setText(String.format(Locale.getDefault(), "%d", successfulDefense));
        tvFailedDefense.setText(String.format(Locale.getDefault(), "%d", failedDefense));
        CustomImageView btnShowAnalysis = dialogView.findViewById(R.id.btn_show_analysis); // Button to show analysis


        // Show analysis button functionality
        btnShowAnalysis.setOnClickListener(v -> showAnalysisDialog());


        // Set up the button click listener
        btnExitMatch.setOnClickListener(v -> {
            // Navigate to HomeActivity
            Intent intent = new Intent(GameActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish GameActivity
            postDialog.dismiss(); // Dismiss the dialog
            deleteMatch(); // Delete the match from Firebase
        });

        // Show the dialog
        postDialog.setCancelable(false); // Prevent dismissing the dialog by tapping outside
        postDialog.show();
    }


    private void showAnalysisDialog() {
        // Inflate the custom layout for the analysis dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View analysisDialogView = inflater.inflate(R.layout.dialog_analysis, null);

        // Create the dialog
        Dialog analysisDialog = new Dialog(this, R.style.FullscreenDialog);

        // Set the custom layout as the content of the dialog
        analysisDialog.setContentView(analysisDialogView);

        // Set up views
        ScrollView scrollView = analysisDialogView.findViewById(R.id.analysis_scroll_view);
        LinearLayout descriptionContainer = analysisDialogView.findViewById(R.id.analysis_container);
        CustomImageView btnCloseAnalysis = analysisDialogView.findViewById(R.id.btn_close_analysis); // Close button for analysis dialog

        // Add rows for each successful defense
        for (int i = 0; i < successfulDefenseDescriptions.size(); i++) {
            // Inflate a row layout dynamically
            View rowView = LayoutInflater.from(this).inflate(R.layout.analysis_row, descriptionContainer, false);

            // Get the TextViews and ImageViews from the row layout
            TextView tvAttackCard = rowView.findViewById(R.id.tv_attack_card);
            TextView tvDefenseCard = rowView.findViewById(R.id.tv_defense_card);
            TextView tvDescription = rowView.findViewById(R.id.tv_description);
            ImageView ivAttackCard = rowView.findViewById(R.id.iv_attack_card);
            ImageView ivDefenseCard = rowView.findViewById(R.id.iv_defense_card);

            // Set data for this row
            tvAttackCard.setText("Attack: " + successfulDefensePairs.get(i).first.getName());
            tvDefenseCard.setText("Defense: " + successfulDefensePairs.get(i).second.getName());
            tvDescription.setText(successfulDefenseDescriptions.get(i));

            // Load images using Glide
            Glide.with(this)
                    .load(getCardImageResource(successfulDefensePairs.get(i).first.getId())) // Attack card image
                    .into(ivAttackCard);
            Glide.with(this)
                    .load(getCardImageResource(successfulDefensePairs.get(i).second.getId())) // Defense card image
                    .into(ivDefenseCard);

            // Add the row to the container
            descriptionContainer.addView(rowView);
        }

        // Close button functionality
        btnCloseAnalysis.setOnClickListener(v -> analysisDialog.dismiss());

        // Show the dialog
        analysisDialog.setCancelable(true);
        analysisDialog.show();
    }




    private String getMatchResultMessage(String matchResult) {
        switch (matchResult) {
            case "win":
                return "You win!";
            case "lose":
                return "You lose!";
            case "tie":
                return "Tie!";
            default:
                return "Match ended.";
        }
    }



    private void deleteMatch() {
        if (matchRef != null) {
            matchRef.removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Match deleted successfully.");
                        } else {
                            Log.e(TAG, "Failed to delete match: ", task.getException());
                        }
                    });
        }
    }

    private void updatePlayerXP(String playerId, int xpEarned) {
        DatabaseReference playerRef = FirebaseDatabase.getInstance().getReference("Players").child(playerId);

        playerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentXP = snapshot.child("xp").getValue(Integer.class);
                Integer currentLevel = snapshot.child("level").getValue(Integer.class);

                if (currentXP == null) currentXP = 0;
                if (currentLevel == null) currentLevel = 1;

                int newXP = currentXP + xpEarned;
                int xpThreshold = currentLevel * 100;

                while (newXP >= xpThreshold) {
                    newXP -= xpThreshold;
                    currentLevel++;
                    xpThreshold = currentLevel * 100;
                }

                // Prepare the update
                Map<String, Object> updates = new HashMap<>();
                updates.put("xp", newXP);
                updates.put("level", currentLevel);

                // Apply the updates
                playerRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Player XP and level updated successfully for " + playerId);
                    } else {
                        Log.e(TAG, "Failed to update XP and level for " + playerId, task.getException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching player data", error.toException());
            }
        });
    }





    private void updatePlayerCoins(Map<String, Integer> playerCoins) {
        for (Map.Entry<String, Integer> entry : playerCoins.entrySet()) {
            String playerId = entry.getKey();
            int coins = entry.getValue();

            DatabaseReference playerRef = FirebaseDatabase.getInstance()
                    .getReference("Players")
                    .child(playerId)
                    .child("coins");

            playerRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    Integer currentCoins = mutableData.getValue(Integer.class);

                    if (currentCoins == null) {
                        // If the player's coin value doesn't exist, initialize it to 0
                        currentCoins = 0;
                    }

                    Log.d(TAG, "Player ID:" + playerId );
                    Log.d(TAG, "Current Coins:" + currentCoins);
                    Log.d(TAG, "Coins:" + coins);

                    int newCoins = currentCoins + coins;

                    // Abort if the new coin count is negative
                    if (newCoins < 0) {
                        return Transaction.abort(); // Abort if the new coin count would be negative
                    }

                    mutableData.setValue(newCoins);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (error != null) {
                        Log.e(TAG, "Error updating player coins for " + playerId + ": ", error.toException());
                    } else if (committed) {
                        Log.d(TAG, "Player coins updated successfully for " + playerId);
                    }
                }
            });
        }
    }
















    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove the turn start time listener
        if (turnStartTimeListener != null) {
            DatabaseReference turnStartTimeRef = matchRef.child("turnStartTime");
            turnStartTimeRef.removeEventListener(turnStartTimeListener);
        }

        // Remove all hand listeners
        for (Map.Entry<String, ValueEventListener> entry : handListeners.entrySet()) {
            matchRef.child("players").child(entry.getKey()).child("hand")
                    .removeEventListener(entry.getValue());
        }

        handListeners.clear();

        // Remove attack row listener
        if (attackListener != null && playerId != null) {
            matchRef.child("players").child(playerId).child("attack")
                    .removeEventListener(attackListener);
        }

        // Remove defense row listener
        if (defenseListener != null && playerId != null) {
            matchRef.child("players").child(playerId).child("defense")
                    .removeEventListener(defenseListener);
        }

        // Dismiss any dialogs to prevent memory leaks
        if (postDialog != null && postDialog.isShowing()) {
            postDialog.dismiss();
        }

        // Remove match listener
        if (matchRef != null && matchValueEventListener != null) {
            matchRef.removeEventListener(matchValueEventListener);
        }

        // Dismiss any dialogs to prevent memory leaks
        if (rewardsDialog != null && rewardsDialog.isShowing()) {
            rewardsDialog.dismiss();
        }
        if (cardPlaceSfx != null) {
            cardPlaceSfx.release();
            cardPlaceSfx = null;
        }
        if (defenseSuccessSfx != null) {
            defenseSuccessSfx.release();
            defenseSuccessSfx = null;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }
}
