package com.example.medicinereminder.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.Medicine;
import com.example.medicinereminder.utils.AppDatabase;
import com.example.medicinereminder.utils.ReminderReceiver;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText nameEt, dosageEt, timeEt, quantityEt;
    private int selectedHour, selectedMinute;
    private String currentUserEmail;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveMedicine();
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", null);

        nameEt = findViewById(R.id.nameEt);
        dosageEt = findViewById(R.id.dosageEt);
        timeEt = findViewById(R.id.timeEt);
        quantityEt = findViewById(R.id.quantityEt);
        Button saveBtn = findViewById(R.id.saveBtn);

        timeEt.setFocusable(false);
        timeEt.setOnClickListener(v -> showTimePicker());

        saveBtn.setOnClickListener(v -> handleSaveAction());
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minuteOfHour;
                    timeEt.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour));
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void handleSaveAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                saveMedicine();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            saveMedicine();
        }
    }

    private void saveMedicine() {
        String name = nameEt.getText().toString().trim();
        String dosage = dosageEt.getText().toString().trim();
        String time = timeEt.getText().toString().trim();
        String quantityStr = quantityEt.getText().toString().trim();

        if (name.isEmpty() || dosage.isEmpty() || time.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityStr);

        Executors.newSingleThreadExecutor().execute(() -> {
            // Updated constructor with userEmail
            Medicine medicine = new Medicine(currentUserEmail, name, dosage, time, quantity, false, 0, System.currentTimeMillis());
            AppDatabase.getInstance(this).medicineDao().insert(medicine);
            
            runOnUiThread(() -> {
                scheduleNotification(name.hashCode(), name, dosage);
                Toast.makeText(this, "Medicine Added Locally & Reminder Set", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void scheduleNotification(int id, String name, String dosage) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("medicineId", String.valueOf(id));
        intent.putExtra("medicineName", name);
        intent.putExtra("dosage", dosage);
        intent.putExtra("userEmail", currentUserEmail);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
}
