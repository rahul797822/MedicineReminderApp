package com.example.medicinereminder.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    public String email;
    
    public String password;
    public String role;
    public long createdAt;

    public User(@NonNull String email, String password, String role, long createdAt) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }
}
