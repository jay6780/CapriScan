package app.example.capriscan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfilePage extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Firestore instance
    private TextView emailTextView;
    private TextView nameTextView;
    private Button logoutButton;
    private ProgressBar loadingIndicator; // ProgressBar for loading

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        emailTextView = view.findViewById(R.id.emailTextView);
        nameTextView = view.findViewById(R.id.nameTextView);
        logoutButton = view.findViewById(R.id.logoutButton);
        loadingIndicator = view.findViewById(R.id.loadingIndicator); // Initialize ProgressBar

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailTextView.setText(currentUser.getEmail());
            loadingIndicator.setVisibility(View.VISIBLE); // Show loading indicator
            fetchUserName(currentUser.getUid()); // Fetch user name from Firestore
        } else {
            emailTextView.setText("No user logged in");
            nameTextView.setText("N/A");
        }

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            if (getActivity() != null) {
                getActivity().finishAffinity(); // Exits the app completely
            }
        });

        return view;
    }

    private void fetchUserName(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingIndicator.setVisibility(View.GONE); // Hide loading indicator
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        if (userName == null || userName.isEmpty()) {
                            userName = "User " + userId.substring(0, 5);
                        }
                        nameTextView.setText(userName); // Set user name in TextView
                    } else {
                        nameTextView.setText("N/A"); // Handle case where user document does not exist
                    }
                })
                .addOnFailureListener(e -> {
                    loadingIndicator.setVisibility(View.GONE); // Hide loading indicator
                    nameTextView.setText("Error fetching name"); // Handle errors
                });
    }
}
