package app.example.capriscan.adapters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Reply {
    private String id;
    private String userId;
    private String userName;
    private String content;
    private Date timestamp;
    private String parentReplyId;
    private int level;
    private List<Reply> replies;
    private String parentReplyUserName; // Added field for storing who this reply is directed to

    public Reply() {
        // Required empty constructor for Firestore
    }

    public Reply(String id, String userId, String userName, String content, Date timestamp, String parentReplyId, int level) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
        this.parentReplyId = parentReplyId;
        this.level = level;
        this.replies = new ArrayList<>();
        this.parentReplyUserName = "";
    }

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

    public String getParentReplyId() {
        return parentReplyId;
    }

    // Add this method as an alias for getParentReplyId() to fix the compatibility issue
    public String getParentId() {
        return parentReplyId;
    }

    public void setParentReplyId(String parentReplyId) {
        this.parentReplyId = parentReplyId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<Reply> getReplies() {
        return replies;
    }

    public void setReplies(List<Reply> replies) {
        this.replies = replies;
    }

    public String getParentReplyUserName() {
        return parentReplyUserName;
    }

    public void setParentReplyUserName(String parentReplyUserName) {
        this.parentReplyUserName = parentReplyUserName;
    }

    public void addReply(Reply reply) {
        if (this.replies == null) {
            this.replies = new ArrayList<>();
        }
        this.replies.add(reply);
    }

    public void removeReply(Reply reply) {
        if (this.replies != null) {
            this.replies.remove(reply);
        }
    }
}
