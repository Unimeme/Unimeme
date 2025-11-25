package com.example.cse476assignment2.model.Req;

public class DeleteCommentReq {
    public String username;
    public String password;
    public int commentId;

    public DeleteCommentReq(String username, String password, int commentId) {
        this.username = username;
        this.password = password;
        this.commentId = commentId;
    }
}
