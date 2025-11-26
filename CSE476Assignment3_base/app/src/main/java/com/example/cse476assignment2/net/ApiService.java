package com.example.cse476assignment2.net;

import com.example.cse476assignment2.model.Req.AddCommentReq;
import com.example.cse476assignment2.model.Req.CreatePostReq;
import com.example.cse476assignment2.model.Req.DeleteCommentReq;
import com.example.cse476assignment2.model.Req.LoginReq;
import com.example.cse476assignment2.model.Req.SendMessageReq;
import com.example.cse476assignment2.model.Req.SignUpReq;
import com.example.cse476assignment2.model.Req.UpdateUserReq;
import com.example.cse476assignment2.model.Res.AddCommentRes;
import com.example.cse476assignment2.model.Res.CreatePostRes;
import com.example.cse476assignment2.model.Res.DeleteCommentRes;
import com.example.cse476assignment2.model.Res.GetCommentsRes;
import com.example.cse476assignment2.model.Res.GetPartnersRes;
import com.example.cse476assignment2.model.Res.GetPostFeedRes;
import com.example.cse476assignment2.model.Res.GetThreadRes;
import com.example.cse476assignment2.model.Res.LoginRes;
import com.example.cse476assignment2.model.Res.SendMessageRes;
import com.example.cse476assignment2.model.Res.SignUpRes;
import com.example.cse476assignment2.model.Res.UpdateUserRes;
import com.example.cse476assignment2.model.Res.UploadImageRes;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST("cse476/group6/api/users/create")
    Call<SignUpRes> signUp(@Body SignUpReq body);

    @POST("cse476/group6/api/users/login")
    Call<LoginRes> login(@Body LoginReq body);

    @POST("cse476/group6/api/users/update")
    Call<UpdateUserRes> updateUser(@Body UpdateUserReq body);


    //upload image file
    @Multipart
    @POST("cse476/group6/api/posts/upload")
    Call<UploadImageRes> uploadPostImage(
            @Part("username") RequestBody username,
            @Part("password") RequestBody password,
            @Part MultipartBody.Part image
    );

    //create post in DB with imageUrl
    @POST("cse476/group6/api/posts/create")
    Call<CreatePostRes> createPost(@Body CreatePostReq body);


    // get post
    @GET("cse476/group6/api/posts/feed")
    Call<GetPostFeedRes> getPostFeed(
            @Query("username") String username,
            @Query("password") String password,
            @Query("limit") Integer limit,
            @Query("afterId") Integer afterId
    );

    @POST("cse476/group6/api/comments/create")
    Call<AddCommentRes> addComment(@Body AddCommentReq body);

    @GET("cse476/group6/api/comments")
    Call<GetCommentsRes> getComments(
            @Query("username") String username,
            @Query("password") String password,
            @Query("postId") int postId
    );

    @HTTP(method = "DELETE", path = "cse476/group6/api/comments/delete", hasBody = true)
    Call<DeleteCommentRes> deleteComment(@Body DeleteCommentReq body);

    // =============================
// Messaging API
// =============================

    // Send a message
    @POST("cse476/group6/api/messages/send")
    Call<SendMessageRes> sendMessage(@Body SendMessageReq body);

    // Get thread between two users
    @GET("cse476/group6/api/messages/thread")
    Call<GetThreadRes> getThread(
            @Query("username") String username,
            @Query("password") String password,
            @Query("partner") String partnerUsername
    );

    // Get user's chat partner list
    @GET("cse476/group6/api/messages/partners")
    Call<GetPartnersRes> getPartners(
            @Query("username") String username,
            @Query("password") String password
    );


}
