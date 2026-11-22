package com.example.medicinereminder.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.Medicine;
import com.example.medicinereminder.utils.AppDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ViewMedicinesActivity extends AppCompatActivity {

    private MedicineAdapter adapter;
    private List<Medicine> medicineList;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_medicines);

        db = AppDatabase.getInstance(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medicineList = new ArrayList<>();
        adapter = new MedicineAdapter(medicineList);
        recyclerView.setAdapter(adapter);

        loadLocalMedicines();
    }

    private void loadLocalMedicines() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Medicine> list = db.medicineDao().getAllMedicines();
            runOnUiThread(() -> {
                medicineList.clear();
                medicineList.addAll(list);
                adapter.notifyDataSetChanged();
            });
        });
    }

    class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
        private final List<Medicine> list;

        MedicineAdapter(List<Medicine> list) {
            this.list = list;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTv, dosageTv, timeTv, quantityTv, statusTv;
            Button takenBtn;

            ViewHolder(View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.nameTv);
                dosageTv = itemView.findViewById(R.id.dosageTv);
                timeTv = itemView.findViewById(R.id.timeTv);
                quantityTv = itemView.findViewById(R.id.quantityTv);
                statusTv = itemView.findViewById(R.id.statusTv);
                takenBtn = itemView.findViewById(R.id.takenBtn);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Medicine item = list.get(position);
            holder.nameTv.setText(item.name);
            holder.dosageTv.setText("Dosage: " + item.dosage);
            holder.timeTv.setText("Time: " + item.time);
            holder.quantityTv.setText("Quantity: " + item.quantity);

            if (item.quantity < 3) {
                holder.quantityTv.append(" (⚠️ Low Stock!)");
            }

            if (item.taken) {
                holder.statusTv.setText("✅ Taken");
                holder.takenBtn.setEnabled(false);
            } else {
                holder.statusTv.setText("❌ Not Taken");
                holder.takenBtn.setEnabled(true);
            }

            holder.takenBtn.setOnClickListener(v -> {
                if (item.quantity <= 0) {
                    Toast.makeText(ViewMedicinesActivity.this, "Out of stock!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    item.taken = true;
                    item.quantity -= 1;
                    db.medicineDao().update(item);
                    
                    // Streak and points logic would go here locally
                    updateLocalStreakAndPoints();

                    runOnUiThread(() -> {
                        notifyItemChanged(position);
                        Toast.makeText(ViewMedicinesActivity.this, "Marked as Taken", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private void updateLocalStreakAndPoints() {
        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = pref.getString("last_taken_date", "");
        int streak = pref.getInt("streak_count", 0);
        int points = pref.getInt("points", 0);

        if (!today.equals(lastDate)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            if (yesterday.equals(lastDate)) {
                streak++;
            } else {
                streak = 1;
            }
            
            points += 10; // Simple local points
            pref.edit()
                .putString("last_taken_date", today)
                .putInt("streak_count", streak)
                .putInt("points", points)
                .apply();
        }
    }
}
