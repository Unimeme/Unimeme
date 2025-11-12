package com.example.cse476assignment2.net;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static ApiService instance;

    public static ApiService get() {
        if (instance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            // Retrofit baseUrl 은 반드시 / 로 끝나야 함
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://www.egr.msu.edu/") // 호스트만 베이스로
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
