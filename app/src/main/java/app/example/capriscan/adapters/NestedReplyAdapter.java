package app.example.capriscan.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import app.example.capriscan.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class NestedReplyAdapter extends RecyclerView.Adapter<NestedReplyAdapter.NestedReplyViewHolder> {
    private static final String TAG = "NestedReplyAdapter";
    private List<Reply> nestedReplies;
    private final FirebaseStorage storage;

    public NestedReplyAdapter(List<Reply> nestedReplies) {
        this.nestedReplies = nestedReplies != null ? nestedReplies : new ArrayList<>();
        this.storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public NestedReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nested_reply, parent, false);
        return new NestedReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NestedReplyViewHolder holder, int position) {
        try {
            Reply reply = nestedReplies.get(position);
            if (reply != null) {
                holder.bind(reply);
            } else {
                Log.e(TAG, "Null nested reply at position " + position);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding nested reply at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return nestedReplies != null ? nestedReplies.size() : 0;
    }

    public void updateReplies(List<Reply> newReplies) {
        this.nestedReplies = newReplies != null ? newReplies : new ArrayList<>();
        notifyDataSetChanged();
    }

    class NestedReplyViewHolder extends RecyclerView.ViewHolder {
        TextView userName, content, timestamp;
        CircleImageView userAvatar;

        NestedReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                userName = itemView.findViewById(R.id.nested_reply_user_name);
                content = itemView.findViewById(R.id.nested_reply_content);
                timestamp = itemView.findViewById(R.id.nested_reply_timestamp);
                userAvatar = itemView.findViewById(R.id.nested_reply_user_avatar);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing nested reply view holder", e);
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
            } catch (Exception e) {
                Log.e(TAG, "Error binding nested reply view", e);
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
    }
}