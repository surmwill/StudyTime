package com.example.st.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.st.ChatManager;
import com.example.st.GroupManager;
import com.example.st.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;

public class ChatOverviewAdapter extends RecyclerView.Adapter<ChatOverviewAdapter.ViewHolder> {
    private static OnChatDeletionListener mOnChatDeletionListener;

    private List<ChatInfo> chatInfos;
    private LayoutInflater mInflater;
    private Context mContext;
    private ChatManager.UpdateUserChatDataListener mUserCallback;

    public static void bindDeletionListener(OnChatDeletionListener onChatDeletionListener) {
        mOnChatDeletionListener = onChatDeletionListener;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {
        // This is where we get a reference to all the text views
        // Can set static text too.
        final private TextView chatOverviewName;
        final private TextView chatOverviewLastMessage;
        final private TextView chatOverviewStudying;
        final private ImageView chatTypeImage;

        public ViewHolder(View itemView) {
            super(itemView);

            // TextView
            chatOverviewName = itemView.findViewById(R.id.chatOverviewName);
            chatOverviewLastMessage = itemView.findViewById(R.id.chatOverviewLastMessage);
            chatOverviewStudying = itemView.findViewById(R.id.chatOverviewStudying);

            // ImageView
            chatTypeImage = itemView.findViewById(R.id.chatTypeImage);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(ChatInfo chatInfo, Context context) {
            final String chatName = chatInfo.getName();
            final String lastMessage = chatInfo.getLastMessage();
            final String chatType = chatInfo.getType();

            if(chatType.equals(ChatManager.CHAT_TYPE_GROUP)) {
                final String studying = "-" + chatInfo.getStudying();
                final String crossed = chatInfo.getCrossed();

                chatTypeImage.setImageResource(R.drawable.ic_chat_type_group);
                chatOverviewStudying.setText(studying);

                if(crossed.equals(ChatManager.IS_CROSSED)) {
                    chatOverviewName.setPaintFlags(chatOverviewName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    chatOverviewName.setTextColor(ContextCompat.getColor(context, R.color.offline));
                }
            }
            else {
                chatTypeImage.setImageResource(R.drawable.ic_chat_type_friend);
                chatOverviewStudying.setVisibility(View.GONE);
            }

            chatOverviewName.setText(chatName);
            chatOverviewLastMessage.setText(lastMessage);
        }

        @Override
        public void onClick(View view) {
            String chatKey = chatInfos.get(getAdapterPosition()).getKey();
            mUserCallback.launchExistingChat(chatKey);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select the action");
            menu.add(Menu.NONE, v.getId(), Menu.NONE, "Delete Chat").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    deleteChat(getAdapterPosition());
                    return true;
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ChatOverviewAdapter(Context context, List <ChatInfo> chatInfos) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.chatInfos = chatInfos;

        try {
            mUserCallback = (ChatManager.UpdateUserChatDataListener) context;
        }
        catch(ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onChatClickListener");
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ChatOverviewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = mInflater.inflate(R.layout.row_chat_overview, parent, false);
        return new ViewHolder(view);
    }

    // When it gets bound to the list, here based on position we update the text accordingly
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final ChatInfo chatInfo = chatInfos.get(position);
        holder.bind(chatInfo, mContext);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return chatInfos.size();
    }


    private void deleteChat(int index) {
        chatInfos.remove(index);
        notifyItemRemoved(index);

        if(mOnChatDeletionListener != null) mOnChatDeletionListener.chatDeleted(index);
    }

    public void restoreChat(int index, ChatInfo chatInfo) {
        chatInfos.add(index, chatInfo);
        notifyItemInserted(index);
    }

    public interface OnChatDeletionListener {
        void chatDeleted(int index);
    }

    public static class ChatInfo implements Comparable <ChatInfo> {
        private String key;
        private String name;
        private String type;
        private String lastMessage;
        private long lastMessageTimestamp;

        // These are left null for a friend chat
        private String studying;
        private String crossed;

        // For a friend chat
        public ChatInfo(String key, String name, String type, String lastMessage, long lastMessageTimestamp) {
            this.key = key;
            this.name = name;
            this.type = type;
            this.lastMessage = lastMessage;
            this.lastMessageTimestamp = lastMessageTimestamp;
        }

        // For a user chat
        public ChatInfo(String key, String name, String type, String lastMessage, long lastMessageTimestamp, String studying, String crossed) {
            this.key = key;
            this.name = name;
            this.type = type;
            this.lastMessage = lastMessage;
            this.lastMessageTimestamp = lastMessageTimestamp;
            this.studying = studying;
            this.crossed = crossed;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public String getStudying() {
            return studying;
        }

        public String getCrossed() { return crossed; }

        public long getLastMessageTimestamp() {
            return lastMessageTimestamp;
        }

        @Override
        public int compareTo(@NonNull ChatInfo chatInfo) {
            if(lastMessageTimestamp > chatInfo.getLastMessageTimestamp()) return 1;
            else if(lastMessageTimestamp < chatInfo.getLastMessageTimestamp()) return -1;
            else return 0;
        }
    }
}
