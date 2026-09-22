package com.zerodevs.projecturi;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class PaymentActivity extends BaseActivity {

    LinearLayout layoutWithdraws;
    TextView tvNoData;
    FirebaseFirestore db;
    FirebaseAuth auth;

    ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        layoutWithdraws = findViewById(R.id.layoutWithdraws);
        tvNoData = findViewById(R.id.tvNoData);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadWithdrawHistory();
    }

    private void loadWithdrawHistory() {
        db.collection("WithdrawRequests")
                .whereEqualTo("wruserId", auth.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutWithdraws.removeAllViews();

                    if (snapshot.isEmpty()) {
                        layoutWithdraws.setVisibility(View.GONE);
                        if (tvNoData != null) tvNoData.setVisibility(View.VISIBLE);
                        return;
                    }

                    layoutWithdraws.setVisibility(View.VISIBLE);
                    if (tvNoData != null) tvNoData.setVisibility(View.GONE);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        View itemView = getLayoutInflater().inflate(R.layout.item_withdraw, layoutWithdraws, false);

                        TextView tvDetails = itemView.findViewById(R.id.tvAmount);
                        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
                        TextView tvDate = itemView.findViewById(R.id.tvDate);
                        TextView tvRID = itemView.findViewById(R.id.tvRID);
                        TextView tvmethode= itemView.findViewById(R.id.tvmethode);


                        String method = Objects.toString(doc.get("method"), "");
                        String requestId = Objects.toString(doc.get("requestId"), "");
                        String status = Objects.toString(doc.get("status"), "");





                        double amount = 0.0;

                        if (doc.get("amount") instanceof Number) {
                            amount = Math.abs(((Number) doc.get("amount")).doubleValue());
                        }

                        Timestamp ts = doc.getTimestamp("timestamp");
                        Date timestamp = ts != null ? ts.toDate() : new Date();
                        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timestamp);

                        tvDetails.setText("\n৳" + String.format("%.2f", amount));
                        tvStatus.setText(status);
                        tvDate.setText(date);
                        tvmethode.setText(method);
                        tvRID.setText("#" + requestId);


                        layoutWithdraws.addView(itemView);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show());
    }
}
