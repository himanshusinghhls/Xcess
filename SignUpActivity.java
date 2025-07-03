package com.example.xcess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etPasskey;
    private static final String REQUIRED_PASSKEY = "MINI_PROJECT";
    private static final String DOMAIN = "@niet.co.in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etSignUpUsername);
        etPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etSignUpConfirmPassword);
        etPasskey = findViewById(R.id.etPasskey);
        Button btnRegister = findViewById(R.id.btnRegister);

        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                //
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                String emailInput = charSequence.toString().toLowerCase();
                if (!emailInput.endsWith(DOMAIN)) {
                    etEmail.setText(emailInput + DOMAIN);
                    etEmail.setSelection(emailInput.length());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
            }
        });
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String passkey = etPasskey.getText().toString().trim();

        if (!email.endsWith(DOMAIN)) {
            Toast.makeText(this, "Email must end with @niet.co.in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!REQUIRED_PASSKEY.equals(passkey)) {
            Toast.makeText(this, "Incorrect passkey", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("registered_email", email)
                .putString("registered_password", password)
                .apply();

        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignUpActivity.this, AuthActivity.class));
        finish();
    }
}
