package com.example.cse476assignment2.model;

import com.google.gson.annotations.SerializedName;

public class AuthorDto {
    @SerializedName("user_id")
    public int userId;

    @SerializedName("username")
    public String username;

    @SerializedName("pic")
    public String pic;   // profile_url
}

