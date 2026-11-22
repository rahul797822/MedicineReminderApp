package com.example.medicinereminder.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.medicinereminder.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_reminder_channel";
    public static final String ACTION_MARK_TAKEN = "com.example.medicinereminder.MARK_TAKEN";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String medicineId = intent.getStringExtra("medicineId");
        String medicineName = intent.getStringExtra("medicineName");
        String dosage = intent.getStringExtra("dosage");
        String userId = intent.getStringExtra("userId");

        if (ACTION_MARK_TAKEN.equals(action)) {
            handleActionTaken(context, userId, medicineId);
            return;
        }

        // Standard Reminder fired
        if (userId != null && medicineId != null) {
            handleMissedMedicine(userId, medicineId, medicineName);
            // Reschedule for tomorrow
            rescheduleNextAlarm(context, intent);
        }

        showNotification(context, userId, medicineId, medicineName, dosage);
    }

    private void handleActionTaken(Context context, String userId, String medicineId) {
        if (userId == null || medicineId == null) return;

        DocumentReference medRef = FirebaseHelper.getFirestore()
                .collection("users").document(userId)
                .collection("medicines").document(medicineId);

        medRef.update("taken", true, "missedCount", 0)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Medicine marked as taken!", Toast.LENGTH_SHORT).show();
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.cancel(medicineId.hashCode()); // Close the notification
                });
    }

    private void rescheduleNextAlarm(Context context, Intent originalIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Use a new PendingIntent for tomorrow (+24 hours)
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, originalIntent.getIntExtra("requestCode", (int) System.currentTimeMillis()),
                originalIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long nextTrigger = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY;
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent);
        }
    }

    private void handleMissedMedicine(String userId, String medicineId, String medicineName) {
        DocumentReference medRef = FirebaseHelper.getFirestore()
                .collection("users").document(userId)
                .collection("medicines").document(medicineId);

        medRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Boolean taken = documentSnapshot.getBoolean("taken");
                if (Boolean.FALSE.equals(taken)) {
                    medRef.update("missedCount", FieldValue.increment(1)).addOnSuccessListener(a -> {
                        medRef.get().addOnSuccessListener(d -> {
                            Long count = d.getLong("missedCount");
                            if (count != null && count >= 3) triggerEmergencyAlert(userId, medicineName);
                        });
                    });
                }
                medRef.update("taken", false); // Reset for next dose
            }
        });
    }

    private void triggerEmergencyAlert(String userId, String medicineName) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("message", "Emergency: " + medicineName + " missed 3 times!");
        alert.put("timestamp", FieldValue.serverTimestamp());
        alert.put("status", "UNREAD");

        FirebaseHelper.getFirestore()
                .collection("users").document(userId)
                .collection("emergency").add(alert);
    }

    private void showNotification(Context context, String userId, String medicineId, String medicineName, String dosage) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent for the "Taken" button
        Intent takenIntent = new Intent(context, ReminderReceiver.class);
        takenIntent.setAction(ACTION_MARK_TAKEN);
        takenIntent.putExtra("userId", userId);
        takenIntent.putExtra("medicineId", medicineId);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(context, medicineId.hashCode(), takenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time for " + medicineName + " (" + dosage + ")")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_edit, "Mark as Taken", takenPendingIntent);

        notificationManager.notify(medicineId.hashCode(), builder.build());
    }
}
