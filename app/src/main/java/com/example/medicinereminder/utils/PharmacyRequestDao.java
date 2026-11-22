package com.example.medicinereminder.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.medicinereminder.models.PharmacyRequest;

import java.util.List;

@Dao
public interface PharmacyRequestDao {
    @Insert
    void insert(PharmacyRequest request);

    @Query("SELECT * FROM pharmacy_requests WHERE userEmail = :email ORDER BY timestamp DESC")
    List<PharmacyRequest> getRequestsByUser(String email);
}
