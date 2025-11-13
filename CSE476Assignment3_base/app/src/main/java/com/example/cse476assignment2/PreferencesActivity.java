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
    private static final String KEY_EMAIL = "USER_EMAIL"; // school email

    private EditText displayNameInput, bioInput;
    private SwitchCompat locationSwitch, darkModeSwitch;
    private Button saveButton, backButton;

    private SharedPreferences userPrefs;
    private String username; // login/server username

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
        displayNameInput.setText(userPrefs.getString(KEY_DISPLAY_NAME, ""));
        bioInput.setText(userPrefs.getString(KEY_BIO, ""));
        locationSwitch.setChecked(userPrefs.getBoolean(KEY_LOCATION, false));
        darkModeSwitch.setChecked(userPrefs.getBoolean(KEY_DARK_MODE, false));

        // Dark mode switch
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

        // Save local preferences first
        userPrefs.edit()
                .putString(KEY_DISPLAY_NAME, newDisplayName)
                .putString(KEY_BIO, newBio)
                .putBoolean(KEY_LOCATION, locationSwitch.isChecked())
                .putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked())
                .apply();

        // Determine if username change is requested
        String newUsernameForServer =
                (!newDisplayName.isEmpty() && !newDisplayName.equals(username))
                        ? newDisplayName
                        : null;

        String bioForServer = newBio.isEmpty() ? null : newBio;

        // Build server request
        UpdateUserReq req = new UpdateUserReq(username, newUsernameForServer, bioForServer);

        ApiClient.get().updateUser(req).enqueue(new Callback<UpdateUserRes>() {
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
                    String msg = "Update failed: " + body.error;
                    Toast.makeText(PreferencesActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Handle username change
                if (newUsernameForServer != null) {

                    SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
                    loginPrefs.edit()
                            .putString("USERNAME", newUsernameForServer)
                            .apply();

                    SharedPreferences newUserPrefs =
                            getSharedPreferences("USER_PREFS_" + newUsernameForServer, MODE_PRIVATE);

                    SharedPreferences.Editor e = newUserPrefs.edit();
                    e.putString(KEY_DISPLAY_NAME, newDisplayName);
                    e.putString(KEY_BIO, newBio);
                    e.putBoolean(KEY_LOCATION, locationSwitch.isChecked());
                    e.putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked());

                    // Keep user's school email tied to new username
                    if (storedEmail != null) {
                        e.putString(KEY_EMAIL, storedEmail);
                    }

                    e.apply();

                    // Update local reference
                    username = newUsernameForServer;
                    userPrefs = newUserPrefs;
                }

                Toast.makeText(PreferencesActivity.this,
                        "Preferences successfully updated.",
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
