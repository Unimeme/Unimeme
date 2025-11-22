package com.example.cse476assignment2.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostDto {
    @SerializedName("post_id")
    public int postId;

    @SerializedName("created_at")
    public String createdAt;  // UTC String

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("caption")
    public String caption;

    @SerializedName("author")
    public AuthorDto author;

    @SerializedName("location")
    public LocationDto location;

    @SerializedName("comments")
    public List<CommentDto> comments;
}

