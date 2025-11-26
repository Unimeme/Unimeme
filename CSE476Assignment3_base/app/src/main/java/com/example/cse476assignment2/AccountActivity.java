package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    TextView displayNameTextView, emailTextView;
    Button leaderboardButton, communityPostsButton, logoutButton, accountSettingsButton, messageButton, achievementButton;
    SharedPreferences sharedPreferences;

    private String currentUsername;
    private String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        currentUsername = getIntent().getStringExtra("USERNAME");
        if (currentUsername == null) {
            currentUsername = sharedPreferences.getString("USERNAME", "User");
        }

        displayNameTextView = findViewById(R.id.displayNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        leaderboardButton = findViewById(R.id.button4);
        communityPostsButton = findViewById(R.id.buttonCommunityPosts);
        logoutButton = findViewById(R.id.logoutButton);
        accountSettingsButton = findViewById(R.id.buttonAccountSettings);
        messageButton = findViewById(R.id.buttonMessages);
        achievementButton = findViewById(R.id.buttonAchievements);

        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + currentUsername, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", currentUsername);
        currentEmail = userPrefs.getString("USER_EMAIL", currentUsername);

        displayNameTextView.setText(displayName);
        emailTextView.setText(currentEmail);

        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        communityPostsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, CommunityPostsActivity.class);
            startActivity(intent);
        });

        accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, PreferencesActivity.class);
            intent.putExtra("USERNAME", currentUsername);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(AccountActivity.this, MainActivity.class));
            finish();
        });

        // ★ NEW: Open chat system
        messageButton.setOnClickListener(v -> {

            SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
            String username = loginPrefs.getString("USERNAME", null);
            String password = loginPrefs.getString("PASSWORD", null);
            long userId = loginPrefs.getLong("USER_ID", -1);

            if (username == null || password == null || userId == -1) {
                Toast.makeText(AccountActivity.this, "Login info missing", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(AccountActivity.this, com.example.cse476assignment2.ChatPartnersActivity.class);
            i.putExtra("username", username);
            i.putExtra("password", password);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        achievementButton.setOnClickListener(v -> {
            Toast.makeText(AccountActivity.this, "Coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        }

        currentUsername = sharedPreferences.getString("USERNAME", currentUsername);

        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + currentUsername, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", currentUsername);
        currentEmail = userPrefs.getString("USER_EMAIL", currentUsername);

        displayNameTextView.setText(displayName);
        emailTextView.setText(currentEmail);
    }
}
