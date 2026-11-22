package com.example.medicinereminder.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.medicinereminder.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmergencyManager {

    public static void trackMissedDose(Context context) {

        String userId = FirebaseHelper.getAuth().getCurrentUser().getUid();

        var ref = FirebaseHelper.getFirestore()
                .collection("users")
                .document(userId)
                .collection("emergency")
                .document("alertData");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        ref.get().addOnSuccessListener(doc -> {

            int missedCount = 0;

            if (doc.exists()) {
                Long count = doc.getLong("missedCount");
                if (count != null) missedCount = count.intValue();
            }

            missedCount++;

            Map<String, Object> data = new HashMap<>();
            data.put("missedCount", missedCount);
            data.put("lastMissedDate", today);

            ref.set(data);

            if (missedCount >= 3) {
                triggerEmergency(context);
            }
        });
    }

    public static void resetMissedCount() {

        String userId = FirebaseHelper.getAuth().getCurrentUser().getUid();

        FirebaseHelper.getFirestore()
                .collection("users")
                .document(userId)
                .collection("emergency")
                .document("alertData")
                .update("missedCount", 0);
    }

    private static void triggerEmergency(Context context) {

        Toast.makeText(context,
                "🚨 Emergency! Missed medicine 3 times",
                Toast.LENGTH_LONG).show();

        sendFCMNotification(context);
    }

    private static void sendFCMNotification(Context context) {

        // ⚠️ In real apps, use backend (Node.js / Firebase Functions)
        // Here we simulate for demo

        Toast.makeText(context,
                "📡 FCM Alert Triggered",
                Toast.LENGTH_SHORT).show();
    }
}