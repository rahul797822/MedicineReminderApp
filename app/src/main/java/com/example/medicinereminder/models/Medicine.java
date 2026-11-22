package com.example.medicinereminder.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medicines")
public class Medicine {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String userEmail; // Link medicine to a specific local user
    public String name;
    public String dosage;
    public String time;
    public int quantity;
    public boolean taken;
    public int missedCount;
    public long createdAt;

    public Medicine(String userEmail, String name, String dosage, String time, int quantity, boolean taken, int missedCount, long createdAt) {
        this.userEmail = userEmail;
        this.name = name;
        this.dosage = dosage;
        this.time = time;
        this.quantity = quantity;
        this.taken = taken;
        this.missedCount = missedCount;
        this.createdAt = createdAt;
    }
}
