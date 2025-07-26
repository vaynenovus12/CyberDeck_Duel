package com.example.cyberdeckduel;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView playerName, playerDob, playerGender, playerCoins, playerLevel, playerXP, playerWins;
    private ImageView playerProfilePic;
    private GridLayout deckLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Set up the back button
        CustomImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity and return to the previous screen
            }
        });

        // Initialize views
        playerName = findViewById(R.id.playerName);
        playerDob = findViewById(R.id.playerDob);
        playerGender = findViewById(R.id.playerGender);
        playerCoins = findViewById(R.id.playerCoins);
        playerLevel = findViewById(R.id.playerLevel);
        playerXP = findViewById(R.id.playerXP);
        playerWins = findViewById(R.id.playerWins);
        playerProfilePic = findViewById(R.id.playerProfilePic);
        deckLayout = findViewById(R.id.deckLayout);  // Correct reference for deckLayout

        // Fetch current user's ID from Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            showUserDetails(firebaseUser);
        }
    }

    private void showUserDetails(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();

        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Players");
        referenceProfile.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    // Extract user details from Firebase
                    String name = firebaseUser.getDisplayName();
                    String dob = readUserDetails.dob;
                    String gender = readUserDetails.gender;
                    int coins = readUserDetails.coins;
                    int level = readUserDetails.level;
                    int xp = readUserDetails.xp;
                    int wins = readUserDetails.wins;

                    // Set TextView data
                    playerName.setText(name);
                    playerDob.setText("DOB: " + dob);
                    playerGender.setText("Gender: " + gender);
                    playerCoins.setText("Coins: " + coins);
                    playerLevel.setText("Level: " + level);
                    playerXP.setText("XP: " + xp + "/" + (level * 100));
                    playerWins.setText("Wins: " + wins);

                    // Dynamically display the deck images
                    deckLayout.removeAllViews();  // Remove any previous views before adding new ones
                    for (Card card : readUserDetails.deck) {
                        // Get the image resource for the card
                        int cardImageResId = getCardImageResource(card.getId());

                        // Create a new ImageView for each card
                        ImageView cardImageView = new ImageView(ProfileActivity.this);

                        // Set fixed width and height for the card images (e.g., 60dp x 80dp)
                        int cardWidth = (int) getResources().getDisplayMetrics().density * 60; // Convert dp to px
                        int cardHeight = (int) getResources().getDisplayMetrics().density * 80; // Convert dp to px

                        // Set LayoutParams with fixed size
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
                        params.setMargins(8, 8, 8, 8); // Optional margin for each card

                        cardImageView.setLayoutParams(params);

                        // Load the image into the ImageView using Glide
                        if (!isDestroyed()) {  // Check if the activity is not destroyed
                            Glide.with(ProfileActivity.this)
                                    .load(cardImageResId)  // Load the appropriate card image
                                    .into(cardImageView);  // Set the loaded image into the ImageView
                        }

                        // Add the ImageView to the LinearLayout
                        deckLayout.addView(cardImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
            }
        });
    }

    // Method to get the image resource for a given card ID
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
        // Add more cards here...

        // Find the matching CardImage
        for (CardImage cardImage : cardImageList) {
            if (cardImage.getCardId().equals(cardId)) {
                return cardImage.getResourceId();
            }
        }

        // Return the default image if not found
        return R.drawable.back_of_card;
    }
}
