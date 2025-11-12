package com.example.cse476assignment2.net;

import com.example.cse476assignment2.model.Req.SignUpReq;
import com.example.cse476assignment2.model.Res.SignUpRes;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    // curl 경로 그대로
    @POST("cse476/group6/api/users/create")
    Call<SignUpRes> signUp(@Body SignUpReq body);
}
