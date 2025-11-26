package com.example.cse476assignment2.model.Req;

public class DeletePostReq {
    public String username;
    public String password;
    public int postId;

    public DeletePostReq(String username, String password, int postId) {
        this.username = username;
        this.password = password;
        this.postId = postId;
    }
}