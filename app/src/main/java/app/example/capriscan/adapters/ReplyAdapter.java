package app.example.capriscan.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button; // Added import for Button

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.example.capriscan.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private static final String TAG = "ReplyAdapter";
    private List<Reply> replies;
    private final String postId;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;
    private boolean showAllReplies = false;
    private static final int INITIAL_REPLIES_TO_SHOW = 3;

    public interface OnReplyActionListener {
        void onReplyDeleted(String replyId);
        void onNestedReplyAdded(Reply parentReply, String content);
        void onNestedReplyDeleted(String parentReplyId, String nestedReplyId);
        void onNestedReplyEdited(String parentReplyId, String nestedReplyId, String newContent);
    }

    private OnReplyActionListener actionListener;

    public ReplyAdapter(String postId, List<Reply> replies) {
        this.postId = postId;
        this.replies = replies != null ? replies : new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public void setOnReplyActionListener(OnReplyActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        try {
            Reply reply = replies.get(position);
            if (reply != null) {
                holder.bind(reply);
            } else {
                Log.e(TAG, "Null reply at position " + position);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding reply at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return replies != null ? replies.size() : 0;
    }

    public void updateReplies(List<Reply> newReplies) {
        this.replies = newReplies != null ? newReplies : new ArrayList<>();
        notifyDataSetChanged();
    }

    class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, timestamp, replyButton, toggleRepliesButton, likeButton;
        ImageView menuButton;
        EditText nestedReplyEditText;
        Button sendNestedReplyButton; // Changed from ImageButton to Button
        LinearLayout nestedReplyInputContainer, nestedRepliesContainer;
        CircleImageView userAvatar, currentUserAvatar;
        TextView viewMoreRepliesButton;
        boolean repliesVisible = false;

        ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                userName = itemView.findViewById(R.id.reply_user_name);
                content = itemView.findViewById(R.id.reply_content);
                timestamp = itemView.findViewById(R.id.reply_timestamp);
                replyButton = itemView.findViewById(R.id.reply_to_reply_button);
                toggleRepliesButton = itemView.findViewById(R.id.toggle_replies_button);
                likeButton = itemView.findViewById(R.id.reply_like_button);
                menuButton = itemView.findViewById(R.id.reply_menu_button);
                userAvatar = itemView.findViewById(R.id.reply_user_avatar);
                currentUserAvatar = itemView.findViewById(R.id.current_user_avatar);
                nestedReplyEditText = itemView.findViewById(R.id.nested_reply_edit_text);
                sendNestedReplyButton = itemView.findViewById(R.id.send_nested_reply_button);
                nestedReplyInputContainer = itemView.findViewById(R.id.nested_reply_input_container);
                nestedRepliesContainer = itemView.findViewById(R.id.nested_replies_container);
                viewMoreRepliesButton = itemView.findViewById(R.id.view_more_replies_button);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing view holder", e);
            }
        }

        void bind(Reply reply) {
            try {
                // Basic information
                if (reply.getUserName() != null) {
                    userName.setText(reply.getUserName());
                } else {
                    userName.setText("Anonymous");
                }

                if (reply.getContent() != null) {
                    content.setText(reply.getContent());
                } else {
                    content.setText("");
                }

                // Format timestamp as relative time
                timestamp.setText(getRelativeTimeSpan(reply.getTimestamp()));

                // Setup user avatar
                loadUserAvatar(reply.getUserId(), userAvatar);

                // Setup current user avatar for reply input
                if (auth.getCurrentUser() != null) {
                    loadUserAvatar(auth.getCurrentUser().getUid(), currentUserAvatar);
                }

                // Setup menu button with delete option
                setupMenuButton(reply);

                // Handle reply button
                replyButton.setOnClickListener(v -> {
                    try {
                        nestedReplyInputContainer.setVisibility(
                                nestedReplyInputContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        if (nestedReplyInputContainer.getVisibility() == View.VISIBLE) {
                            nestedReplyEditText.requestFocus();
                            // Add '@username' to pre-fill the reply
                            if (reply.getUserName() != null && !reply.getUserName().isEmpty()) {
                                nestedReplyEditText.setText("@" + reply.getUserName() + " ");
                                nestedReplyEditText.setSelection(nestedReplyEditText.getText().length());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling reply button click", e);
                    }
                });

                // Handle send nested reply button
                sendNestedReplyButton.setOnClickListener(v -> {
                    try {
                        String replyContent = nestedReplyEditText.getText().toString().trim();
                        if (replyContent.isEmpty()) {
                            Toast.makeText(itemView.getContext(), "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (auth.getCurrentUser() == null) {
                            Toast.makeText(itemView.getContext(), "You must be logged in to reply", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (actionListener != null) {
                            actionListener.onNestedReplyAdded(reply, replyContent);
                            nestedReplyEditText.setText("");
                            nestedReplyInputContainer.setVisibility(View.GONE);

                            // Make sure nested replies are visible after adding a new one
                            if (!repliesVisible && reply.getReplies() != null && !reply.getReplies().isEmpty()) {
                                toggleRepliesVisibility(reply);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending nested reply", e);
                        Toast.makeText(itemView.getContext(), "Error sending reply", Toast.LENGTH_SHORT).show();
                    }
                });

                // Disable like functionality since we don't need it
                likeButton.setVisibility(View.GONE);

                // Setup nested replies if any
                setupNestedReplies(reply);
            } catch (Exception e) {
                Log.e(TAG, "Error binding reply view", e);
            }
        }

        private void loadUserAvatar(String userId, CircleImageView avatarView) {
            try {
                if (userId == null || avatarView == null) {
                    if (avatarView != null) {
                        avatarView.setImageResource(R.drawable.default_avatar);
                    }
                    return;
                }

                StorageReference avatarRef = storage.getReference().child("profile_images/" + userId + ".jpg");
                avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    try {
                        if (itemView.getContext() != null && avatarView != null) {
                            Picasso.get()
                                    .load(uri)
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(avatarView);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading avatar with Picasso", e);
                        if (avatarView != null) {
                            avatarView.setImageResource(R.drawable.default_avatar);
                        }
                    }
                }).addOnFailureListener(e -> {
                    // Use default avatar if no profile image exists
                    if (avatarView != null) {
                        avatarView.setImageResource(R.drawable.default_avatar);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading user avatar", e);
                if (avatarView != null) {
                    avatarView.setImageResource(R.drawable.default_avatar);
                }
            }
        }

        private String getRelativeTimeSpan(Date date) {
            try {
                if (date == null) return "";

                long now = System.currentTimeMillis();
                long time = date.getTime();
                long diff = now - time;

                if (diff < TimeUnit.MINUTES.toMillis(1)) {
                    return "Just now";
                } else if (diff < TimeUnit.HOURS.toMillis(1)) {
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    return minutes + "m";
                } else if (diff < TimeUnit.DAYS.toMillis(1)) {
                    long hours = TimeUnit.MILLISECONDS.toHours(diff);
                    return hours + "h";
                } else if (diff < TimeUnit.DAYS.toMillis(7)) {
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    return days + "d";
                } else if (diff < TimeUnit.DAYS.toMillis(30)) {
                    long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
                    return weeks + "w";
                } else {
                    // For older dates, use a simple date format
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                    return sdf.format(date);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating relative time", e);
                return "";
            }
        }

        private void setupMenuButton(Reply reply) {
            try {
                boolean isCurrentUserAuthor = auth.getCurrentUser() != null &&
                        auth.getCurrentUser().getUid().equals(reply.getUserId());

                // Show menu button only to the author of the reply
                menuButton.setVisibility(isCurrentUserAuthor ? View.VISIBLE : View.GONE);

                menuButton.setOnClickListener(v -> {
                    try {
                        // Create menu options
                        String[] options = {"Edit", "Delete", "Cancel"};

                        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                        builder.setItems(options, (dialog, which) -> {
                            try {
                                switch (which) {
                                    case 0: // Edit
                                        showEditDialog(reply);
                                        break;
                                    case 1: // Delete
                                        showDeleteConfirmation(reply);
                                        break;
                                    case 2: // Cancel
                                        dialog.dismiss();
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error handling menu option selection", e);
                            }
                        });
                        builder.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing menu options", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error setting up menu button", e);
                if (menuButton != null) {
                    menuButton.setVisibility(View.GONE);
                }
            }
        }

        private void showEditDialog(Reply reply) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                builder.setTitle("Edit Reply");

                // Set up the input
                final EditText input = new EditText(itemView.getContext());
                input.setText(reply.getContent());
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String newContent = input.getText().toString().trim();
                        if (!newContent.isEmpty()) {
                            updateReplyContent(reply, newContent);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving edited reply", e);
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing edit dialog", e);
            }
        }

        private void updateReplyContent(Reply reply, String newContent) {
            try {
                DocumentReference replyRef = getReplyReference(reply);
                if (replyRef != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("content", newContent);

                    replyRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                try {
                                    reply.setContent(newContent);
                                    content.setText(newContent);
                                    Toast.makeText(itemView.getContext(), "Reply updated", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating UI after content update", e);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating reply content in Firestore", e);
                                Toast.makeText(itemView.getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating reply content", e);
                Toast.makeText(itemView.getContext(), "Error updating reply", Toast.LENGTH_SHORT).show();
            }
        }

        private void showDeleteConfirmation(Reply reply) {
            try {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Reply")
                        .setMessage("Are you sure you want to delete this reply?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteReply(reply))
                        .setNegativeButton("Cancel", null)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing delete confirmation", e);
            }
        }

        private void deleteReply(Reply reply) {
            try {
                DocumentReference replyRef = getReplyReference(reply);
                if (replyRef != null) {
                    replyRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                try {
                                    if (actionListener != null) {
                                        actionListener.onReplyDeleted(reply.getId());
                                    }
                                    Toast.makeText(itemView.getContext(), "Reply deleted", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error handling successful reply deletion", e);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting reply from Firestore", e);
                                Toast.makeText(itemView.getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting reply", e);
                Toast.makeText(itemView.getContext(), "Error deleting reply", Toast.LENGTH_SHORT).show();
            }
        }

        private DocumentReference getReplyReference(Reply reply) {
            try {
                if (reply == null || reply.getId() == null) {
                    Log.e(TAG, "Invalid reply or reply ID");
                    return null;
                }

                // All replies are stored in the same collection, regardless of nesting level
                return db.collection("posts").document(postId)
                        .collection("replies").document(reply.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error getting reply reference", e);
                return null;
            }
        }

        private void setupNestedReplies(Reply reply) {
            try {
                if (reply.getReplies() != null && !reply.getReplies().isEmpty()) {
                    // Set up the toggle button
                    toggleRepliesButton.setVisibility(View.VISIBLE);
                    toggleRepliesButton.setText(String.format(Locale.getDefault(),
                            "%s %d %s", repliesVisible ? "Hide" : "View",
                            reply.getReplies().size(),
                            reply.getReplies().size() == 1 ? "reply" : "replies"));

                    // Make the toggle button more visible
                    toggleRepliesButton.setBackgroundResource(R.drawable.rounded_button_background);
                    toggleRepliesButton.setPadding(16, 8, 16, 8);

                    // Set up the listener for the toggle button
                    toggleRepliesButton.setOnClickListener(v -> {
                        try {
                            toggleRepliesVisibility(reply);
                        } catch (Exception e) {
                            Log.e(TAG, "Error toggling replies visibility", e);
                        }
                    });
                } else {
                    toggleRepliesButton.setVisibility(View.GONE);
                    nestedRepliesContainer.setVisibility(View.GONE);
                    viewMoreRepliesButton.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up nested replies", e);
                if (toggleRepliesButton != null) {
                    toggleRepliesButton.setVisibility(View.GONE);
                }
                if (nestedRepliesContainer != null) {
                    nestedRepliesContainer.setVisibility(View.GONE);
                }
                if (viewMoreRepliesButton != null) {
                    viewMoreRepliesButton.setVisibility(View.GONE);
                }
            }
        }

        private void toggleRepliesVisibility(Reply reply) {
            try {
                repliesVisible = !repliesVisible;

                if (repliesVisible) {
                    // Show nested replies
                    nestedRepliesContainer.setVisibility(View.VISIBLE);

                    // Clear any existing views
                    nestedRepliesContainer.removeAllViews();

                    List<Reply> nestedReplies = reply.getReplies();
                    int replyCount = nestedReplies.size();

                    // Determine how many replies to show initially
                    int repliesToShow = showAllReplies ? replyCount : Math.min(INITIAL_REPLIES_TO_SHOW, replyCount);

                    // Add visible replies
                    for (int i = 0; i < repliesToShow; i++) {
                        addNestedReplyView(nestedRepliesContainer, reply, nestedReplies.get(i));
                    }

                    // Show "View more replies" button if needed
                    if (!showAllReplies && replyCount > INITIAL_REPLIES_TO_SHOW) {
                        viewMoreRepliesButton.setVisibility(View.VISIBLE);
                        viewMoreRepliesButton.setText(String.format(Locale.getDefault(),
                                "View %d more %s",
                                replyCount - INITIAL_REPLIES_TO_SHOW,
                                (replyCount - INITIAL_REPLIES_TO_SHOW) == 1 ? "reply" : "replies"));

                        viewMoreRepliesButton.setOnClickListener(v -> {
                            showAllReplies = true;

                            // Add remaining replies
                            for (int i = INITIAL_REPLIES_TO_SHOW; i < replyCount; i++) {
                                addNestedReplyView(nestedRepliesContainer, reply, nestedReplies.get(i));
                            }

                            // Hide the button
                            viewMoreRepliesButton.setVisibility(View.GONE);
                        });
                    } else {
                        viewMoreRepliesButton.setVisibility(View.GONE);
                    }

                    // Change button color to indicate active state
                    toggleRepliesButton.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.blue));
                } else {
                    // Hide nested replies
                    nestedRepliesContainer.setVisibility(View.GONE);
                    viewMoreRepliesButton.setVisibility(View.GONE);
                    toggleRepliesButton.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.dark_gray));

                    // Reset the flag when hiding replies
                    showAllReplies = false;
                }

                // Update button text
                toggleRepliesButton.setText(String.format(Locale.getDefault(),
                        "%s %d %s", repliesVisible ? "Hide" : "View",
                        reply.getReplies().size(),
                        reply.getReplies().size() == 1 ? "reply" : "replies"));
            } catch (Exception e) {
                Log.e(TAG, "Error toggling replies visibility", e);
            }
        }

        private void addNestedReplyView(ViewGroup container, Reply parentReply, Reply nestedReply) {
            View nestedReplyView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_nested_reply, container, false);

            // Bind the nested reply data to the view
            TextView nestedUserName = nestedReplyView.findViewById(R.id.nested_reply_user_name);
            TextView nestedContent = nestedReplyView.findViewById(R.id.nested_reply_content);
            TextView nestedTimestamp = nestedReplyView.findViewById(R.id.nested_reply_timestamp);
            CircleImageView nestedUserAvatar = nestedReplyView.findViewById(R.id.nested_reply_user_avatar);
            ImageView nestedMenuButton = nestedReplyView.findViewById(R.id.nested_reply_menu_button);
            TextView nestedReplyButton = nestedReplyView.findViewById(R.id.nested_reply_reply_button);
            LinearLayout nestedReplyInputContainer = nestedReplyView.findViewById(R.id.nested_reply_input_container);
            EditText nestedReplyEditText = nestedReplyView.findViewById(R.id.nested_reply_edit_text);
            Button sendNestedReplyButton = nestedReplyView.findViewById(R.id.send_nested_reply_button); // Changed from ImageButton to Button
            CircleImageView nestedCurrentUserAvatar = nestedReplyView.findViewById(R.id.nested_reply_current_user_avatar);

            // Set the data
            nestedUserName.setText(nestedReply.getUserName() != null ?
                    nestedReply.getUserName() : "Anonymous");

            // Format content to include @mention if this is a reply to another reply
            String replyContent = nestedReply.getContent();

            // Check if the content already starts with an @mention
            if (!replyContent.startsWith("@") && nestedReply.getParentReplyUserName() != null &&
                    !nestedReply.getParentReplyUserName().isEmpty() &&
                    !nestedReply.getParentReplyUserName().equals(parentReply.getUserName())) {
                // Add @mention only if it's not already there and if it's not replying to the parent comment
                replyContent = "@" + nestedReply.getParentReplyUserName() + " " + replyContent;
            }

            nestedContent.setText(replyContent);
            nestedTimestamp.setText(getRelativeTimeSpan(nestedReply.getTimestamp()));
            loadUserAvatar(nestedReply.getUserId(), nestedUserAvatar);

            // Setup menu button for nested reply
            setupNestedReplyMenuButton(nestedMenuButton, parentReply, nestedReply, nestedContent);

            // Setup reply button for nested reply
            nestedReplyButton.setOnClickListener(v -> {
                // Toggle reply input visibility
                nestedReplyInputContainer.setVisibility(
                        nestedReplyInputContainer.getVisibility() == View.VISIBLE ?
                                View.GONE : View.VISIBLE);

                if (nestedReplyInputContainer.getVisibility() == View.VISIBLE) {
                    nestedReplyEditText.requestFocus();
                    // Add '@username' to pre-fill the reply
                    if (nestedReply.getUserName() != null && !nestedReply.getUserName().isEmpty()) {
                        nestedReplyEditText.setText("@" + nestedReply.getUserName() + " ");
                        nestedReplyEditText.setSelection(nestedReplyEditText.getText().length());
                    }

                    // Load current user avatar
                    if (auth.getCurrentUser() != null) {
                        loadUserAvatar(auth.getCurrentUser().getUid(), nestedCurrentUserAvatar);
                    }
                }
            });

            // Setup send button for replying to this nested reply
            sendNestedReplyButton.setOnClickListener(v -> {
                String content = nestedReplyEditText.getText().toString().trim();
                if (content.isEmpty()) {
                    Toast.makeText(itemView.getContext(), "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (auth.getCurrentUser() == null) {
                    Toast.makeText(itemView.getContext(), "You must be logged in to reply", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create a new nested reply with the parent reply as parent (Facebook-style flat hierarchy)
                addFlatNestedReply(parentReply, nestedReply.getUserName(), content);

                // Clear input and hide
                nestedReplyEditText.setText("");
                nestedReplyInputContainer.setVisibility(View.GONE);
            });

            // Add the view to the container
            container.addView(nestedReplyView);
        }

        private void addFlatNestedReply(Reply parentReply, String replyingToUserName, String content) {
            if (auth.getCurrentUser() == null) return;

            String userId = auth.getCurrentUser().getUid();
            String userName = "Anonymous"; // Default

            // Create a new Reply object - always as a direct child of the parent reply (flat hierarchy)
            Reply nestedReply = new Reply(
                    null,
                    userId,
                    userName,
                    content,
                    new Date(),
                    parentReply.getId(),
                    1  // Always level 1 for Facebook-style flat hierarchy
            );

            // Store who this reply is directed to
            nestedReply.setParentReplyUserName(replyingToUserName);

            // Add it to Firestore
            db.collection("posts").document(postId)
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

                        Toast.makeText(itemView.getContext(), "Reply added", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(itemView.getContext(), "Failed to add reply", Toast.LENGTH_SHORT).show()
                    );
        }

        private void setupNestedReplyMenuButton(ImageView menuButton, Reply parentReply, Reply nestedReply, TextView contentView) {
            try {
                boolean isCurrentUserAuthor = auth.getCurrentUser() != null &&
                        auth.getCurrentUser().getUid().equals(nestedReply.getUserId());

                // Show menu button only to the author of the reply
                menuButton.setVisibility(isCurrentUserAuthor ? View.VISIBLE : View.GONE);

                menuButton.setOnClickListener(v -> {
                    try {
                        // Create menu options
                        String[] options = {"Edit", "Delete", "Cancel"};

                        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                        builder.setItems(options, (dialog, which) -> {
                            try {
                                switch (which) {
                                    case 0: // Edit
                                        showNestedReplyEditDialog(parentReply, nestedReply, contentView);
                                        break;
                                    case 1: // Delete
                                        showNestedReplyDeleteConfirmation(parentReply, nestedReply);
                                        break;
                                    case 2: // Cancel
                                        dialog.dismiss();
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error handling nested reply menu option selection", e);
                            }
                        });
                        builder.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing nested reply menu options", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error setting up nested reply menu button", e);
                if (menuButton != null) {
                    menuButton.setVisibility(View.GONE);
                }
            }
        }

        private void showNestedReplyEditDialog(Reply parentReply, Reply nestedReply, TextView contentView) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                builder.setTitle("Edit Reply");

                // Set up the input
                final EditText input = new EditText(itemView.getContext());
                input.setText(nestedReply.getContent());
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String newContent = input.getText().toString().trim();
                        if (!newContent.isEmpty()) {
                            updateNestedReplyContent(parentReply, nestedReply, newContent, contentView);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving edited nested reply", e);
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing nested reply edit dialog", e);
            }
        }

        private void updateNestedReplyContent(Reply parentReply, Reply nestedReply, String newContent, TextView contentView) {
            try {
                DocumentReference nestedReplyRef = getReplyReference(nestedReply);
                if (nestedReplyRef != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("content", newContent);

                    nestedReplyRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                try {
                                    nestedReply.setContent(newContent);

                                    // Format content to include @mention if needed
                                    String formattedContent = newContent;
                                    if (nestedReply.getParentReplyUserName() != null &&
                                            !nestedReply.getParentReplyUserName().isEmpty() &&
                                            !formattedContent.startsWith("@") &&
                                            !nestedReply.getParentReplyUserName().equals(parentReply.getUserName())) {
                                        formattedContent = "@" + nestedReply.getParentReplyUserName() + " " + formattedContent;
                                    }

                                    contentView.setText(formattedContent);

                                    // Notify listener if available
                                    if (actionListener != null) {
                                        actionListener.onNestedReplyEdited(parentReply.getId(), nestedReply.getId(), newContent);
                                    }

                                    Toast.makeText(itemView.getContext(), "Reply updated", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating UI after nested reply content update", e);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating nested reply content in Firestore", e);
                                Toast.makeText(itemView.getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating nested reply content", e);
                Toast.makeText(itemView.getContext(), "Error updating reply", Toast.LENGTH_SHORT).show();
            }
        }

        private void showNestedReplyDeleteConfirmation(Reply parentReply, Reply nestedReply) {
            try {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Reply")
                        .setMessage("Are you sure you want to delete this reply?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteNestedReply(parentReply, nestedReply))
                        .setNegativeButton("Cancel", null)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing nested reply delete confirmation", e);
            }
        }

        private void deleteNestedReply(Reply parentReply, Reply nestedReply) {
            try {
                DocumentReference nestedReplyRef = getReplyReference(nestedReply);
                if (nestedReplyRef != null) {
                    nestedReplyRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                try {
                                    // Remove the nested reply from parent's replies list
                                    if (parentReply.getReplies() != null) {
                                        parentReply.getReplies().remove(nestedReply);
                                    }

                                    // Refresh the nested replies view
                                    toggleRepliesVisibility(parentReply);

                                    // Notify listener if available
                                    if (actionListener != null) {
                                        actionListener.onNestedReplyDeleted(parentReply.getId(), nestedReply.getId());
                                    }

                                    Toast.makeText(itemView.getContext(), "Reply deleted", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error handling successful nested reply deletion", e);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting nested reply from Firestore", e);
                                Toast.makeText(itemView.getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting nested reply", e);
                Toast.makeText(itemView.getContext(), "Error deleting reply", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
