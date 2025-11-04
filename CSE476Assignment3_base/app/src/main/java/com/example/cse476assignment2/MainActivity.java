package com.example.cse476assignment2;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CheckBox;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;


// The following code is written based on YouTube videos I watched
// ChatGPT was used to beautify code and add comments in places they were not in already
public class MainActivity extends AppCompatActivity {

    // Declare UI elements and shared preferences
    EditText username, password;
    Button loginButton;
    CheckBox rememberMe;
    SharedPreferences sharedPreferences;

    // Coordinates for MSU center (approx.)
    private static final double MSU_LAT = 42.7311;
    private static final double MSU_LON = -84.4875;
    private static final float RADIUS_METERS = 1000f;

    private static final int LOCATION_PERMISSION_REQUEST = 123;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the layout for the login screen
        setContentView(R.layout.activity_main);

        // Link UI elements to their XML IDs
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        rememberMe = findViewById(R.id.rememberMe);



        // Initialize SharedPreferences to store login data
        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        // Check if the user was previously logged in (auto-login feature)
        boolean loggedIn = sharedPreferences.getBoolean("LOGGED_IN", false);
        if (loggedIn) {
            // If logged in, retrieve the saved username and go directly to AccountActivity
            String savedUser = sharedPreferences.getString("USERNAME", "User");
            goToAccountPage(savedUser);
        }


        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String user = username.getText().toString();
            String pass = password.getText().toString();

            // Valid login if email ends with @msu.edu + password is 1234
            if (user.endsWith("@msu.edu") && pass.equals("1234")) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                // Save login state if checkbox to "Keep me logged in" is checked
                if (rememberMe.isChecked()) {
                    sharedPreferences.edit()
                            .putBoolean("LOGGED_IN", true)
                            .putString("USERNAME", user)
                            .apply();
                }

                // Navigate to AccountActivity with username passed as extra
                goToAccountPage(user);
            }
            // Error if username or password fields are empty
            else if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            }
            // Error for invalid credentials
            else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }


        });
    }

    // Save current input values if activity is killed (e.g., screen rotation)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("USERNAME_INPUT", username.getText().toString());
        outState.putString("PASSWORD_INPUT", password.getText().toString());
        outState.putBoolean("REMEMBER_ME", rememberMe.isChecked());
    }

    // Restore saved input values after rotation or recreation
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        username.setText(savedInstanceState.getString("USERNAME_INPUT", ""));
        password.setText(savedInstanceState.getString("PASSWORD_INPUT", ""));
        rememberMe.setChecked(savedInstanceState.getBoolean("REMEMBER_ME", false));
    }

    // Save temporary input values when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USERNAME_INPUT", username.getText().toString());
        editor.putString("PASSWORD_INPUT", password.getText().toString());
        editor.putBoolean("REMEMBER_ME", rememberMe.isChecked());
        editor.apply();
    }

    // Restore input values when activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        username.setText(sharedPreferences.getString("USERNAME_INPUT", ""));
        password.setText(sharedPreferences.getString("PASSWORD_INPUT", ""));
        rememberMe.setChecked(sharedPreferences.getBoolean("REMEMBER_ME", false));
    }

    // Helper method: move to AccountActivity, passing the username
    private void goToAccountPage(String username) {
        Intent intent = new Intent(MainActivity.this, AccountActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }


}