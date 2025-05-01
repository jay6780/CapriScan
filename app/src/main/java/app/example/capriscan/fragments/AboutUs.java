package app.example.capriscan.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.widget.LinearLayout;

import app.example.capriscan.R;

public class AboutUs extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_us, container, false); // Make sure to inflate the correct layout

        // Setting up the cards (no longer expandable)
        int[] cardIds = {R.id.card_1, R.id.card_2, R.id.card_3, R.id.card_4, R.id.card_5};
        int[] expandedContentIds = {R.id.expanded_content_1, R.id.expanded_content_2, R.id.expanded_content_3, R.id.expanded_content_4, R.id.expanded_content_5};

        for (int i = 0; i < cardIds.length; i++) {
            setupCard(view, cardIds[i], expandedContentIds[i]);
        }

        return view;
    }

    private void setupCard(View view, int cardViewId, int expandedContentId) {
        CardView cardView = view.findViewById(cardViewId);
        LinearLayout expandedContent = view.findViewById(expandedContentId);

        if (cardView != null && expandedContent != null) {
            // Set the expanded content to be always visible
            expandedContent.setVisibility(View.VISIBLE);
            // Optionally, you can disable click events on the CardView if you don't want any interaction
            cardView.setClickable(false);
        }
    }
}