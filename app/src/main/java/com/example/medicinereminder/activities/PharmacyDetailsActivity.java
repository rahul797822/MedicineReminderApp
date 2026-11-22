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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicinereminder.R;
import com.example.medicinereminder.models.PharmacyRequest;
import com.example.medicinereminder.utils.AppDatabase;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class PharmacyDetailsActivity extends AppCompatActivity {

    private TextView pharmacyNameTv, pharmacyDetailsTv;
    private RecyclerView medicinesRv;
    private EditText requestMedEt;
    private Button requestBtn;
    private MaterialButton navigateBtn;
    private String pharmacyName, pharmacyId;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_details);

        SharedPreferences pref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("current_user_email", "guest");

        pharmacyNameTv = findViewById(R.id.pharmacyNameTv);
        pharmacyDetailsTv = findViewById(R.id.pharmacyDetailsTv);
        medicinesRv = findViewById(R.id.medicinesRv);
        requestMedEt = findViewById(R.id.requestMedEt);
        requestBtn = findViewById(R.id.requestBtn);
        navigateBtn = findViewById(R.id.navigateBtn);

        // Get Data from Intent
        pharmacyName = getIntent().getStringExtra("name");
        String details = getIntent().getStringExtra("details");
        
        if (pharmacyName == null) pharmacyName = "Pharmacy";
        pharmacyId = pharmacyName.replaceAll("\\s+", "_").toLowerCase();

        pharmacyNameTv.setText(pharmacyName);
        pharmacyDetailsTv.setText(details);

        setupMedicinesList();

        requestBtn.setOnClickListener(v -> sendMedicineRequestLocally());
        
        navigateBtn.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(pharmacyName));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Maps app not found", Toast.LENGTH_SHORT).show();
            }
        });
        
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupMedicinesList() {
        List<MedicineStock> stock = new ArrayList<>();
        stock.add(new MedicineStock("Paracetamol (500mg)", "₹25.00", "15% OFF"));
        stock.add(new MedicineStock("Vitamin D3", "₹120.00", "Buy 1 Get 1"));
        stock.add(new MedicineStock("Omega-3 Fish Oil", "₹450.00", "10% Discount"));
        stock.add(new MedicineStock("Cough Syrup", "₹85.00", "Flat ₹10 Off"));
        stock.add(new MedicineStock("Insulin Glargine", "₹1100.00", "Prescription Required"));

        medicinesRv.setLayoutManager(new LinearLayoutManager(this));
        medicinesRv.setAdapter(new StockAdapter(stock));
    }

    private void sendMedicineRequestLocally() {
        String medName = requestMedEt.getText().toString().trim();
        if (medName.isEmpty()) {
            requestMedEt.setError("Enter medicine name");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            PharmacyRequest request = new PharmacyRequest(medName, pharmacyName, currentUserEmail, "Pending", System.currentTimeMillis());
            AppDatabase.getInstance(this).pharmacyRequestDao().insert(request);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Local Request logged! We will notify you when available.", Toast.LENGTH_LONG).show();
                requestMedEt.setText("");
                simulateAvailabilityNotification(medName);
            });
        });
    }

    private void simulateAvailabilityNotification(String medName) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Medicine Restocked! 🔔")
                    .setMessage("Great news! " + medName + " is now available at " + pharmacyName + ".")
                    .setPositiveButton("Order Now", (dialog, which) -> {
                        Toast.makeText(this, "Refill order for " + medName + " placed successfully!", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }, 5000); 
    }

    static class MedicineStock {
        String name, price, discount;
        MedicineStock(String name, String price, String discount) {
            this.name = name; this.price = price; this.discount = discount;
        }
    }

    class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {
        private final List<MedicineStock> list;
        StockAdapter(List<MedicineStock> list) { this.list = list; }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTv, priceTv, offerTv;
            MaterialButton buyBtn;
            ViewHolder(View v) {
                super(v);
                nameTv = v.findViewById(R.id.medNameTv);
                priceTv = v.findViewById(R.id.medPriceTv);
                offerTv = v.findViewById(R.id.medOfferTv);
                buyBtn = v.findViewById(R.id.buyBtn);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicineStock m = list.get(position);
            holder.nameTv.setText(m.name);
            holder.priceTv.setText(m.price);
            holder.offerTv.setText(m.discount);

            holder.buyBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(PharmacyDetailsActivity.this)
                        .setTitle("Confirm Purchase")
                        .setMessage("Buy " + m.name + " for " + m.price + "?")
                        .setPositiveButton("Place Order", (dialog, which) -> {
                            Toast.makeText(PharmacyDetailsActivity.this, "Order Confirmed Locally!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }
    }
}
