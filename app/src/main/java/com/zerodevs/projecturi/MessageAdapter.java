package com.zerodevs.projecturi;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<DocumentSnapshot> messages;
    private final String myId;
    private final Context context;

    public MessageAdapter(List<DocumentSnapshot> messages, String myId, Context context) {
        this.messages = messages;
        this.myId = myId;
        this.context = context;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMsg, tvTime;
        ImageView ivMedia, ivPlay;
        LinearLayout fileLayout;
        View layout;

        ViewHolder(View view) {
            super(view);
            tvMsg = view.findViewById(R.id.tvMsg);
            tvTime = view.findViewById(R.id.tvMsgTime);
            ivMedia = view.findViewById(R.id.ivMedia);
            ivPlay = view.findViewById(R.id.ivPlay);
            fileLayout = view.findViewById(R.id.fileLayout);
            layout = view;
        }

        void bind(DocumentSnapshot doc) {
            String senderId = doc.getString("senderId");
            String type = doc.getString("type");
            Date ts = doc.getDate("timestamp");
            String time = ts != null ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts) : "";

            tvMsg.setVisibility(View.GONE);
            ivMedia.setVisibility(View.GONE);
            ivPlay.setVisibility(View.GONE);
            fileLayout.setVisibility(View.GONE);

            if ("text".equals(type)) {
                tvMsg.setText(doc.getString("message"));
                tvMsg.setVisibility(View.VISIBLE);
            } else if ("image".equals(type)) {
                ivMedia.setVisibility(View.VISIBLE);
                Picasso.get().load(doc.getString("fileUrl")).into(ivMedia);
            } else if ("audio".equals(type)) {
                ivPlay.setVisibility(View.VISIBLE);
                ivPlay.setOnClickListener(v -> {
                    MediaPlayer player = new MediaPlayer();
                    try {
                        player.setDataSource(doc.getString("fileUrl"));
                        player.prepare();
                        player.start();
                    } catch (Exception e) {
                        Toast.makeText(context, "Can't play audio", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if ("application".equals(type) || "file".equals(type)) {
                fileLayout.setVisibility(View.VISIBLE);
                fileLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(doc.getString("fileUrl")));
                    context.startActivity(intent);
                });
            }

            boolean seen = doc.getBoolean("seen") != null && doc.getBoolean("seen");
            boolean delivered = doc.getBoolean("delivered") != null && doc.getBoolean("delivered");

            if (getAdapterPosition() == getItemCount() - 1 && senderId.equals(myId)) {
                if (seen) tvTime.setText(time + " ✓✓ Seen");
                else if (delivered) tvTime.setText(time + " ✓ Delivered");
                else tvTime.setText(time + " ✓ Sent");
            } else {
                tvTime.setText(time);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getString("senderId").equals(myId) ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 1 ? R.layout.item_msg_right : R.layout.item_msg_left, parent, false);
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
