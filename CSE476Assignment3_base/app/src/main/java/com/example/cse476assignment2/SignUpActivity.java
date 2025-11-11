package com.example.cse476assignment2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputEmail, inputUsername, inputBio, inputPassword, inputConfirm;
    private CheckBox checkTos, checkData;
    private Button buttonCreate, buttonViewTerms;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        inputEmail    = findViewById(R.id.inputEmail);
        inputUsername = findViewById(R.id.inputUsername);
        inputBio      = findViewById(R.id.inputBio);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirm  = findViewById(R.id.inputConfirm);
        checkTos      = findViewById(R.id.checkTos);
        checkData     = findViewById(R.id.checkData);
        buttonCreate  = findViewById(R.id.buttonCreateAccount);
        buttonViewTerms = findViewById(R.id.buttonViewTerms);

        TextWatcher w = new SimpleWatcher(this::updateCta);
        inputEmail.addTextChangedListener(w);
        inputUsername.addTextChangedListener(w);
        inputPassword.addTextChangedListener(w);
        inputConfirm.addTextChangedListener(w);
        checkTos.setOnCheckedChangeListener((b,c)->updateCta());
        checkData.setOnCheckedChangeListener((b,c)->updateCta());
        updateCta();

        buttonViewTerms.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, TermsActivity.class)));

        buttonCreate.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String user  = inputUsername.getText().toString().trim();
            String pass  = inputPassword.getText().toString();
            String conf  = inputConfirm.getText().toString();

            // Email must end with @msu.edu
            if (!email.endsWith("@msu.edu")) {
                inputEmail.setError("Email must end with @msu.edu");
                inputEmail.requestFocus();
                return;
            }

            // Username required (uniqueness = server later)
            if (user.isEmpty()) {
                inputUsername.setError(getString(R.string.err_username_required));
                inputUsername.requestFocus();
                return;
            }

            // Password rules
            if (!validPassword(pass)) {
                inputPassword.setError(getString(R.string.err_password_invalid));
                inputPassword.requestFocus();
                return;
            }

            // Password match — show inline guidance
            if (!pass.equals(conf)) {
                inputConfirm.setError(getString(R.string.err_password_mismatch));
                inputConfirm.requestFocus();
                Toast.makeText(this, getString(R.string.err_password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkTos.isChecked() || !checkData.isChecked()) {
                Toast.makeText(this, getString(R.string.err_consent_required), Toast.LENGTH_SHORT).show();
                return;
            }

            // UI only
            Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateCta() {
        boolean ok = !get(inputEmail).isEmpty()
                && !get(inputUsername).isEmpty()
                && !get(inputPassword).isEmpty()
                && !get(inputConfirm).isEmpty()
                && checkTos.isChecked()
                && checkData.isChecked();
        buttonCreate.setEnabled(ok);
    }

    private String get(EditText e){ return e.getText().toString().trim(); }

    private boolean validPassword(String p) {
        if (p.length() < 8) return false;
        boolean hasL=false, hasD=false;
        for (char ch : p.toCharArray()) {
            if (Character.isLetter(ch)) hasL = true;
            if (Character.isDigit(ch))  hasD = true;
        }
        return hasL && hasD;
    }

    static class SimpleWatcher implements TextWatcher {
        private final Runnable cb; SimpleWatcher(Runnable cb){this.cb=cb;}
        public void beforeTextChanged(CharSequence s,int a,int b,int c){}
        public void onTextChanged(CharSequence s,int a,int b,int c){cb.run();}
        public void afterTextChanged(Editable s){}
    }
}
