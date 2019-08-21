package com.example.st;

import android.content.Context;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import net.frakbot.glowpadbackport.util.Const;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserProfile {
    // Fields filled when registering
    private static String userKey;
    private DatabaseReference profileDBReference;

    public static final String PARENT_USERS = "Users";
    public static final String PARENT_FRIENDS = "Friends";
    public static final String PARENT_BOOKMARKED_GROUPS = "BookmarkedGroups";
    public static final String PARENT_CHATS = "Chats";
    public static final String PARENT_FRIEND_REQUESTS = "FriendRequests";

    public static final String VALUE_PROFILE_COMPLETED = "profileCompleted";
    public static final String VALUE_USER_KEY = "userKey";
    public static final String VALUE_EMAIL = "email";
    public static final String VALUE_FIRST_NAME = "firstName";
    public static final String VALUE_MAJOR = "major";
    public static final String VALUE_IN_GROUP = "inGroup";
    public static final String VALUE_USER_ONLINE = "userOnline";
    public static final String VALUE_COURSE_LOAD = "courseLoad";
    public static final String VALUE_OTHER = "other";
    public static final String VALUE_MESSAGE_LOCKED = "messageLocked";
    public static final String VALUE_CHAT_LOCKED = "chatLocked";

    public static final String STATUS_NOT_IN_GROUP = "false";
    public static final String STATUS_PROFILE_INCOMPLETE = "false";
    public static final String STATUS_PROFILE_COMPLETE = "true";
    public static final String STATUS_ONLINE = "true";
    public static final String STATUS_OFFLINE = "false";
    public static final String STATUS_FRIEND_REQUESTED = "requested";
    public static final String STATUS_FRIEND_ACCEPTED = "accepted";
    public static final String STATUS_LOCKED = "true";
    public static final String STATUS_UNLOCKED = "false";

    public static final String STORAGE_VALUE_PROFILE_IMAGE = "profileImage";

    // IMPORTANT: This constructor should only ever be called ONCE for the current user of the app
    public UserProfile(String key) {
        userKey = key;

        profileDBReference = FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(key);
        profileDBReference.child(VALUE_USER_ONLINE).setValue(STATUS_ONLINE);

        //profileDBReference.child(VALUE_USER_ONLINE).onDisconnect().setValue(STATUS_OFFLINE);
    }

    public void updateUserProfile(@Nullable String newFirstName, @Nullable String newMajor, @Nullable String newCourseLoad, @Nullable String newAddInfo) {
        if(newFirstName != null) profileDBReference.child(VALUE_FIRST_NAME).setValue(newFirstName);
        if(newMajor != null) profileDBReference.child(VALUE_MAJOR).setValue(newMajor);
        if(newCourseLoad != null) profileDBReference.child(VALUE_COURSE_LOAD).setValue(newCourseLoad);
        if(newAddInfo != null) profileDBReference.child(VALUE_OTHER).setValue(newAddInfo);

        profileDBReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) return;

                if(!dataSnapshot.child(VALUE_FIRST_NAME).getValue().toString().isEmpty() && !dataSnapshot.child(VALUE_MAJOR).getValue().toString().isEmpty())
                    profileDBReference.child(VALUE_PROFILE_COMPLETED).setValue(STATUS_PROFILE_COMPLETE);
                else
                    profileDBReference.child(VALUE_PROFILE_COMPLETED).setValue(STATUS_PROFILE_INCOMPLETE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // DataSnapshot should be in reference to Users -> userkey
    public static HashMap<String, String> getUserProperties(DataSnapshot userDataSnapshot) {
        HashMap <String, String> data = new HashMap<>();

        data.put(VALUE_FIRST_NAME, userDataSnapshot.child(VALUE_FIRST_NAME).getValue().toString());
        data.put(VALUE_MAJOR, userDataSnapshot.child(VALUE_MAJOR).getValue().toString());
        data.put(VALUE_USER_KEY, userDataSnapshot.child(VALUE_USER_KEY).getValue().toString());
        data.put(VALUE_USER_ONLINE, userDataSnapshot.child(VALUE_USER_ONLINE).getValue().toString());
        data.put(VALUE_COURSE_LOAD, userDataSnapshot.child(VALUE_COURSE_LOAD).getValue().toString());
        data.put(VALUE_OTHER, userDataSnapshot.child(VALUE_OTHER).getValue().toString());
        data.put(VALUE_IN_GROUP, userDataSnapshot.child(VALUE_IN_GROUP).getValue().toString());

        return data;
    }

    public void startGroup(String groupKey) {
        profileDBReference.child(VALUE_IN_GROUP).setValue(groupKey);
    }

    public void bookmarkGroup(String groupKey) {
        profileDBReference.child(PARENT_BOOKMARKED_GROUPS).child(groupKey).setValue(ServerValue.TIMESTAMP);
    }

    public void unBookmarkGroup(String groupKey) {
        profileDBReference.child(PARENT_BOOKMARKED_GROUPS).child(groupKey).removeValue();
    }

    public void joinGroup(String groupKey) {
        profileDBReference.child(VALUE_IN_GROUP).setValue(groupKey);
    }

    public void leaveGroup() {
        profileDBReference.child(VALUE_IN_GROUP).setValue(STATUS_NOT_IN_GROUP);
    }

    // Adds a new entry <chat key, gfKey> under Chats, where gf key is either a group key or a friend key (depending on type)
    public void newChat(String chatKey, String gfKey) {
        profileDBReference.child(PARENT_CHATS).child(chatKey).setValue(gfKey);
    }

    // This chat no longer exists, delete references from both chat participants
    public void deleteChat(String chatKey, String otherKey) {
        profileDBReference.child(PARENT_CHATS).child(chatKey).removeValue();
        FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(otherKey).child(PARENT_CHATS).child(chatKey).removeValue();
    }

    public void sentMessageTo(String chatKey, String otherKey) {
        // Maybe delete?
    }

    // Creates a new user profile in the database, returning a UserProfile
    // representation of it
    public static void createNewProfile(String key, String email) {
        DatabaseReference profileDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(key);

        profileDBRef.child(VALUE_PROFILE_COMPLETED).setValue(STATUS_PROFILE_INCOMPLETE);
        profileDBRef.child(VALUE_USER_KEY).setValue(key);
        profileDBRef.child(VALUE_EMAIL).setValue(email);
        profileDBRef.child(VALUE_FIRST_NAME).setValue("");
        profileDBRef.child(VALUE_MAJOR).setValue("");
        profileDBRef.child(VALUE_IN_GROUP).setValue(STATUS_NOT_IN_GROUP);
        profileDBRef.child(VALUE_COURSE_LOAD).setValue("");
        profileDBRef.child(VALUE_OTHER).setValue("");
        profileDBRef.child(VALUE_MESSAGE_LOCKED).setValue(STATUS_UNLOCKED);
    }

    public static String getKey() {
        if(userKey.isEmpty()) return "";
        else return userKey;
    }

    public void sendFriendRequest(final String friendKey) {
         FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(friendKey).child(PARENT_FRIEND_REQUESTS)
                 .child(UserProfile.getKey()).setValue(ServerValue.TIMESTAMP);
    }

    public void acceptFriendRequest(final String friendKey) {
        profileDBReference.child(PARENT_FRIEND_REQUESTS).child(friendKey).removeValue();
        profileDBReference.child(PARENT_FRIENDS).child(friendKey).setValue(ServerValue.TIMESTAMP);

        // in case we send friend requests at the same time only one of the friend requests will get deleted, ensure both are in this case
        final DatabaseReference friendDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(friendKey);
        friendDBRef.child(PARENT_FRIEND_REQUESTS).child(UserProfile.getKey()).removeValue();
        friendDBRef.child(PARENT_FRIENDS).child(UserProfile.getKey()).setValue(ServerValue.TIMESTAMP);
    }

    public void rejectFriendRequest(final String friendKey) {
        profileDBReference.child(PARENT_FRIEND_REQUESTS).child(friendKey).removeValue();
    }

    public void removeFriend(final String friendKey) {
        profileDBReference.child(PARENT_FRIENDS).child(friendKey).removeValue();
        FirebaseDatabase.getInstance().getReference().child(PARENT_USERS).child(friendKey)
            .child(PARENT_FRIENDS).child(UserProfile.getKey()).removeValue();
    }

    public void setMessageLocked(boolean messageLocked) {
        if(messageLocked) profileDBReference.child(VALUE_MESSAGE_LOCKED).setValue(STATUS_LOCKED);
        else profileDBReference.child(VALUE_MESSAGE_LOCKED).setValue(STATUS_UNLOCKED);
    }

    public void setChatLocked(boolean chatLocked) {
        if(chatLocked) profileDBReference.child(VALUE_CHAT_LOCKED).setValue(STATUS_LOCKED);
        else profileDBReference.child(VALUE_CHAT_LOCKED).setValue(STATUS_UNLOCKED);
    }
}
