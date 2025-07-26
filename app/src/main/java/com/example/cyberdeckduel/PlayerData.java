package com.example.cyberdeckduel;

import java.util.HashMap;

public class PlayerData {

    public String name;
    public HashMap<String, Card> hand;
    public boolean isTurn;
    public int score;
    public int totalCardsLeft;



    // Default constructor required for calls to DataSnapshot.getValue(PlayerData.class)
    public PlayerData() {
        this.hand = new HashMap<>(); // Initialize an empty HashMap
    }

    public PlayerData(String name, HashMap<String, Card> hand, boolean isTurn, int totalCardsLeft) {
        this.name = name;
        this.hand = (hand != null) ? hand : new HashMap<>(); // Use provided hand or initialize it
        this.isTurn = isTurn;
        this.score = 0;
        this.totalCardsLeft = totalCardsLeft;
    }

    // Add getters and setters if needed
}
