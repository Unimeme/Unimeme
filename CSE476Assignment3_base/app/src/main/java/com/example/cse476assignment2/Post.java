// /app/src/main/java/com/example/cse476assignment2/Post.java
package com.example.cse476assignment2;

import android.net.Uri;
import androidx.annotation.DrawableRes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private transient Uri imageUri;
    private String imageUriString;
    private final Integer imageResId;
    private final String caption;
    private final String author;
    private final List<String> comments;
    private final String location;
    private final long createdAt;
    private int likeCount;
    private final List<Comment> commentObjects;
    private boolean isLikedByCurrentUser = false;

    private List<String> hashtags;

    public Post(Uri imageUri, String caption, String author, String location) {
        this(imageUri, caption, author, location, 0, System.currentTimeMillis());
    }

    public Post(@DrawableRes int imageResId, String caption, String author, String location) {
        this(imageResId, caption, author, location, 0, System.currentTimeMillis());
    }

    public Post(Uri imageUri, String caption, String author, String location, int likeCount, long createdAt) {
        this.imageUri = imageUri;
        if (imageUri != null) {
            this.imageUriString = imageUri.toString();
        }
        this.imageResId = null;
        this.caption = caption;
        this.author = author;
        this.location = location;
        this.comments = new ArrayList<>();
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.commentObjects = new ArrayList<>();
        this.hashtags = new ArrayList<>();
    }

    public Post(@DrawableRes int imageResId, String caption, String author, String location, int likeCount, long createdAt) {
        this.imageUri = null;
        this.imageUriString = null;
        this.imageResId = imageResId;
        this.caption = caption;
        this.author = author;
        this.location = location;
        this.comments = new ArrayList<>();
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.commentObjects = new ArrayList<>();
        this.hashtags = new ArrayList<>();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString);
        }
    }

    public Uri getImageUri() { return imageUri; }
    public Integer getImageResId() { return imageResId; }
    public String getCaption() { return caption; }
    public String getAuthor() { return author; }
    public String getLocation() { return location; }
    public List<String> getComments() { return comments; }
    public void addComment(String comment) { comments.add(0, comment); }
    public List<Comment> getCommentObjects() { return commentObjects; }
    public void addCommentObject(Comment comment) { commentObjects.add(0, comment); }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public long getCreatedAt() { return createdAt; }

    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }

    public void toggleLike() {
        if (isLikedByCurrentUser) {
            likeCount--;
        } else {
            likeCount++;
        }
        isLikedByCurrentUser = !isLikedByCurrentUser;
    }

    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }
}