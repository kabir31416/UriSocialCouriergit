package com.zerodevs.projecturi;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends BaseActivity {

    private String chatId, otherUserId, currentUserId;
    private FirebaseFirestore db;
    private EditText etMessage;
    private TextView tvTyping, tvChatTitle;
    private RecyclerView recyclerMessages;

    private ImageView btnCall, ivBack, btnMedia;
    private MessageAdapter adapter;
    private List<DocumentSnapshot> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .update("fcmToken", token);
                });


        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();


        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> {
            super.onBackPressed();
        });


        etMessage = findViewById(R.id.etMessage);

        btnCall = findViewById(R.id.btnCall);

        tvTyping = findViewById(R.id.tvTypingStatus);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerMessages.setAdapter(adapter);

        listenMessages();
        setupTypingStatus();
        getReceiverInfo();
        markAsRead();

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnOptions).setOnClickListener(this::showOptions);
    }

    private void getReceiverInfo() {
        db.collection("users").document(otherUserId).get().addOnSuccessListener(doc -> {
            tvChatTitle.setText(doc.getString("name"));

            String phone = doc.getString("phone");
            if (phone != null && !phone.isEmpty()) {
                btnCall.setVisibility(View.VISIBLE);
                btnCall.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(android.net.Uri.parse("tel:" + phone));
                    startActivity(intent);
                });
            } else {
                btnCall.setVisibility(View.GONE); //
            }
        });
    }


    private void listenMessages() {
        db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp")
                .addSnapshotListener((snap, error) -> {
                    if (snap != null) {
                        messageList.clear();
                        messageList.addAll(snap.getDocuments());
                        adapter.notifyDataSetChanged();
                        recyclerMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        // Check if blocked
        db.collection("Blocked").document(otherUserId)
                .collection("users").document(currentUserId)
                .get().addOnSuccessListener(blockDoc -> {
                    if (blockDoc.exists()) {
                        Toast.makeText(this, "You are blocked by this user.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> message = new HashMap<>();
                    message.put("senderId", currentUserId);
                    message.put("message", msg);
                    message.put("receiverId", otherUserId);
                    message.put("timestamp", FieldValue.serverTimestamp());

                    db.collection("Chats").document(chatId)
                            .collection("Messages")
                            .add(message);

                    // Update chat
                    db.collection("Chats").document(chatId).update(new HashMap<String, Object>() {{
                        put("lastMessage", msg);
                        put("lastTimestamp", FieldValue.serverTimestamp());
                        put(currentUserId.equals(chatId.split("_")[0]) ? "unread_user2" : "unread_user1", FieldValue.increment(1));
                    }});

                    etMessage.setText("");
                });
    }

    private void setupTypingStatus() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                db.collection("Chats").document(chatId)
                        .collection("typingStatus")
                        .document(currentUserId)
                        .set(Collections.singletonMap("typing", s.length() > 0));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });

        db.collection("Chats").document(chatId)
                .collection("typingStatus").document(otherUserId)
                .addSnapshotListener((doc, err) -> {
                    if (doc != null && Boolean.TRUE.equals(doc.getBoolean("typing"))) {
                        tvTyping.setText("Typing...");
                        tvTyping.setVisibility(View.VISIBLE);
                    } else {
                        tvTyping.setVisibility(View.GONE);
                    }
                });
    }

    private void markAsRead() {
        db.collection("Chats").document(chatId).update(
                currentUserId.equals(chatId.split("_")[0]) ? "unread_user1" : "unread_user2", 0
        );
    }

    private void showOptions(View v) {
        PopupMenu menu = new PopupMenu(this, v);
        menu.getMenuInflater().inflate(R.menu.chat_options, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_delete) {
                deleteChat();
                return true;
            } else if (id == R.id.menu_block) {
                blockUser();
                return true;
            } else if (id == R.id.menu_report) {
                reportUser();
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void deleteChat() {
        db.collection("Chats").document(chatId)
                .collection("Messages").get().addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    db.collection("Chats").document(chatId).delete();
                    Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void blockUser() {
        db.collection("Blocked").document(currentUserId)
                .collection("users").document(otherUserId)
                .set(Collections.singletonMap("blocked", true))
                .addOnSuccessListener(unused -> Toast.makeText(this, "User Blocked", Toast.LENGTH_SHORT).show());
    }

    private void reportUser() {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUserId);
        report.put("reportedUserId", otherUserId);
        report.put("chatId", chatId);
        report.put("reason", "Misbehavior");
        report.put("timestamp", FieldValue.serverTimestamp());

        db.collection("Reports").add(report)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show());
    }

    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        List<DocumentSnapshot> messages;
        String myId;

        MessageAdapter(List<DocumentSnapshot> messages, String myId) {
            this.messages = messages;
            this.myId = myId;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMsg, tvTime;
            View layout;

            ViewHolder(View itemView) {
                super(itemView);
                tvMsg = itemView.findViewById(R.id.tvMsg);
                tvTime = itemView.findViewById(R.id.tvMsgTime);
                layout = itemView;
            }

            void bind(DocumentSnapshot doc) {
                String msg = doc.getString("message");
                Date ts = doc.getDate("timestamp");
                String time = ts != null ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts) : "";

                tvMsg.setText(msg);
                tvTime.setText(time);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).getString("senderId").equals(myId) ? 1 : 0;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(viewType == 1 ? R.layout.item_msg_right : R.layout.item_msg_left, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(messages.get(position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}  