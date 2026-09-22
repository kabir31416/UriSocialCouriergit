package com.zerodevs.projecturi;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class WalletActivity extends BaseActivity {

    TextView  username, uid, wbalance, codcharge, tbalance, netbalance;
    Button btnRequestWithdraw;

    ImageView ivBack;
    FirebaseFirestore db;
    FirebaseAuth auth;
    double totalEarned = 0, codCharge = 0, netBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        btnRequestWithdraw = findViewById(R.id.btnRequestWithdraw);

        username = findViewById(R.id.username);
        uid = findViewById(R.id.uid);
        wbalance = findViewById(R.id.wbalance);
        tbalance = findViewById(R.id.tbalance);
        netbalance = findViewById(R.id.netbalance);
        codcharge = findViewById(R.id.codcharge);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadWalletBalance();

        btnRequestWithdraw.setOnClickListener(v -> showWithdrawDialog());
    }

    private void loadWalletBalance() {
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(doc -> {
            double balance = 0.0;

            String userName = doc.getString("name");
            Object balanceObj = doc.get("balance");

            if (balanceObj instanceof Number) {
                balance = ((Number) balanceObj).doubleValue();
            } else if (balanceObj instanceof String) {
                try {
                    balance = Double.parseDouble((String) balanceObj);
                } catch (NumberFormatException e) {
                    balance = 0.0;
                }
            }

            if (balance <= 0.0) {
                totalEarned = 0.00;
                codCharge = 0.00;
                netBalance = 0.00;
            } else {
                totalEarned = balance;
                codCharge = totalEarned * 0.01;
                netBalance = totalEarned - codCharge;
            }


            String nbalance = "৳ " + String.format("%.2f", netBalance);
            String tEarned = "৳ " + String.format("%.2f", totalEarned);
            String codc = "৳ " + String.format("%.2f", codCharge);


            codcharge.setText(codc);
            tbalance.setText(tEarned);
            netbalance.setText(nbalance);
            username.setText(userName);
            wbalance.setText(nbalance);

        });
    }



    private void showWithdrawDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_withdraw, null);
        Spinner spinner = view.findViewById(R.id.spinnerMethod);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etAmount = view.findViewById(R.id.etAmount);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        String[] methods = {"bKash", "Nagad", "Rocket"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        spinner.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String method = spinner.getSelectedItem().toString();
            String strAmount = etAmount.getText().toString().trim();

            if (phone.isEmpty() || strAmount.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(strAmount);
            if (amount > netBalance) {
                Toast.makeText(this, "Amount exceeds available balance", Toast.LENGTH_SHORT).show();
                return;
            }

            String requestId = "WR" + (100000 + new Random().nextInt(900000));
            Map<String, Object> data = new HashMap<>();
            data.put("wruserId", auth.getUid());
            data.put("method", method);
            data.put("phone", phone);
            data.put("amount", -amount);
            data.put("status", "Pending");
            data.put("timestamp", new Date());
            data.put("requestId", requestId);

            db.collection("WithdrawRequests").add(data).addOnSuccessListener(docRef -> {
                db.collection("users").document(auth.getUid()).update("balance", totalEarned - amount);
                Toast.makeText(this, "Withdraw Requested", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadWalletBalance();
            });
        });

        dialog.show();
    }
}
