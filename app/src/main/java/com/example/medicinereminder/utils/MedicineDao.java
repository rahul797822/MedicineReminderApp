package com.example.medicinereminder.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.medicinereminder.models.Medicine;

import java.util.List;

@Dao
public interface MedicineDao {
    @Insert
    void insert(Medicine medicine);

    @Query("SELECT * FROM medicines WHERE userEmail = :email ORDER BY createdAt DESC")
    List<Medicine> getMedicinesByUser(String email);

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    List<Medicine> getAllMedicines();

    @Update
    void update(Medicine medicine);

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    Medicine getMedicineById(int id);
}
