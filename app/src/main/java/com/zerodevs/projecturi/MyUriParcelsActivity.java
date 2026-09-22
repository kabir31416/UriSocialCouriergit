package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MyUriParcelsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ParcelAdapter adapter;
    private List<DocumentSnapshot> parcelList;
    private FirebaseFirestore db;
    private String currentUserId;
    private TextView tvEmpty;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_uri_parcels);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        recyclerView = findViewById(R.id.recyclerMyUriParcels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        parcelList = new ArrayList<>();
        adapter = new ParcelAdapter(parcelList);
        recyclerView.setAdapter(adapter);
        tvEmpty = findViewById(R.id.tvEmpty);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadMyAcceptedParcels();
    }

    private void loadMyAcceptedParcels() {
        db.collection("Parcels").whereEqualTo("acceptedBy", currentUserId)
                .get().addOnSuccessListener(querySnapshot -> {
                    parcelList.clear();
                    parcelList.addAll(querySnapshot.getDocuments());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(parcelList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class ParcelAdapter extends RecyclerView.Adapter<ParcelAdapter.ViewHolder> {
        List<DocumentSnapshot> items;

        ParcelAdapter(List<DocumentSnapshot> items) {
            this.items = items;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvParcelId, tvrecname, tvStatus, tvlot, tvDate, tvCharge;
            Button btnTrack;

            ViewHolder(View itemView) {
                super(itemView);
                tvParcelId = itemView.findViewById(R.id.parcelcn);
                tvStatus = itemView.findViewById(R.id.parcelstatus);
                tvDate = itemView.findViewById(R.id.parceldate);
                tvCharge = itemView.findViewById(R.id.parceldc);
                tvrecname = itemView.findViewById(R.id.recname);
                tvlot = itemView.findViewById(R.id.tvlot);

                btnTrack = itemView.findViewById(R.id.btnTrack);
            }

            void bind(DocumentSnapshot doc) {
                String parcelId = doc.getString("parcelId");
                String receiverName = doc.getString("receiverName");
                String status = doc.getString("status");
                Double lotValue = doc.getDouble("lot");
                if (lotValue != null) {
                    tvlot.setText("Lot: " + lotValue);
                } else {
                    tvlot.setText("Lot: N/A");
                }



                tvParcelId.setText("CN#" + parcelId);
                tvStatus.setText(status);
                tvrecname.setText(receiverName);

                Timestamp timestamp = doc.getTimestamp("timestamp");
                if (timestamp != null) {
                    String formattedDate = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(timestamp.toDate());
                    tvDate.setText(formattedDate);
                } else {
                    tvDate.setText("");
                }

                Double charge = doc.getDouble("Rdc");
                if (charge != null) {
                    tvCharge.setText("Charge: ৳" + charge);
                } else {
                    tvCharge.setText("Charge: N/A");
                }

                btnTrack.setOnClickListener(v -> {
                    Intent intent = new Intent(MyUriParcelsActivity.this, UriStatus.class);
                    intent.putExtra("parcelId", parcelId);
                    startActivity(intent);
                });
            }
        }

        @NonNull
        @Override
        public ParcelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_my_uri_parcel, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParcelAdapter.ViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
