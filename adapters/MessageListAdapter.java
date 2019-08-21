package com.example.st.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.st.Message;
import com.example.st.R;
import com.example.st.UserProfile;

import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<Message> mMessageList;

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        // TextView timeText;

        public ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            // timeText = itemView.findViewById(R.id.text_message_time);
        }

        void bind(Message message) {
            messageText.setText(message.getMessage());
            // timeText.setText(message.getDate());
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        // TextView timeText;

        public SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            // timeText = itemView.findViewById(R.id.text_message_time);
        }

        void bind(Message message) {
            messageText.setText(message.getMessage());
            // timeText.setText(message.getDate());
        }
    }

    public MessageListAdapter(Context context, List<Message> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mMessageList.get(position);

        if(message.getSender().equals(UserProfile.getKey())) return VIEW_TYPE_MESSAGE_SENT;
        else return VIEW_TYPE_MESSAGE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if(viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        }
        else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public void addMessage(Message message) {
        mMessageList.add(message);
        notifyDataSetChanged();
    }
}
