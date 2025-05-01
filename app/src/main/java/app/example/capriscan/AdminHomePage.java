package app.example.capriscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminHomePage extends AppCompatActivity implements AnnouncementAdapter.OnAnnouncementClickListener {

    private RecyclerView announcementsRecyclerView;
    private FloatingActionButton addAnnouncementFab;
    private FirebaseFirestore db;
    private AnnouncementAdapter adapter;
    private ImageView logoImageView;
    private long backPressedTime;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_home_page);

        // Set system bar (status bar) color to green
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        announcementsRecyclerView = findViewById(R.id.announcements_recycler_view);
        addAnnouncementFab = findViewById(R.id.add_announcement_fab);
        logoImageView = findViewById(R.id.logo);

        setupRecyclerView();
        loadAnnouncements();

        addAnnouncementFab.setOnClickListener(v -> showAddAnnouncementDialog());

        logoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminHomePage.this, AdminProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(new ArrayList<>(), this);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        announcementsRecyclerView.setAdapter(adapter);
    }

    private void loadAnnouncements() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showToast("Error loading announcements");
                        return;
                    }

                    if (value != null) {
                        ArrayList<Announcement> announcements = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Announcement announcement = doc.toObject(Announcement.class);
                            if (announcement != null) {
                                announcement.setId(doc.getId());
                                announcements.add(announcement);
                            }
                        }
                        adapter.updateAnnouncements(announcements);
                    }
                });
    }

    private void showAddAnnouncementDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.announcement_title_input);
        TextInputEditText contentInput = dialogView.findViewById(R.id.announcement_content_input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add New Announcement")
                .setView(dialogView)
                .setPositiveButton("Post", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String content = contentInput.getText().toString().trim();
                    if (!title.isEmpty() && !content.isEmpty()) {
                        postAnnouncement(title, content);
                    } else {
                        showToast("Please fill in all fields");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void postAnnouncement(String title, String content) {
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("content", content);
        announcement.put("timestamp", System.currentTimeMillis());

        db.collection("announcements")
                .add(announcement)
                .addOnSuccessListener(documentReference -> showToast("Announcement posted successfully"))
                .addOnFailureListener(e -> showToast("Error posting announcement"));
    }

    @Override
    public void onEditAnnouncement(Announcement announcement) {
        showEditAnnouncementDialog(announcement);
    }

    @Override
    public void onDeleteAnnouncement(Announcement announcement) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("announcements").document(announcement.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> showToast("Announcement deleted successfully"))
                            .addOnFailureListener(e -> showToast("Error deleting announcement"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditAnnouncementDialog(Announcement announcement) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.announcement_title_input);
        TextInputEditText contentInput = dialogView.findViewById(R.id.announcement_content_input);

        titleInput.setText(announcement.getTitle());
        contentInput.setText(announcement.getContent());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Announcement")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String content = contentInput.getText().toString().trim();
                    if (!title.isEmpty() && !content.isEmpty()) {
                        updateAnnouncement(announcement.getId(), title, content);
                    } else {
                        showToast("Please fill in all fields");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateAnnouncement(String announcementId, String title, String content) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", title);
        updatedData.put("content", content);

        db.collection("announcements").document(announcementId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> showToast("Announcement updated successfully"))
                .addOnFailureListener(e -> showToast("Error updating announcement"));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}