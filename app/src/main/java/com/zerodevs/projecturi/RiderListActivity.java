package com.zerodevs.projecturi;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class RiderListActivity extends BaseActivity {

    ListView listRiders;
    ArrayAdapter<String> adapter;
    ArrayList<String> riderList = new ArrayList<>();
    FirebaseFirestore db;
    String hubId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_list);

        hubId = getIntent().getStringExtra("hubId");
        db = FirebaseFirestore.getInstance();
        listRiders = findViewById(R.id.listRiders);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, riderList);
        listRiders.setAdapter(adapter);

        loadRiders();
    }

    private void loadRiders() {
        db.collection("users")
                .whereEqualTo("pointId", hubId)
                .whereEqualTo("role", "rider")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        riderList.add(name + " (" + phone + ")");
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
