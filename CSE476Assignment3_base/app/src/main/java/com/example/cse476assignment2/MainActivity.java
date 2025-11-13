package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.cse476assignment2.net.ApiClient;
import com.example.cse476assignment2.net.ApiService;
import com.example.cse476assignment2.model.Req.LoginReq;
import com.example.cse476assignment2.model.Res.LoginRes;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    EditText username, password;
    Button loginButton, newMemberButton;
    CheckBox rememberMe;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        rememberMe = findViewById(R.id.rememberMe);
        newMemberButton = findViewById(R.id.buttonNewMember);

        sharedPreferences = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);

        // Auto-login only if we have both USERNAME and PASSWORD
        boolean loggedIn = sharedPreferences.getBoolean("LOGGED_IN", false);
        String savedUser = sharedPreferences.getString("USERNAME", null);
        String savedPass = sharedPreferences.getString("PASSWORD", null);

        if (loggedIn && savedUser != null && savedPass != null) {
            applyUserDarkModePreference(savedUser);
            goToAccountPage(savedUser);
        }


        loginButton.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiService api = ApiClient.get();
            LoginReq req = new LoginReq(user, pass);

            api.login(req).enqueue(new Callback<LoginRes>() {
                @Override
                public void onResponse(Call<LoginRes> call, Response<LoginRes> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginRes res = response.body();
                        if (res.ok && res.user != null) {
                            Toast.makeText(MainActivity.this,
                                    "Welcome " + res.user.username,
                                    Toast.LENGTH_SHORT).show();

                            // NEW: Always store USERNAME + PASSWORD for this session
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("USERNAME", res.user.username); // for account / posts
                            editor.putString("PASSWORD", pass);              // for server auth (upload, feed)

                            // NEW: "Remember me" only controls auto-login flag
                            if (rememberMe.isChecked()) {
                                editor.putBoolean("LOGGED_IN", true);
                            } else {
                                editor.putBoolean("LOGGED_IN", false);
                            }

                            editor.apply();

                            applyUserDarkModePreference(res.user.username);
                            goToAccountPage(res.user.username);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Login failed: " + res.error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Server error (" + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginRes> call, Throwable t) {
                    Toast.makeText(MainActivity.this,
                            "Network error: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        newMemberButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("USERNAME_INPUT", username.getText().toString());
        outState.putString("PASSWORD_INPUT", password.getText().toString());
        outState.putBoolean("REMEMBER_ME", rememberMe.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        username.setText(savedInstanceState.getString("USERNAME_INPUT", ""));
        password.setText(savedInstanceState.getString("PASSWORD_INPUT", ""));
        rememberMe.setChecked(savedInstanceState.getBoolean("REMEMBER_ME", false));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USERNAME_INPUT", username.getText().toString());
        editor.putString("PASSWORD_INPUT", password.getText().toString());
        editor.putBoolean("REMEMBER_ME", rememberMe.isChecked());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        username.setText(sharedPreferences.getString("USERNAME_INPUT", ""));
        password.setText(sharedPreferences.getString("PASSWORD_INPUT", ""));
        rememberMe.setChecked(sharedPreferences.getBoolean("REMEMBER_ME", false));
    }

    private void goToAccountPage(String username) {
        Intent intent = new Intent(MainActivity.this, AccountActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    private void applyUserDarkModePreference(String username) {
        SharedPreferences userPrefs = getSharedPreferences("USER_PREFS_" + username, MODE_PRIVATE);
        boolean isDarkMode = userPrefs.getBoolean("DARK_MODE", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
