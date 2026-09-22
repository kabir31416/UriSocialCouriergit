package com.zerodevs.projecturi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.*;

public class TrackActivity extends BaseActivity {

    TextView tvDetails, ParcelCn, tvParcelInfo,tvParcelNote, ptrack;

    TextView recn, recm, reca, rect;

    TextView pstatus, pdate, pcod, pnote, pdc;

    TextView rin, rim;

    FirebaseFirestore db;
    DocumentSnapshot currentParcelDoc;

    ImageView qrCodeImage, btnCallRec, btnCallRider;

    ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);


        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        btnCallRec = findViewById(R.id.btnCallRec);
        btnCallRider = findViewById(R.id.btnCallRider);
        recn = findViewById(R.id.recn);
        recm = findViewById(R.id.recm);
        reca = findViewById(R.id.reca);
        rect = findViewById(R.id.rect);
        pstatus = findViewById(R.id.pstatus);
        pdate = findViewById(R.id.pdate);
        pcod = findViewById(R.id.pcod);
        pdc = findViewById(R.id.pdc);
        ptrack = findViewById(R.id.ptrack);
        rin = findViewById(R.id.rin);
        rim = findViewById(R.id.rim);
        pnote = findViewById(R.id.pnote);

        qrCodeImage = findViewById(R.id.qrCodeImage);

        ParcelCn = findViewById(R.id.ParcelCn);

        db = FirebaseFirestore.getInstance();

        // Get parcel ID from intent
        String parcelId = getIntent().getStringExtra("parcelId");
        if (parcelId != null && !parcelId.isEmpty()) {
            fetchParcelById(parcelId);
        } else {
            pdate.setText("No Parcel ID Provided.");
        }
    }

    private void fetchParcelById(String parcelId) {
        db.collection("Parcels").whereEqualTo("parcelId", parcelId)
                .get().addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        currentParcelDoc = querySnapshot.getDocuments().get(0);
                        showParcelInfo(currentParcelDoc);
                    } else {
                        tvParcelInfo.setText("Parcel not found");
                    }
                });
    }

    private void showParcelInfo(DocumentSnapshot doc) {

        StringBuilder Cn = new StringBuilder();
        StringBuilder tvpnote = new StringBuilder();

        StringBuilder tvrecn = new StringBuilder();
        StringBuilder tvrecm = new StringBuilder();
        StringBuilder tvreca = new StringBuilder();
        StringBuilder tvpstatus = new StringBuilder();
        StringBuilder tvpcod = new StringBuilder();
        StringBuilder tvpdc = new StringBuilder();
        StringBuilder tvrin = new StringBuilder();
        StringBuilder tvrim = new StringBuilder();
        StringBuilder tvptrack = new StringBuilder();
        StringBuilder tvps = new StringBuilder();


        String parcelId = doc.getString("parcelId"); // ✅ doc এর ভিতর

        Double charge = doc.getDouble("deliveryCharge");
        if (charge != null) {
            tvpdc.append("৳").append(charge).append("");
        }

        Cn.append("CN#").append(doc.getString("parcelId")).append("");
        tvpnote.append(doc.getString("note")).append("");
        tvrecn.append(doc.getString("receiverName"));
        tvrecm.append(doc.getString("receiverMobile"));
        tvreca.append(doc.getString("deliveryAddress"));
        tvpstatus.append(doc.getString("status"));
        tvpcod.append("COD: ৳").append(doc.getDouble("cod"));
        tvrin.append(doc.getString("riderName"));
        tvrim.append(doc.getString("riderMobile"));
        tvps.append("Police Station: ").append(doc.getString("deliveryUpazila")).append(", ").append(doc.getString("deliveryDistrict"));



        Timestamp timestamp = doc.getTimestamp("timestamp");
        if (timestamp != null) {
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
            pdate.setText(sdf.format(date));
        }

        ParcelCn.setText(Cn.toString());
        recn.setText(tvrecn.toString());
        pnote.setText(tvpnote.toString());
        rect.setText(tvps.toString());
        recm.setText(tvrecm.toString());
        reca.setText(tvreca.toString());
        pcod.setText(tvpcod.toString());
        pdc.setText(tvpdc.toString());
        rin.setText(tvrin.toString());
        rim.setText(tvrim.toString());


        btnCallRider.setOnClickListener(v -> {
            String mobile = rim.getText().toString().trim();  // rim = riderMobile TextView
            if (!mobile.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + mobile));
                startActivity(intent);
            } else {
                Toast.makeText(this, "নাম্বার পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            }
        });


        btnCallRec.setOnClickListener(v -> {
            String mobile = recm.getText().toString().trim();  // rim = riderMobile TextView
            if (!mobile.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + mobile));
                startActivity(intent);
            } else {
                Toast.makeText(this, "নাম্বার পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            }
        });




        // ✅ QR কোড জেনারেট করো এখানেই
        generateQRCode(parcelId);
        showTrackingHistory(doc);

    }


    private void generateQRCode(String parcelId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(parcelId, BarcodeFormat.QR_CODE, 200, 200);
            qrCodeImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "QR কোড জেনারেট করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTrackingHistory(DocumentSnapshot doc) {
        db.collection("Parcels").document(doc.getId()).collection("TrackingHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(snapshot -> {
                    StringBuilder sb = new StringBuilder();
                    for (DocumentSnapshot historyDoc : snapshot) {
                        String loc = historyDoc.getString("location");
                        Timestamp ts = historyDoc.getTimestamp("timestamp");
                        String time = ts != null ? new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(ts.toDate()) : "Unknown";
                        sb.append("\uD83D\uDCE6 ").append(loc).append(" - ").append(time).append("\n\n");
                    }
                    ptrack.append("" + sb.toString());

                });
    }
}
