package com.example.cse476assignment2;

import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.MessageAdapter;
import com.example.cse476assignment2.R;
import com.example.cse476assignment2.model.Req.SendMessageReq;
import com.example.cse476assignment2.model.Res.GetThreadRes;
import com.example.cse476assignment2.model.Res.SendMessageRes;
import com.example.cse476assignment2.net.ApiClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText editMessage;

    private String username;
    private String password;
    private String partnerUsername;
    private long myUserId;

    private Handler handler = new Handler();
    private Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        partnerUsername = getIntent().getStringExtra("partnerUsername");
        myUserId = getIntent().getLongExtra("userId", -1);

        recyclerView = findViewById(R.id.recyclerChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MessageAdapter(new ArrayList<>(), myUserId);
        recyclerView.setAdapter(adapter);

        editMessage = findViewById(R.id.editMessage);

        ImageButton btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        ApiClient.get().getThread(username, password, partnerUsername)
                .enqueue(new Callback<GetThreadRes>() {
                    @Override
                    public void onResponse(Call<GetThreadRes> call, Response<GetThreadRes> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            adapter.update(res.body().Messages);
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        }
                    }
                    @Override
                    public void onFailure(Call<GetThreadRes> call, Throwable t) {}
                });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        SendMessageReq req = new SendMessageReq(username, password, partnerUsername, text);

        ApiClient.get().sendMessage(req)
                .enqueue(new Callback<SendMessageRes>() {
                    @Override
                    public void onResponse(Call<SendMessageRes> call, Response<SendMessageRes> res) {
                        editMessage.setText("");
                        loadMessages();
                    }
                    @Override
                    public void onFailure(Call<SendMessageRes> call, Throwable t) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(pollTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pollTask);
    }
}
