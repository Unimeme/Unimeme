package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

// The following code is written based on YouTube videos I watched
// ChatGPT was used to beautify code and add comments in places they were not in already
// change for testing merge in git repo

public class AccountActivity extends AppCompatActivity {

    // Declare UI elements and shared preferences
    TextView usernameTextView;
    Button leaderboardButton, communitypostsButton, logoutButton, accountSettingsButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        String username = getIntent().getStringExtra("USERNAME");
        if (username == null) {
            username = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE).getString("USERNAME", "User");
        }


        usernameTextView = findViewById(R.id.textView);
        leaderboardButton = findViewById(R.id.button4);
        communitypostsButton = findViewById(R.id.buttonCommunityPosts);
        logoutButton = findViewById(R.id.logoutButton);
        accountSettingsButton = findViewById(R.id.buttonAccountSettings);

        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        usernameTextView.setText(username);

        leaderboardButton.setText(R.string.go_to_leaderboard);
        communitypostsButton.setText(R.string.go_to_community_posts);
        logoutButton.setText(R.string.logout);
        accountSettingsButton.setText(R.string.account_settings_btn);

        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        communitypostsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, CommunityPostsActivity.class);
            startActivity(intent);
        });

        final String finalUsername = username;
        accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, PreferencesActivity.class);
            intent.putExtra("USERNAME", finalUsername);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(AccountActivity.this, MainActivity.class));
            finish();
        });
    }
}
