package com.zerodevs.projecturi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class Sender extends BaseActivity {

    // UI Components
    private ImageView myurireq, routes;
    private Button addh2h;

    // Chart
    private BarChart barChart;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    // Parcel counters
    int total = 0;
    int completed = 0;
    int canceled = 0;
    int inProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sender);

        // ---------- UI INIT ----------
        addh2h = findViewById(R.id.addh2h);
        myurireq = findViewById(R.id.myurireq);
        routes = findViewById(R.id.routes);
        barChart = findViewById(R.id.parcelBarChart);

        // ---------- BUTTON CLICKS ----------
        addh2h.setOnClickListener(v ->
                startActivity(new Intent(Sender.this, H2hParcel.class)));

        myurireq.setOnClickListener(v ->
                startActivity(new Intent(Sender.this, Myh2hrequest.class)));

        routes.setOnClickListener(v ->
                startActivity(new Intent(Sender.this, ViewRouteActivity.class)));

        // ---------- FIREBASE AUTH ----------
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Safety check (user not logged in)
        if (currentUser == null) {
            return;
        }

        String currentUid = currentUser.getUid();
        db = FirebaseFirestore.getInstance();


        // ---------- LOAD DATA ----------
        loadParcelSummary();
    }

    /**
     * Reads parcel data from Firebase in realtime
     * Counts parcel status category-wise
     */
    private void loadParcelSummary() {

        db = FirebaseFirestore.getInstance();

        db.collection("Parcels")
                .whereEqualTo("userId", currentUser.getUid()) // 🔥 logged-in user
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    total = completed = canceled = inProgress = 0;

                    for (QueryDocumentSnapshot doc : value) {

                        total++;

                        String status = doc.getString("status");
                        if (status == null) continue;

                        status = status.trim().toLowerCase();

                        switch (status) {

                            // In Progress
                            case "picked up":
                            case "received in":
                            case "sent to":
                            case "accepted":
                            case "pending":
                                inProgress++;
                                break;

                            // Completed
                            case "delivered":
                                completed++;
                                break;

                            // Canceled
                            case "expired":
                            case "return to":
                                canceled++;
                                break;
                        }
                    }

                    setupChart(total, completed, canceled, inProgress);
                });
    }


    /**
     * Sets up MPAndroidChart BarChart
     */
    private void setupChart(int total, int completed, int inProgress, int canceled ) {

        // ---------- Bar Values ----------
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, total));        // Total
        entries.add(new BarEntry(1, completed));    // Completed
        entries.add(new BarEntry(2, inProgress));   // In Progress
        entries.add(new BarEntry(3, canceled));     // Canceled


        // ---------- Dataset ----------
        BarDataSet dataSet = new BarDataSet(entries, "Parcel Summary");

        // 🎨 Multi Colors (index wise)
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#086471"));    // Total
        colors.add(Color.parseColor("#4CAF50"));    // Green - Completed
        colors.add(Color.parseColor("#F6BE00"));    // Yellow - In Progress
        colors.add(Color.parseColor("#F44336"));    // Red - Canceled


        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        // ---------- BarData ----------
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

        // ---------- X Axis ----------
        String[] labels = {"Total", "Completed","In Progress" , "Canceled"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // ---------- Y Axis ----------
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);

        // ---------- Design ----------
        barChart.getDescription().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

}
