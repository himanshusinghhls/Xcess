package com.example.xcess;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private Button btnAccess, btnLock, btnSignIn;
    private TextView tvAppNameMain, tvAppVersion, tvTimer, tvLockUnlockLog, tvRecentActionsLabel;
    private int authAttempts = 0;
    private boolean isLockedOut = false;
    private CountDownTimer countDownTimer;

    private List<String> lockUnlockLog = new ArrayList<>();
    private static final int MAX_LOG_SIZE = 5;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        btnAccess = findViewById(R.id.btnAccess);
        btnLock = findViewById(R.id.btnLock);
        btnSignIn = findViewById(R.id.btnSignIn);
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        tvAppNameMain = findViewById(R.id.tvAppNameMain);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tvTimer = findViewById(R.id.tvTimer);
        tvLockUnlockLog = findViewById(R.id.tvLockUnlockLog);
        tvRecentActionsLabel = findViewById(R.id.tvRecentActionsLabel);

        btnSignIn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

        btnSignIn.setOnClickListener(v -> {
            Log.d("Sign In", "Sign In button clicked!");
            Toast.makeText(MainActivity.this, "Redirecting to Sign In...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        themeSwitch.setChecked(darkMode);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Toast.makeText(this, isChecked ? "Dark Mode Enabled" : "Light Mode Enabled", Toast.LENGTH_SHORT).show();
            updateThemeElements(isChecked);
        });

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText("Version " + versionName);
        } catch (Exception e) {
            tvAppVersion.setText("Version N/A");
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Toast.makeText(getApplicationContext(), "Error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                Toast.makeText(getApplicationContext(), "Access Granted!", Toast.LENGTH_SHORT).show();
                authAttempts = 0;
                unlockDoor();
            }

            @Override
            public void onAuthenticationFailed() {
                authAttempts++;
                if (authAttempts >= 3) {
                    isLockedOut = true;
                    Toast.makeText(getApplicationContext(), "Too many attempts. Try again in 2 minutes.", Toast.LENGTH_LONG).show();
                    startCountdownTimer();
                    btnAccess.setEnabled(false);
                    tvTimer.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), "Authentication Failed. Attempt " + authAttempts + " of 3.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your fingerprint to access secure content")
                .setNegativeButtonText("Cancel")
                .build();
        btnAccess.setOnClickListener(view -> biometricPrompt.authenticate(promptInfo));
        btnLock.setOnClickListener(v -> lockDoor());
        loadLockUnlockLog();
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(120000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                int secs = seconds % 60;
                tvTimer.setText(String.format("Try again in %02d:%02d", minutes, secs));
            }

            @Override
            public void onFinish() {
                authAttempts = 0;
                isLockedOut = false;
                btnAccess.setEnabled(true);
                tvTimer.setVisibility(View.GONE);
            }
        };
        countDownTimer.start();
    }

    private void lockDoor() {
        new Thread(() -> {
            try {
                String esp32Url = "http://192.168.86.170/lock";

                java.net.URL url = new java.net.URL(esp32Url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Door locked successfully!", Toast.LENGTH_SHORT).show();
                        logLockUnlockAction("Locked");
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to lock door", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error contacting ESP32: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void unlockDoor() {
        new Thread(() -> {
            try {
                String esp32Url = "http://192.168.86.170/unlock";

                java.net.URL url = new java.net.URL(esp32Url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Door unlocked successfully!", Toast.LENGTH_SHORT).show();
                        logLockUnlockAction("Unlocked");
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to unlock door", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error contacting ESP32: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void logLockUnlockAction(String action) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = action + " at " + timestamp;

        if (lockUnlockLog.size() >= MAX_LOG_SIZE) {
            lockUnlockLog.remove(0);
        }
        lockUnlockLog.add(logEntry);
        saveLockUnlockLog();
        updateLockUnlockLogDisplay();
    }

    private void updateLockUnlockLogDisplay() {
        StringBuilder logText = new StringBuilder();
        for (String logEntry : lockUnlockLog) {
            logText.append(logEntry).append("\n");
        }
        tvLockUnlockLog.setText(logText.toString());
    }

    private void saveLockUnlockLog() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("lock_unlock_log", new HashSet<>(lockUnlockLog));
        editor.apply();
    }

    private void loadLockUnlockLog() {
        Set<String> savedLogs = sharedPreferences.getStringSet("lock_unlock_log", new HashSet<>());
        lockUnlockLog.clear();
        lockUnlockLog.addAll(savedLogs);
        updateLockUnlockLogDisplay();
    }

    private void updateThemeElements(boolean isDarkMode) {
        int logTextColor = isDarkMode ? R.color.logTextColorDark : R.color.logTextColorLight;
        tvLockUnlockLog.setTextColor(getResources().getColor(logTextColor));
        int themeSwitchTextColor = isDarkMode ? R.color.themeSwitchTextColorDark : R.color.themeSwitchTextColorLight;
        tvAppNameMain.setTextColor(getResources().getColor(themeSwitchTextColor));
        tvRecentActionsLabel.setTextColor(getResources().getColor(themeSwitchTextColor));
        tvAppVersion.setTextColor(getResources().getColor(themeSwitchTextColor));
        tvLockUnlockLog.setTextColor(getResources().getColor(logTextColor));
    }
}
