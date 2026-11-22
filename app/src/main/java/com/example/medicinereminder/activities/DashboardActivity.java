package com.example.medicinereminder.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.User;
import com.example.medicinereminder.utils.AppDatabase;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private TextView pointsTv, levelTv, streakStatusTv, roleTv;
    private CalendarView calendarView;
    private ImageView calendarIcon;
    private MaterialCardView calendarCard;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Session check - Local only
        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", null);
        if (currentUserEmail == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        roleTv = findViewById(R.id.roleTv);
        pointsTv = findViewById(R.id.pointsTv);
        levelTv = findViewById(R.id.levelTv);
        streakStatusTv = findViewById(R.id.streakStatusTv);
        calendarView = findViewById(R.id.calendarView);
        calendarIcon = findViewById(R.id.calendarIcon);
        calendarCard = findViewById(R.id.calendarCard);

        findViewById(R.id.addMedicineBtn).setOnClickListener(v -> startActivity(new Intent(this, AddMedicineActivity.class)));
        findViewById(R.id.viewMedicinesBtn).setOnClickListener(v -> startActivity(new Intent(this, ViewMedicinesActivity.class)));
        findViewById(R.id.pharmacyBtn).setOnClickListener(v -> startActivity(new Intent(this, PharmacyActivity.class)));
        findViewById(R.id.doctorAccessBtn).setOnClickListener(v -> startActivity(new Intent(this, DoctorAccessActivity.class)));
        
        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            pref.edit().remove("current_user_email").remove("current_user_role").apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        calendarCard.setVisibility(View.VISIBLE);
        calendarIcon.setOnClickListener(v -> calendarCard.setVisibility(calendarCard.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            checkLocalHistory(selectedDate);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalData();
    }

    private void loadLocalData() {
        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int points = pref.getInt("points", 0);
        int streak = pref.getInt("streak_count", 0);
        String lastDate = pref.getString("last_taken_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        pointsTv.setText(String.valueOf(points));
        
        // Level logic
        int level;
        if (points >= 500) level = 5;
        else if (points >= 300) level = 4;
        else if (points >= 150) level = 3;
        else if (points >= 50) level = 2;
        else level = 1;
        levelTv.setText(String.valueOf(level));

        if (today.equals(lastDate)) {
            streakStatusTv.setText("🔥 STREAK: " + streak + " DAYS (COMPLETED)");
        } else {
            streakStatusTv.setText("⏰ STREAK: " + streak + " DAYS (PENDING)");
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUserByEmail(currentUserEmail);
            if (user != null) {
                runOnUiThread(() -> roleTv.setText(user.role.toUpperCase()));
            }
        });
    }

    private void checkLocalHistory(String date) {
        // We'll use a simple shared pref for historical 'taken' status per date
        SharedPreferences historyPref = getSharedPreferences("medicine_history", Context.MODE_PRIVATE);
        boolean wasTaken = historyPref.getBoolean(date, false);
        
        if (wasTaken) {
            Toast.makeText(this, "✅ Medicines were taken on " + date, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ No records for " + date, Toast.LENGTH_SHORT).show();
        }
    }
}
