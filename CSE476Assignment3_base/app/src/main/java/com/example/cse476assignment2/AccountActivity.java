package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    TextView displayNameTextView, emailTextView;
    Button leaderboardButton, communitypostsButton, logoutButton, accountSettingsButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        String email = getIntent().getStringExtra("USERNAME");
        if (email == null) {
            email = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE).getString("USERNAME", "User");
        }

        displayNameTextView = findViewById(R.id.displayNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        leaderboardButton = findViewById(R.id.button4);
        communitypostsButton = findViewById(R.id.buttonCommunityPosts);
        logoutButton = findViewById(R.id.logoutButton);
        accountSettingsButton = findViewById(R.id.buttonAccountSettings);

        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        // Load the display name from the user-specific preferences
        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + email, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", "User");

        // Set the text for both fields
        displayNameTextView.setText(displayName);
        emailTextView.setText(email);

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

        final String finalEmail = email;
        accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, PreferencesActivity.class);
            intent.putExtra("USERNAME", finalEmail);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(AccountActivity.this, MainActivity.class));
            finish();
        });
    }

    // UPDATED: This onResume will refresh the display name if the user changes it in settings
    @Override
    protected void onResume() {
        super.onResume();
        String email = emailTextView.getText().toString();
        if (email.isEmpty()) {
            email = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE).getString("USERNAME", "User");
        }
        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + email, MODE_PRIVATE);
        String displayName = userPrefs.getString("USER_DISPLAY_NAME", "User");
        displayNameTextView.setText(displayName);
        emailTextView.setText(email);
    }
}