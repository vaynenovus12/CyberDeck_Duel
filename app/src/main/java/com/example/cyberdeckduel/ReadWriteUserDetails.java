package com.example.cyberdeckduel;

import java.util.ArrayList;
import java.util.List;

public class ReadWriteUserDetails {
    public String name, dob, gender;
    public int coins,  level, xp, wins;
    public List<Card> deck;

    //Constructor
    public ReadWriteUserDetails(){
    }
    public ReadWriteUserDetails(String name, String dob, String gender, int coins, int level, int xp, int wins, ArrayList<Card> deck) {
        this.name = name;
        this.dob = dob;
        this.gender = gender;
        this.coins = coins;
        this.level = level; // Initialize level
        this.xp = xp;       // Initialize XP
        this.wins = wins;
        this.deck = deck;
    }
}

