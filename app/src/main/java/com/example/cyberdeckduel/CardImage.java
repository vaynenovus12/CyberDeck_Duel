package com.example.cyberdeckduel;

public class CardImage {
    private String cardId;
    private int resourceId;

    public CardImage(String cardId, int resourceId) {
        this.cardId = cardId;
        this.resourceId = resourceId;
    }

    public String getCardId() {
        return cardId;
    }

    public int getResourceId() {
        return resourceId;
    }
}
