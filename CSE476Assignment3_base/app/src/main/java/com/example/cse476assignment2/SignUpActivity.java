package com.example.cse476assignment2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutUsername, layoutPassword, layoutConfirm;
    private TextInputEditText inputEmail, inputUsername, inputBio, inputPassword, inputConfirm;
    private CheckBox checkTos, checkData;
    private Button buttonCreate, buttonViewTerms;

    // Track whether user has interacted with fields
    private boolean touchedEmail = false;
    private boolean touchedUsername = false;
    private boolean touchedPassword = false;
    private boolean touchedConfirm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Layouts
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirm = findViewById(R.id.layoutConfirm);

        // Inputs
        inputEmail = findViewById(R.id.inputEmail);
        inputUsername = findViewById(R.id.inputUsername);
        inputBio = findViewById(R.id.inputBio);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirm = findViewById(R.id.inputConfirm);

        // Checks and buttons
        checkTos = findViewById(R.id.checkTos);
        checkData = findViewById(R.id.checkData);
        buttonCreate = findViewById(R.id.buttonCreateAccount);
        buttonViewTerms = findViewById(R.id.buttonViewTerms);

        // Watchers: validate as user types
        TextWatcher watcher = new SimpleWatcher(this::validateAllLive);
        inputEmail.addTextChangedListener(watcher);
        inputUsername.addTextChangedListener(watcher);
        inputPassword.addTextChangedListener(watcher);
        inputConfirm.addTextChangedListener(watcher);

        // Mark fields as "touched" once they lose focus
        inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) touchedEmail = true;
            validateAllLive();
        });

        inputUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) touchedUsername = true;
            validateAllLive();
        });

        inputPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) touchedPassword = true;
            validateAllLive();
        });

        inputConfirm.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) touchedConfirm = true;
            validateAllLive();
        });

        // Consents also affect CTA validity
        checkTos.setOnCheckedChangeListener((b, c) -> validateAllLive());
        checkData.setOnCheckedChangeListener((b, c) -> validateAllLive());

        validateAllLive();

        buttonViewTerms.setOnClickListener(
                v -> startActivity(new android.content.Intent(this, TermsActivity.class))
        );

        buttonCreate.setOnClickListener(v -> {
            // On submit, force showing required errors if empty
            touchedEmail = touchedUsername = touchedPassword = touchedConfirm = true;

            boolean ok = validateAllLive();
            if (!ok) return;

            // UI-only success for now
            android.widget.Toast.makeText(
                    this,
                    getString(R.string.signup_success),
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            finish();
        });
    }

    /**
     * Runs all validations and updates CTA.
     * @return true if the whole form is valid.
     */
    private boolean validateAllLive() {
        boolean emailOk = validateEmail();
        boolean usernameOk = validateUsername();
        boolean passwordOk = validatePassword();
        boolean confirmOk = validateConfirm();
        boolean consentOk = validateConsents();

        boolean allOk = emailOk && usernameOk && passwordOk && confirmOk && consentOk;
        buttonCreate.setEnabled(allOk);
        return allOk;
    }

    private boolean validateEmail() {
        String email = get(inputEmail);

        // Do not show "required" until touched
        if (email.isEmpty()) {
            layoutEmail.setError(touchedEmail ? "Email is required." : null);
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Invalid email format.");
            return false;
        }

        if (!email.endsWith("@msu.edu")) {
            layoutEmail.setError("Email must end with @msu.edu.");
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
            layoutUsername.setError("Username must be at least 3 characters.");
            return false;
        }

        // Placeholder UI: uniqueness will be checked on server later
        layoutUsername.setError(null);
        return true;
    }

    private boolean validatePassword() {
        String pass = inputPassword.getText() == null ? "" : inputPassword.getText().toString();

        if (pass.isEmpty()) {
            layoutPassword.setError(touchedPassword ? "Password is required." : null);
            return false;
        }

        if (!validPassword(pass)) {
            layoutPassword.setError("Password must be 8+ chars with letters and digits.");
            return false;
        }

        layoutPassword.setError(null);
        return true;
    }

    private boolean validateConfirm() {
        String pass = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
        String conf = inputConfirm.getText() == null ? "" : inputConfirm.getText().toString();

        if (conf.isEmpty()) {
            layoutConfirm.setError(touchedConfirm ? "Please confirm your password." : null);
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
        // No inline error widget for checkboxes in this layout,
        // so we just block CTA if not checked.
        return checkTos.isChecked() && checkData.isChecked();
    }

    private String get(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean validPassword(String p) {
        if (p.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char ch : p.toCharArray()) {
            if (Character.isLetter(ch)) hasLetter = true;
            if (Character.isDigit(ch)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    static class SimpleWatcher implements TextWatcher {
        private final Runnable cb;
        SimpleWatcher(Runnable cb) { this.cb = cb; }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) { cb.run(); }
        public void afterTextChanged(Editable s) {}
    }
}
