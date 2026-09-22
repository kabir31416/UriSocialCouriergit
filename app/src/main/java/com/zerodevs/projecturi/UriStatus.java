package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class UriStatus extends BaseActivity {

    EditText etManualParcelId;
    ImageView btnSearchParcel, btnScan, ivBack;
    Button btnUpdateLocation;

    TextView ParcelCn, pstatus, pdate, pcod, pdc, ptype, ptrack;
    TextView recn, recm, reca, rect;
    TextView sen, sem;

    Spinner spinnerStatus, spinnerPoints;

    FirebaseFirestore db;
    DocumentSnapshot currentParcelDoc;

    List<String> pointList = new ArrayList<>();
    List<String> statusList = Arrays.asList(
            "Picked Up",
            "Received In",
            "Sent To",
            "Delivered",
            "Return To"
    );

    String generatedOTP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri_status);

        etManualParcelId = findViewById(R.id.etManualParcelId);
        btnSearchParcel = findViewById(R.id.btnSearchParcel);
        btnScan = findViewById(R.id.btnScanQR);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);

        ParcelCn = findViewById(R.id.ParcelCn);
        pstatus = findViewById(R.id.pstatus);
        pdate = findViewById(R.id.pdate);
        pcod = findViewById(R.id.pcod);
        pdc = findViewById(R.id.pdc);
        ptype = findViewById(R.id.ptype);
        ptrack = findViewById(R.id.ptrack);

        recn = findViewById(R.id.recn);
        recm = findViewById(R.id.recm);
        reca = findViewById(R.id.reca);
        rect = findViewById(R.id.rect);

        sen = findViewById(R.id.sen);
        sem = findViewById(R.id.sem);

        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerPoints = findViewById(R.id.spinnerPoints);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> onBackPressed());


        db = FirebaseFirestore.getInstance();

        spinnerStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                statusList
        ));

        loadPointsIntoSpinner();

        btnScan.setOnClickListener(v -> startQRScan());

        btnSearchParcel.setOnClickListener(v -> {
            String id = etManualParcelId.getText().toString().trim();
            if (!id.isEmpty()) fetchParcelById(id);
        });

        btnUpdateLocation.setOnClickListener(v -> handleStatusUpdate());

        String parcelId = getIntent().getStringExtra("parcelId");
        if (parcelId != null && !parcelId.isEmpty()) {
            etManualParcelId.setText(parcelId);
            fetchParcelById(parcelId);
        }

    }

    private void handleStatusUpdate() {
        if (currentParcelDoc == null) return;

        String status = spinnerStatus.getSelectedItem().toString();
        String point = spinnerPoints.getSelectedItem() != null
                ? spinnerPoints.getSelectedItem().toString()
                : "";

        if ("Delivered".equals(status)) {
            sendOtpToPhone(currentParcelDoc.getString("receiverMobile"));
        } else {
            updateTracking(status, point);
        }
    }

    private void startQRScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan Parcel QR");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            fetchParcelById(result.getContents());
        }
    }

    private void fetchParcelById(String parcelId) {
        db.collection("Parcels")
                .whereEqualTo("parcelId", parcelId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        currentParcelDoc = snapshot.getDocuments().get(0);
                        showParcel(currentParcelDoc);
                    } else {
                        Toast.makeText(this, "Parcel not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showParcel(DocumentSnapshot doc) {
        ParcelCn.setText("CN#" + doc.getString("parcelId"));
        pstatus.setText(doc.getString("status"));
        pcod.setText("COD: ৳" + doc.getDouble("cod"));
        ptype.setText(doc.getString("parcelType"));
        pdc.setText("Charge: ৳" + doc.getDouble("Rdc"));

        recn.setText(doc.getString("receiverName"));
        recm.setText(doc.getString("receiverMobile"));
        reca.setText(doc.getString("deliveryAddress"));
        rect.setText(doc.getString("deliveryUpazila") + ", " + doc.getString("deliveryDistrict"));

        sen.setText(doc.getString("senderName"));
        sem.setText(doc.getString("senderPhone"));

        Timestamp ts = doc.getTimestamp("timestamp");
        if (ts != null) {
            pdate.setText(new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            ).format(ts.toDate()));
        }

        showTrackingHistory(doc);
    }

    private void showTrackingHistory(DocumentSnapshot doc) {
        ptrack.setText("");
        db.collection("Parcels")
                .document(doc.getId())
                .collection("TrackingHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot h : snapshot) {
                        Timestamp ts = h.getTimestamp("timestamp");
                        String time = ts != null
                                ? new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(ts.toDate())
                                : "";
                        ptrack.append("📦 " + h.getString("location") + " - " + time + "\n\n");
                    }
                });
    }

    private void updateTracking(String status, String point) {
        WriteBatch batch = db.batch();
        DocumentReference ref = currentParcelDoc.getReference();

        batch.update(ref, "status", status);
        if (!point.isEmpty()) batch.update(ref, "currentLocation", point);

        Map<String, Object> history = new HashMap<>();
        history.put("location", status + " " + point);
        history.put("timestamp", FieldValue.serverTimestamp());
        history.put("updatedBy", "rider");

        batch.set(ref.collection("TrackingHistory").document(), history);

        batch.commit().addOnSuccessListener(unused ->
                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
        );
    }


    // ====== OTP SEND & VERIFY ======
    private void sendOtpToPhone(String phone) {
        // Generate 4-digit OTP
        generatedOTP = String.valueOf(1000 + new Random().nextInt(9000));

        // Format phone number (Bangladesh: 880)
        String fullPhone = "880" + phone.substring(phone.length() - 10);

        // SMS API details
        String apiUrl = "https://bulksmsbd.net/api/smsapi";
        String apiKey = "E9UedDGJTK6mv5CJQORP";
        String senderId = "8809617621429";
        String message = "Your URI OTP For Delivery is " + generatedOTP;

        // Send SMS in background thread
        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "api_key=" + apiKey +
                        "&senderid=" + senderId +
                        "&number=" + fullPhone +
                        "&message=" + URLEncoder.encode(message, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while (reader.readLine() != null) {}  // read response
                reader.close();

                // Show OTP dialog on UI thread
                runOnUiThread(this::showOTPDialog);

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "OTP পাঠাতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // ====== OTP DIALOG ======
    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delivery");

        // LinearLayout for EditText
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // OTP input field
        final EditText input = new EditText(this);
        input.setHint("Enter 4-digit OTP");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(18);
        input.setPadding(20, 30, 20, 30);
        input.setBackgroundResource(android.R.drawable.edit_text);
        layout.addView(input);

        builder.setView(layout);

        // Buttons
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button verifyBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            verifyBtn.setTextColor(getResources().getColor(R.color.purple_500));
            verifyBtn.setOnClickListener(view -> {
                String enteredOtp = input.getText().toString().trim();
                if (enteredOtp.equals(generatedOTP)) {
                    String status = "Delivered";
                    String point = spinnerPoints.getSelectedItem() != null ?
                            spinnerPoints.getSelectedItem().toString() : "";

                    // Update parcel tracking & rider balance
                    updateTracking(status, point);
                    updateRiderBalance();
                    dialog.dismiss();
                } else {
                    input.setError("Invalid OTP");
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            });

            Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            cancelBtn.setTextColor(getResources().getColor(R.color.black));
        });

        dialog.show();
    }

    // ====== UPDATE RIDER BALANCE ======
    private void updateRiderBalance() {
        if (currentParcelDoc == null) {
            Toast.makeText(this, "Parcel not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String riderId = currentParcelDoc.getString("riderId");
        Double deliveryCharge = currentParcelDoc.getDouble("Rdc");

        if (riderId == null || deliveryCharge == null) {
            Toast.makeText(this, "Invalid rider or delivery charge", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference riderRef = db.collection("users").document(riderId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot riderSnap = transaction.get(riderRef);

            // Safe cast for balance
            double currentBalance = 0.0;
            Object balanceObj = riderSnap.get("balance");
            if (balanceObj instanceof Number) {
                currentBalance = ((Number) balanceObj).doubleValue();
            }

            // Update balance
            double updatedBalance = currentBalance + deliveryCharge;
            transaction.update(riderRef, "balance", updatedBalance);

            return null;
        }).addOnSuccessListener(aVoid ->
                Toast.makeText(this, "Rider balance updated", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to update balance: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }


    private void loadPointsIntoSpinner() {
        db.collection("Points").get().addOnSuccessListener(snapshot -> {
            pointList.clear();
            for (DocumentSnapshot d : snapshot) {
                pointList.add(d.getString("pointName"));
            }
            spinnerPoints.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    pointList
            ));
        });
    }
}
