package com.example.medicinereminder.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "doctor_access", primaryKeys = {"patientEmail", "doctorEmail"})
public class DoctorAccess {
    @NonNull
    public String patientEmail;
    @NonNull
    public String doctorEmail;
    public boolean accessGranted;

    public DoctorAccess(@NonNull String patientEmail, @NonNull String doctorEmail, boolean accessGranted) {
        this.patientEmail = patientEmail;
        this.doctorEmail = doctorEmail;
        this.accessGranted = accessGranted;
    }
}
