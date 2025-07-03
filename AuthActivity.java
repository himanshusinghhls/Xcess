package com.example.xcess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignUp;
    private SharedPreferences sharedPreferences;
    private static final String DOMAIN = "@niet.co.in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        etUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                //
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                String emailInput = charSequence.toString().toLowerCase();
                if (!emailInput.endsWith(DOMAIN)) {
                    etUsername.setText(emailInput + DOMAIN);
                    etUsername.setSelection(emailInput.length());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                //
            }
        });

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String registeredEmail = sharedPreferences.getString("registered_email", "");
            String registeredPassword = sharedPreferences.getString("registered_password", "");
            if (username.equals(registeredEmail) && password.equals(registeredPassword)) {
                sharedPreferences.edit().putBoolean("logged_in", true).apply();
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(AuthActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(AuthActivity.this, SignUpActivity.class));
        });
    }
}
