package com.example.cse476assignment2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class PreferencesActivity extends AppCompatActivity {

    private static final String USER_PREFS = "USER_PREFS";
    private static final String KEY_BIO = "USER_BIO";
    private static final String KEY_LOCATION_TRACKING = "LOCATION_TRACKING";

    private EditText bioEditText;
    private SwitchCompat locationSwitch;
    private Button saveButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        sharedPreferences = getSharedPreferences(USER_PREFS, MODE_PRIVATE);

        bioEditText = findViewById(R.id.editTextBio);
        locationSwitch = findViewById(R.id.switchLocationTracking);
        saveButton = findViewById(R.id.buttonSavePreferences);

        bioEditText.setText(sharedPreferences.getString(KEY_BIO, ""));
        locationSwitch.setChecked(sharedPreferences.getBoolean(KEY_LOCATION_TRACKING, false));

        saveButton.setOnClickListener(v -> {
            sharedPreferences
                    .edit()
                    .putString(KEY_BIO, bioEditText.getText().toString())
                    .putBoolean(KEY_LOCATION_TRACKING, locationSwitch.isChecked())
                    .apply();

            Toast.makeText(PreferencesActivity.this, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
        });
    }
}
