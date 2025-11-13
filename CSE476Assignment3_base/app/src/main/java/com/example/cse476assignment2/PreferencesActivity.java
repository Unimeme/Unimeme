package com.example.cse476assignment2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.cse476assignment2.model.Req.UpdateUserReq;
import com.example.cse476assignment2.model.Res.UpdateUserRes;
import com.example.cse476assignment2.net.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesActivity extends AppCompatActivity {

    private static final String KEY_DISPLAY_NAME = "USER_DISPLAY_NAME";
    private static final String KEY_BIO = "USER_BIO";
    private static final String KEY_LOCATION_TRACKING = "LOCATION_TRACKING";
    private static final String KEY_DARK_MODE = "DARK_MODE";

    private EditText displayNameEditText;
    private EditText bioEditText;
    private SwitchCompat locationSwitch;
    private SwitchCompat darkModeSwitch;
    private Button saveButton, backButton;

    private SharedPreferences userPrefs;
    private String username;   // Current logged-in username

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Get USERNAME from Intent
        username = getIntent().getStringExtra("USERNAME");

        // If not found, try from login prefs
        if (username == null) {
            SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
            username = loginPrefs.getString("USERNAME", null);
        }

        // Fallback (should rarely be used)
        if (username == null) {
            username = "default_user";
        }

        userPrefs = getSharedPreferences("USER_PREFS_" + username, MODE_PRIVATE);

        // Bind UI
        displayNameEditText = findViewById(R.id.editTextDisplayName);
        bioEditText = findViewById(R.id.editTextBio);
        locationSwitch = findViewById(R.id.switchLocationTracking);
        darkModeSwitch = findViewById(R.id.switchDarkMode);
        saveButton = findViewById(R.id.buttonSavePreferences);
        backButton = findViewById(R.id.buttonBackPreferences);

        // Load existing preferences
        displayNameEditText.setText(userPrefs.getString(KEY_DISPLAY_NAME, ""));
        bioEditText.setText(userPrefs.getString(KEY_BIO, ""));
        locationSwitch.setChecked(userPrefs.getBoolean(KEY_LOCATION_TRACKING, false));
        darkModeSwitch.setChecked(userPrefs.getBoolean(KEY_DARK_MODE, false));

        // Dark mode toggle
        darkModeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Save button
        saveButton.setOnClickListener(v -> savePreferences());

        // Back button
        backButton.setOnClickListener(v -> finish());
    }

    private void savePreferences() {

        String newDisplayName = displayNameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        // Save to local SharedPreferences
        userPrefs.edit()
                .putString(KEY_DISPLAY_NAME, newDisplayName)
                .putString(KEY_BIO, newBio)
                .putBoolean(KEY_LOCATION_TRACKING, locationSwitch.isChecked())
                .putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked())
                .apply();

        // Determine new username (if changed)
        final String currentUsername = username;
        final String newUsernameForServer =
                (!newDisplayName.isEmpty() && !newDisplayName.equals(currentUsername))
                        ? newDisplayName
                        : null;

        final String bioForServer = newBio.isEmpty() ? null : newBio;

        // Create request object
        UpdateUserReq req = new UpdateUserReq(
                currentUsername,
                newUsernameForServer,
                bioForServer
        );

        // Send update request
        ApiClient.get().updateUser(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<UpdateUserRes> call, Response<UpdateUserRes> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PreferencesActivity.this,
                            "Server error: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                UpdateUserRes body = response.body();

                if (!body.ok) {
                    String msg = "Update failed.";

                    if (body.error != null) {
                        if (body.error.equals("username_taken")) {
                            msg = "That username is already taken.";
                        } else if (body.error.equals("nothing_to_update")) {
                            msg = "There is nothing to update.";
                        } else {
                            msg = "Update failed: " + body.error;
                        }
                    }

                    Toast.makeText(PreferencesActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sync username change
                if (newUsernameForServer != null) {

                    // Update login prefs
                    SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
                    loginPrefs.edit()
                            .putString("USERNAME", newUsernameForServer)
                            .apply();

                    // Update local user prefs using the new username
                    SharedPreferences newUserPrefs =
                            getSharedPreferences("USER_PREFS_" + newUsernameForServer, MODE_PRIVATE);

                    newUserPrefs.edit()
                            .putString(KEY_DISPLAY_NAME, newDisplayName)
                            .putString(KEY_BIO, newBio)
                            .putBoolean(KEY_LOCATION_TRACKING, locationSwitch.isChecked())
                            .putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked())
                            .apply();

                    // Update current username in memory
                    username = newUsernameForServer;
                }

                Toast.makeText(PreferencesActivity.this,
                        "saved!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<UpdateUserRes> call, Throwable t) {
                Toast.makeText(PreferencesActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
