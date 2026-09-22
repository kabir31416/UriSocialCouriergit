package com.zerodevs.projecturi;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

public class RouteDetailActivity extends BaseActivity {

    TextView tvDetail;
    FirebaseFirestore db;
    String routeId;
    ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        // Back button
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> super.onBackPressed());

        tvDetail = findViewById(R.id.tvRouteDetail);
        db = FirebaseFirestore.getInstance();

        routeId = getIntent().getStringExtra("routeId");

        if (routeId != null && !routeId.isEmpty()) {
            loadDetails();
        } else {
            tvDetail.setText("No route selected.");
        }
    }

    private void loadDetails() {
        db.collection("Routes").document(routeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Fetch data with null-checks
                        String userName = doc.getString("userName") != null ? doc.getString("userName") : "N/A";
                        String phone = doc.getString("phone") != null ? doc.getString("phone") : "N/A";

                        String startDistrict = doc.getString("startDistrict") != null ? doc.getString("startDistrict") : "";
                        String startUpazila = doc.getString("startUpazila") != null ? doc.getString("startUpazila") : "";
                        String startAddress = doc.getString("startAddress") != null ? doc.getString("startAddress") : "";

                        String endDistrict = doc.getString("endDistrict") != null ? doc.getString("endDistrict") : "";
                        String endUpazila = doc.getString("endUpazila") != null ? doc.getString("endUpazila") : "";
                        String endAddress = doc.getString("endAddress") != null ? doc.getString("endAddress") : "";

                        String date = doc.getString("date") != null ? doc.getString("dateTime") : "N/A";
                        String note = doc.getString("note") != null ? doc.getString("note") : "N/A";

                        Double maxWeight = doc.getDouble("maxWeight") != null ? doc.getDouble("maxWeight") : 0.0;

                        // Build detailed info string
                        String detail = "Rider Name: " + userName + "\n"
                                + "Phone: " + phone + "\n\n"
                                + "Start From:\n"
                                + "  District: " + startDistrict + "\n"
                                + "  Upazila: " + startUpazila + "\n"
                                + "  Address: " + startAddress + "\n\n"
                                + "Destination:\n"
                                + "  District: " + endDistrict + "\n"
                                + "  Upazila: " + endUpazila + "\n"
                                + "  Address: " + endAddress + "\n\n"
                                + "Max Weight: " + maxWeight + " kg\n"
                                + "Date: " + date + "\n"
                                + "Note: " + note;

                        tvDetail.setText(detail);

                    } else {
                        tvDetail.setText("Route not found.");
                    }
                })
                .addOnFailureListener(e -> tvDetail.setText("Failed to load route details."));
    }
}
