package com.zerodevs.projecturi;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/**
 * Activity for adding H2H (Home-to-Home) parcels.
 * Handles pickup & delivery locations, parcel details, and submission to Firestore.
 */
public class H2hParcel extends BaseActivity {

    // UI elements
    private Spinner spinnerPickupDistrict, spinnerPickupUpazila;
    private Spinner spinnerDeliveryDistrict, spinnerDeliveryUpazila;
    private Spinner spinnerParcelTypes;
    private EditText etPickupAddress, etDeliveryAddress;
    private EditText etReceiverName, etReceiverMobile;
    private EditText etWeight, etSenderNote, etpv, etRdc, etlot, etDescription;
    private Button btnSubmit;
    private ImageView ivBack;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Selected district/upazila
    private String selectedPickupDistrict = "", selectedPickupUpazila = "";
    private String selectedDeliveryDistrict = "", selectedDeliveryUpazila = "";

    // Parcel type list
    private List<String> parcelTypeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h2h_parcel);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bind views
        bindViews();

        // Load pickup & delivery districts and upazilas
        loadDistricts(spinnerPickupDistrict, spinnerPickupUpazila, true);
        loadDistricts(spinnerDeliveryDistrict, spinnerDeliveryUpazila, false);

        // Load parcel types from Firestore
        loadParcelTypes();

        // Submit button click
        btnSubmit.setOnClickListener(v -> submitParcel());
    }

    /**
     * Bind all views from layout
     */
    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> super.onBackPressed());

        spinnerPickupDistrict = findViewById(R.id.PickupDistrict);
        spinnerPickupUpazila = findViewById(R.id.PickupUpazila);
        etPickupAddress = findViewById(R.id.PickupAddress);

        spinnerDeliveryDistrict = findViewById(R.id.DeliveryDistrict);
        spinnerDeliveryUpazila = findViewById(R.id.DeliveryUpazila);
        etDeliveryAddress = findViewById(R.id.DeliveryAddress);

        spinnerParcelTypes = findViewById(R.id.spinnerptypes);
        etReceiverName = findViewById(R.id.etReceiverName);
        etReceiverMobile = findViewById(R.id.etReceiverMobile);
        etWeight = findViewById(R.id.etWeight);
        etSenderNote = findViewById(R.id.etSenderNote);
        etpv = findViewById(R.id.etpv);
        etRdc = findViewById(R.id.etRdc);
        etlot = findViewById(R.id.etlot);
        etDescription = findViewById(R.id.etDescription);

        btnSubmit = findViewById(R.id.btnSubmitParcel);
    }

    /**
     * Load districts from Firestore
     * @param districtSpinner Spinner for districts
     * @param upazilaSpinner Spinner for upazilas
     * @param isPickup True if loading pickup district, false for delivery
     */
    private void loadDistricts(Spinner districtSpinner, Spinner upazilaSpinner, boolean isPickup) {
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

                    // District selection listener
                    districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedDistrict = districtList.get(position);
                            if (isPickup) selectedPickupDistrict = selectedDistrict;
                            else selectedDeliveryDistrict = selectedDistrict;

                            loadUpazilas(selectedDistrict, upazilaSpinner, isPickup);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * Load upazilas for selected district
     */
    private void loadUpazilas(String districtName, Spinner upazilaSpinner, boolean isPickup) {
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
                            if (isPickup) selectedPickupUpazila = upazilaList.get(position);
                            else selectedDeliveryUpazila = upazilaList.get(position);
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
                    parcelTypeList.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("PTName");
                        if (name != null) parcelTypeList.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, parcelTypeList);
                    spinnerParcelTypes.setAdapter(adapter);
                });
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
                    if (snapshot.isEmpty()) callback.onGenerated(parcelId);
                    else generateUniqueParcelId(callback); // retry if exists
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
            // Get input values
            String name = etReceiverName.getText().toString().trim();
            String mobile = etReceiverMobile.getText().toString().trim();
            String parcelType = spinnerParcelTypes.getSelectedItem().toString();
            String pickupAddress = etPickupAddress.getText().toString().trim();
            String deliveryAddress = etDeliveryAddress.getText().toString().trim();
            String note = etSenderNote.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            // Validate required fields
            if (selectedPickupDistrict.isEmpty() || selectedPickupUpazila.isEmpty() ||
                    selectedDeliveryDistrict.isEmpty() || selectedDeliveryUpazila.isEmpty()) {
                Toast.makeText(this, "Please select both pickup and delivery locations", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etWeight.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter weight", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse numeric fields
            double weight = Double.parseDouble(etWeight.getText().toString());
            double parcelValue = Double.parseDouble(etpv.getText().toString());
            double deliveryCharge = Double.parseDouble(etRdc.getText().toString());
            double lot = Double.parseDouble(etlot.getText().toString());

            String userId = auth.getCurrentUser().getUid();

            // Get sender info from Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String senderPhone = userDoc.getString("phone");
                        String senderName = userDoc.getString("name");

                        // Generate unique parcel ID
                        generateUniqueParcelId(parcelId -> {
                            // Prepare parcel data
                            Map<String, Object> parcel = new HashMap<>();
                            parcel.put("parcelId", parcelId);
                            parcel.put("pickupDistrict", selectedPickupDistrict);
                            parcel.put("pickupUpazila", selectedPickupUpazila);
                            parcel.put("pickupAddress", pickupAddress);
                            parcel.put("deliveryDistrict", selectedDeliveryDistrict);
                            parcel.put("deliveryUpazila", selectedDeliveryUpazila);
                            parcel.put("deliveryAddress", deliveryAddress);
                            parcel.put("receiverName", name);
                            parcel.put("receiverMobile", mobile);
                            parcel.put("riderName", " ");
                            parcel.put("riderMobile", " ");
                            parcel.put("cod", 0.00);
                            parcel.put("parcelCat", "H2h");
                            parcel.put("weight", weight);
                            parcel.put("note", note);
                            parcel.put("description", description);
                            parcel.put("userId", userId);
                            parcel.put("senderPhone", senderPhone != null ? senderPhone : "N/A");
                            parcel.put("senderName", senderName);
                            parcel.put("parcelType", parcelType);
                            parcel.put("status", "Pending");
                            parcel.put("deliveryCharge", deliveryCharge);
                            parcel.put("lot", lot);
                            parcel.put("ParcelValue", parcelValue);
                            parcel.put("isPaid", false);
                            parcel.put("timestamp", new Date());

                            // Add parcel to Firestore
                            db.collection("Parcels")
                                    .add(parcel)
                                    .addOnSuccessListener(docRef -> showSuccessPopup(parcelId));
                        });
                    });

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
                .setMessage("Parcel ID: " + parcelId +
                        "\nPickup: " + selectedPickupDistrict + ", " + selectedPickupUpazila +
                        "\nDelivery: " + selectedDeliveryDistrict + ", " + selectedDeliveryUpazila)
                .setPositiveButton("OK", null)
                .show();
    }
}
