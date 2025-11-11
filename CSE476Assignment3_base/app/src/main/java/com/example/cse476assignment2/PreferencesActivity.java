package com.example.cse476assignment2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class PreferencesActivity extends AppCompatActivity {

    private static final String KEY_BIO = "USER_BIO";
    private static final String KEY_LOCATION_TRACKING = "LOCATION_TRACKING";
    private static final String KEY_DARK_MODE = "DARK_MODE";

    private EditText bioEditText;
    private SwitchCompat locationSwitch;
    private SwitchCompat darkModeSwitch;
    private Button saveButton;
    private Button backButton;
    private SharedPreferences sharedPreferences;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Get username from intent
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) {
            username = "default_user"; // Fallback if no username provided
        }

        // Use per-user SharedPreferences
        sharedPreferences = getSharedPreferences("USER_PREFS_" + username, MODE_PRIVATE);

        bioEditText = findViewById(R.id.editTextBio);
        locationSwitch = findViewById(R.id.switchLocationTracking);
        darkModeSwitch = findViewById(R.id.switchDarkMode);
        saveButton = findViewById(R.id.buttonSavePreferences);
        backButton = findViewById(R.id.buttonBackPreferences);

        bioEditText.setText(sharedPreferences.getString(KEY_BIO, ""));
        locationSwitch.setChecked(sharedPreferences.getBoolean(KEY_LOCATION_TRACKING, false));
        darkModeSwitch.setChecked(sharedPreferences.getBoolean(KEY_DARK_MODE, false));

        // Immediate dark mode toggle without saving
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        saveButton.setOnClickListener(v -> {
            sharedPreferences
                    .edit()
                    .putString(KEY_BIO, bioEditText.getText().toString())
                    .putBoolean(KEY_LOCATION_TRACKING, locationSwitch.isChecked())
                    .putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked())
                    .apply();

            Toast.makeText(PreferencesActivity.this, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
        });

        backButton.setOnClickListener(v -> finish());
    }
}
