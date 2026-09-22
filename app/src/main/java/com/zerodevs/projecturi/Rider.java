package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Rider extends AppCompatActivity {


    private ImageView UriParcels, myuri;
    private Button addroute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rider);


        UriParcels = findViewById(R.id.UriParcels);
        addroute = findViewById(R.id.addroute);
        myuri = findViewById(R.id.myuri);

        UriParcels.setOnClickListener(v -> startActivity(new Intent(Rider.this, UriActivity.class)));
        addroute.setOnClickListener(v -> startActivity(new Intent(Rider.this, AddRouteActivity.class)));
        myuri.setOnClickListener(v -> startActivity(new Intent(Rider.this, MyUriParcelsActivity.class)));



    }
}