package com.example.cse476assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cse476assignment2.R;

public class ChatSearchActivity extends AppCompatActivity {

    private EditText edtUsername;
    private Button btnStartChat;

    private String username;
    private String password;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_search);

        edtUsername = findViewById(R.id.edtSearchUsername);
        btnStartChat = findViewById(R.id.btnStartChat);

        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        userId = getIntent().getLongExtra("userId", -1);

        btnStartChat.setOnClickListener(v -> {
            String target = edtUsername.getText().toString().trim();

            if (target.isEmpty()) {
                Toast.makeText(this, "Please enter a username.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(ChatSearchActivity.this, ChatRoomActivity.class);
            i.putExtra("username", username);
            i.putExtra("password", password);
            i.putExtra("userId", userId);
            i.putExtra("partnerUsername", target);
            startActivity(i);
        });
    }
}
