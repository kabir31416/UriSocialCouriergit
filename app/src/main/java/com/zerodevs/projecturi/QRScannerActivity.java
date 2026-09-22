package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRScannerActivity extends BaseActivity {

    TextView tvParcelInfo;
    Button btnScan, btnApprove, btnUpdateLocation;
    FirebaseFirestore db;
    DocumentSnapshot currentParcelDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        tvParcelInfo = findViewById(R.id.tvParcelInfo);
        btnScan = findViewById(R.id.btnScanQR);
        btnApprove = findViewById(R.id.btnApproveParcel);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);

        db = FirebaseFirestore.getInstance();

        btnScan.setOnClickListener(v -> startQRScan());

        btnApprove.setOnClickListener(v -> approveParcel());

        btnUpdateLocation.setOnClickListener(v -> {
            if (currentParcelDoc != null) {
                updateLocation("Point B"); // You can make it dynamic
            }
        });
    }

    private void startQRScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan Parcel QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String parcelId = result.getContents();
            fetchParcelById(parcelId);
        } else {
            Toast.makeText(this, "Scan cancelled or failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchParcelById(String parcelId) {
        db.collection("Parcels").whereEqualTo("parcelId", parcelId)
                .get().addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        currentParcelDoc = querySnapshot.getDocuments().get(0);
                        String status = currentParcelDoc.getString("status");

                        if ("pending".equals(status)) {
                            showPendingParcel(currentParcelDoc);
                        } else {
                            showApprovedParcel(currentParcelDoc);
                        }

                    } else {
                        Toast.makeText(this, "Parcel not found", Toast.LENGTH_SHORT).show();
                        tvParcelInfo.setText("");
                    }
                });
    }

    private void showPendingParcel(DocumentSnapshot doc) {
        tvParcelInfo.setText("Parcel ID: " + doc.getString("parcelId") + "\nStatus: Pending");
        btnApprove.setVisibility(View.VISIBLE);
        btnUpdateLocation.setVisibility(View.GONE);
    }

    private void showApprovedParcel(DocumentSnapshot doc) {
        btnApprove.setVisibility(View.GONE);
        btnUpdateLocation.setVisibility(View.VISIBLE);

        StringBuilder sb = new StringBuilder();
        sb.append("Parcel ID: ").append(doc.getString("parcelId")).append("\n");
        sb.append("From: ").append(doc.getString("startPoint")).append("\n");
        sb.append("To: ").append(doc.getString("endPoint")).append("\n");
        sb.append("Status: ").append(doc.getString("status")).append("\n");
        sb.append("Current Point: ").append(doc.getString("currentLocation")).append("\n\n");

        // Sender Info
        String senderId = doc.getString("userId");
        db.collection("users").document(senderId).get()
                .addOnSuccessListener(senderDoc -> {
                    sb.append("Sender: ").append(senderDoc.getString("name")).append("\n");

                    // Receiver Info
                    sb.append("Receiver: ").append(doc.getString("receiverName")).append("\n");
                    sb.append("Receiver Address: ").append(doc.getString("receiverAddress")).append("\n");

                    tvParcelInfo.setText(sb.toString());
                });
    }

    private void approveParcel() {
        if (currentParcelDoc != null) {
            currentParcelDoc.getReference().update("status", "approved")
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Parcel Approved!", Toast.LENGTH_SHORT).show();
                        showApprovedParcel(currentParcelDoc);
                    });
        }
    }

    private void updateLocation(String newPoint) {
        if (currentParcelDoc != null) {
            currentParcelDoc.getReference().update("currentLocation", newPoint)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Location Updated", Toast.LENGTH_SHORT).show();
                        showApprovedParcel(currentParcelDoc); // Refresh info
                    });
        }
    }
}
