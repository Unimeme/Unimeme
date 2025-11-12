package com.example.cse476assignment2.net;

import com.example.cse476assignment2.model.Req.LoginReq;
import com.example.cse476assignment2.model.Req.SignUpReq;
import com.example.cse476assignment2.model.Res.LoginRes;
import com.example.cse476assignment2.model.Res.SignUpRes;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("cse476/group6/api/users/create")
    Call<SignUpRes> signUp(@Body SignUpReq body);

    @POST("cse476/group6/api/users/login")
    Call<LoginRes> login(@Body LoginReq body);
}
