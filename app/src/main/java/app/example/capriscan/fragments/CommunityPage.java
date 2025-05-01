package app.example.capriscan.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.util.Log;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.example.capriscan.R;
import app.example.capriscan.adapters.Post;
import app.example.capriscan.adapters.PostAdapter;
import app.example.capriscan.adapters.Reply;
import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityPage extends Fragment {

    private static final String TAG = "CommunityPage";
    private View view;
    private EditText postEditText;
    private Button postButton;
    private FloatingActionButton fabAddPost;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SwipeRefreshLayout swipeRefreshLayout; // Swipe-to-refresh container
    private CircleImageView currentUserProfilePicture;
    private FirebaseStorage storage;

    private Button rateButton;
    private Button showAllButton; // Added button to toggle post visibility
    private boolean arePostsVisible = true; // Track post visibility state

    private SearchView searchView; // Added SearchView

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            view = inflater.inflate(R.layout.fragment_community_page, container, false);

            // Initialize Firebase instances first
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();

            // Initialize view references
            initializeViews();

            // Setup UI components and listeners
            setupUIComponents();

            // Setup recycler view and load posts
            setupRecyclerView();
            loadPosts();

            // Setup SearchView
            if (searchView != null) {
                setupSearchView();
            }

            return view;
        } catch (Exception e) {
            // Log the error
            Log.e(TAG, "Error in onCreateView", e);
            e.printStackTrace();

            // Create a simple fallback view with an error message
            TextView errorView = new TextView(getContext());
            errorView.setText(R.string.community_page_error);
            errorView.setPadding(30, 30, 30, 30);
            errorView.setGravity(Gravity.CENTER);

            return errorView;
        }
    }

    /**
     * Initialize all view references
     */
    private void initializeViews() {
        if (view == null) return;

        postEditText = view.findViewById(R.id.post_edit_text);
        postButton = view.findViewById(R.id.post_button);
        fabAddPost = view.findViewById(R.id.fab_add_post);
        postsRecyclerView = view.findViewById(R.id.posts_recycler_view);
        rateButton = view.findViewById(R.id.rate_button);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        currentUserProfilePicture = view.findViewById(R.id.current_user_profile_picture);
        showAllButton = view.findViewById(R.id.showall);
        searchView = view.findViewById(R.id.search_view);
    }

    /**
     * Setup all UI components and their listeners
     */
    private void setupUIComponents() {
        if (view == null) return;

        // Setup swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadPosts();
                swipeRefreshLayout.setRefreshing(false);
            });

            swipeRefreshLayout.setColorSchemeResources(
                    R.color.blue, R.color.green, R.color.orange, R.color.purple_500);
        }

        // Post prompt click to show post creation dialog
        TextView postPromptText = view.findViewById(R.id.post_prompt_text);
        if (postPromptText != null) {
            postPromptText.setOnClickListener(v -> showPostCreationDialog());
        }

        // Setup post button to show the same dialog as the floating action button
        if (postButton != null) {
            postButton.setOnClickListener(v -> showPostCreationDialog());
        }

        // Load current user info
        loadCurrentUserInfo();

        // Setup floating action button
        if (fabAddPost != null) {
            fabAddPost.setOnClickListener(v -> showPostCreationDialog());
        }
        if (rateButton != null) {
            rateButton.setOnClickListener(v -> showRatingDialog());
        }

        // Setup the show all button to toggle posts visibility
        if (showAllButton != null) {
            showAllButton.setOnClickListener(v -> togglePostVisibility());
            // Initialize button text properly
            showAllButton.setText(arePostsVisible ? R.string.hide_posts : R.string.show_posts);
        }
    }

    private void setupSearchView() {
        if (searchView == null) return;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPosts(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchPosts(newText);
                return true;
            }
        });
    }

    private void searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadPosts();
            return;
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> filteredPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Post post = documentSnapshot.toObject(Post.class);
                        if (post != null && post.getContent() != null && post.getContent().toLowerCase().contains(query.toLowerCase())) {
                            post.setId(documentSnapshot.getId());
                            filteredPosts.add(post);
                        }
                    }
                    if (postAdapter != null) {
                        postAdapter.updatePosts(filteredPosts);
                    }

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching posts", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to search posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void togglePostVisibility() {
        arePostsVisible = !arePostsVisible; // Toggle the state

        if (postsRecyclerView != null) {
            postsRecyclerView.setVisibility(arePostsVisible ? View.VISIBLE : View.GONE);
        }

        if (showAllButton != null) {
            showAllButton.setText(arePostsVisible ? R.string.hide_posts : R.string.show_posts);
        }
    }

    private void showRatingDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rate_post, null);
        builder.setView(dialogView);

        // Initialize star rating views
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        Button submitRatingButton = dialogView.findViewById(R.id.submit_rating_button);

        final AlertDialog dialog = builder.create();

        submitRatingButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating == 0) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            // Post the rating
            postRating(rating);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void postRating(float rating) {
        if (auth.getCurrentUser () == null) {
            Toast.makeText(getContext(), R.string.please_login_to_rate, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser ().getUid();
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("userId", userId);
        ratingData.put("rating", rating);
        ratingData.put("timestamp", new Date());

        // Add rating to Firestore
        db.collection("ratings")
                .add(ratingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Rating submitted successfully", Toast.LENGTH_SHORT).show();
                    // Optionally, you can refresh the posts or update UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error submitting rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showPostCreationDialog() {
        if (getContext() == null) return;

        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), R.string.please_login_to_post, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_create_post, null);

            if (dialogView == null) {
                Toast.makeText(getContext(), "Could not load post dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            builder.setView(dialogView);

            EditText postContentEditText = dialogView.findViewById(R.id.post_text);
            Button dialogPostButton = dialogView.findViewById(R.id.post_button);
            CircleImageView userProfilePic = dialogView.findViewById(R.id.user_profile_pic);
            TextView userName = dialogView.findViewById(R.id.user_name);

            // Load user profile pic and name
            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();

                // Load profile picture
                StorageReference profileRef = storage.getReference()
                        .child("profile_images/" + userId + ".jpg");
                profileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            if (getContext() != null) {
                                Picasso.get().load(uri)
                                        .placeholder(R.drawable.default_avatar)
                                        .into(userProfilePic);
                            }
                        });

                // Load user name
                db.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    userName.setText(name);
                                }
                            }
                        });
            }

            final AlertDialog dialog = builder.create();

            // Setup post button
            if (dialogPostButton != null && postContentEditText != null) {
                dialogPostButton.setOnClickListener(v -> {
                    String content = postContentEditText.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(getContext(), R.string.post_cannot_be_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dialog.dismiss();
                    addPost(content);
                });
            }

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing post creation dialog", e);
            Toast.makeText(getContext(), "Error creating post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        // Initialize with the updated interface methods
        postAdapter = new PostAdapter(
                new ArrayList<>(),
                this::deletePost,
                this::showReplyDialog  // Changed to use the new reply dialog method
        );
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postsRecyclerView.setAdapter(postAdapter);
    }

    private void loadPosts() {
        try {
            // Show loading indicator
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }

            if (db == null) {
                db = FirebaseFirestore.getInstance();
            }

            db.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            if (getContext() == null || getActivity() == null || isDetached()) {
                                return; // Fragment is no longer active
                            }

                            // Clear existing posts
                            postAdapter.updatePosts(new ArrayList<>());

                            // Process results
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Post post = documentSnapshot.toObject(Post.class);
                                if (post != null) {
                                    post.setId(documentSnapshot.getId());
                                    postAdapter.addPost(post);

                                    // Load replies for this post
                                    loadRepliesForPost(post);
                                }
                            }

                            // Hide loading indicator
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }

                            // Fetch usernames for all posts
                            for (Post post : postAdapter.getPosts()) {
                                fetchUserName(post);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing posts", e);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
                            }

                            // Hide loading indicator
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading posts", e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        // Hide loading indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadPosts()", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // Hide loading indicator
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    // Load replies for a specific post
    private void loadRepliesForPost(Post post) {
        if (post == null || post.getId() == null) return;

        db.collection("posts").document(post.getId())
                .collection("replies")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(replySnapshots -> {
                    List<Reply> topLevelReplies = new ArrayList<>();
                    Map<String, Reply> replyMap = new HashMap<>();

                    // First pass: create all Reply objects and store in a map by ID
                    for (QueryDocumentSnapshot replyDoc : replySnapshots) {
                        Reply reply = replyDoc.toObject(Reply.class);
                        reply.setId(replyDoc.getId());
                        replyMap.put(reply.getId(), reply);
                    }

                    // Second pass: build the hierarchy
                    for (Reply reply : replyMap.values()) {
                        if (reply.getParentId() == null || reply.getParentId().isEmpty()) {
                            // This is a top-level reply
                            topLevelReplies.add(reply);
                        } else {
                            // This is a nested reply, add it to its parent
                            Reply parentReply = replyMap.get(reply.getParentId());
                            if (parentReply != null) {
                                // Set the level for proper indentation
                                reply.setLevel(parentReply.getLevel() + 1);
                                parentReply.addReply(reply);
                            } else {
                                // If parent not found, treat as top-level
                                topLevelReplies.add(reply);
                            }
                        }
                    }

                    // Update the post with the hierarchical replies
                    post.setReplies(topLevelReplies);
                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading replies for post " + post.getId(), e);
                });
    }

    private void fetchUserName(Post post) {
        try {
            if (post == null || post.getCreatorId() == null || post.getCreatorId().isEmpty()) {
                Log.e(TAG, "Invalid post or creator ID");
                return;
            }

            if (db == null) {
                db = FirebaseFirestore.getInstance();
            }

            db.collection("users").document(post.getCreatorId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot.exists()) {
                                String username = documentSnapshot.getString("username");
                                if (username != null && !username.isEmpty()) {
                                    post.setCreatorName(username);
                                    if (postAdapter != null) {
                                        postAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing username", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching username", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchUserName()", e);
        }
    }

    private void loadCurrentUserInfo() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            // Update user name in post creation dialog when opened instead

                            // Load profile picture
                            StorageReference profileRef = storage.getReference()
                                    .child("profile_images/" + userId + ".jpg");
                            profileRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        if (getContext() != null) {
                                            Picasso.get().load(uri)
                                                    .placeholder(R.drawable.default_avatar)
                                                    .into(currentUserProfilePicture);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Use default avatar
                                    });
                        }
                    });
        }
    }

    private void addPost(String postContent) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), R.string.please_login_to_post, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", postContent);
        post.put("timestamp", new Date());
        post.put("postType", "normal");
        post.put("creatorId", userId); // Ensure creatorId is set
        post.put("imageUrl", ""); // Empty string since we're not uploading images

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        if (userName != null) {
                            post.put("userName", userName);
                            post.put("creatorName", userName); // Ensure creatorName is set

                            // Add post to Firestore
                            db.collection("posts")
                                    .add(post)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(getContext(), R.string.post_added_successfully, Toast.LENGTH_SHORT).show();

                                        // Reset UI
                                        postEditText.setText("");
                                        if (postButton != null) {
                                            postButton.setEnabled(true);
                                            postButton.setText(R.string.post);
                                        }

                                        // Reload posts to show the new one
                                        loadPosts();

                                        // Scroll to top of post list
                                        scrollToTop();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), R.string.error_adding_post, Toast.LENGTH_SHORT).show();
                                        if (postButton != null) {
                                            postButton.setEnabled(true);
                                            postButton.setText(R.string.post);
                                        }
                                    });
                        }
                    }
                });
    }

    private void deletePost(String postId) {
        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.post_deleted_successfully, Toast.LENGTH_SHORT).show();
                    // Reload posts after deletion
                    loadPosts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), R.string.error_deleting_post, Toast.LENGTH_SHORT).show());
    }

    // Show dialog to add a reply
    private void showReplyDialog(String postId, String replyContent) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), R.string.please_login_to_reply, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and add the reply directly
        addReply(postId, replyContent, null);
    }

    // Add a reply to a post or to another reply
    private void addReply(String postId, String replyContent, String parentReplyId) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), R.string.please_login_to_reply, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> reply = new HashMap<>();
        reply.put("userId", userId);
        reply.put("content", replyContent);
        reply.put("timestamp", new Date());
        reply.put("reactions", new HashMap<String, List<String>>()); // Initialize empty reactions map

        // If parentReplyId is provided, this is a nested reply
        if (parentReplyId != null && !parentReplyId.isEmpty()) {
            reply.put("parentId", parentReplyId);
            // Set level to 1 for direct replies to top-level comments
            // The actual level will be calculated when loading the hierarchy
            reply.put("level", 1);
        } else {
            // This is a top-level reply
            reply.put("level", 0);
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        if (userName != null && !userName.isEmpty()) {
                            reply.put("userName", userName);
                        } else {
                            reply.put("userName", "User " + userId.substring(0, Math.min(5, userId.length())));
                        }

                        // Add to Firestore
                        db.collection("posts").document(postId)
                                .collection("replies")
                                .add(reply)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getContext(), R.string.reply_added_successfully, Toast.LENGTH_SHORT).show();
                                    // Reload the post to show the new reply
                                    loadPosts();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), getString(R.string.error_adding_reply, e.getMessage()), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), getString(R.string.error_fetching_user_data, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void scrollToTop() {
        if (postsRecyclerView != null && postsRecyclerView.getAdapter() != null &&
                postsRecyclerView.getAdapter().getItemCount() > 0) {
            postsRecyclerView.smoothScrollToPosition(0);
        }
        if (postEditText != null) {
            postEditText.requestFocus();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // No explicit cleanup needed for now
    }
}
