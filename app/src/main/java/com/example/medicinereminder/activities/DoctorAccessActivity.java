package com.example.medicinereminder.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.Medicine;
import com.example.medicinereminder.models.User;
import com.example.medicinereminder.utils.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DoctorAccessActivity extends AppCompatActivity {

    private EditText doctorEmailEt;
    private View patientSection, doctorSection;
    private ProgressBar loadingPb;
    private RecyclerView patientsRv;
    private PatientAdapter patientAdapter;
    private List<User> patientList;
    private AppDatabase db;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_access);

        db = AppDatabase.getInstance(this);
        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "");

        // Initialize UI
        doctorEmailEt = findViewById(R.id.doctorEmailEt);
        Button shareBtn = findViewById(R.id.shareBtn);
        patientSection = findViewById(R.id.patientSection);
        doctorSection = findViewById(R.id.doctorSection);
        loadingPb = findViewById(R.id.loadingPb);
        patientsRv = findViewById(R.id.patientsRv);

        patientsRv.setLayoutManager(new LinearLayoutManager(this));
        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(patientList);
        patientsRv.setAdapter(patientAdapter);

        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> shareWithDoctorLocally());
        }

        loadLocalData();
    }

    private void loadLocalData() {
        loadingPb.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().getUserByEmail(currentUserEmail);
            runOnUiThread(() -> {
                loadingPb.setVisibility(View.GONE);
                if (user != null) {
                    if ("doctor".equalsIgnoreCase(user.role)) {
                        doctorSection.setVisibility(View.VISIBLE);
                        patientSection.setVisibility(View.GONE);
                        loadPatientsForDoctor();
                    } else {
                        patientSection.setVisibility(View.VISIBLE);
                        doctorSection.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void shareWithDoctorLocally() {
        String email = doctorEmailEt.getText().toString().trim().toLowerCase();
        if (email.isEmpty()) {
            doctorEmailEt.setError("Enter doctor email");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            User doctor = db.userDao().getUserByEmail(email);
            runOnUiThread(() -> {
                if (doctor != null && "doctor".equalsIgnoreCase(doctor.role)) {
                    // Locally mapping access
                    SharedPreferences accessPrefs = getSharedPreferences("local_access_mappings", Context.MODE_PRIVATE);
                    String patients = accessPrefs.getString(email, "");
                    if (!patients.contains(currentUserEmail)) {
                        accessPrefs.edit().putString(email, (patients.isEmpty() ? "" : patients + ",") + currentUserEmail).apply();
                    }
                    Toast.makeText(this, "Access granted to Doctor: " + email, Toast.LENGTH_SHORT).show();
                    doctorEmailEt.setText("");
                } else {
                    Toast.makeText(this, "Doctor not found on this device.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadPatientsForDoctor() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences accessPrefs = getSharedPreferences("local_access_mappings", Context.MODE_PRIVATE);
            String patientEmails = accessPrefs.getString(currentUserEmail, "");
            String[] emails = patientEmails.split(",");
            
            patientList.clear();
            for (String pEmail : emails) {
                if (!pEmail.isEmpty()) {
                    User p = db.userDao().getUserByEmail(pEmail.trim());
                    if (p != null) patientList.add(p);
                }
            }
            runOnUiThread(() -> {
                patientAdapter.notifyDataSetChanged();
                if (patientList.isEmpty()) {
                    Toast.makeText(this, "No patients have shared access with you.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {
        private final List<User> list;
        PatientAdapter(List<User> list) { this.list = list; }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView emailTv, streakTv;
            Button detailsBtn;
            ViewHolder(View v) {
                super(v);
                emailTv = v.findViewById(R.id.patientEmailTv);
                streakTv = v.findViewById(R.id.patientStreakTv);
                detailsBtn = v.findViewById(R.id.viewDetailsBtn);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User p = list.get(position);
            holder.emailTv.setText(p.email);
            // Fetch local streak from user's specific prefs
            SharedPreferences upref = getSharedPreferences("user_prefs_" + p.email, Context.MODE_PRIVATE);
            int streak = upref.getInt("streak_count", 0);
            holder.streakTv.setText(String.format(Locale.getDefault(), "🔥 Streak: %d Days", streak));
            holder.detailsBtn.setOnClickListener(v -> showPatientDetails(p));
        }

        @Override
        public int getItemCount() { return list.size(); }
    }

    private void showPatientDetails(User p) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Medicine> meds = db.medicineDao().getMedicinesByUser(p.email);
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Local Records for ").append(p.email).append(":\n\n");
                for (Medicine m : meds) {
                    sb.append("• ").append(m.name)
                            .append("\n  Status: ").append(m.taken ? "✅ Taken" : "⏳ Pending")
                            .append("\n  Stock: ").append(m.quantity).append(" left\n\n");
                }
                new AlertDialog.Builder(this)
                        .setTitle("Patient Health Records")
                        .setMessage(sb.toString().isEmpty() ? "No medication records found." : sb.toString())
                        .setPositiveButton("Close", null).show();
            });
        });
    }
}
