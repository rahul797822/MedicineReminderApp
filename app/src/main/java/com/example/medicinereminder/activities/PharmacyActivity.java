package com.example.medicinereminder.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.Medicine;
import com.example.medicinereminder.utils.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class PharmacyActivity extends AppCompatActivity {

    private ListView pharmacyList;
    private TextView suggestRefillTv;
    private MaterialCardView refillCard;
    private ChipGroup filterChipGroup;
    private MaterialButton orderBtn;
    
    private List<PharmacyModel> masterList;
    private List<PharmacyModel> filteredList;
    private PharmacyAdapter adapter;
    
    private PharmacyModel selectedPharmacy = null;
    private String currentSearchText = "";
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);

        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "");

        // Initialize Views
        pharmacyList = findViewById(R.id.pharmacyList);
        suggestRefillTv = findViewById(R.id.suggestRefillTv);
        refillCard = findViewById(R.id.refillCard);
        orderBtn = findViewById(R.id.orderBtn);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        SearchView searchView = findViewById(R.id.searchView);
        MaterialButton priceCheckBtn = findViewById(R.id.priceCheckBtn);
        MaterialButton uploadPrescriptionBtn = findViewById(R.id.uploadPrescriptionBtn);

        setupPharmacies();
        checkLocalLowStock();

        // 1. Search Functionality
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchText = query;
                    applyFilters();
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchText = newText;
                    applyFilters();
                    return false;
                }
            });
        }

        // 2. Filter Chips
        if (filterChipGroup != null) {
            filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
        }

        // 3. Pharmacy Selection
        pharmacyList.setOnItemClickListener((parent, view, position, id) -> {
            selectedPharmacy = filteredList.get(position);
            Intent intent = new Intent(this, PharmacyDetailsActivity.class);
            intent.putExtra("name", selectedPharmacy.name);
            intent.putExtra("details", selectedPharmacy.details);
            startActivity(intent);
        });

        // 4. Order Action
        orderBtn.setOnClickListener(v -> {
            if (selectedPharmacy == null) {
                Toast.makeText(this, "Please select a pharmacy first!", Toast.LENGTH_SHORT).show();
            } else {
                showOrderConfirmation(selectedPharmacy);
            }
        });

        // 5. Price Check (Mock)
        if (priceCheckBtn != null) {
            priceCheckBtn.setOnClickListener(v -> {
                Toast.makeText(this, "Comparing local prices...", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> 
                    Toast.makeText(this, "Lowest price found at NetMeds! (Save 15%)", Toast.LENGTH_LONG).show(), 1000);
            });
        }

        // 6. Upload Prescription (Mock)
        if (uploadPrescriptionBtn != null) {
            uploadPrescriptionBtn.setOnClickListener(v -> Toast.makeText(this, "Opening Local Gallery...", Toast.LENGTH_SHORT).show());
        }
    }

    private void applyFilters() {
        if (masterList == null || filteredList == null) return;
        filteredList.clear();
        int checkedChipId = filterChipGroup != null ? filterChipGroup.getCheckedChipId() : View.NO_ID;

        for (PharmacyModel item : masterList) {
            boolean matchesSearch = item.name.toLowerCase(Locale.ROOT).contains(currentSearchText.toLowerCase(Locale.ROOT));
            boolean matchesChip = false;

            if (checkedChipId == R.id.chipAll || checkedChipId == View.NO_ID) {
                matchesChip = true;
            } else if (checkedChipId == R.id.chipNearby) {
                matchesChip = item.distance < 1.0f;
            } else if (checkedChipId == R.id.chipOpenNow) {
                matchesChip = item.details.contains("24/7");
            } else if (checkedChipId == R.id.chipTopRated) {
                matchesChip = item.rating >= 4.5f;
            }

            if (matchesSearch && matchesChip) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void checkLocalLowStock() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Medicine> lowStockMeds = new ArrayList<>();
            List<Medicine> allMeds = AppDatabase.getInstance(this).medicineDao().getMedicinesByUser(currentUserEmail);
            
            for (Medicine m : allMeds) {
                if (m.quantity < 3) lowStockMeds.add(m);
            }

            runOnUiThread(() -> {
                if (!lowStockMeds.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Local Refill needed: ");
                    for (int i = 0; i < lowStockMeds.size(); i++) {
                        sb.append(lowStockMeds.get(i).name);
                        if (i < lowStockMeds.size() - 1) sb.append(", ");
                    }
                    suggestRefillTv.setText(sb.toString());
                    refillCard.setVisibility(View.VISIBLE);
                } else {
                    refillCard.setVisibility(View.GONE);
                }
            });
        });
    }

    private void showOrderConfirmation(PharmacyModel pharmacy) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Order")
                .setMessage("Place local refill order from " + pharmacy.name + "?")
                .setPositiveButton("Confirm", (dialog, which) -> Toast.makeText(this, "📦 Order placed locally at " + pharmacy.name, Toast.LENGTH_LONG).show())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupPharmacies() {
        masterList = new ArrayList<>();
        masterList.add(new PharmacyModel("Apollo Pharmacy", "0.5km away • Open 24/7", "1234567890", 4.5f, 0.5f));
        masterList.add(new PharmacyModel("MedPlus Store", "1.2km away • 9 AM - 11 PM", "0987654321", 4.2f, 1.2f));
        masterList.add(new PharmacyModel("NetMeds Digital", "2.0km away • Home Delivery", "1122334455", 4.8f, 2.0f));
        masterList.add(new PharmacyModel("City Hospital Pharma", "3.5km away • In Hospital", "5566778899", 4.0f, 3.5f));
        masterList.add(new PharmacyModel("Local Medical Shop", "0.8km away • Closed on Sunday", "9988776655", 3.9f, 0.8f));

        filteredList = new ArrayList<>(masterList);
        adapter = new PharmacyAdapter(this, filteredList);
        pharmacyList.setAdapter(adapter);
    }

    static class PharmacyModel {
        String name, details, phone;
        float rating, distance;
        PharmacyModel(String name, String details, String phone, float rating, float distance) {
            this.name = name; this.details = details; this.phone = phone;
            this.rating = rating; this.distance = distance;
        }
    }

    static class PharmacyAdapter extends ArrayAdapter<PharmacyModel> {
        PharmacyAdapter(Context context, List<PharmacyModel> list) { super(context, 0, list); }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_pharmacy, parent, false);
            PharmacyModel p = getItem(position);
            if (p != null) {
                ((TextView)convertView.findViewById(R.id.pharmacyNameTv)).setText(p.name);
                ((TextView)convertView.findViewById(R.id.pharmacyDetailsTv)).setText(p.details);
                ((TextView)convertView.findViewById(R.id.pharmacyRatingTv)).setText(String.format(Locale.getDefault(), "⭐ %.1f", p.rating));
                convertView.findViewById(R.id.callIv).setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + p.phone));
                    getContext().startActivity(intent);
                });
            }
            return convertView;
        }
    }
}
