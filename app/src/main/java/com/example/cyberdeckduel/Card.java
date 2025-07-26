package com.example.cyberdeckduel;

public class Card {
    private String id, name, description, type, category;
    private int power, unlockLevel;

    public Card() {
        // Default constructor required for calls to DataSnapshot.getValue(Card.class)
    }

    public Card(String id, String name, String description, String category, String type, int power, int unlockLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.type = type;
        this.power = power;
        this.unlockLevel = unlockLevel; // Initialize unlockLevel
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getUnlockLevel() {
        return unlockLevel; // Get the level required to unlock the card
    }

    public void setUnlockLevel(int unlockLevel) {
        this.unlockLevel = unlockLevel; // Set the unlock level
    }
}
