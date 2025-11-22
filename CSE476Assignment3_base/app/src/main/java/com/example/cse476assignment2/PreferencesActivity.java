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
    private static final String KEY_LOCATION = "LOCATION_TRACKING";
    private static final String KEY_DARK_MODE = "DARK_MODE";
    private static final String KEY_EMAIL = "USER_EMAIL";

    private EditText displayNameInput, bioInput;
    private SwitchCompat locationSwitch, darkModeSwitch;
    private Button saveButton, backButton;

    private SharedPreferences userPrefs;
    private String username; // current username stored on server

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Load username from Intent or LOGIN_PREFS
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) {
            SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
            username = loginPrefs.getString("USERNAME", "default_user");
        }

        // User-specific SharedPreferences
        userPrefs = getSharedPreferences("USER_PREFS_" + username, MODE_PRIVATE);

        // Bind views
        displayNameInput = findViewById(R.id.editTextDisplayName);
        bioInput = findViewById(R.id.editTextBio);
        locationSwitch = findViewById(R.id.switchLocationTracking);
        darkModeSwitch = findViewById(R.id.switchDarkMode);
        saveButton = findViewById(R.id.buttonSavePreferences);
        backButton = findViewById(R.id.buttonBackPreferences);

        // Load existing values
        displayNameInput.setText(userPrefs.getString(KEY_DISPLAY_NAME, username));
        bioInput.setText(userPrefs.getString(KEY_BIO, ""));
        locationSwitch.setChecked(userPrefs.getBoolean(KEY_LOCATION, false));
        darkModeSwitch.setChecked(userPrefs.getBoolean(KEY_DARK_MODE, false));

        // Dark mode toggle
        darkModeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        });

        saveButton.setOnClickListener(v -> savePreferences());
        backButton.setOnClickListener(v -> finish());
    }

    private void savePreferences() {

        String newDisplayName = displayNameInput.getText().toString().trim();
        String newBio = bioInput.getText().toString().trim();
        String storedEmail = userPrefs.getString(KEY_EMAIL, null);

        // Determine if username change is requested
        String newUsernameForServer =
                (!newDisplayName.isEmpty() && !newDisplayName.equals(username))
                        ? newDisplayName
                        : null;

        // Bio: if empty, send null (server treats it as "no change")
        String bioForServer = newBio.isEmpty() ? null : newBio;

        // If nothing is changing on server → local only
        if (newUsernameForServer == null && bioForServer == null) {
            saveLocalPrefs(newDisplayName, newBio, storedEmail);
            Toast.makeText(this, "Preferences successfully updated.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build server request
        UpdateUserReq req = new UpdateUserReq(username, newUsernameForServer, bioForServer);

        saveButton.setEnabled(false);

        ApiClient.get().updateUser(req).enqueue(new Callback<UpdateUserRes>() {
            @Override
            public void onResponse(Call<UpdateUserRes> call, Response<UpdateUserRes> response) {
                saveButton.setEnabled(true);

                // HTTP-level error (409, 500, etc.)
                if (!response.isSuccessful() || response.body() == null) {

                    if (response.code() == 409) {
                        displayNameInput.setError("This username is already taken.");
                        Toast.makeText(PreferencesActivity.this,
                                "Username already taken.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(PreferencesActivity.this,
                            "Server error: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                UpdateUserRes body = response.body();

                // Server business errors
                if (!body.ok) {

                    if ("username_taken".equals(body.error)) {
                        displayNameInput.setError("This username is already taken.");
                        Toast.makeText(PreferencesActivity.this,
                                "Username already taken.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(PreferencesActivity.this,
                            "Update failed: " + body.error,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // SUCCESS: now update local preferences
                saveLocalPrefs(newDisplayName, newBio, storedEmail);

                // Handle username change locally
                if (newUsernameForServer != null) {

                    // Update LOGIN_PREFS
                    SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
                    loginPrefs.edit()
                            .putString("USERNAME", newUsernameForServer)
                            .apply();

                    // Create or load the new prefs file
                    SharedPreferences newUserPrefs =
                            getSharedPreferences("USER_PREFS_" + newUsernameForServer, MODE_PRIVATE);

                    SharedPreferences.Editor editor = newUserPrefs.edit();
                    editor.putString(KEY_DISPLAY_NAME, newDisplayName);
                    editor.putString(KEY_BIO, newBio);
                    editor.putBoolean(KEY_LOCATION, locationSwitch.isChecked());
                    editor.putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked());
                    if (storedEmail != null) editor.putString(KEY_EMAIL, storedEmail);
                    editor.apply();

                    // Update references
                    username = newUsernameForServer;
                    userPrefs = newUserPrefs;
                }

                Toast.makeText(PreferencesActivity.this,
                        "Preferences successfully updated.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<UpdateUserRes> call, Throwable t) {
                saveButton.setEnabled(true);
                Toast.makeText(PreferencesActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Save local preferences only after server success.
     */
    private void saveLocalPrefs(String displayName, String bio, String storedEmail) {
        SharedPreferences.Editor edit = userPrefs.edit();
        edit.putString(KEY_DISPLAY_NAME, displayName);
        edit.putString(KEY_BIO, bio);
        edit.putBoolean(KEY_LOCATION, locationSwitch.isChecked());
        edit.putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked());
        if (storedEmail != null) edit.putString(KEY_EMAIL, storedEmail);
        edit.apply();
    }
}
