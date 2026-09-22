package com.zerodevs.projecturi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ParcelStatusActivity extends BaseActivity {

    EditText etManualParcelId;
    Button btnUpdateLocation, btnEditParcel, btnAssignRider, btnstatusup, btnGenerateLabel;
    ImageView btnSearchParcel, btnScan, ivBack;
    TextView tvParcelInfo;
    Spinner spinnerStatus, spinnerPoints;
    FirebaseFirestore db;
    DocumentSnapshot currentParcelDoc;
    ArrayAdapter<String> pointAdapter, statusAdapter;
    List<String> pointList = new ArrayList<>();
    List<String> statusList = Arrays.asList("Picked Up", "Received In", "Sent To", "Delivered", "Return To");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_status);

        etManualParcelId = findViewById(R.id.etManualParcelId);
        btnSearchParcel = findViewById(R.id.btnSearchParcel);
        btnScan = findViewById(R.id.btnScanQR);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);
        btnEditParcel = findViewById(R.id.btnEditParcel);
        btnAssignRider = findViewById(R.id.btnAssignRider);
        tvParcelInfo = findViewById(R.id.tvParcelInfo);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerPoints = findViewById(R.id.spinnerPoints);
        btnstatusup = findViewById(R.id.btnstatusup);
        btnGenerateLabel = findViewById(R.id.btnGenerateLabel);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> super.onBackPressed());

        db = FirebaseFirestore.getInstance();

        statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusList);
        spinnerStatus.setAdapter(statusAdapter);
        loadPointsIntoSpinner();

        btnScan.setOnClickListener(v -> startQRScan());
        btnSearchParcel.setOnClickListener(v -> {
            String id = etManualParcelId.getText().toString().trim();
            if (!id.isEmpty()) fetchParcelById(id);
        });

        btnUpdateLocation.setOnClickListener(v -> {
            if (currentParcelDoc == null) {
                Toast.makeText(this, "No parcel selected", Toast.LENGTH_SHORT).show();
                return;
            }
            String status = spinnerStatus.getSelectedItem().toString();
            String point = spinnerPoints.getSelectedItem() != null ? spinnerPoints.getSelectedItem().toString() : "";

            if (!"Approved".equals(currentParcelDoc.getString("status"))) {
                Toast.makeText(this, "Parcel must be Approved to update location", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!status.isEmpty()) updateLocation(status, point);
            else Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
        });

        btnEditParcel.setOnClickListener(v -> showEditDialog());
        btnAssignRider.setOnClickListener(v -> showAssignRiderDialog());
        btnstatusup.setOnClickListener(v -> showStatusDialog());
        btnGenerateLabel.setOnClickListener(v -> generateParcelLabel());
    }

    private void generateParcelLabel() {
        if (currentParcelDoc == null) {
            Toast.makeText(this, "No parcel selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate layout
        View labelView = LayoutInflater.from(this).inflate(R.layout.label_layout, null);

        // Layout views
        TextView tvSenderName = labelView.findViewById(R.id.tvSenderName);
        TextView tvParcelId = labelView.findViewById(R.id.tvSfId);
        TextView tvDeliveryType = labelView.findViewById(R.id.tvDeliveryType);
        TextView tvWeight = labelView.findViewById(R.id.tvWeight);
        ImageView ivBarcode = labelView.findViewById(R.id.ivBarcode);
        TextView tvReceiverName = labelView.findViewById(R.id.tvReceiverName);
        TextView tvReceiverPhone = labelView.findViewById(R.id.tvReceiverPhone);
        TextView tvReceiverAddress = labelView.findViewById(R.id.tvReceiverAddress);
        TextView tvCodAmount = labelView.findViewById(R.id.tvCodAmount);
        ImageView ivQr = labelView.findViewById(R.id.ivQrCode);

        // Get data from Firestore
        String parcelId = currentParcelDoc.getString("parcelId");




        String receiverName = currentParcelDoc.getString("receiverName");
        String receiverPhone = currentParcelDoc.getString("receiverMobile");
        String receiverAddress = currentParcelDoc.getString("deliveryAddress");
        String dtype = currentParcelDoc.getString("deliveryType");
        String weight = String.valueOf(currentParcelDoc.getDouble("weight"));
        String codamount = String.valueOf(currentParcelDoc.getDouble("cod"));
        String sendername = currentParcelDoc.getString("senderName");





        // Set values
        tvSenderName.setText(sendername);
        tvDeliveryType.setText("D. Type: " + dtype);
        tvWeight.setText("Weight: " + weight + "Kg");
        tvReceiverPhone.setText("Mobile: " + receiverPhone);
        tvReceiverAddress.setText("Address: " +receiverAddress);
        tvCodAmount.setText("৳ " + codamount+ "Taka");
        tvParcelId.setText("CN#" + parcelId);
        tvReceiverName.setText("Name: " + receiverName);


        // Generate QR and Barcode
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.encodeBitmap(parcelId, BarcodeFormat.QR_CODE, 300, 300);
            ivQr.setImageBitmap(qrBitmap);

            Bitmap barcodeBitmap = encoder.encodeBitmap(parcelId, BarcodeFormat.CODE_128, 600, 200);
            ivBarcode.setImageBitmap(barcodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Code generation failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert view to bitmap
        labelView.setDrawingCacheEnabled(true);
        labelView.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        labelView.layout(0, 0, labelView.getMeasuredWidth(), labelView.getMeasuredHeight());
        labelView.buildDrawingCache();
        Bitmap labelBitmap = Bitmap.createBitmap(labelView.getDrawingCache());
        labelView.setDrawingCacheEnabled(false);

        // Create PDF
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(labelBitmap.getWidth(), labelBitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        page.getCanvas().drawBitmap(labelBitmap, 0, 0, null);
        document.finishPage(page);

        // Save to Downloads/ParcelLabels
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ParcelLabels");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, parcelId + "_label.pdf");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
            Toast.makeText(this, "Label saved in Downloads/ParcelLabels", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        document.close();

        // Open PDF
        try {
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "PDF open করতে সমস্যা হচ্ছে। PDF viewer ইনস্টল আছে তো?", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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
            fetchParcelById(result.getContents());
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchParcelById(String parcelId) {
        db.collection("Parcels").whereEqualTo("parcelId", parcelId)
                .get().addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        currentParcelDoc = querySnapshot.getDocuments().get(0);
                        showApprovedParcel(currentParcelDoc);
                    } else {
                        tvParcelInfo.setText("Parcel not found");
                    }
                });
    }

    private void showApprovedParcel(DocumentSnapshot doc) {
        btnUpdateLocation.setVisibility(View.VISIBLE);
        spinnerStatus.setVisibility(View.VISIBLE);
        spinnerPoints.setVisibility(View.VISIBLE);
        btnEditParcel.setVisibility(View.VISIBLE);
        btnAssignRider.setVisibility(View.VISIBLE);

        StringBuilder sb = new StringBuilder();

        sb.append("Parcel ID: ").append(doc.getString("parcelId")).append("\n");
        sb.append("From: ").append(doc.getString("startPoint")).append("\n");
        sb.append("To: ").append(doc.getString("endPoint")).append("\n");
        sb.append("Status: ").append(doc.getString("status")).append("\n");
        sb.append("Current Point: ").append(doc.getString("currentLocation")).append("\n\n");

        String senderId = doc.getString("userId");
        db.collection("users").document(senderId).get().addOnSuccessListener(senderDoc -> {
            sb.append("Sender: ").append(senderDoc.getString("name")).append("\n");
            sb.append("Receiver: ").append(doc.getString("receiverName")).append("\n");
            sb.append("Receiver Address: ").append(doc.getString("receiverAddress")).append("\n");

            tvParcelInfo.setText(sb.toString());
            showTrackingHistory(doc);
        });
    }

    private void showTrackingHistory(DocumentSnapshot doc) {
        db.collection("Parcels").document(doc.getId()).collection("TrackingHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(snapshot -> {
                    StringBuilder sb = new StringBuilder("\nTracking History:\n");
                    for (DocumentSnapshot historyDoc : snapshot) {
                        String loc = historyDoc.getString("location");
                        Timestamp ts = historyDoc.getTimestamp("timestamp");
                        String time = ts != null ? new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(ts.toDate()) : "Unknown";
                        sb.append("\uD83D\uDCE6 ").append(loc).append(" - ").append(time).append("\n\n");
                    }
                    tvParcelInfo.append(sb.toString());
                });
    }

    private void updateLocation(String status, String point) {
        if (currentParcelDoc != null) {
            WriteBatch batch = db.batch();
            DocumentReference parcelRef = currentParcelDoc.getReference();

            if (!point.isEmpty()) {
                batch.update(parcelRef, "currentLocation", point);
            }

            if ("Delivered".equals(status)) {
                batch.update(parcelRef, "status", "Delivered");
            }

            DocumentReference historyRef = parcelRef.collection("TrackingHistory").document();
            Map<String, Object> history = new HashMap<>();
            String formattedStatus;
            String safePoint = point != null ? point : "";

            if (status == null) {
                formattedStatus = "Status Not Available";
            } else {
                switch (status) {
                    case "Picked Up":
                        formattedStatus = "Picked Up From " + safePoint;
                        break;
                    case "Received In":
                        formattedStatus = "Received In " + safePoint;
                        break;
                    case "Sent To":
                        formattedStatus = "Sent To " + safePoint;
                        break;
                    case "Delivered":
                        formattedStatus = "Delivered At " + safePoint;
                        break;
                    default:
                        formattedStatus = status;
                        break;
                }
            }


            history.put("location", formattedStatus);
            history.put("timestamp", FieldValue.serverTimestamp());
            history.put("updatedBy", "admin");
            batch.set(historyRef, history);

            if ("Delivered".equals(status)) {
                Boolean isCodPaid = currentParcelDoc.getBoolean("isCodPaid");
                double codAmount = currentParcelDoc.getDouble("cod") != null ? currentParcelDoc.getDouble("cod") : 0.00;
                String userId = currentParcelDoc.getString("userId");

                if (Boolean.TRUE.equals(isCodPaid)) {
                    Toast.makeText(this, "COD already added", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (userId != null && codAmount > 0) {
                    DocumentReference userRef = db.collection("users").document(userId);
                    userRef.get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            Double currentBalance = userDoc.getDouble("balance");
                            if (currentBalance == null) currentBalance = 0.00;

                            double updatedBalance = currentBalance + codAmount;
                            batch.update(parcelRef, "isCodPaid", true);
                            userRef.update("balance", updatedBalance).addOnSuccessListener(unused -> {
                                batch.commit().addOnSuccessListener(unused2 -> {
                                    Toast.makeText(this, "Delivered & COD added to balance", Toast.LENGTH_SHORT).show();
                                    showApprovedParcel(currentParcelDoc);
                                });
                            });
                        }
                    });
                    return;
                }
            }

            batch.commit().addOnSuccessListener(unused -> {
                Toast.makeText(this, "Location Updated", Toast.LENGTH_SHORT).show();
                showApprovedParcel(currentParcelDoc);
            });
        }
    }

    private void loadPointsIntoSpinner() {
        db.collection("Points").get().addOnSuccessListener(snapshot -> {
            pointList.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                pointList.add(doc.getString("pointName"));
            }
            pointAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pointList);
            spinnerPoints.setAdapter(pointAdapter);
        });
    }

    private void showStatusDialog() {
        if (currentParcelDoc == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_status, null);
        Spinner statusSpinner = dialogView.findViewById(R.id.statusup);

        List<String> statusOptions = Arrays.asList("Pending", "Approved", "Delivered", "Canceled", "Pending Payment");
        StatusColorAdapter colorAdapter = new StatusColorAdapter(this, statusOptions);
        statusSpinner.setAdapter(colorAdapter);

        String currentStatus = currentParcelDoc.getString("status");
        int selectedIndex = statusOptions.indexOf(currentStatus);
        if (selectedIndex != -1) statusSpinner.setSelection(selectedIndex);

        new AlertDialog.Builder(this)
                .setTitle("Edit Parcel Status")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedStatus = statusSpinner.getSelectedItem().toString();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", selectedStatus);

                    currentParcelDoc.getReference().update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                                showApprovedParcel(currentParcelDoc);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog() {
        if (currentParcelDoc == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_parcel, null);
        EditText etReceiverName = dialogView.findViewById(R.id.etReceiverName);
        EditText etReceiverAddress = dialogView.findViewById(R.id.etReceiverAddress);

        etReceiverName.setText(currentParcelDoc.getString("receiverName"));
        etReceiverAddress.setText(currentParcelDoc.getString("receiverAddress"));

        new AlertDialog.Builder(this)
                .setTitle("Edit Parcel Info")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("receiverName", etReceiverName.getText().toString().trim());
                    updates.put("receiverAddress", etReceiverAddress.getText().toString().trim());

                    currentParcelDoc.getReference().update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Parcel info updated", Toast.LENGTH_SHORT).show();
                                showApprovedParcel(currentParcelDoc);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAssignRiderDialog() {
        if (currentParcelDoc == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_assign_rider, null);
        EditText etRiderName = dialogView.findViewById(R.id.etRiderName);
        EditText etRiderPhone = dialogView.findViewById(R.id.etRiderPhone);

        new AlertDialog.Builder(this)
                .setTitle("Assign Rider")
                .setView(dialogView)
                .setPositiveButton("Assign", (dialog, which) -> {
                    String riderName = etRiderName.getText().toString().trim();
                    String riderPhone = etRiderPhone.getText().toString().trim();

                    if (!riderName.isEmpty() && !riderPhone.isEmpty()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("riderName", riderName);
                        updates.put("riderPhone", riderPhone);

                        DocumentReference parcelRef = currentParcelDoc.getReference();
                        parcelRef.update(updates).addOnSuccessListener(unused -> {
                            Map<String, Object> history = new HashMap<>();
                            history.put("location", "Rider Assigned: " + riderName + " (" + riderPhone + ")");
                            history.put("timestamp", FieldValue.serverTimestamp());
                            history.put("updatedBy", "admin");

                            parcelRef.collection("TrackingHistory").add(history);
                            Toast.makeText(this, "Rider Assigned", Toast.LENGTH_SHORT).show();
                            showApprovedParcel(currentParcelDoc);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
