package com.example.cse476assignment2.model;

import com.google.gson.annotations.SerializedName;

public class CommentDto {
    @SerializedName("comment_id")
    public int commentId;

    @SerializedName("post_id")
    public int postId;

    @SerializedName("user_id")
    public int userId;

    @SerializedName("username")
    public String username;

    @SerializedName("content")
    public String content;

    @SerializedName("created_at")
    public String createdAt;
}
