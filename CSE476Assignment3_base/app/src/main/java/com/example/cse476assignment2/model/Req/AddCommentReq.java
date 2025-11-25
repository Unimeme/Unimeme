package com.example.cse476assignment2.model.Req;

public class AddCommentReq {
    public String username;
    public String password;
    public int postId;

    public String comment;

    public AddCommentReq(String username, String password, int postId, String comment) {
        this.username = username;
        this.password = password;
        this.postId = postId;
        this.comment = comment;
    }
}
