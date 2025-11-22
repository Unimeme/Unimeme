package com.example.cse476assignment2.model.Res;

import com.example.cse476assignment2.model.PostDto;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GetPostFeedRes {
    @SerializedName("posts")
    public List<PostDto> posts;

    @SerializedName("nextAfterId")
    public Integer nextAfterId;
}