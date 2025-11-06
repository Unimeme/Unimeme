package com.example.cse476assignment2;

import android.net.Uri;

import androidx.annotation.DrawableRes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {

    private final Uri imageUri;
    private final Integer imageResId;
    private final String caption;
    private final String author;
    private final List<String> comments;
    private final String location;
    private final long createdAt;
    private int likeCount;

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
