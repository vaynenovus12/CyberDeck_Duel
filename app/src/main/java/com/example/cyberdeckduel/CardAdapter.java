package com.example.cyberdeckduel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private final List<Card> cardList;
    private final int playerLevel;
    private final GameActivity gameActivity; // Reference to GameActivity to use getCardImageResource

    public CardAdapter(List<Card> cardList, int playerLevel, GameActivity gameActivity) {
        this.cardList = cardList;
        this.playerLevel = playerLevel;
        this.gameActivity = gameActivity; // Pass GameActivity instance
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cardList.get(position);
        holder.cardName.setText(card.getName());
        holder.cardDescription.setText(card.getDescription());
        holder.cardPower.setText("Power: " + card.getPower());
        holder.cardUnlockLevel.setText("Unlock Level: " + card.getUnlockLevel());

        // Use GameActivity's getCardImageResource method to get the image
        int imageResource = gameActivity.getCardImageResource(card.getId());
        holder.cardImage.setImageResource(imageResource);

        // Check if the card is locked or unlocked
        if (playerLevel >= card.getUnlockLevel()) {
            holder.cardStatus.setText("Status: Unlocked");
            holder.cardStatus.setTextColor(holder.cardStatus.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.cardStatus.setText("Status: Locked");
            holder.cardStatus.setTextColor(holder.cardStatus.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardName, cardDescription, cardPower, cardUnlockLevel, cardStatus;
        ImageView cardImage;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardName = itemView.findViewById(R.id.cardName);
            cardDescription = itemView.findViewById(R.id.cardDescription);
            cardPower = itemView.findViewById(R.id.cardPower);
            cardStatus = itemView.findViewById(R.id.cardStatus);
            cardImage = itemView.findViewById(R.id.cardImage);
            cardUnlockLevel = itemView.findViewById(R.id.cardUnlockLevel); // New Unlock Level TextView

        }
    }
}
