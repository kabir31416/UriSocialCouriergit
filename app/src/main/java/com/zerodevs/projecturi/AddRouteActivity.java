package com.zerodevs.projecturi;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * Activity to add a route with start & end points.
 * Includes Date & Time picker for route scheduling.
 */
public class AddRouteActivity extends BaseActivity {

    // UI elements
    private Spinner startDistrict, startUpazila;
    private Spinner endDistrict, endUpazila;
    private EditText startAddress, endAddress;
    private EditText etMaxWeight, etDate, etNote;
    private Button btnSubmit;
    private ImageView ivBack;

    // Selected district & upazila
    private String selectedStartDistrict = "", selectedStartUpazila = "";
    private String selectedEndDistrict = "", selectedEndUpazila = "";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bind views
        bindViews();

        // Load districts & upazilas
        loadDistricts(startDistrict, startUpazila, true);   // Start
        loadDistricts(endDistrict, endUpazila, false);     // End

        // Set Date & Time picker for etDate
        etDate.setOnClickListener(v -> showDateTimePicker());

        // Submit route
        btnSubmit.setOnClickListener(v -> submitRoute());
    }

    /**
     * Bind all UI elements
     */
    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> super.onBackPressed());

        startDistrict = findViewById(R.id.startDistrict);
        startUpazila = findViewById(R.id.startUpazila);
        startAddress = findViewById(R.id.startAddress);

        endDistrict = findViewById(R.id.endDistrict);
        endUpazila = findViewById(R.id.endUpazila);
        endAddress = findViewById(R.id.endAddress);

        etMaxWeight = findViewById(R.id.etMaxWeight);
        etDate = findViewById(R.id.etDate);
        etNote = findViewById(R.id.etNote);

        btnSubmit = findViewById(R.id.btnSubmitRoute);
    }

    /**
     * Load districts from Firestore
     */
    private void loadDistricts(Spinner districtSpinner, Spinner upazilaSpinner, boolean isStart) {
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

                    // Set district selection listener
                    districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String district = districtList.get(position);
                            if (isStart) selectedStartDistrict = district;
                            else selectedEndDistrict = district;

                            loadUpazilas(district, upazilaSpinner, isStart);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * Load upazilas for selected district
     */
    private void loadUpazilas(String districtName, Spinner upazilaSpinner, boolean isStart) {
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
                            if (isStart) selectedStartUpazila = upazilaList.get(position);
                            else selectedEndUpazila = upazilaList.get(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * Show Date & Time picker
     */
    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        // DatePickerDialog
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // TimePickerDialog
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                // Format Date & Time
                                String selectedDateTime = android.text.format.DateFormat.format(
                                        "dd MMM yyyy - hh:mm a", calendar).toString();
                                etDate.setText(selectedDateTime);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false); // 24-hour format
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    /**
     * Submit route to Firestore
     */
    private void submitRoute() {
        String startAddr = startAddress.getText().toString().trim();
        String endAddr = endAddress.getText().toString().trim();
        String weightStr = etMaxWeight.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();

        if (weightStr.isEmpty()) {
            etMaxWeight.setError("Max weight required");
            return;
        }

        double maxWeight = Double.parseDouble(weightStr);
        String currentUid = auth.getCurrentUser().getUid();

        // Get user info
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    String userName = doc.getString("name");
                    String phone = doc.getString("phone");

                    Map<String, Object> route = new HashMap<>();
                    route.put("startDistrict", selectedStartDistrict);
                    route.put("startUpazila", selectedStartUpazila);
                    route.put("startAddress", startAddr);
                    route.put("endDistrict", selectedEndDistrict);
                    route.put("endUpazila", selectedEndUpazila);
                    route.put("endAddress", endAddr);
                    route.put("maxWeight", maxWeight);
                    route.put("dateTime", dateStr); // store selected DateTime
                    route.put("note", noteStr);
                    route.put("userId", currentUid);
                    route.put("userName", userName);
                    route.put("phone", phone);
                    route.put("timestamp", new Date());

                    db.collection("Routes").add(route)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Route submitted successfully", Toast.LENGTH_SHORT).show();
                                clearFields();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to submit route", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Clear input fields after successful submission
     */
    private void clearFields() {
        startAddress.setText("");
        endAddress.setText("");
        etMaxWeight.setText("");
        etDate.setText("");
        etNote.setText("");
        startDistrict.setSelection(0);
        startUpazila.setSelection(0);
        endDistrict.setSelection(0);
        endUpazila.setSelection(0);
    }
}
