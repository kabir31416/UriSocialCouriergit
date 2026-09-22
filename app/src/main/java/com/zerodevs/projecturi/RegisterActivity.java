package com.zerodevs.projecturi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RegisterActivity extends BaseActivity {

    private EditText nameET, fatherET, motherET, dobET, nidET, phoneET, emailET, addressET, passwordET;
    private RadioGroup genderRG;
    private Button registerBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView loginpage;

    private String generatedOTP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameET = findViewById(R.id.name);
        dobET = findViewById(R.id.dob);
        nidET = findViewById(R.id.nid);
        phoneET = findViewById(R.id.phone);
        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password); // নতুন পাসওয়ার্ড ফিল্ড

        loginpage = findViewById(R.id.loginpage);

        registerBtn = findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(v -> registerUser());


        loginpage.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));


        dobET.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        String formattedDate = String.format(
                                Locale.getDefault(),
                                "%02d-%02d-%04d",
                                d, m + 1, y
                        );
                        dobET.setText(formattedDate);
                    },
                    year, month, day
            );

            // Optional: future date block (DOB এর জন্য দরকার)
            datePickerDialog.getDatePicker()
                    .setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });



    }



    private void registerUser() {
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();
        String phone = phoneET.getText().toString().trim();
        String nid = nidET.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || phone.isEmpty() || nid.isEmpty()) {
            Toast.makeText(this, "Email, Password, Phone এবং NID আবশ্যক", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ NID validation: 10 বা 13 ডিজিট হতে হবে
        if (!nid.matches("\\d{10}|\\d{13}")) {
            nidET.setError("NID অবশ্যই 10 অথবা 13 ডিজিটের হতে হবে");
            nidET.requestFocus();
            return;
        }

        if (!phone.matches("01[0-9]{9}")) {
            phoneET.setError("বৈধ বাংলাদেশি মোবাইল নম্বর দিন");
            phoneET.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordET.setError("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে");
            passwordET.requestFocus();
            return;
        }

        sendOtpToPhone(phone);
    }



    private void saveUserToFirestore(String uid) {
        String name = nameET.getText().toString();
        String dob = dobET.getText().toString();
        String email = emailET.getText().toString();
        String nid = nidET.getText().toString();
        String phone = phoneET.getText().toString();


        String userIdNumber = "UID" + (int)(Math.random() * 900000 + 100000);

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userIdNumber);
        user.put("name", name);
        user.put("dob", dob);
        user.put("nid", nid);
        user.put("phone", phone);
        user.put("email", email);
        user.put("balance", 0.00);
        user.put("status", "pending");
        user.put("isAdmin", false);

        db.collection("users").document(uid).set(user).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Registration Succesfull !!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }



    private void sendOtpToPhone(String phone) {
        generatedOTP = String.valueOf(100000 + new Random().nextInt(900000)); // 6-digit OTP

        String fullPhone = "880" + phone.substring(phone.length() - 10); // বাংলাদেশি ফরম্যাট

        String apiUrl = "https://bulksmsbd.net/api/smsapi";
        String apiKey = "E9UedDGJTK6mv5CJQORP";
        String senderId = "8809617621429";
        String message = "Your URI OTP is " + generatedOTP;

        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "api_key=" + apiKey + "&senderid=" + senderId + "&number=" + fullPhone + "&message=" + URLEncoder.encode(message, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                runOnUiThread(() -> showOTPDialog());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "OTP পাঠাতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp, null);
        builder.setView(dialogView);

        EditText otpInput = dialogView.findViewById(R.id.otpInput);
        Button btnVerify = dialogView.findViewById(R.id.btnVerify);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        btnVerify.setOnClickListener(v -> {
            String enteredOtp = otpInput.getText().toString().trim();
            if (enteredOtp.equals(generatedOTP)) {
                dialog.dismiss();
                proceedToRegister(); // OTP ঠিক থাকলে রেজিস্টার
            } else {
                Toast.makeText(this, "Wrong OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }




    private void proceedToRegister() {
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    saveUserToFirestore(uid);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "রেজিস্ট্রেশন ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }







}
