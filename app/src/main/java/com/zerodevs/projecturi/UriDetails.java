package com.zerodevs.projecturi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UriDetails extends BaseActivity {

    TextView ParcelCn, pstatus, pdc, pnote, pdes, ptype, pweight;
    TextView recn, reca, rect;
    TextView sen, sena, sent, sem;

    Button btnRequest, btnChat;
    ImageView qrCodeImage, ivBack, btnCallSender;

    FirebaseFirestore db;
    String parcelDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri_details);

        db = FirebaseFirestore.getInstance();
        parcelDocId = getIntent().getStringExtra("parcelDocId");

        initViews();

        ivBack.setOnClickListener(v -> onBackPressed());

        loadParcelDetails();
    }

    private void initViews() {

        ParcelCn = findViewById(R.id.ParcelCn);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        btnRequest = findViewById(R.id.btnRequest);
        btnChat = findViewById(R.id.btnChat);
        ivBack = findViewById(R.id.ivBack);
        btnCallSender = findViewById(R.id.btnCallSender);

        recn = findViewById(R.id.recn);
        reca = findViewById(R.id.reca);
        rect = findViewById(R.id.rect);

        pstatus = findViewById(R.id.pstatus);
        pdc = findViewById(R.id.pdc);
        pnote = findViewById(R.id.pnote);
        pdes = findViewById(R.id.pdes);
        ptype = findViewById(R.id.ptype);
        pweight = findViewById(R.id.pweight);

        sen = findViewById(R.id.sen);
        sena = findViewById(R.id.sena);
        sent = findViewById(R.id.sent);
        sem = findViewById(R.id.sem);
    }

    private void loadParcelDetails() {

        db.collection("Parcels").document(parcelDocId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Parcel not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String parcelId = doc.getString("parcelId");

                    ParcelCn.setText("CN#" + parcelId);
                    pstatus.setText(doc.getString("status"));
                    pnote.setText(doc.getString("note"));
                    pdes.setText(doc.getString("description"));
                    ptype.setText(doc.getString("parcelType"));
                    pweight.setText(doc.getDouble("weight") + " Kg");

                    Double charge = doc.getDouble("deliveryCharge");
                    if (charge != null) {
                        pdc.setText("৳" + charge);
                    }

                    recn.setText(doc.getString("receiverName"));
                    reca.setText(doc.getString("deliveryAddress"));
                    rect.setText(doc.getString("deliveryUpazila")
                            + ", " + doc.getString("deliveryDistrict"));

                    sen.setText(doc.getString("senderName"));
                    sem.setText(doc.getString("senderPhone"));
                    sena.setText(doc.getString("pickupAddress"));
                    sent.setText(doc.getString("pickupUpazila")
                            + ", " + doc.getString("pickupDistrict"));

                    generateQRCode(parcelId);

                    btnCallSender.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(android.net.Uri
                                .parse("tel:" + sem.getText().toString()));
                        startActivity(intent);
                    });

                    btnRequest.setOnClickListener(v ->
                            showProposeDialog(parcelId, parcelDocId));

                    btnChat.setOnClickListener(v ->
                            openChat(parcelId, doc.getString("userId")));
                });
    }


    // ===================== PROPOSE DIALOG =====================

    private void showProposeDialog(String parcelId, String parcelDocId) {

        new AlertDialog.Builder(this)
                .setTitle("Custom Delivery Charge")
                .setMessage("আপনি কি কাস্টম ডেলিভারি চার্জ অফার করতে চান?")
                .setPositiveButton("হ্যাঁ", (dialog, which) -> {

                    View view = getLayoutInflater()
                            .inflate(R.layout.dialog_propose_charge, null);

                    EditText etCharge =
                            view.findViewById(R.id.etCharge);

                    new AlertDialog.Builder(this)
                            .setTitle("Propose Your Charge")
                            .setView(view)
                            .setPositiveButton("Send Request",
                                    (d, w) -> {

                                        String chargeStr =
                                                etCharge.getText()
                                                        .toString().trim();

                                        if (chargeStr.isEmpty()) {
                                            Toast.makeText(
                                                    this,
                                                    "চার্জ লিখুন",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            return;
                                        }

                                        double charge =
                                                Double.parseDouble(chargeStr);

                                        sendDeliveryRequest(
                                                parcelId,
                                                parcelDocId,
                                                charge
                                        );
                                    })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("না", (dialog, which) -> {
                    sendDeliveryRequest(parcelId, parcelDocId, -1);
                })
                .show();
    }

    // ===================== SEND REQUEST (DUPLICATE SAFE) =====================

    private void sendDeliveryRequest(
            String parcelId,
            String parcelDocId,
            double proposedCharge) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String riderId = user.getUid();

        // 🔒 Duplicate block
        db.collection("Parcels")
                .document(parcelDocId)
                .collection("DeliveryRequests")
                .whereEqualTo("requesterId", riderId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        Toast.makeText(
                                this,
                                "আপনি আগেই request পাঠিয়েছেন",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    db.collection("users")
                            .document(riderId)
                            .get()
                            .addOnSuccessListener(riderDoc -> {

                                String riderName =
                                        riderDoc.getString("name");
                                String riderPhone =
                                        riderDoc.getString("phone");

                                Map<String, Object> req = new HashMap<>();
                                req.put("requesterId", riderId);
                                req.put("requesterName", riderName);
                                req.put("requesterPhone", riderPhone);
                                req.put("parcelId", parcelId);
                                req.put("status", "pending");
                                req.put("timestamp",
                                        FieldValue.serverTimestamp());

                                if (proposedCharge >= 0) {
                                    req.put("proposedCharge",
                                            proposedCharge);
                                }

                                db.collection("Parcels")
                                        .document(parcelDocId)
                                        .collection("DeliveryRequests")
                                        .add(req)
                                        .addOnSuccessListener(unused -> {

                                            notifySender(
                                                    parcelDocId,
                                                    riderName,
                                                    parcelId
                                            );

                                            Toast.makeText(
                                                    this,
                                                    "Request sent",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            btnRequest.setEnabled(false);
                                            btnRequest.setText("Requested");
                                            btnRequest.setVisibility(View.GONE);
                                        });
                            });
                });
    }

    // ===================== NOTIFICATION =====================

    private void notifySender(
            String parcelDocId,
            String riderName,
            String parcelId) {

        db.collection("Parcels")
                .document(parcelDocId)
                .get()
                .addOnSuccessListener(parcelDoc -> {

                    String senderId =
                            parcelDoc.getString("userId");

                    db.collection("users")
                            .document(senderId)
                            .get()
                            .addOnSuccessListener(senderDoc -> {

                                String playerId =
                                        senderDoc.getString(
                                                "oneSignalPlayerId");

                                if (playerId != null) {
                                    sendOneSignalNotification(
                                            playerId,
                                            "New Delivery Request 🚚",
                                            riderName +
                                                    " sent you a request",
                                            parcelId
                                    );
                                }
                            });
                });
    }

    private void sendOneSignalNotification(
            String playerId,
            String title,
            String message,
            String parcelId) {

        try {
            JSONObject json = new JSONObject();
            json.put("include_player_ids",
                    new JSONArray().put(playerId));

            json.put("headings",
                    new JSONObject().put("en", title));

            json.put("contents",
                    new JSONObject().put("en", message));

            json.put("data",
                    new JSONObject().put("parcelId", parcelId));

            OneSignal.postNotification(json, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== CHAT =====================

    private void openChat(String parcelId, String senderId) {

        String myId = FirebaseAuth.getInstance().getUid();

        if (myId.equals(senderId)) {
            Toast.makeText(
                    this,
                    "You cannot chat with yourself",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String chatId = myId.compareTo(senderId) < 0
                ? myId + "_" + senderId + "_" + parcelId
                : senderId + "_" + myId + "_" + parcelId;

        Intent intent =
                new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", senderId);
        startActivity(intent);
    }

    // ===================== QR =====================

    private void generateQRCode(String parcelId) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(
                    parcelId,
                    BarcodeFormat.QR_CODE,
                    200,
                    200
            );
            qrCodeImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
