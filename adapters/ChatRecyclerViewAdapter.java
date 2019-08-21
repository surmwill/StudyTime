package com.example.st.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.st.R;

import java.util.ArrayList;
import java.util.List;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ViewHolder> {
    private List<String> mData;
    private LayoutInflater mInflater;

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // This is where we get a reference to all the text views
        // Can set static text too.
        final public TextView chatMessage;

        public ViewHolder(View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.rowChatMessage);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ChatRecyclerViewAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = new ArrayList<>();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ChatRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = mInflater.inflate(R.layout.row_chat, parent, false);
        return new ChatRecyclerViewAdapter.ViewHolder(view);
    }

    // When it gets bound to the list, here based on position we update the text accordingly
    @Override
    public void onBindViewHolder(ChatRecyclerViewAdapter.ViewHolder holder, int position) {
        String message = mData.get(position);
        holder.chatMessage.setText(message);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public String getItem(int id) {
        return mData.get(id);
    }

    public void newAddedData(String newData) {
        mData.add(newData);
        notifyDataSetChanged();
    }

    public void removeFrontItem() {
        mData.remove(0);
    }
}
