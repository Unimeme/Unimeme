package com.example.cse476assignment2.model.Req;

public class SendMessageReq {
    public String username;
    public String password;
    public String receiverUsername;
    public String content;

    public SendMessageReq(String username, String password, String receiverUsername, String content) {
        this.username = username;
        this.password = password;
        this.receiverUsername = receiverUsername;
        this.content = content;
    }
}