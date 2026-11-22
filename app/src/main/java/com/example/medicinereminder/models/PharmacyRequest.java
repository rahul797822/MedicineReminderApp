package com.example.medicinereminder.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pharmacy_requests")
public class PharmacyRequest {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String medicineName;
    public String pharmacyName;
    public String userEmail;
    public String status;
    public long timestamp;

    public PharmacyRequest(String medicineName, String pharmacyName, String userEmail, String status, long timestamp) {
        this.medicineName = medicineName;
        this.pharmacyName = pharmacyName;
        this.userEmail = userEmail;
        this.status = status;
        this.timestamp = timestamp;
    }
}
