package com.example.cse476assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.ChatRoomActivity;
import com.example.cse476assignment2.ChatSearchActivity;
import com.example.cse476assignment2.PartnersAdapter;
import com.example.cse476assignment2.R;
import com.example.cse476assignment2.model.Res.GetPartnersRes;
import com.example.cse476assignment2.net.ApiClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatPartnersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PartnersAdapter adapter;

    private String username;
    private String password;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_partners);

        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        userId = getIntent().getLongExtra("userId", -1);

        recyclerView = findViewById(R.id.recyclerPartners);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PartnersAdapter(new ArrayList<>(), partner -> {
            Intent i = new Intent(ChatPartnersActivity.this, ChatRoomActivity.class);
            i.putExtra("username", username);
            i.putExtra("password", password);
            i.putExtra("partnerUsername", partner.username);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        recyclerView.setAdapter(adapter);

        Button btnNewChat = findViewById(R.id.btnNewChat);
        btnNewChat.setOnClickListener(v -> {
            Intent i = new Intent(ChatPartnersActivity.this, ChatSearchActivity.class);
            i.putExtra("username", username);
            i.putExtra("password", password);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        loadPartners();
    }

    private void loadPartners() {
        ApiClient.get().getPartners(username, password)
                .enqueue(new Callback<GetPartnersRes>() {
                    @Override
                    public void onResponse(Call<GetPartnersRes> call, Response<GetPartnersRes> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.update(response.body().Partners);
                        } else {
                            Toast.makeText(ChatPartnersActivity.this,
                                    "Failed to load partners", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GetPartnersRes> call, Throwable t) {
                        Toast.makeText(ChatPartnersActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
