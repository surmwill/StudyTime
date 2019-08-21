package com.example.st;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;

import com.example.st.dialog_fragments.ChatLockedDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatManager {
    public static final String PARENT_CHATS = "Chats";

    public static final String PARENT_MEMBERS = "Members";
    public static final String VALUE_MEMBER_THRESHOLD = "threshold";
    public static final int THRESHOLD_EVERYTHING = -1;
    public static final String VALUE_MEMBER_NAME = "name";
    public static final String VALUE_MESSAGES_TRACKED = "messagesTracked";

    public static final String VALUE_MEMBER_REFERENCE = "reference";
    public static final String HAS_REFERENCE = "true";
    public static final String NO_REFERENCE = "false";

    public static final String PARENT_MESSAGES = "Messages";
    public static final String VALUE_MESSAGE_TIMESTAMP = "messageTimestamp";
    public static final String VALUE_MESSAGE = "message";
    public static final String VALUE_MESSAGE_OWNER = "whoSent";

    public static final String VALUE_CHAT_TYPE = "chatType";
    public static final String CHAT_TYPE_GROUP = "group";
    public static final String CHAT_TYPE_FRIEND = "friend";

    public static final String VALUE_GROUP_CROSSED = "crossed";
    public static final String IS_CROSSED = "true";
    public static final String NOT_CROSSED = "false";

    public static final String VALUE_CHAT_KEY = "chatKey";
    public static final String VALUE_CHAT_TIMESTAMP = "chatTimestamp";
    public static final String VALUE_LAST_MESSAGE = "lastMessage";
    public static final String VALUE_GROUP_TITLE = "groupTitle";
    public static final String VALUE_GROUP_STUDYING = "groupStudying";
    public static final String VALUE_LAST_MESSAGE_TIMESTAMP = "lastMessageTS";

    public static final String ERROR_LISTENERS_NOT_BINDED = "error";
    public static final String ERROR_NO_NAME = "Fetching name...";
    public static final String ERROR_NO_KEY = "NO_KEY";

    public static final int GROUP_DAYS_UNTIL_DELETION = 2;
    public static final int FRIEND_DAYS_UNTIL_DELETION = 2;

    public static final int MAX_MESSAGES = 500;
    public static final int MAX_LIVE_CHATS = 100;

    private static UpdateUserChatDataListener mUserCallback;

    // Non-static, only needed for messaging
    private DatabaseReference chatDBRef;
    private String chatKey;

    public static void bindListeners(Context context) {
        try {
            mUserCallback = (UpdateUserChatDataListener) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement UpdateUserChatDataListener");
        }
    }

    // launches an existing group chat, otherwise creates a new one and launches that
    // User -> UserKey -> Chats
    public static void launchGroupChat(DataSnapshot userChatData, String groupKey, String leaderKey, String groupTitle, String studying) {
        if(mUserCallback == null) return; // TODO: show error message

        LoadChatTask loadChatTask = new LoadChatTask(groupKey, leaderKey, groupTitle, studying);
        loadChatTask.execute(userChatData);
    }

    // helper to create a new group chat
    private static void createNewGroupChat(final String groupKey, final String leaderKey, final String groupTitle, final String studying) {
        // Create a new skeleton chat in the DB
        DatabaseReference chatDBRef = createNewChatWith(leaderKey);

        // Don't need to know real names of members
        chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MEMBER_NAME).setValue("CHANGE THIS");
        chatDBRef.child(PARENT_MEMBERS).child(leaderKey).child(VALUE_MEMBER_NAME).setValue("LEADER");

        /* Exclusive extra entries for a group chat */
        chatDBRef.child(VALUE_CHAT_TYPE).setValue(CHAT_TYPE_GROUP);
        chatDBRef.child(VALUE_GROUP_STUDYING).setValue(studying);
        chatDBRef.child(VALUE_GROUP_TITLE).setValue(groupTitle);
        chatDBRef.child(VALUE_GROUP_CROSSED).setValue(NOT_CROSSED);

        mUserCallback.launchNewChat(chatDBRef.getKey(), groupKey);
    }

    // launches an existing friend chat, otherwise creates a new one and launches that
    // User -> UserKey -> Chats
    public static void launchFriendChat(DataSnapshot userChatData, String friendKey, String userName, String otherName) {
        if(mUserCallback == null) return; // TODO: show error message

        LoadChatTask loadChatTask = new LoadChatTask(friendKey, userName, otherName);
        loadChatTask.execute(userChatData);
    }

    private static void createNewFriendChat(String friendKey, String userName, String friendName) {
        DatabaseReference chatDBRef = createNewChatWith(friendKey);

        MyUtils.shitDebug(friendKey);
        MyUtils.shitDebug(friendName);

        // Use our actual names
        chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MEMBER_NAME).setValue(userName);
        chatDBRef.child(PARENT_MEMBERS).child(friendKey).child(VALUE_MEMBER_NAME).setValue(friendName);
        chatDBRef.child(VALUE_CHAT_TYPE).setValue(CHAT_TYPE_FRIEND);

        mUserCallback.launchNewChat(chatDBRef.getKey(), friendKey);
    }

    // Creates new chat, gfKey is either a group key or friend key depending on the chat type
    private static DatabaseReference createNewChatWith(final String otherKey) {
        // otherwise create a new chat
        final DatabaseReference chatDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).push();
        String newChatKey = chatDBRef.getKey();

        // User values (set everything but the name)
        DatabaseReference userRef = chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey());
        userRef.child(VALUE_MEMBER_THRESHOLD).setValue(THRESHOLD_EVERYTHING);
        userRef.child(VALUE_MEMBER_REFERENCE).setValue(HAS_REFERENCE);
        userRef.child(VALUE_MESSAGES_TRACKED).setValue(0);

        // Other user's values (set everything but the name)
        DatabaseReference otherRef = chatDBRef.child(PARENT_MEMBERS).child(otherKey);
        otherRef.child(VALUE_MEMBER_THRESHOLD).setValue(THRESHOLD_EVERYTHING);
        otherRef.child(VALUE_MEMBER_REFERENCE).setValue(NO_REFERENCE);
        otherRef.child(VALUE_MESSAGES_TRACKED).setValue(0);

        /* Common values */
        Map<String, String> timestamp = ServerValue.TIMESTAMP;
        chatDBRef.child(VALUE_CHAT_TIMESTAMP).setValue(timestamp);
        chatDBRef.child(VALUE_LAST_MESSAGE_TIMESTAMP).setValue(timestamp);
        chatDBRef.child(VALUE_LAST_MESSAGE).setValue("");
        chatDBRef.child(VALUE_CHAT_KEY).setValue(newChatKey);

        mUserCallback.launchNewChat(newChatKey, otherKey);

        return chatDBRef;
    }

    // Get details from an existing chat
    public ChatManager(String chatKey) {
        // TODO: check if chatKey is valid first
        this.chatKey = chatKey;
        this.chatDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(this.chatKey);
    }

    // Chats -> chatKey
    public static String getChatFriendsKey(DataSnapshot dataSnapshot) {
        if(dataSnapshot.child(PARENT_MEMBERS).exists()) {
            for(DataSnapshot memberData : dataSnapshot.child(PARENT_MEMBERS).getChildren()) {
                if(!memberData.getKey().equals(UserProfile.getKey())) return memberData.getKey();
            }
        }

        return ERROR_NO_KEY;
    }

    // If the chat is older than 2 days, delete the chat form the DB and the two members
    // DB references to it
    public static void deleteChatIfOld(final String chatKey) {
        final DatabaseReference chatDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey);
        chatDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot chatDataSnapshot) {
                if(!chatDataSnapshot.exists()) return;

                final String chatType = chatDataSnapshot.child(VALUE_CHAT_TYPE).getValue().toString();

                if(!chatDataSnapshot.hasChild(VALUE_CHAT_TIMESTAMP)) {
                    // TODO: error
                    return;
                }

                // If we have had a group chat for more than 2 days, delete the chat to save DB room
                long timeStamp = chatDataSnapshot.child(VALUE_CHAT_TIMESTAMP).getValue(Long.class);
                long cutoff = new Date().getTime() - TimeUnit.MILLISECONDS.convert(GROUP_DAYS_UNTIL_DELETION, TimeUnit.DAYS);
                if(timeStamp < cutoff) deleteChat(chatKey, chatDataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Removes the user from the chat, deletes the chat ref is the user if the last reference
    // Chats -> ChatKey
    public static void deleteChat(final String chatKey, DataSnapshot chatDataSnapshot) {
        final DatabaseReference chatDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey);

        // we are good to delete messages in the chat up until now
        chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MEMBER_THRESHOLD).setValue(ServerValue.TIMESTAMP);
        chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MEMBER_REFERENCE).setValue(NO_REFERENCE);
        chatDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MESSAGES_TRACKED).setValue(0);

        final String otherKey = getChatFriendsKey(chatDataSnapshot);
        final String otherHasRef = chatDataSnapshot.child(PARENT_MEMBERS).child(otherKey).child(VALUE_MEMBER_REFERENCE).getValue().toString();

        if(otherHasRef.equals(NO_REFERENCE)) {
            chatDBRef.removeValue();
            mUserCallback.deleteChat(chatKey, otherKey);
        }
        else {
            final long userThreshold = chatDataSnapshot.child(PARENT_MEMBERS).child(UserProfile.getKey()).child(VALUE_MEMBER_THRESHOLD).getValue(Long.class);
            final long otherThreshold = chatDataSnapshot.child(PARENT_MEMBERS).child(getChatFriendsKey(chatDataSnapshot))
                    .child(VALUE_MEMBER_THRESHOLD).getValue(Long.class);

            if(otherThreshold <= userThreshold && otherThreshold != THRESHOLD_EVERYTHING) deleteMessagesUntil(chatKey, otherThreshold);
        }
    }

    // deletes all the messages in chat with timestamps <= threshold
    private static void deleteMessagesUntil(final String chatKey, final long threshold) {
        Query messages = FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey).child(PARENT_MESSAGES);
        messages.orderByChild(VALUE_MESSAGE_TIMESTAMP).endAt(threshold).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot messageData : dataSnapshot.getChildren()) {
                    messageData.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // deletes the earliest messages in the conversation if we go over our defined limit.
    // We don't have infinite storage so this is a necessary evil
    public void deleteEarliestMessages(int numMessages) {
        Query oldMessages = chatDBRef.child(PARENT_MESSAGES).orderByChild(VALUE_MESSAGE_TIMESTAMP).limitToFirst(numMessages);
        oldMessages.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot messageData : dataSnapshot.getChildren()) {
                    messageData.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendMessage(final String userMessage, final ConstraintLayout constraintLayout) {
        chatDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    Snackbar snackbar = Snackbar.make(constraintLayout, "ERROR chat doesn't exist", Snackbar.LENGTH_LONG);
                    snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();
                    return;
                }

                final String otherKey = getChatFriendsKey(dataSnapshot);
                if(otherKey.equals(ERROR_NO_KEY)) {
                    Snackbar snackbar = Snackbar.make(constraintLayout, "Fetching data... try again soon", Snackbar.LENGTH_LONG);
                    snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();
                    return;
                }

                final String hasRef = dataSnapshot.child(PARENT_MEMBERS).child(otherKey).child(VALUE_MEMBER_REFERENCE).getValue().toString();
                if(hasRef.equals(NO_REFERENCE)) {
                    final String chatType = dataSnapshot.child(VALUE_CHAT_TYPE).getValue().toString();
                    addChatRefFor(otherKey, chatType);
                }

                HashMap <String, Object> messageData = new HashMap<>();
                Map <String, String> timestamp = ServerValue.TIMESTAMP;

                messageData.put(VALUE_MESSAGE, userMessage);
                messageData.put(VALUE_MESSAGE_TIMESTAMP, ServerValue.TIMESTAMP);
                messageData.put(VALUE_MESSAGE_OWNER, UserProfile.getKey());

                chatDBRef.child(PARENT_MESSAGES).push().updateChildren(messageData);
                chatDBRef.child(VALUE_LAST_MESSAGE).setValue(userMessage);
                chatDBRef.child(VALUE_LAST_MESSAGE_TIMESTAMP).setValue(timestamp);

                increaseMessagesTracked(UserProfile.getKey(), otherKey);
                mUserCallback.sentMessageTo(getChatKey(), otherKey);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // increase the number of messages tracked by both chat recipients, a user can only store so many
    // messages before taking up too much space in the database
    private void increaseMessagesTracked(String userKey, String otherKey) {
        chatDBRef.child(PARENT_MEMBERS).child(userKey).child(VALUE_MESSAGES_TRACKED).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null) {
                    final int messagesTracked = mutableData.getValue(Integer.class) + 1;
                    mutableData.setValue(messagesTracked);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        chatDBRef.child(PARENT_MEMBERS).child(otherKey).child(VALUE_MESSAGES_TRACKED).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null) {
                    final int messagesTracked = mutableData.getValue(Integer.class) + 1;
                    mutableData.setValue(messagesTracked);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

    // Allows the other user to see the chat and send messages, Chats -> ChatKey
    private void addChatRefFor(final String otherKey, final String chatType) {
        // don't use the actual group key. If we leave and then join again we don't want to start the chat with the random person
        // who tried to join the group in the past
        if(chatType.equals(CHAT_TYPE_GROUP))
            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(otherKey).child(UserProfile.PARENT_CHATS)
                    .child(getChatKey()).setValue("");
        else
        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(otherKey).child(UserProfile.PARENT_CHATS)
                .child(getChatKey()).setValue(otherKey);

        chatDBRef.child(PARENT_MEMBERS).child(otherKey).child(VALUE_MEMBER_REFERENCE).setValue(HAS_REFERENCE);
    }

    public String getChatKey() {
        return chatKey;
    }

    // Probably unnecessary, but I like to learn
    private static class LoadChatTask extends AsyncTask <DataSnapshot, Void, List<String>> {
        private List<String> chatKeys = new ArrayList<>();

        private int oldChatsFetched = 0;
        private boolean chatLaunched = false;

        // either a friend key or a leader key (whoever we are starting the chat with)
        String otherKey;
        String chatType;

        // group exclusive
        String groupKey;
        String groupTitle;
        String studying;

        // friend exclusive
        String userName;
        String friendName;

        // For loading a group chat
        LoadChatTask(String groupKey, String leaderKey, String groupTitle, String studying) {
            this.chatType = CHAT_TYPE_GROUP;
            this.groupKey = groupKey;
            this.otherKey = leaderKey;
            this.groupTitle = groupTitle;
            this.studying = studying;
        }

        // For loading a friend chat
        LoadChatTask(String friendKey, String userName, String friendName) {
            this.chatType = CHAT_TYPE_FRIEND;
            this.userName = userName;
            this.friendName = friendName;
            this.otherKey = friendKey;
        }

        // TODO: send a list of possible chats to on complete, then check leaders of all possible chats and gross out the bad ones
        // Users -> UserKey -> Chats
        @Override
        protected List<String> doInBackground(DataSnapshot... dataSnapshot) {
            // Only ever one
            DataSnapshot chatDataSnapshot = dataSnapshot[0];

            if (chatType.equals(CHAT_TYPE_GROUP)) {
                for (DataSnapshot chatData : chatDataSnapshot.getChildren()) {
                    final String testGroupKey = chatData.getValue().toString();
                    if (testGroupKey.equals(groupKey)) chatKeys.add(chatData.getKey());
                }
            } else if (chatType.equals(CHAT_TYPE_FRIEND)) {
                for (DataSnapshot chatData : chatDataSnapshot.getChildren()) {
                    final String testFriendKey = chatData.getValue().toString();
                    if (testFriendKey.equals(otherKey)) chatKeys.add(chatData.getKey());
                }
            }

            return chatKeys;
        }

        @Override
        protected void onPostExecute(List<String> chatKeys) {
            // else launch the according chat
            if(chatType.equals(CHAT_TYPE_GROUP)) {
                if(chatKeys.isEmpty()) createNewGroupChat(groupKey, otherKey, groupTitle, studying);
                else iterateGroupChatKeys(chatKeys);
            } else {
                if(chatKeys.isEmpty()) createNewFriendChat(otherKey, userName, friendName);
                else {
                    // There can only be one chat reference per friend
                    final String chatKey = chatKeys.get(0);
                    FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey).child(PARENT_MEMBERS)
                        .child(UserProfile.getKey()).child(VALUE_MEMBER_REFERENCE).setValue(HAS_REFERENCE);
                    mUserCallback.launchExistingChat(chatKey);
                }
            }

        }

        // Iterates through the group chat keys searching for an existing group chat to launch if there is one.
        // If there isn't we create a new group chat
        private void iterateGroupChatKeys(List<String> chatKeys) {
            final int totalChats = chatKeys.size();

            for (final String chatKey : chatKeys) {
                FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists()) return;
                                final String oldLeaderKey = getChatFriendsKey(dataSnapshot);

                                // Leader has changed, cross out the chat with the old leader and keep looking
                                if (!otherKey.equals(oldLeaderKey)) {
                                    FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey)
                                            .child(VALUE_GROUP_CROSSED).setValue(IS_CROSSED);

                                    oldChatsFetched++;

                                    // Once we know that there's no existing chat, create a new one
                                    if(oldChatsFetched == totalChats) createNewGroupChat(groupKey, otherKey, groupTitle, studying);
                                }
                                // The chat already exists, launch that
                                else {
                                    FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey)
                                            .child(VALUE_GROUP_CROSSED).setValue(NOT_CROSSED);

                                    // we once again have a reference to this chat
                                    FirebaseDatabase.getInstance().getReference().child(PARENT_CHATS).child(chatKey).child(PARENT_MEMBERS)
                                            .child(UserProfile.getKey()).child(VALUE_MEMBER_REFERENCE).setValue(HAS_REFERENCE);

                                    // In theory this should only ever be called once, but just in case
                                    if(!chatLaunched) {
                                        mUserCallback.launchExistingChat(chatKey);
                                        chatLaunched = true;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        }
    }

    public interface UpdateUserChatDataListener {
        void launchNewChat(String newChatKey, String otherKey);
        void deleteChat(String chatKey, String otherKey);
        void launchExistingChat(String chatKey);
        void sentMessageTo(String chatKey, String otherKey);
    }
}
