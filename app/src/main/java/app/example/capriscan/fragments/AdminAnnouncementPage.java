package app.example.capriscan.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

import app.example.capriscan.AdminAnnouncement;
import app.example.capriscan.AdminAnnouncementAdapter;
import app.example.capriscan.R;

public class AdminAnnouncementPage extends Fragment {

    private View view;
    private RecyclerView announcementsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private AdminAnnouncementAdapter adminAnnouncementAdapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_admin_announcement_page, container, false);

        announcementsRecyclerView = view.findViewById(R.id.announcements_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        loadAnnouncements();

        return view;
    }

    private void setupRecyclerView() {
        adminAnnouncementAdapter = new AdminAnnouncementAdapter(new ArrayList<>());
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementsRecyclerView.setAdapter(adminAnnouncementAdapter);
    }

    private void loadAnnouncements() {
        // Show progress bar and hide RecyclerView and empty state initially
        progressBar.setVisibility(View.VISIBLE);
        announcementsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // Hide progress bar after loading
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        List<AdminAnnouncement> adminAnnouncements = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            AdminAnnouncement announcement = document.toObject(AdminAnnouncement.class);
                            adminAnnouncements.add(announcement);
                        }

                        if (!adminAnnouncements.isEmpty()) {
                            // Show RecyclerView and update adapter
                            announcementsRecyclerView.setVisibility(View.VISIBLE);
                            adminAnnouncementAdapter.setAnnouncements(adminAnnouncements);
                        } else {
                            // Show empty state text if no announcements found
                            emptyStateText.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading announcements", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
