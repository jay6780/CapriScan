package app.example.capriscan.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.widget.LinearLayout;

import app.example.capriscan.ProfilePage;
import app.example.capriscan.R;

public class HomePage extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);

        // Setting up the cards to be expandable
        int[] cardIds = {R.id.card_1, R.id.card_2, R.id.card_3, R.id.card_4, R.id.card_5};
        int[] expandedContentIds = {R.id.expanded_content_1, R.id.expanded_content_2, R.id.expanded_content_3, R.id.expanded_content_4, R.id.expanded_content_5};

        for (int i = 0; i < cardIds.length; i++) {
            setupCardExpansion(view, cardIds[i], expandedContentIds[i]);
        }

        // Navigate to ProfilePage when the logo is clicked
        View logoButton = view.findViewById(R.id.logo);
        if (logoButton != null) {
            logoButton.setOnClickListener(v -> navigateToProfilePage());
        }

        return view;
    }

    private void setupCardExpansion(View view, int cardViewId, int expandedContentId) {
        CardView cardView = view.findViewById(cardViewId);
        LinearLayout expandedContent = view.findViewById(expandedContentId);

        if (cardView != null && expandedContent != null) {
            // Set an initial visibility state for the expanded content
            expandedContent.setVisibility(View.GONE);

            cardView.setOnClickListener(v -> toggleVisibility(expandedContent));
        }
    }

    private void toggleVisibility(LinearLayout expandedContent) {
        if (expandedContent.getVisibility() == View.VISIBLE) {
            expandedContent.setVisibility(View.GONE);
        } else {
            expandedContent.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToProfilePage() {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new ProfilePage());
        transaction.addToBackStack(null);
        transaction.commit();
    }
}