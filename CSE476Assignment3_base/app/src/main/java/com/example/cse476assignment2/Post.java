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

    public Post(Uri imageUri, String caption, String author) {
        this.imageUri = imageUri;
        this.imageResId = null;
        this.caption = caption;
        this.author = author;
        this.comments = new ArrayList<>();
    }

    public Post(@DrawableRes int imageResId, String caption, String author) {
        this.imageUri = null;
        this.imageResId = imageResId;
        this.caption = caption;
        this.author = author;
        this.comments = new ArrayList<>();
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

    public List<String> getComments() {
        return comments;
    }

    public void addComment(String comment) {
        comments.add(0, comment);
    }
}