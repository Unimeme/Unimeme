package com.example.cse476assignment2.model.Req;

public class SignUpReq {
    public String username;
    public String password;
    public String bio;
    public String profile_url;

    public SignUpReq(String username, String password, String bio, String profileUrl) {
        this.username = username;
        this.password = password;
        this.bio = bio;
        this.profile_url = profileUrl;
    }
}
