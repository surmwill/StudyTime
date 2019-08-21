package com.example.st;


import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.st.adapters.MessageListAdapter;
import com.example.st.dialog_fragments.MessageLockedDialogFragment;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MessageListFragment extends Fragment {
    private RecyclerView mMessageRecycler;
    private Button mChatboxSend;
    private ConstraintLayout cLayout;

    MessageListAdapter mMessageAdapter;
    private EditText mChatbox;
    private ChatManager mChat;


    private static final String ARG_CHAT_KEY = "ARG_CHAT_KEY";
    private String title = null;

    public MessageListFragment() {
        // Required empty public constructor
    }

    public static MessageListFragment newInstance(String chatKey) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_KEY, chatKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            String chatKey = getArguments().getString(ARG_CHAT_KEY);
            mChat = new ChatManager(chatKey);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(title != null) getActivity().setTitle(title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_message_list, container, false);

        // ConstraintLayout
        cLayout = view.findViewById(R.id.messageListCLayout);

        // Buttons
        mChatboxSend = view.findViewById(R.id.buttonChatboxSend);

        // EditText
        mChatbox = view.findViewById(R.id.edittextChatbox);

        // Recycler
        mMessageRecycler = view.findViewById(R.id.reyclerviewMessageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(layoutManager);

        mMessageAdapter = new MessageListAdapter(getActivity(), new ArrayList<Message>());
        mMessageRecycler.setAdapter(mMessageAdapter);

        FirebaseDatabase.getInstance().getReference().child(ChatManager.PARENT_CHATS).child(mChat.getChatKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()) return;

                        // Set the title of the chat accordingly
                        final String chatType = dataSnapshot.child(ChatManager.VALUE_CHAT_TYPE).getValue().toString();
                        if(chatType.equals(ChatManager.CHAT_TYPE_GROUP))
                            getActivity().setTitle(dataSnapshot.child(ChatManager.VALUE_GROUP_TITLE).getValue().toString());
                        else {
                            final String friendName = dataSnapshot.child(ChatManager.PARENT_MEMBERS).child(ChatManager.getChatFriendsKey(dataSnapshot))
                                    .child(ChatManager.VALUE_MEMBER_NAME).getValue().toString();
                            getActivity().setTitle(friendName);
                        }

                        // Only display messages up to our last delete
                        final long threshold = dataSnapshot.child(ChatManager.PARENT_MEMBERS)
                                .child(UserProfile.getKey()).child(ChatManager.VALUE_MEMBER_THRESHOLD).getValue(Long.class);

                        Query chats = FirebaseDatabase.getInstance().getReference().child(ChatManager.PARENT_CHATS).child(mChat.getChatKey()).child(ChatManager.PARENT_MESSAGES);
                        chats.orderByChild(ChatManager.VALUE_MESSAGE_TIMESTAMP).startAt(threshold).addChildEventListener(messageListener);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey()).child(UserProfile.VALUE_MESSAGE_LOCKED)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String messageLocked = dataSnapshot.getValue().toString();

                        if(messageLocked.equals(UserProfile.STATUS_UNLOCKED)) {
                            mChatboxSend.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mChat.sendMessage(mChatbox.getText().toString(), cLayout);
                                    mChatbox.setText("");
                                }
                            });
                        } else {
                            MessageLockedDialogFragment popup = new MessageLockedDialogFragment();
                            popup.show(getChildFragmentManager(), "messageLockedPopup");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        return view;
    }

    // Message child
    private ChildEventListener messageListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final String message = dataSnapshot.child(ChatManager.VALUE_MESSAGE).getValue().toString();
            final String sender = dataSnapshot.child(ChatManager.VALUE_MESSAGE_OWNER).getValue().toString();
            final String timestamp = dataSnapshot.child(ChatManager.VALUE_MESSAGE_TIMESTAMP).getValue().toString();

            mMessageAdapter.addMessage(new Message(sender, message, timestamp));
            if(sender.equals(UserProfile.getKey()))
                mMessageRecycler.scrollToPosition(mMessageAdapter.getItemCount() - 1);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
}
