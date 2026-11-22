package com.example.medicinereminder.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.User;
import com.example.medicinereminder.utils.AppDatabase;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEt, passwordEt;
    private RadioGroup roleRg;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Views
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        roleRg = findViewById(R.id.roleRg);
        progressBar = findViewById(R.id.progressBar);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView goToLoginTv = findViewById(R.id.goToLoginTv);

        if (registerBtn != null) {
            registerBtn.setOnClickListener(v -> registerUser());
        }

        if (goToLoginTv != null) {
            goToLoginTv.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void registerUser() {
        // Clear errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();
        
        int selectedRoleId = roleRg.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRoleRb = findViewById(selectedRoleId);
        String role = selectedRoleRb.getText().toString().toLowerCase();

        // Validation
        boolean isValid = true;
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address");
            isValid = false;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Min 6 characters required");
            isValid = false;
        }

        if (!isValid) return;

        progressBar.setVisibility(View.VISIBLE);

        // Room Database operations must be on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            User existingUser = db.userDao().getUserByEmail(email);

            if (existingUser != null) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emailLayout.setError("Email already registered");
                });
            } else {
                User newUser = new User(email, password, role, System.currentTimeMillis());
                db.userDao().insert(newUser);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Registered Locally Successfully", Toast.LENGTH_SHORT).show();
                    
                    // Save session in SharedPreferences
                    SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    pref.edit()
                        .putString("current_user_email", email)
                        .putString("current_user_role", role)
                        .apply();

                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                });
            }
        });
    }
}
