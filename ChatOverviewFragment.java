package com.example.st;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.st.adapters.ChatOverviewAdapter;
import com.example.st.views.PieChartView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatOverviewFragment extends Fragment implements ChatOverviewAdapter.OnChatDeletionListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TITLE = "Chats";

    private int totalChats;
    private int chatsFetched;

    private int totalLiveChats; // Chats which we have a reference to (hence will show up in the list)
    private boolean chatLocked; // Once we reach a certain amount of chats we will be unable to create anymore

    private int totalMessagesTracked;
    private boolean messageLocked; // Once we have a reference to a certain amount of messages we will be unable to send anymore messages

    private RecyclerView recyclerView;
    private ChatOverviewAdapter adapter;
    private ConstraintLayout cLayout;
    private SwipeRefreshLayout chatsRefreshLayout;

    private PopupMenu filterPopupMenu;
    private FloatingActionButton fab;
    private MenuItem prevMenuItem;

    //private ArrayList<ChatOverviewAdapter.ChatInfo> fullChatInfos;
    private ArrayList<ChatOverviewAdapter.ChatInfo> fullChatInfos;
    private ArrayList<ChatOverviewAdapter.ChatInfo> filteredChatInfos;

    private LockedListener mCallback;

    public ChatOverviewFragment() {
        // Required empty public constructor
    }

    public static ChatOverviewFragment newInstance() {
        ChatOverviewFragment fragment = new ChatOverviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        try {
            mCallback = (LockedListener) context;
        }
        catch(ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MessageLockedListener");
        }

        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(TITLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_overview, container, false);

        chatsFetched = 0;
        totalMessagesTracked = 0;
        messageLocked = false;
        totalLiveChats = 0;
        chatLocked = false;

        ChatOverviewAdapter.bindDeletionListener(this);

        fullChatInfos = new ArrayList<>();

        // ConstraintLayout
        cLayout = view.findViewById(R.id.chatOverviewCLayout);

        // SwipeRefreshLayout
        chatsRefreshLayout = view.findViewById(R.id.chatOverviewRefresh);
        chatsRefreshLayout.setOnRefreshListener(this);

        // FloatingActionButton
        fab = view.findViewById(R.id.chatOverviewFloatingActionButton);

        filterPopupMenu = new PopupMenu(getContext(), fab);
        filterPopupMenu.getMenuInflater().inflate(R.menu.chats_popup_filter_menu, filterPopupMenu.getMenu());
        filterPopupMenu.setOnMenuItemClickListener(filterPopupMenuListener);

        // RecyclerView reference
        recyclerView = view.findViewById(R.id.chatOverviewRecycler);
        registerForContextMenu(recyclerView);

        // Set the RecyclerView adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // Add a divider to the adapter
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        // We will order the chats later
        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey()).child(UserProfile.PARENT_CHATS)
                .addListenerForSingleValueEvent(fetchChatsListener);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterPopupMenu.show();
            }
        });

        return view;
    }

    private PopupMenu.OnMenuItemClickListener filterPopupMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            // Haven't fetched the profiles yet so we have nothing to filter
            if(item.equals(prevMenuItem) || recyclerView.getAdapter() == null) return true;

            if(prevMenuItem != null) prevMenuItem.setChecked(false);
            item.setChecked(true);
            prevMenuItem = item;

            switch (item.getItemId()) {
                case R.id.chatsFilterFriends: {
                    filterFriends();
                    break;
                }
                case R.id.chatsFilterGroup: {
                    filterGroup();
                    break;
                }
                case R.id.chatsFilterNone: {
                    filterNone();
                    break;
                }
            }
            return true;
        }
    };

    private void filterFriends() {
        filteredChatInfos.clear();

        for(ChatOverviewAdapter.ChatInfo chatInfo : fullChatInfos) {
            if(chatInfo.getType().equals(ChatManager.CHAT_TYPE_FRIEND))
                filteredChatInfos.add(chatInfo);
        }

        adapter = new ChatOverviewAdapter(getContext(), filteredChatInfos);
        recyclerView.setAdapter(adapter);
    }

    private void filterGroup() {
        filteredChatInfos.clear();

        for(ChatOverviewAdapter.ChatInfo chatInfo : fullChatInfos) {
            if(chatInfo.getType().equals(ChatManager.CHAT_TYPE_GROUP))
                filteredChatInfos.add(chatInfo);
        }

        adapter = new ChatOverviewAdapter(getContext(), filteredChatInfos);
        recyclerView.setAdapter(adapter);
    }

    private void filterNone() {
        // Have to recreate adapter in case a deletion happened
        filteredChatInfos = new ArrayList<>(fullChatInfos);

        adapter = new ChatOverviewAdapter(getContext(), filteredChatInfos);
        recyclerView.setAdapter(adapter);
    }

    // Users -> UserKey -> Chats, populates the list of chat keys in order of chat creation
    private ValueEventListener fetchChatsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            totalChats = (int)dataSnapshot.getChildrenCount();

            for(final DataSnapshot chatData : dataSnapshot.getChildren()) {
                final String chatKey = chatData.getKey();

                FirebaseDatabase.getInstance().getReference().child(ChatManager.PARENT_CHATS).child(chatKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) {
                                    chatData.getRef().removeValue();
                                    increaseChatsFetched(0, false);
                                    return;
                                }

                                final String hasRef = dataSnapshot.child(ChatManager.PARENT_MEMBERS).child(UserProfile.getKey())
                                        .child(ChatManager.VALUE_MEMBER_REFERENCE).getValue().toString();

                                // We want to display chats to which we have a reference to
                                if(!hasRef.equals(ChatManager.HAS_REFERENCE)) {
                                    increaseChatsFetched(0, false);
                                    return;
                                }

                                // Otherwise we have a reference to the chat
                                ChatOverviewAdapter.ChatInfo chatInfo;
                                final String chatName;

                                // How many messages are in this chat?
                                final int messagesFetchedFromChat = dataSnapshot.child(ChatManager.PARENT_MEMBERS).child(UserProfile.getKey())
                                        .child(ChatManager.VALUE_MESSAGES_TRACKED).getValue(Integer.class);

                                // If we deleted the chat there is no last message to show
                                final String lastMessage;
                                final long threshold = dataSnapshot.child(ChatManager.PARENT_MEMBERS).child(UserProfile.getKey())
                                        .child(ChatManager.VALUE_MEMBER_THRESHOLD).getValue(Long.class);
                                final long lastMessageTS = dataSnapshot.child(ChatManager.VALUE_LAST_MESSAGE_TIMESTAMP).getValue(Long.class);

                                if(threshold >= lastMessageTS) lastMessage = "";
                                else lastMessage = dataSnapshot.child(ChatManager.VALUE_LAST_MESSAGE).getValue().toString();

                                final String chatKey = dataSnapshot.child(ChatManager.VALUE_CHAT_KEY).getValue().toString();
                                final String chatType = dataSnapshot.child(ChatManager.VALUE_CHAT_TYPE).getValue().toString();

                                if (chatType.equals(ChatManager.CHAT_TYPE_GROUP)) {
                                    final String studying = dataSnapshot.child(ChatManager.VALUE_GROUP_STUDYING).getValue().toString();
                                    final String crossed = dataSnapshot.child(ChatManager.VALUE_GROUP_CROSSED).getValue().toString();
                                    chatName = dataSnapshot.child(ChatManager.VALUE_GROUP_TITLE).getValue().toString();

                                    chatInfo = new ChatOverviewAdapter.ChatInfo(chatKey, chatName, chatType, lastMessage, lastMessageTS, studying, crossed);
                                } else {
                                    chatName = dataSnapshot.child(ChatManager.PARENT_MEMBERS).child(ChatManager.getChatFriendsKey(dataSnapshot))
                                            .child(ChatManager.VALUE_MEMBER_NAME).getValue().toString();
                                    chatInfo = new ChatOverviewAdapter.ChatInfo(chatKey, chatName, chatType, lastMessage, lastMessageTS);
                                }

                                fullChatInfos.add(chatInfo);
                                increaseChatsFetched(messagesFetchedFromChat, true);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    public void chatDeleted(final int index) {
        final ChatOverviewAdapter.ChatInfo chatInfo = fullChatInfos.remove(index);
        final String chatKey = chatInfo.getKey();

        FirebaseDatabase.getInstance().getReference().child(ChatManager.PARENT_CHATS).child(chatKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot chatDataSnapshot) {
                        // We are freeing up this amount of messages
                        final int messagesRemoved = chatDataSnapshot.child(ChatManager.PARENT_MEMBERS).child(UserProfile.getKey())
                                .child(ChatManager.VALUE_MESSAGES_TRACKED).getValue(Integer.class);

                        // While the SnackBar is visible give the user the option to restore the chat
                        Snackbar snackbar = Snackbar.make(cLayout, "Chat Removed: " + chatInfo.getName(), Snackbar.LENGTH_LONG);
                        snackbar.setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((ChatOverviewAdapter)recyclerView.getAdapter()).restoreChat(index, chatInfo);
                            }
                        });

                        // Once the SnackBar disappears delete the chat (possibly from the database)
                        snackbar.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);
                                if(event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                    ChatManager.deleteChat(chatKey, chatDataSnapshot);
                                    deletedMessages(messagesRemoved);
                                    freedChatSpace();
                                }
                            }
                        });

                        snackbar.setActionTextColor(Color.YELLOW);
                        snackbar.show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void freedChatSpace() {
        totalLiveChats--;
        if(totalLiveChats < ChatManager.MAX_LIVE_CHATS) {
            mCallback.chatLocked(false);
            // chatLocked = false;
        }
    }

    // Once we delete a chat, see if we are still message locked (prevented from sending messages)
    private void deletedMessages(int numMessages) {
        totalMessagesTracked -= numMessages;
        if(totalMessagesTracked < ChatManager.MAX_MESSAGES) {
            mCallback.messageLocked(false);
            // messageLocked = false;
        }
    }

    // Once we have iterated through all chat references, sort and set the adapter
    public void increaseChatsFetched(int messagesTrackedFromChat, boolean live) {
        chatsFetched++;
        totalMessagesTracked += messagesTrackedFromChat;
        if(live) totalLiveChats++;

        if(!chatLocked && totalLiveChats >= ChatManager.MAX_LIVE_CHATS) {
            mCallback.chatLocked(true);
            chatLocked = true; // ensure we only show the alert dialog once
        }

        if(!messageLocked && totalMessagesTracked >= ChatManager.MAX_MESSAGES) {
            mCallback.messageLocked(true);
            messageLocked = true; // ensure we only show the alert dialog once
        }

        if(chatsFetched == totalChats) {
            Collections.sort(fullChatInfos, Collections.<ChatOverviewAdapter.ChatInfo>reverseOrder());
            filteredChatInfos = new ArrayList<>(fullChatInfos);

            adapter = new ChatOverviewAdapter(getContext(), filteredChatInfos);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onRefresh() {
        if(adapter != null) filterNone();
        if(prevMenuItem != null) prevMenuItem.setChecked(false);
        chatsRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStop() {
        // We adjusted the counter, set it back to 0
        super.onStop();
    }

    public interface LockedListener {
        void messageLocked(boolean messageLocked);
        void chatLocked(boolean chatLocked);
    }
}
