// /app/src/main/java/com/example/cse476assignment2/Comment.java
package com.example.cse476assignment2;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    private final String author;
    private final String text;
    private final long timestamp;
    private final int authorProfileImageResId;
    private int commentId;

    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int id) {
        this.commentId = id;
    }


    private int likeCount = 0;
    private boolean isLikedByCurrentUser = false;

    public Comment(String author, String text, int authorProfileImageResId) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date().getTime();
        this.authorProfileImageResId = authorProfileImageResId;
    }

    public Comment(String author, String text, int authorProfileImageResId, long timestamp) {
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
        this.authorProfileImageResId = authorProfileImageResId;
    }

    public String getAuthor() { return author; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    public int getAuthorProfileImageResId() { return authorProfileImageResId; }

    public int getLikeCount() { return likeCount; }
    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }

    public void toggleLike() {
        if (isLikedByCurrentUser) likeCount--;
        else likeCount++;
        isLikedByCurrentUser = !isLikedByCurrentUser;
    }
}
