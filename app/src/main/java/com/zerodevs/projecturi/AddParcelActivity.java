package com.zerodevs.projecturi;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Activity for adding a new parcel.
 * Handles form inputs, parcel type, delivery type selection, and submission to Firestore.
 */
public class AddParcelActivity extends BaseActivity {

    // UI elements
    private Spinner spinnerDeliveryDistrict, spinnerDeliveryUpazila, spinnerParcelTypes;
    private EditText etReceiverName, etReceiverMobile, etReceiverAddress, etWeight, etSenderNote, etCod;
    private Button btnSubmit;
    private ImageView ivBack;

    private ImageView imgRegular, imgExpress, imgOvernight;
    private ImageView selectedImageView = null; // To highlight selected delivery type
    private String selectedDeliveryType = "";

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Data lists
    private List<String> parcelTypes = new ArrayList<>();

    // Selected district/upazila
    private String selectedDeliveryDistrict = "";
    private String selectedDeliveryUpazila = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parcel);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bind UI elements
        bindViews();

        // Load district & upazila data
        loadDistricts(spinnerDeliveryDistrict, spinnerDeliveryUpazila);

        // Load parcel types from Firestore
        loadParcelTypes();

        // Setup delivery type selection
        setupDeliveryTypeSelection();

        // Submit button click
        btnSubmit.setOnClickListener(v -> submitParcel());
    }

    /**
     * Bind all views from layout.
     */
    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerDeliveryDistrict = findViewById(R.id.DeliveryDistrict);
        spinnerDeliveryUpazila = findViewById(R.id.DeliveryUpazila);
        spinnerParcelTypes = findViewById(R.id.spinnerptypes);

        etReceiverName = findViewById(R.id.etReceiverName);
        etReceiverMobile = findViewById(R.id.etReceiverMobile);
        etReceiverAddress = findViewById(R.id.etReceiverAddress);
        etWeight = findViewById(R.id.etWeight);
        etSenderNote = findViewById(R.id.etSenderNote);
        etCod = findViewById(R.id.etCod);

        btnSubmit = findViewById(R.id.btnSubmitParcel);

        imgRegular = findViewById(R.id.homedeli);
        imgExpress = findViewById(R.id.pointdeli);
        imgOvernight = findViewById(R.id.h2hdeli);

        // Back button
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Load all districts from Firestore into spinner
     */
    private void loadDistricts(Spinner districtSpinner, Spinner upazilaSpinner) {
        db.collection("districts")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> districtList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        districtList.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, districtList);
                    districtSpinner.setAdapter(adapter);

                    // Handle district selection
                    districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedDeliveryDistrict = districtList.get(position);
                            loadUpazilas(selectedDeliveryDistrict, upazilaSpinner);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * Load upazilas based on selected district
     */
    private void loadUpazilas(String districtName, Spinner upazilaSpinner) {
        db.collection("districts").document(districtName).collection("upazilas")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> upazilaList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        upazilaList.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, upazilaList);
                    upazilaSpinner.setAdapter(adapter);

                    upazilaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedDeliveryUpazila = upazilaList.get(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * Load parcel types from Firestore
     */
    private void loadParcelTypes() {
        db.collection("ParcelTypes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    parcelTypes.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("PTName");
                        if (name != null) parcelTypes.add(name);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, parcelTypes);
                    spinnerParcelTypes.setAdapter(adapter);
                });
    }

    /**
     * Setup delivery type selection UI
     */
    private void setupDeliveryTypeSelection() {
        View.OnClickListener deliveryClickListener = view -> {
            if (selectedImageView != null) {
                selectedImageView.setBackgroundResource(R.drawable.bg_unselected);
            }

            selectedImageView = (ImageView) view;
            selectedImageView.setBackgroundResource(R.drawable.bg_selected);

            selectedDeliveryType = selectedImageView.getContentDescription().toString();
        };

        imgRegular.setOnClickListener(deliveryClickListener);
        imgExpress.setOnClickListener(deliveryClickListener);

        // Overnight redirects to another activity
        imgOvernight.setOnClickListener(v -> startActivity(new Intent(AddParcelActivity.this, H2hParcel.class)));
    }

    /**
     * Generate a unique parcel ID
     */
    private void generateUniqueParcelId(OnParcelIdGenerated callback) {
        String parcelId = String.format("%06d", new Random().nextInt(900000) + 100000);

        db.collection("Parcels")
                .whereEqualTo("parcelId", parcelId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onGenerated(parcelId);
                    } else {
                        // If already exists, try again
                        generateUniqueParcelId(callback);
                    }
                });
    }

    interface OnParcelIdGenerated {
        void onGenerated(String parcelId);
    }

    /**
     * Submit parcel to Firestore
     */
    private void submitParcel() {
        try {
            String name = etReceiverName.getText().toString().trim();
            String mobile = etReceiverMobile.getText().toString().trim();
            String address = etReceiverAddress.getText().toString().trim();
            String note = etSenderNote.getText().toString().trim();
            String parcelType = spinnerParcelTypes.getSelectedItem().toString();

            if (selectedDeliveryType.isEmpty()) {
                Toast.makeText(this, "Please select delivery type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etWeight.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter parcel weight", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etCod.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter COD amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight = Double.parseDouble(etWeight.getText().toString());
            double cod = Double.parseDouble(etCod.getText().toString());
            String userId = auth.getCurrentUser().getUid();

            // Load sender phone from Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String senderPhone = userDoc.getString("phone");

                        generateUniqueParcelId(parcelId -> {
                            // Prepare parcel data
                            Map<String, Object> parcel = new HashMap<>();
                            parcel.put("parcelId", parcelId);
                            parcel.put("receiverName", name);
                            parcel.put("receiverMobile", mobile);
                            parcel.put("deliveryAddress", address);
                            parcel.put("deliveryDistrict", selectedDeliveryDistrict);
                            parcel.put("deliveryUpazila", selectedDeliveryUpazila);
                            parcel.put("weight", weight);
                            parcel.put("note", note);
                            parcel.put("userId", userId);
                            parcel.put("senderPhone", senderPhone != null ? senderPhone : "N/A");
                            parcel.put("riderName", " ");
                            parcel.put("riderMobile", " ");
                            parcel.put("parcelCat", "Official");
                            parcel.put("deliveryType", selectedDeliveryType);
                            parcel.put("parcelType", parcelType);
                            parcel.put("status", "Pending");
                            parcel.put("cod", cod);
                            parcel.put("isPaid", false);
                            parcel.put("timestamp", new Date());
                            parcel.put("deliveryCharge", 100); // Default delivery charge

                            // Add to Firestore
                            db.collection("Parcels")
                                    .add(parcel)
                                    .addOnSuccessListener(docRef -> showSuccessPopup(parcelId))
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to create parcel: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Sender info load failed", Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Show success popup after adding parcel
     */
    private void showSuccessPopup(String parcelId) {
        new AlertDialog.Builder(this)
                .setTitle("Parcel Added")
                .setMessage("Parcel Added Successfully!\n\nID: CN#" + parcelId + "\n\nCheck My Parcel For Details")
                .setPositiveButton("OK", null)
                .show();
    }
}
