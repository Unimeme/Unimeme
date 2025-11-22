package com.example.cse476assignment2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cse476assignment2.model.Req.SignUpReq;
import com.example.cse476assignment2.model.Res.SignUpRes;
import com.example.cse476assignment2.net.ApiClient;
import com.example.cse476assignment2.net.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutUsername, layoutPassword, layoutConfirm;
    private TextInputEditText inputEmail, inputUsername, inputBio, inputPassword, inputConfirm;
    private CheckBox checkTos, checkData;
    private Button buttonCreate, buttonViewTerms;

    private boolean touchedEmail = false;
    private boolean touchedUsername = false;
    private boolean touchedPassword = false;
    private boolean touchedConfirm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirm = findViewById(R.id.layoutConfirm);

        inputEmail = findViewById(R.id.inputEmail);
        inputUsername = findViewById(R.id.inputUsername);
        inputBio = findViewById(R.id.inputBio);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirm = findViewById(R.id.inputConfirm);

        checkTos = findViewById(R.id.checkTos);
        checkData = findViewById(R.id.checkData);
        buttonCreate = findViewById(R.id.buttonCreateAccount);
        buttonViewTerms = findViewById(R.id.buttonViewTerms);

        // Watchers
        TextWatcher watcher = new SimpleWatcher(this::validateAllLive);
        inputEmail.addTextChangedListener(watcher);
        inputUsername.addTextChangedListener(watcher);
        inputPassword.addTextChangedListener(watcher);
        inputConfirm.addTextChangedListener(watcher);

        // On focus lost -> mark touched
        inputEmail.setOnFocusChangeListener((v, f) -> { if (!f) touchedEmail = true; validateAllLive(); });
        inputUsername.setOnFocusChangeListener((v, f) -> { if (!f) touchedUsername = true; validateAllLive(); });
        inputPassword.setOnFocusChangeListener((v, f) -> { if (!f) touchedPassword = true; validateAllLive(); });
        inputConfirm.setOnFocusChangeListener((v, f) -> { if (!f) touchedConfirm = true; validateAllLive(); });

        checkTos.setOnCheckedChangeListener((b,c)->validateAllLive());
        checkData.setOnCheckedChangeListener((b,c)->validateAllLive());

        validateAllLive();

        buttonViewTerms.setOnClickListener(
                v -> startActivity(new android.content.Intent(this, TermsActivity.class))
        );

        buttonCreate.setOnClickListener(v -> submitToServer());
    }

    private void submitToServer() {
        touchedEmail = touchedUsername = touchedPassword = touchedConfirm = true;

        if (!validateAllLive()) return;

        String email = get(inputEmail);
        String username = get(inputUsername);
        String bio = get(inputBio);
        String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();

        // Server does NOT take email!
        SignUpReq req = new SignUpReq(username, password, bio, null);

        buttonCreate.setEnabled(false);

        ApiService api = ApiClient.get();
        api.signUp(req).enqueue(new Callback<SignUpRes>() {
            @Override
            public void onResponse(Call<SignUpRes> call, Response<SignUpRes> response) {
                buttonCreate.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SignUpActivity.this,
                            "Signup failed (" + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SignUpRes res = response.body();

                if (res.ok) {
                    Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();

                    SharedPreferences prefs =
                            getSharedPreferences("USER_PREFS_" + username, MODE_PRIVATE);
                    prefs.edit()
                            .putString("USER_DISPLAY_NAME", username)
                            .putString("USER_EMAIL", email)
                            .apply();

                    finish();
                } else {
                    if ("username_taken".equals(res.error)) {
                        layoutUsername.setError("Username already taken.");
                    }
                    Toast.makeText(SignUpActivity.this,
                            "Signup failed: " + res.error,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SignUpRes> call, Throwable t) {
                buttonCreate.setEnabled(true);
                Toast.makeText(SignUpActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateAllLive() {
        boolean emailOk = validateEmail();
        boolean usernameOk = validateUsername();
        boolean passwordOk = validatePassword();
        boolean confirmOk = validateConfirm();
        boolean consentOk = validateConsents();

        boolean all = emailOk && usernameOk && passwordOk && confirmOk && consentOk;
        buttonCreate.setEnabled(all);
        return all;
    }

    private boolean validateEmail() {
        String email = get(inputEmail);
        if (email.isEmpty()) {
            layoutEmail.setError(touchedEmail ? "Email is required." : null);
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Invalid email format.");
            return false;
        }
        if (!email.endsWith("@msu.edu")) {
            layoutEmail.setError("Must end with @msu.edu");
            return false;
        }
        layoutEmail.setError(null);
        return true;
    }

    private boolean validateUsername() {
        String user = get(inputUsername);
        if (user.isEmpty()) {
            layoutUsername.setError(touchedUsername ? "Username is required." : null);
            return false;
        }
        if (user.length() < 3) {
            layoutUsername.setError("At least 3 characters.");
            return false;
        }
        layoutUsername.setError(null);
        return true;
    }

    private boolean validatePassword() {
        String pass = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
        if (pass.isEmpty()) {
            layoutPassword.setError(touchedPassword ? "Password required." : null);
            return false;
        }
        if (!validPassword(pass)) {
            layoutPassword.setError("8+ chars with letters & digits.");
            return false;
        }
        layoutPassword.setError(null);
        return true;
    }

    private boolean validateConfirm() {
        String pass = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
        String conf = inputConfirm.getText() == null ? "" : inputConfirm.getText().toString();
        if (conf.isEmpty()) {
            layoutConfirm.setError(touchedConfirm ? "Confirm password." : null);
            return false;
        }
        if (!conf.equals(pass)) {
            layoutConfirm.setError("Passwords do not match.");
            return false;
        }
        layoutConfirm.setError(null);
        return true;
    }

    private boolean validateConsents() {
        return checkTos.isChecked() && checkData.isChecked();
    }

    private String get(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean validPassword(String p) {
        if (p.length() < 8) return false;
        boolean hasL=false, hasD=false;
        for (char ch : p.toCharArray()) {
            if (Character.isLetter(ch)) hasL = true;
            if (Character.isDigit(ch)) hasD = true;
        }
        return hasL && hasD;
    }

    static class SimpleWatcher implements TextWatcher {
        private final Runnable cb;
        SimpleWatcher(Runnable cb) { this.cb = cb; }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) { cb.run(); }
        public void afterTextChanged(Editable s) {}
    }
}
