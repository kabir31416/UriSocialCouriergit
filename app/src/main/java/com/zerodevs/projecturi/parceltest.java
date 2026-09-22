package com.zerodevs.projecturi;

import android.os.Bundle;
import android.widget.*;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class parceltest extends BaseActivity{

    Spinner spinnerStart, spinnerEnd, spinnerdtypes, spinnerptypes;
    EditText etReceiverName, etReceiverMobile, etReceiverAddress;
    EditText etLength, etWidth, etHeight, etWeight, etSenderNote, etCod;
    Button btnSubmit;
    ImageView ivBack;

    FirebaseFirestore db;
    FirebaseAuth auth;
    List<String> points = new ArrayList<>();
    List<String> Dt = new ArrayList<>();
    List<String> Pt = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parceltest);


        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });



        // Bind views
        spinnerStart = findViewById(R.id.spinnerStartPoint);
        spinnerEnd = findViewById(R.id.spinnerEndPoint);
        spinnerdtypes = findViewById(R.id.spinnerdtypes);
        spinnerptypes = findViewById(R.id.spinnerptypes);

        etReceiverName = findViewById(R.id.etReceiverName);
        etReceiverMobile = findViewById(R.id.etReceiverMobile);
        etReceiverAddress = findViewById(R.id.etReceiverAddress);
        etLength = findViewById(R.id.etLength);
        etWidth = findViewById(R.id.etWidth);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etSenderNote = findViewById(R.id.etSenderNote);
        btnSubmit = findViewById(R.id.btnSubmitParcel);
        etCod = findViewById(R.id.etCod);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadPoints();
        loadDT();
        loadPT();

        btnSubmit.setOnClickListener(v -> submitParcel());
    }

    private void loadPoints() {
        db.collection("Points")
                .get()
                .addOnSuccessListener(snapshot -> {
                    points.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("pointName");
                        if (name != null) points.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, points);
                    spinnerStart.setAdapter(adapter);
                    spinnerEnd.setAdapter(adapter);
                });
    }

    private void loadDT() {
        db.collection("DeliveryTypes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Dt.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("DTName");
                        if (name != null) Dt.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, Dt);
                    spinnerdtypes.setAdapter(adapter);
                });
    }

    private void loadPT() {
        db.collection("ParcelTypes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Pt.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("PTName");
                        if (name != null) Pt.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, Pt);
                    spinnerptypes.setAdapter(adapter);
                });
    }

    private void generateUniqueParcelId(OnParcelIdGenerated callback) {
        String parcelId = String.format("%06d", new Random().nextInt(900000) + 100000);
        db.collection("Parcels")
                .whereEqualTo("parcelId", parcelId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onGenerated(parcelId);
                    } else {
                        generateUniqueParcelId(callback);
                    }
                });
    }

    interface OnParcelIdGenerated {
        void onGenerated(String parcelId);
    }

    private void submitParcel() {
        try {
            String start = spinnerStart.getSelectedItem().toString();
            String end = spinnerEnd.getSelectedItem().toString();
            String name = etReceiverName.getText().toString().trim();
            String mobile = etReceiverMobile.getText().toString().trim();
            String address = etReceiverAddress.getText().toString().trim();
            String note = etSenderNote.getText().toString().trim();
            String deliveryType = spinnerdtypes.getSelectedItem().toString();
            String parcelType = spinnerptypes.getSelectedItem().toString();

            // ✅ বাধ্যতামূলক weight চেক
            if (etWeight.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter parcel weight", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Optional field হলে 0 রাখা হবে
            double length = etLength.getText().toString().isEmpty() ? 0 : Double.parseDouble(etLength.getText().toString());
            double width = etWidth.getText().toString().isEmpty() ? 0 : Double.parseDouble(etWidth.getText().toString());
            double height = etHeight.getText().toString().isEmpty() ? 0 : Double.parseDouble(etHeight.getText().toString());
            double weight = Double.parseDouble(etWeight.getText().toString());
            double cod = Double.parseDouble(etCod.getText().toString());

            String userId = auth.getCurrentUser().getUid();

            generateUniqueParcelId(parcelId -> {
                Map<String, Object> parcel = new HashMap<>();
                parcel.put("parcelId", parcelId);
                parcel.put("startPoint", start);
                parcel.put("endPoint", end);
                parcel.put("receiverName", name);
                parcel.put("receiverMobile", mobile);
                parcel.put("receiverAddress", address);
                parcel.put("length", length);
                parcel.put("width", width);
                parcel.put("height", height);
                parcel.put("weight", weight);
                parcel.put("note", note);
                parcel.put("userId", userId);
                parcel.put("deliveryType", deliveryType);
                parcel.put("parcelType", parcelType);
                parcel.put("status", "Pending");
                parcel.put("cod", cod);
                parcel.put("isPaid", false); // ➕ এই লাইনটি যুক্ত করুন
                parcel.put("timestamp", new Date());

                // ✅ Default delivery charge
                parcel.put("deliveryCharge", 100);

                db.collection("Parcels")
                        .add(parcel)
                        .addOnSuccessListener(docRef -> showSuccessPopup(parcelId, start, end));
            });

        } catch (Exception e) {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
        }
    }


    private void showSuccessPopup(String id, String start, String end) {
        new AlertDialog.Builder(this)
                .setTitle("Parcel Added")
                .setMessage("Parcel ID: " + id + "\nFrom: " + start + " → To: " + end)
                .setPositiveButton("OK", null)
                .show();
    }
}
