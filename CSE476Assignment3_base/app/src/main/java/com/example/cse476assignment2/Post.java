package com.example.cse476assignment2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private String photoPath;
    private String caption;
    // New: List to store comments for this specific post
    private List<String> comments;

    public Post(String photoPath, String caption) {
        this.photoPath = photoPath;
        this.caption = caption;
        this.comments = new ArrayList<>(); // Initialize the comments list
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public String getCaption() {
        return caption;
    }

    public List<String> getComments() {
        return comments;
    }

    // New: Method to add a comment
    public void addComment(String comment) {
        // Might want to add new comments to the beginning (index 0)
        // to show the most recent comment first.
        this.comments.add(0, comment);
    }
}