package app.example.capriscan.adapters;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    private String id;
    private String userId;
    private String userName;
    private String creatorId;
    private String creatorName;
    private String content;
    private Date timestamp;
    private String postType;
    private String imageUrl;
    private List<Reply> replies;
    private Map<String, List<String>> reactions; // Map of reaction type to list of userIds
    private int commentsCount;
    private String location;
    private String privacy; // "public", "friends", "private"

    // Empty constructor for Firestore
    public Post() {
        replies = new ArrayList<>();
        reactions = new HashMap<>();
        commentsCount = 0;
        privacy = "public";
    }

    public Post(String id, String userId, String userName, String content, Date timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.creatorId = userId; // Default to userId
        this.creatorName = userName; // Default to userName
        this.content = content;
        this.timestamp = timestamp;
        this.postType = "normal";
        this.replies = new ArrayList<>();
        this.reactions = new HashMap<>();
        this.commentsCount = 0;
        this.privacy = "public";
    }

    public Post(String id, String userId, String userName, String content, Date timestamp, String postType, String imageUrl) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.creatorId = userId; // Default to userId
        this.creatorName = userName; // Default to userName
        this.content = content;
        this.timestamp = timestamp;
        this.postType = postType;
        this.imageUrl = imageUrl;
        this.replies = new ArrayList<>();
        this.reactions = new HashMap<>();
        this.commentsCount = 0;
        this.privacy = "public";
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public String getPostType() {
        return postType;
    }
    public void setPostType(String postType) {
        this.postType = postType;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    @Exclude
    public List<Reply> getReplies() {
        return replies;
    }
    public void setReplies(List<Reply> replies) {
        this.replies = replies;
    }

    public Map<String, List<String>> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, List<String>> reactions) {
        this.reactions = reactions != null ? reactions : new HashMap<>();
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public void addReply(Reply reply) {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(reply);
        commentsCount++;
    }

    public void addReaction(String reactionType, String userId) {
        if (reactions == null) {
            reactions = new HashMap<>();
        }

        // First remove any existing reaction from this user
        for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
            entry.getValue().remove(userId);
        }

        // Add the new reaction
        if (!reactions.containsKey(reactionType)) {
            reactions.put(reactionType, new ArrayList<>());
        }

        if (!reactions.get(reactionType).contains(userId)) {
            reactions.get(reactionType).add(userId);
        }
    }

    public void removeReaction(String reactionType, String userId) {
        if (reactions != null && reactions.containsKey(reactionType)) {
            reactions.get(reactionType).remove(userId);

            // Clean up empty reaction types
            if (reactions.get(reactionType).isEmpty()) {
                reactions.remove(reactionType);
            }
        }
    }

    public boolean hasUserReacted(String userId, String reactionType) {
        return reactions != null &&
                reactions.containsKey(reactionType) &&
                reactions.get(reactionType).contains(userId);
    }

    public int getReactionsCount() {
        int count = 0;
        if (reactions != null) {
            for (List<String> users : reactions.values()) {
                count += users.size();
            }
        }
        return count;
    }

    public String getMostCommonReaction() {
        if (reactions == null || reactions.isEmpty()) {
            return null;
        }

        String mostCommon = null;
        int maxCount = 0;

        for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
            if (entry.getValue().size() > maxCount) {
                maxCount = entry.getValue().size();
                mostCommon = entry.getKey();
            }
        }

        return mostCommon;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
}
