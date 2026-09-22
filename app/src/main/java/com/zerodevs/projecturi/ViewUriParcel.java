package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewUriParcel extends BaseActivity {

    FirebaseFirestore db;
    FirebaseAuth auth;
    LinearLayout layoutParcels;
    ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_uri_parcel);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        layoutParcels = findViewById(R.id.layoutParcels);
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        loadParcels();
    }

    private void loadParcels() {
        db.collection("Parcels")
                .whereEqualTo("userId", auth.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutParcels.removeAllViews();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        View parcelCard = getLayoutInflater().inflate(R.layout.item_my_urip, layoutParcels, false);

                        TextView parcelcn = parcelCard.findViewById(R.id.parcelcn);
                        Button btnViewDetails = parcelCard.findViewById(R.id.btnViewDetails);
                        Button btnViewRequests = parcelCard.findViewById(R.id.btnViewRequests); // ✅ Properly added
                        TextView parcelstatus = parcelCard.findViewById(R.id.parcelstatus);
                        TextView recname = parcelCard.findViewById(R.id.recname);
                        TextView parcellot = parcelCard.findViewById(R.id.tvlot);
                        TextView parceldate = parcelCard.findViewById(R.id.parceldate);
                        TextView parceldc = parcelCard.findViewById(R.id.parceldc);

                        String parcelId = doc.getString("parcelId");
                        parcelcn.setText("CN#" + parcelId);
                        recname.setText(doc.getString("receiverName"));
                        parcelstatus.setText(doc.getString("status"));
                        parcellot.setText("Lot: " + doc.getDouble("lot"));

                        Double charge = doc.getDouble("Rdc");
                        if (charge != null) {
                            parceldc.setText("Charge: ৳" + charge);
                        }

                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                            parceldate.setText(sdf.format(date));
                        }

                        btnViewDetails.setOnClickListener(v -> {
                            Intent intent = new Intent(this, UriDetails.class);
                            intent.putExtra("parcelDocId", doc.getId());
                            startActivity(intent);
                        });



                        btnViewRequests.setOnClickListener(v -> {
                            Intent intent = new Intent(this, DeliveryRequestActivity.class);
                            intent.putExtra("parcelDocId", doc.getId()); // ✅ Correct parcelDocId
                            startActivity(intent);
                        });




                        layoutParcels.addView(parcelCard);
                    }
                });
    }

}
