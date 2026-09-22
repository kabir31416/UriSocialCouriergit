package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

import java.util.*;

public class ViewRouteActivity extends BaseActivity {

    LinearLayout layoutRoutes;

    // Filter spinners
    Spinner spinnerStartDistrict, spinnerStartUpazila;
    Spinner spinnerEndDistrict, spinnerEndUpazila;

    FirebaseFirestore db;
    ImageView ivBack;

    // Lists to hold spinner options
    ArrayList<String> startDistricts = new ArrayList<>();
    ArrayList<String> startUpazilas = new ArrayList<>();
    ArrayList<String> endDistricts = new ArrayList<>();
    ArrayList<String> endUpazilas = new ArrayList<>();

    String selectedStartDistrict = "All", selectedStartUpazila = "All";
    String selectedEndDistrict = "All", selectedEndUpazila = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_route);

        // Bind views
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> super.onBackPressed());

        layoutRoutes = findViewById(R.id.layoutRoutes);

        spinnerStartDistrict = findViewById(R.id.spinnerStartDistrict);
        spinnerStartUpazila = findViewById(R.id.spinnerStartUpazila);
        spinnerEndDistrict = findViewById(R.id.spinnerEndDistrict);
        spinnerEndUpazila = findViewById(R.id.spinnerEndUpazila);

        db = FirebaseFirestore.getInstance();

        loadFilterOptions(); // Load initial options
        setupSpinnerListeners(); // Setup listeners for filter spinners
    }

    /**
     * Load unique district and upazila options from Firestore
     */
    private void loadFilterOptions() {
        db.collection("Routes").get().addOnSuccessListener(snapshots -> {
            startDistricts.clear();
            startUpazilas.clear();
            endDistricts.clear();
            endUpazilas.clear();

            // Add default "All" option
            startDistricts.add("All");
            startUpazilas.add("All");
            endDistricts.add("All");
            endUpazilas.add("All");

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                String sDist = doc.getString("startDistrict");
                String sUpz = doc.getString("startUpazila");
                String eDist = doc.getString("endDistrict");
                String eUpz = doc.getString("endUpazila");

                if (sDist != null && !startDistricts.contains(sDist)) startDistricts.add(sDist);
                if (sUpz != null && !startUpazilas.contains(sUpz)) startUpazilas.add(sUpz);
                if (eDist != null && !endDistricts.contains(eDist)) endDistricts.add(eDist);
                if (eUpz != null && !endUpazilas.contains(eUpz)) endUpazilas.add(eUpz);
            }

            // Set adapters
            spinnerStartDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, startDistricts));
            spinnerStartUpazila.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, startUpazilas));
            spinnerEndDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, endDistricts));
            spinnerEndUpazila.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, endUpazilas));

            // Initial load
            loadRoutes();
        });
    }

    /**
     * Setup spinner listeners for filtering
     */
    private void setupSpinnerListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStartDistrict = spinnerStartDistrict.getSelectedItem().toString();
                selectedStartUpazila = spinnerStartUpazila.getSelectedItem().toString();
                selectedEndDistrict = spinnerEndDistrict.getSelectedItem().toString();
                selectedEndUpazila = spinnerEndUpazila.getSelectedItem().toString();

                loadRoutes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerStartDistrict.setOnItemSelectedListener(listener);
        spinnerStartUpazila.setOnItemSelectedListener(listener);
        spinnerEndDistrict.setOnItemSelectedListener(listener);
        spinnerEndUpazila.setOnItemSelectedListener(listener);
    }

    /**
     * Load routes from Firestore and apply selected filters
     */
    private void loadRoutes() {
        db.collection("Routes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            layoutRoutes.removeAllViews();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String sDist = doc.getString("startDistrict");
                String sUpz = doc.getString("startUpazila");
                String eDist = doc.getString("endDistrict");
                String eUpz = doc.getString("endUpazila");


                // Apply filters
                if (!selectedStartDistrict.equals("All") && !selectedStartDistrict.equals(sDist)) continue;
                if (!selectedStartUpazila.equals("All") && !selectedStartUpazila.equals(sUpz)) continue;
                if (!selectedEndDistrict.equals("All") && !selectedEndDistrict.equals(eDist)) continue;
                if (!selectedEndUpazila.equals("All") && !selectedEndUpazila.equals(eUpz)) continue;

                // Inflate route card layout
                View routeCard = getLayoutInflater().inflate(R.layout.item_routes_card, layoutRoutes, false);

                // TextViews in the card
                TextView tvName = routeCard.findViewById(R.id.name);
                TextView tvFrom = routeCard.findViewById(R.id.from);
                TextView tvTo = routeCard.findViewById(R.id.to);
                TextView tvDate = routeCard.findViewById(R.id.date);

                // Button
                Button btnViewRouteDetails = routeCard.findViewById(R.id.btnViewRouteDetails);

                // Get data from Firestore
                String routeId = doc.getId();
                String userName = doc.getString("userName");
                String dateTime = doc.getString("dateTime");

                // Prepare display strings
                String displayName = "" + userName;
                String displayFrom = "" + sUpz + ", " + sDist ;
                String displayTo = "" + eUpz + ", " + eDist;
                String displayDate = "" + dateTime;

                // Set values to TextViews
                tvName.setText(displayName);
                tvFrom.setText(displayFrom);
                tvTo.setText(displayTo);
                tvDate.setText(displayDate);

                // Button click listener
                btnViewRouteDetails.setOnClickListener(v -> {
                    Intent intent = new Intent(ViewRouteActivity.this, RouteDetailActivity.class);
                    intent.putExtra("routeId", routeId);
                    startActivity(intent);
                });

                // Add card to layout
                layoutRoutes.addView(routeCard);
            }
        });
    }
}
