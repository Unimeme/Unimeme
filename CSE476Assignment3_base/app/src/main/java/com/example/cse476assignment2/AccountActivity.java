package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// The following code is written based on YouTube videos I watched
// ChatGPT was used to beautify code and add comments in places they were not in already
public class AccountActivity extends AppCompatActivity {

    // Declare UI elements and shared preferences
    TextView usernameTextView;
    Button leaderboardButton, communitypostsButton, logoutButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Link UI elements to their XML IDs
        usernameTextView = findViewById(R.id.textView);
        leaderboardButton = findViewById(R.id.button4);
        communitypostsButton = findViewById(R.id.buttonCommunityPosts);
        logoutButton = findViewById(R.id.logoutButton);

        // Initialize SharedPreferences (used for saving login state)
        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        // Get the username from Intent if coming from login (MainActivity)
        String username = getIntent().getStringExtra("USERNAME");

        // If no username was passed (e.g., auto-login), fetch it from SharedPreferences
        //if (username == null) {
        //    username = sharedPreferences.getString("USERNAME", "User");
        //}

        // Display the username on the account page
        usernameTextView.setText(username);

        // Set button labels from string resources
        leaderboardButton.setText(R.string.go_to_leaderboard);
        communitypostsButton.setText(R.string.go_to_community_posts);
        logoutButton.setText(R.string.logout);

        // Handle "Go to Leaderboard" button click
        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        // Handle "Go to Community Posts" button click
        communitypostsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, CommunityPostsActivity.class);
            startActivity(intent);
        });



        // Handle "Logout" button click
        logoutButton.setOnClickListener(v -> {
            // Clear saved login data so auto-login won’t trigger next time
            sharedPreferences.edit().clear().apply();
            // Navigate back to MainActivity (login page)
            startActivity(new Intent(AccountActivity.this, MainActivity.class));
            // Finish AccountActivity so user can’t return with back button
            finish();
        });
    }
}