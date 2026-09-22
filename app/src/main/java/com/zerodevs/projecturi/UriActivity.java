package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UriActivity extends BaseActivity{

    LinearLayout layoutUriParcels;
    FirebaseFirestore db;

    Button btnAccept;

    ImageView myuri, ivBack, myurireq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri);

        layoutUriParcels = findViewById(R.id.layoutUriParcels);
        layoutUriParcels = findViewById(R.id.layoutUriParcels);

        myuri = findViewById(R.id.myuri);


        myuri.setOnClickListener(v -> startActivity(new Intent(UriActivity.this, MyUriParcelsActivity.class)));
        db = FirebaseFirestore.getInstance();

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });


        loadUriParcels();
    }

    private void loadUriParcels() {
        db.collection("Parcels")
                .whereEqualTo("parcelCat", "H2h")
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutUriParcels.removeAllViews();

                    long currentTime = System.currentTimeMillis();
                    long expireTime = currentTime - (48 * 60 * 60 * 1000); // ৪৮ ঘণ্টা আগে
                    long soonExpireTime = currentTime - (44 * 60 * 60 * 1000); // ৪৪ ঘণ্টা আগে

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp == null) continue;

                        long parcelTime = timestamp.toDate().getTime();
                        String status = doc.getString("status");

                        // 1. Expired → স্ট্যাটাস চেঞ্জ করে দাও এবং স্কিপ করো
                        if (parcelTime < expireTime) {
                            doc.getReference().update("status", "Expired");
                            continue; // লিস্টে দেখানোর দরকার নেই
                        }

                        // 2. Expire Soon → স্ট্যাটাস চেঞ্জ করো
                        if (parcelTime < soonExpireTime && !"expire_soon".equals(status)) {
                            doc.getReference().update("status", "expire_soon");
                        }

                        // 3. Valid or Expire Soon → শো করো
                        View parcelCard = getLayoutInflater().inflate(R.layout.item_uri_parcel_card, layoutUriParcels, false);

                        TextView parcelcn = parcelCard.findViewById(R.id.parcelcn);
                        Button btnViewDetails = parcelCard.findViewById(R.id.btnViewDetails);
                        TextView parcelstatus = parcelCard.findViewById(R.id.parcelstatus);
                        TextView recname = parcelCard.findViewById(R.id.recname);
                        TextView parcellot = parcelCard.findViewById(R.id.tvlot);
                        TextView parceldate = parcelCard.findViewById(R.id.parceldate);
                        TextView parceldc = parcelCard.findViewById(R.id.parceldc);

                        TextView pickupDistrict = parcelCard.findViewById(R.id.pickupDistrict);
                        TextView deliveryDistrict = parcelCard.findViewById(R.id.deliveryDistrict);

                        String parcelId = doc.getString("parcelId");
                        parcelcn.setText("CN#" + parcelId);
                        recname.setText(doc.getString("receiverName"));
                        parcelstatus.setText(doc.getString("status"));
                        parcellot.setText("Lot: " + doc.getDouble("lot"));
                        pickupDistrict.setText(doc.getString("pickupDistrict"));
                        deliveryDistrict.setText(doc.getString("deliveryDistrict"));

                        Double charge = doc.getDouble("deliveryCharge");
                        if (charge != null) {
                            parceldc.setText("Charge: ৳" + charge);
                        }

                        Date date = timestamp.toDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                        parceldate.setText(sdf.format(date));

                        btnViewDetails.setOnClickListener(v -> {
                            Intent intent = new Intent(this, UriDetails.class);
                            intent.putExtra("parcelDocId", doc.getId());
                            startActivity(intent);
                        });

                        layoutUriParcels.addView(parcelCard);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "লোডিংয়ে সমস্যা: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }







}
