package com.zerodevs.projecturi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.onesignal.OneSignal;

public class HomeActivity extends BaseActivity {

    private ImageView cardAddParcel, admin, support, inbox, wallet, payments, cardViewParcels, btnScan, btnProfile, btnSearch ;
    private EditText etParcelId;

    private CardView cardsender, cardrider;

    private TextView nameET, uidTV ;

    private FirebaseFirestore db;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        OneSignal.initWithContext(this);
        OneSignal.setAppId("b099eae8-7e02-45a2-991d-37a1f093ee65");

        String playerId = OneSignal.getDeviceState().getUserId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && playerId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("playerId", playerId);
        }

        cardAddParcel = findViewById(R.id.cardAddParcel);
        cardViewParcels = findViewById(R.id.cardViewParcels);
        wallet = findViewById(R.id.wallet);
        btnProfile = findViewById(R.id.btnProfile);
        nameET = findViewById(R.id.name);
        uidTV = findViewById(R.id.uid);
        admin = findViewById(R.id.admin);
        payments = findViewById(R.id.payments);
        inbox = findViewById(R.id.inbox);
        support = findViewById(R.id.support);



        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        cardAddParcel.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AddParcelActivity.class)));
        cardViewParcels.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ViewParcelActivity.class)));
        wallet.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, WalletActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        payments.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PaymentActivity.class)));
        admin.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HubActivity.class)));
        inbox.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, InboxActivity.class)));
        support.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, Support.class)));


        etParcelId = findViewById(R.id.etParcelId);
        btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(view -> {
            String parcelId = etParcelId.getText().toString().trim();

            if (parcelId.isEmpty()) {
                Toast.makeText(this, "Please enter a Parcel ID", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(HomeActivity.this, TrackActivity.class);
                intent.putExtra("parcelId", parcelId);
                startActivity(intent);
            }
        });


        cardsender = findViewById(R.id.cardsender);
        cardsender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, Sender.class);
                startActivity(intent);
            }
        });

        cardrider = findViewById(R.id.cardrider);
        cardrider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, Rider.class);
                startActivity(intent);
            }
        });


        btnScan = findViewById(R.id.btnScan);

        btnScan.setOnClickListener(v -> startQRScan());

        loadUserProfile();

    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.addSnapshotListener(MetadataChanges.INCLUDE, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                if (snapshot != null && snapshot.exists()) {
                    uidTV.setText(snapshot.getString("userId"));
                    nameET.setText(snapshot.getString("name"));

                }
            }
        });
    }

    private void startQRScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan Parcel QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    // স্ক্যানের রেজাল্ট হ্যান্ডল করা
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scannedParcelId = result.getContents().trim();
            goToTrackActivity(scannedParcelId);
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    // TrackActivity তে যাওয়ার জন্য কমন মেথড
    private void goToTrackActivity(String parcelId) {
        Intent intent = new Intent(HomeActivity.this, TrackActivity.class);
        intent.putExtra("parcelId", parcelId);
        startActivity(intent);
    }

}