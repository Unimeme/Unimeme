package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    TextView displayNameTextView, emailTextView;
    Button leaderboardButton, communityPostsButton, logoutButton, accountSettingsButton;
    SharedPreferences sharedPreferences;

    private String currentUsername;  // Username used for login and server
    private String currentEmail;     // School email (display only)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        // Load login username from Intent or LOGIN_PREFS
        currentUsername = getIntent().getStringExtra("USERNAME");
        if (currentUsername == null) {
            currentUsername = sharedPreferences.getString("USERNAME", "User");
        }

        // Bind UI elements
        displayNameTextView = findViewById(R.id.displayNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        leaderboardButton = findViewById(R.id.button4);
        communityPostsButton = findViewById(R.id.buttonCommunityPosts);
        logoutButton = findViewById(R.id.logoutButton);
        accountSettingsButton = findViewById(R.id.buttonAccountSettings);

        // Load user-specific preferences
        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + currentUsername, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", currentUsername);
        currentEmail = userPrefs.getString("USER_EMAIL", currentUsername); // fallback to username if email is missing

        // Display data
        displayNameTextView.setText(displayName);
        emailTextView.setText(currentEmail);

        // Button listeners
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
    }

    // Refresh values in case user changed display name or username in PreferencesActivity
    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        }

        // Username may have changed
        currentUsername = sharedPreferences.getString("USERNAME", currentUsername);

        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + currentUsername, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", currentUsername);
        currentEmail = userPrefs.getString("USER_EMAIL", currentUsername);

        displayNameTextView.setText(displayName);
        emailTextView.setText(currentEmail);
    }
}
