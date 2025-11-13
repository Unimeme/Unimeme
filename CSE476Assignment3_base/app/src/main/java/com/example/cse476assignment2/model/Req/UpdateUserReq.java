package com.example.cse476assignment2.model.Req;

public class UpdateUserReq {
    public String username;
    public String new_username;
    public String bio;

    public UpdateUserReq(String username, String newUsername, String bio) {
        this.username = username;
        this.new_username = newUsername;
        this.bio = bio;
    }
}
