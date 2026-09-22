package com.zerodevs.projecturi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends BaseActivity {

    private EditText emailOrPhoneET, passwordET;
    private Button loginBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView regpage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        // যদি ইউজার ইতোমধ্যে লগইন করা থাকে, তাহলে সরাসরি হোম অ্যাক্টিভিটিতে পাঠিয়ে দাও
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish(); // যাতে ব্যাকপ্রেস করলে আবার লগইন স্ক্রিন না আসে
            return;
        }

        emailOrPhoneET = findViewById(R.id.emailOrPhone);
        passwordET = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        regpage = findViewById(R.id.regpage);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        regpage.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        loginBtn.setOnClickListener(v -> {
            String input = emailOrPhoneET.getText().toString().trim();
            String password = passwordET.getText().toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "ইমেইল/মোবাইল এবং পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            if (input.contains("@")) {
                // ইমেইল দিয়ে লগইন
                loginWithEmail(input, password);
            } else {
                // ফোন নাম্বার দিয়ে লগইন
                loginWithPhone(input, password);
            }
        });
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "ভুল ইমেইল বা পাসওয়ার্ড", Toast.LENGTH_SHORT).show();
                });
    }

    private void loginWithPhone(String phone, String password) {
        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String email = querySnapshot.getDocuments().get(0).getString("email");
                        loginWithEmail(email, password);  // ফোন নাম্বারের মাধ্যমে ইমেইল বের করে লগইন
                    } else {
                        Toast.makeText(this, "এই নাম্বারে কোন একাউন্ট নেই", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "ডাটাবেজ থেকে তথ্য আনতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show();
                });
    }
}
