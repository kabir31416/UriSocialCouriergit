package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.*;

public class InboxActivity extends BaseActivity {

    private RecyclerView recyclerInbox;
    private TextView tvEmpty;

    ImageView ivBack;

    private FirebaseFirestore db;
    private String currentUserId;
    private List<DocumentSnapshot> chatList = new ArrayList<>();
    private ChatInboxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);


        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .update("fcmToken", token);
                });


        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });

        recyclerInbox = findViewById(R.id.recyclerInbox);
        tvEmpty = findViewById(R.id.tvEmptyInbox);
        recyclerInbox.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatInboxAdapter(chatList);
        recyclerInbox.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadInboxChats();
    }

    private void loadInboxChats() {
        db.collection("Chats")
                .whereEqualTo("user1", currentUserId)
                .get().addOnSuccessListener(user1Chats -> {
                    db.collection("Chats")
                            .whereEqualTo("user2", currentUserId)
                            .get().addOnSuccessListener(user2Chats -> {
                                chatList.clear();
                                chatList.addAll(user1Chats.getDocuments());
                                chatList.addAll(user2Chats.getDocuments());

                                // Sort by latest timestamp
                                chatList.sort((a, b) -> {
                                    Date t1 = a.getDate("lastTimestamp");
                                    Date t2 = b.getDate("lastTimestamp");
                                    if (t1 == null || t2 == null) return 0;
                                    return t2.compareTo(t1);
                                });

                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }

    class ChatInboxAdapter extends RecyclerView.Adapter<ChatInboxAdapter.ViewHolder> {
        List<DocumentSnapshot> chats;

        ChatInboxAdapter(List<DocumentSnapshot> chats) {
            this.chats = chats;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLastMessage, tvTime, tvParcelId;
            ImageView imgProfile;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvInboxUserName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTime = itemView.findViewById(R.id.tvMessageTime);
                tvParcelId = itemView.findViewById(R.id.tvParcelId);
                imgProfile = itemView.findViewById(R.id.imgProfile);
            }

            void bind(DocumentSnapshot doc) {
                String user1 = doc.getString("user1");
                String user2 = doc.getString("user2");
                String otherUserId = currentUserId.equals(user1) ? user2 : user1;
                String chatId = doc.getId();
                String parcelId = doc.getString("parcelId");

                // CN নম্বর
                if (parcelId != null && !parcelId.isEmpty()) {
                    tvParcelId.setText("CN#" + parcelId);
                } else {
                    tvParcelId.setText("Parcel: Unknown");
                }

                // শেষ মেসেজ
                tvLastMessage.setText(doc.getString("lastMessage") != null ? doc.getString("lastMessage") : "");

                // সময়
                Date ts = doc.getDate("lastTimestamp");
                if (ts != null) {
                    String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts);
                    tvTime.setText(time);
                } else {
                    tvTime.setText("");
                }

                db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
                    String otherUserName = userDoc.getString("name");
                    tvName.setText(otherUserName != null ? otherUserName : "Unknown User");

                });

                // চ্যাট অপেন
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("otherUserId", otherUserId);
                    startActivity(intent);
                });
            }

        }

        @NonNull
        @Override
        public ChatInboxAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_inbox_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatInboxAdapter.ViewHolder holder, int position) {
            holder.bind(chats.get(position));
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }
    }
}
