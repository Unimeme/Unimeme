// /app/src/main/java/com/example/cse476assignment2/Comment.java

package com.example.cse476assignment2;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    private final String author;
    private final String text;
    private final long timestamp;
    // In a real app, this might be a URI to a profile picture.
    // For now, we'll use a drawable resource ID as a placeholder.
    private final int authorProfileImageResId;

    public Comment(String author, String text, int authorProfileImageResId) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date().getTime();
        this.authorProfileImageResId = authorProfileImageResId;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getAuthorProfileImageResId() {
        return authorProfileImageResId;
    }
}