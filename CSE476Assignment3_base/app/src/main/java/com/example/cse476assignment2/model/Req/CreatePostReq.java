package com.example.cse476assignment2.model.Req;

public class CreatePostReq {
    public String username;
    public String password;
    public String imageUrl;
    public String caption;
    public Integer locationId;   // or int, if you always have one

    public CreatePostReq(String username, String password,
                         String imageUrl, String caption, Integer locationId) {
        this.username = username;
        this.password = password;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.locationId = locationId;
    }
}
