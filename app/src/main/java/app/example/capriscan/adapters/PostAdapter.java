package app.example.capriscan.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import app.example.capriscan.R;
import com.squareup.picasso.Picasso;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";
    private List<Post> posts;
    private final OnDeleteClickListener deleteListener;
    private final OnReplyClickListener replyListener;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface OnDeleteClickListener {
        void onDeleteClick(String postId);
    }

    public interface OnReplyClickListener {
        void onReplyClick(String postId, String replyContent);
    }

    public PostAdapter(List<Post> posts, OnDeleteClickListener deleteListener, OnReplyClickListener replyListener) {
        this.posts = posts != null ? posts : new ArrayList<>();
        this.deleteListener = deleteListener;
        this.replyListener = replyListener;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updatePosts(List<Post> newPosts) {
        this.posts = newPosts != null ? newPosts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPost(Post post) {
        if (post != null) {
            this.posts.add(post);
            notifyItemInserted(posts.size() - 1);
        }
    }

    public List<Post> getPosts() {
        return posts;
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, timestamp;
        Button deleteButton, replyButton;
        EditText replyEditText;
        RecyclerView repliesRecyclerView;
        ImageView postImage;
        ReplyAdapter replyAdapter;
        ListenerRegistration repliesListenerRegistration;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.post_user_name);
            content = itemView.findViewById(R.id.post_content);
            timestamp = itemView.findViewById(R.id.post_timestamp);
            deleteButton = itemView.findViewById(R.id.post_delete_button);
            replyButton = itemView.findViewById(R.id.post_reply_button);
            replyEditText = itemView.findViewById(R.id.post_reply_edit_text);
            repliesRecyclerView = itemView.findViewById(R.id.post_replies_recycler_view);
            postImage = itemView.findViewById(R.id.post_image);
        }

        void bind(Post post) {
            // Set basic post fields.
            userName.setText(post.getUserName());
            content.setText(post.getContent());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            Date date = post.getTimestamp(); // Ensure timestamp is a Date object.
            timestamp.setText(sdf.format(date));

            // Handle post image if available
            if (post.hasImage()) {
                postImage.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(post.getImageUrl())
                        .into(postImage);

                // Setup image click for full-screen view
                postImage.setOnClickListener(v -> {
                    // Show full-screen image view
                    if (v.getContext() instanceof Activity) {
                        // Optional: implement a full-screen image viewer
                    }
                });
            } else {
                postImage.setVisibility(View.GONE);
            }

            // Check if this is a rating post.
            if ("rating".equals(post.getPostType())) {
                // For rating posts, hide delete and reply functionalities.
                deleteButton.setVisibility(View.GONE);
                replyButton.setVisibility(View.GONE);
                replyEditText.setVisibility(View.GONE);
                repliesRecyclerView.setVisibility(View.GONE);
            } else {
                // For normal posts:
                // Show the delete button only if the current user is the owner.
                if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(post.getUserId())) {
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(v -> {
                        if (deleteListener != null) {
                            deleteListener.onDeleteClick(post.getId());
                        }
                    });
                } else {
                    deleteButton.setVisibility(View.GONE);
                }

                // Show the reply UI.
                replyButton.setVisibility(View.VISIBLE);
                replyEditText.setVisibility(View.VISIBLE);
                repliesRecyclerView.setVisibility(View.VISIBLE);
                replyButton.setOnClickListener(v -> {
                    if (auth.getCurrentUser() == null) {
                        Toast.makeText(itemView.getContext(), "Please log in to reply", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String replyContent = replyEditText.getText().toString().trim();
                    if (!replyContent.isEmpty()) {
                        if (replyListener != null) {
                            replyListener.onReplyClick(post.getId(), replyContent);
                            replyEditText.setText("");

                            // Hide keyboard
                            InputMethodManager imm = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(replyEditText.getWindowToken(), 0);
                        }
                    }
                });

                // Setup reply adapter with the post ID
                List<Reply> postReplies = post.getReplies() != null ? post.getReplies() : new ArrayList<>();
                replyAdapter = new ReplyAdapter(post.getId(), postReplies);
                replyAdapter.setOnReplyActionListener(new ReplyAdapter.OnReplyActionListener() {
                    @Override
                    public void onReplyDeleted(String replyId) {
                        // Update our local data if needed
                        List<Reply> updatedReplies = new ArrayList<>();
                        if (post.getReplies() != null) {
                            for (Reply reply : post.getReplies()) {
                                if (!reply.getId().equals(replyId)) {
                                    updatedReplies.add(reply);
                                }
                            }
                            post.setReplies(updatedReplies);
                        }
                    }

                    @Override
                    public void onNestedReplyAdded(Reply parentReply, String content) {
                        addNestedReply(post, parentReply, content);
                    }

                    @Override
                    public void onNestedReplyDeleted(String parentReplyId, String nestedReplyId) {
                        // Find the parent reply and remove the nested reply from it
                        if (post.getReplies() != null) {
                            for (Reply reply : post.getReplies()) {
                                if (reply.getId().equals(parentReplyId) && reply.getReplies() != null) {
                                    List<Reply> updatedNestedReplies = new ArrayList<>();
                                    for (Reply nestedReply : reply.getReplies()) {
                                        if (!nestedReply.getId().equals(nestedReplyId)) {
                                            updatedNestedReplies.add(nestedReply);
                                        }
                                    }
                                    reply.setReplies(updatedNestedReplies);
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onNestedReplyEdited(String parentReplyId, String nestedReplyId, String newContent) {
                        // Find the parent reply and update the content of the nested reply
                        if (post.getReplies() != null) {
                            updateNestedReplyContent(post.getReplies(), parentReplyId, nestedReplyId, newContent);
                        }
                    }
                });

                repliesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                repliesRecyclerView.setAdapter(replyAdapter);
                setupRepliesListener(post);
            }
        }

        // Recursively update nested reply content at any depth
        private void updateNestedReplyContent(List<Reply> replies, String parentReplyId, String nestedReplyId, String newContent) {
            for (Reply reply : replies) {
                // Check if this is the parent reply
                if (reply.getId().equals(parentReplyId)) {
                    // Look for the nested reply in this parent's replies
                    if (reply.getReplies() != null) {
                        for (Reply nestedReply : reply.getReplies()) {
                            if (nestedReply.getId().equals(nestedReplyId)) {
                                nestedReply.setContent(newContent);
                                return;
                            }
                            // Check if the nested reply is deeper in the hierarchy
                            if (nestedReply.getReplies() != null && !nestedReply.getReplies().isEmpty()) {
                                updateNestedReplyContent(nestedReply.getReplies(), nestedReply.getId(), nestedReplyId, newContent);
                            }
                        }
                    }
                    return;
                }

                // If not found at this level, check deeper in the hierarchy
                if (reply.getReplies() != null && !reply.getReplies().isEmpty()) {
                    updateNestedReplyContent(reply.getReplies(), parentReplyId, nestedReplyId, newContent);
                }
            }
        }

        private void setupRepliesListener(Post post) {
            // Clean up any existing listener
            if (repliesListenerRegistration != null) {
                repliesListenerRegistration.remove();
            }

            // Setup real-time updates for replies
            repliesListenerRegistration = db.collection("posts").document(post.getId())
                    .collection("replies")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            // Handle error
                            return;
                        }

                        if (value != null) {
                            List<Reply> allReplies = new ArrayList<>();
                            Map<String, Reply> replyMap = new HashMap<>();

                            // First pass: create all Reply objects and store in a map by ID
                            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                Reply reply = doc.toObject(Reply.class);
                                if (reply != null) {
                                    reply.setId(doc.getId());
                                    replyMap.put(reply.getId(), reply);
                                    allReplies.add(reply);
                                }
                            }

                            // Second pass: build the hierarchy
                            List<Reply> topLevelReplies = new ArrayList<>();
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

                            // Update post replies
                            post.setReplies(topLevelReplies);

                            // Update UI
                            if (replyAdapter != null) {
                                replyAdapter.updateReplies(topLevelReplies);
                            }
                        }
                    });
        }

        private void addNestedReply(Post post, Reply parentReply, String content) {
            if (auth.getCurrentUser() == null) return;

            String userId = auth.getCurrentUser().getUid();
            String userName = "Anonymous"; // Default

            // Create a new Reply object
            Reply nestedReply = new Reply(null, userId, userName, content, new Date(), parentReply.getId(), parentReply.getLevel() + 1);

            // Try to add it both to Firestore and our local data
            db.collection("posts").document(post.getId())
                    .collection("replies")
                    .add(nestedReply)
                    .addOnSuccessListener(documentReference -> {
                        // Update the reply with its ID
                        nestedReply.setId(documentReference.getId());

                        // Fetch the user's name
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String name = userDoc.getString("name");
                                        if (name != null && !name.isEmpty()) {
                                            nestedReply.setUserName(name);
                                            documentReference.update("userName", name);
                                        }
                                    }
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(itemView.getContext(), "Failed to add reply", Toast.LENGTH_SHORT).show()
                    );
        }

        void unbind() {
            if (repliesListenerRegistration != null) {
                repliesListenerRegistration.remove();
                repliesListenerRegistration = null;
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }
}