package com.zerodevs.projecturi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private RadioGroup genderRG;
    private TextView uidTV, nameET, nidTV, dobET, emailET, addressET, phoneET;
    private Button updateBtn;
    private ImageView btnLogout;
    private CardView profileCard;

    private ImageView ivBack;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable dark mode by system default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

        nameET = findViewById(R.id.name);
        uidTV = findViewById(R.id.uid);
        nidTV = findViewById(R.id.nid);





        dobET = findViewById(R.id.dob);
        emailET = findViewById(R.id.email);
        phoneET = findViewById(R.id.phone);
        btnLogout = findViewById(R.id.btnLogout);


        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();  // Firebase থেকে Sign Out

            // Optional: ইউজারকে Login স্ক্রিনে ফেরত পাঠাও
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });



        loadUserProfile();

    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.addSnapshotListener(MetadataChanges.INCLUDE, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                if (snapshot != null && snapshot.exists()) {
                    uidTV.setText("URI-" +snapshot.getString("userId"));
                    nidTV.setText("NID: " + snapshot.getString("nid"));
                    nameET.setText(snapshot.getString("name"));
                    dobET.setText(snapshot.getString("dob"));
                    emailET.setText(snapshot.getString("email"));
                    phoneET.setText(snapshot.getString("phone"));

                }
            }
        });
    }




    private void saveToFirestore(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> showSnackbar("প্রোফাইল আপডেট সফল"))
                .addOnFailureListener(e -> showSnackbar("আপডেট ব্যর্থ: " + e.getMessage()));
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }



}
