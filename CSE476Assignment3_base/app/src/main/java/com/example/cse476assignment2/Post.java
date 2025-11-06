// /app/src/main/java/com/example/cse476assignment2/Post.java

package com.example.cse476assignment2;

import android.net.Uri;
import androidx.annotation.DrawableRes;

import java.io.Serializable; // Import Serializable
import java.util.ArrayList;   // Import ArrayList
import java.util.List;        // Import List

// Make the class Serializable to pass it between activities
public class Post implements Serializable {

    private final Uri imageUri;
    private final Integer imageResId;
    private final String caption;
    private final String author;
    private final List<String> comments; // This seems to be an old field, we will replace it.
    private final String location;
    private final long createdAt;
    private int likeCount;

    // NEW: Add a list to hold Comment objects
    private final List<Comment> commentObjects;

    public Post(Uri imageUri, String caption, String author, String location) {
        this(imageUri, caption, author, location, 0, System.currentTimeMillis());
    }

    public Post(@DrawableRes int imageResId, String caption, String author, String location) {
        this(imageResId, caption, author, location, 0, System.currentTimeMillis());
    }

    public Post(Uri imageUri, String caption, String author, String location, int likeCount, long createdAt) {
        this.imageUri = imageUri;
        this.imageResId = null;
        this.caption = caption;
        this.author = author;
        this.location = location;
        this.comments = new ArrayList<>();
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.commentObjects = new ArrayList<>(); // Initialize the new list
    }

    public Post(@DrawableRes int imageResId, String caption, String author, String location, int likeCount, long createdAt) {
        this.imageUri = null;
        this.imageResId = imageResId;
        this.caption = caption;
        this.author = author;
        this.location = location;
        this.comments = new ArrayList<>();
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.commentObjects = new ArrayList<>(); // Initialize the new list
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public Integer getImageResId() {
        return imageResId;
    }

    public String getCaption() {
        return caption;
    }

    public String getAuthor() {
        return author;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getComments() {
        return comments;
    }

    public void addComment(String comment) {
        comments.add(0, comment);
    }

    // NEW: Methods to manage the new comment objects
    public List<Comment> getCommentObjects() {
        return commentObjects;
    }

    public void addCommentObject(Comment comment) {
        commentObjects.add(0, comment); // Add new comments to the top of the list
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}