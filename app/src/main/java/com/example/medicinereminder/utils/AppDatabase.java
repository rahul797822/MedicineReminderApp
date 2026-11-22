package com.example.medicinereminder.utils;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.medicinereminder.models.DoctorAccess;
import com.example.medicinereminder.models.Medicine;
import com.example.medicinereminder.models.PharmacyRequest;
import com.example.medicinereminder.models.User;

@Database(entities = {Medicine.class, User.class, PharmacyRequest.class, DoctorAccess.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract MedicineDao medicineDao();
    public abstract PharmacyRequestDao pharmacyRequestDao();
    public abstract DoctorAccessDao doctorAccessDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "medicine_reminder_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
