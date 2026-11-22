package com.example.medicinereminder.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.medicinereminder.models.DoctorAccess;

import java.util.List;

@Dao
public interface DoctorAccessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void grantAccess(DoctorAccess access);

    @Query("DELETE FROM doctor_access WHERE patientEmail = :pEmail AND doctorEmail = :dEmail")
    void revokeAccess(String pEmail, String dEmail);

    @Query("SELECT patientEmail FROM doctor_access WHERE doctorEmail = :dEmail AND accessGranted = 1")
    List<String> getPatientsForDoctor(String dEmail);

    @Query("SELECT doctorEmail FROM doctor_access WHERE patientEmail = :pEmail AND accessGranted = 1")
    List<String> getDoctorsForPatient(String pEmail);
}
