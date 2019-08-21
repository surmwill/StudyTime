package com.example.st;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class MemberProfileFragment extends Fragment implements ImageStore.ImageOperationListener {
    private static final String ARG_MEMBER_KEY = "ARG_MEMBER_KEY";
    private static final String ARG_PROFILE_BITMAP = "ARG_PROFILE_BITMAP";
    private static final int STATUS_NOT_FRIENDS = 1;
    private static final int STATUS_FRIEND_REQUEST_RECIEVED = 2;
    private static final int STATUS_FRIENDS = 3;
    private static final int STATUS_UNKNOWN = 0;

    private int friendStatus = STATUS_UNKNOWN;

    private String memberKey;
    private Bitmap profileBitmap;
    private String memberName; // Used for launching chat

    private ConstraintLayout cLayout;
    private ImageView profileImage;

    private TextView firstNameText;
    private TextView majorText;
    private TextView courseLoadText;
    private TextView additionalInfoText;

    private Button leftButton;
    private Button rightButton;

    private ImageStore memberImageStore;
    private FriendListener mCallback;

    public MemberProfileFragment() {
        // Required empty public constructor
    }

    public static MemberProfileFragment newInstance(String memberKey, @Nullable Bitmap bitmap) {
        MemberProfileFragment fragment = new MemberProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEMBER_KEY, memberKey);
        args.putParcelable(ARG_PROFILE_BITMAP, bitmap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (FriendListener) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(e.toString() + " must implement AddedFriendListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            memberKey = getArguments().getString(ARG_MEMBER_KEY);
            memberImageStore = new ImageStore(memberKey, this);
            profileBitmap = getArguments().getParcelable(ARG_PROFILE_BITMAP);
        }
    }

    // TODO: dynamic title
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_member_profile, container, false);

        // TextViews
        firstNameText = view.findViewById(R.id.memberFragProfileFirstName);
        majorText = view.findViewById(R.id.memberFragProfileMajor);
        courseLoadText = view.findViewById(R.id.memberFragCourseLoad);
        additionalInfoText = view.findViewById(R.id.memberFragAdditionalInfo);

        // ImageView
        profileImage = view.findViewById(R.id.memberFragProfileImage);

        // Buttons
        leftButton = view.findViewById(R.id.memberFragLeftButton);
        rightButton = view.findViewById(R.id.memberFragRightButton);

        // ConstraintLayout
        cLayout = view.findViewById(R.id.memberFragCLayout);

        if(profileBitmap == null) memberImageStore.fetchProfileImage(getContext());
        else profileImage.setImageBitmap(profileBitmap);

        if(!memberKey.equals(UserProfile.getKey()))
            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                .addListenerForSingleValueEvent(determineFriendStatusListener);
        else {
            leftButton.setVisibility(View.GONE);
            rightButton.setVisibility(View.GONE);
        }

        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(memberKey)
                .addListenerForSingleValueEvent(memberProfileListener);

        return view;
    }

    // Users -> UserKey
    private ValueEventListener determineFriendStatusListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // First check if they are a friend
            for(DataSnapshot friendData : dataSnapshot.child(UserProfile.PARENT_FRIENDS).getChildren()) {
                if(friendData.getKey().equals(memberKey)) {
                    setFriendsButtonLayout();
                    return;
                }
            }

            // Then check if they sent us a friend request
            for(DataSnapshot friendRequestData : dataSnapshot.child(UserProfile.PARENT_FRIEND_REQUESTS).getChildren()) {
                if(friendRequestData.getKey().equals(memberKey)) {
                    setFriendRequestButtonLayout();
                    return;
                }
            }

            // Otherwise they are not a friend
            setNotFriendButtonLayout();

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    // left button = start chat, right button = add friend
    private void setNotFriendButtonLayout () {
        leftButton.setText(getString(R.string.startChatButtonText));
        leftButton.setOnClickListener(startChatListener);

        rightButton.setText(getString(R.string.sendFriendRequestButtonText));
        rightButton.setOnClickListener(sendFriendRequestButtonListener);
        friendStatus = STATUS_NOT_FRIENDS;
    }

    // left button = Accept Friend Request, right button = Reject Friend Request
    private void setFriendRequestButtonLayout() {
        leftButton.setText(getString(R.string.acceptFriendRequestButtonText));
        leftButton.setOnClickListener(acceptFriendRequestButtonListener);

        rightButton.setText(getString(R.string.rejectFriendRequestButtonText));
        rightButton.setOnClickListener(rejectFriendRequestButtonListener);
        friendStatus = STATUS_NOT_FRIENDS;
    }

    // left button = start chat, right button = delete friend
    private void setFriendsButtonLayout() {
        leftButton.setText(getString(R.string.startChatButtonText));
        leftButton.setOnClickListener(startChatListener);

        rightButton.setText(getString(R.string.deleteFriendButtonText));
        rightButton.setOnClickListener(deleteFriendButtonListener);
        friendStatus = STATUS_FRIENDS;
    }

    private View.OnClickListener startChatListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(memberKey.equals(UserProfile.getKey())) {
                MyUtils.simpleSnackBar(cLayout, "You cannot start a chat with yourself");
                return;
            }

            // We pass the member's name to start a new chat, make sure we load that first
            // as well as the friend status (see below)
            if(memberName == null || friendStatus == STATUS_UNKNOWN) {
                MyUtils.simpleSnackBar(cLayout, "Fetching data... try again shortly");
                return;
            }

            // We must be friends to start the chat
            if(friendStatus != STATUS_FRIENDS) {
                AddFriendFirstDialogFragment popup = new AddFriendFirstDialogFragment();
                popup.show(getChildFragmentManager(), "addFriendFirstPopup");
                return;
            }

            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final String userName = dataSnapshot.child(UserProfile.VALUE_FIRST_NAME).getValue().toString();
                            final DataSnapshot userChatData = dataSnapshot.child(UserProfile.PARENT_CHATS);
                            ChatManager.launchFriendChat(userChatData, memberKey, userName, memberName);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    };

    private View.OnClickListener acceptFriendRequestButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallback.acceptFriendRequest(memberKey);
            MyUtils.simpleSnackBar(cLayout, "request accepted");
            setFriendsButtonLayout();
        }
    };

    private View.OnClickListener sendFriendRequestButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallback.sendFriendRequest(memberKey);
            final Button button = (Button) v;
            button.setText(getString(R.string.sentFriendRequestSuccessButtonText));
            button.setClickable(false);
        }
    };

    private View.OnClickListener rejectFriendRequestButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallback.rejectFriendRequest(memberKey);
            setNotFriendButtonLayout();
        }
    };

    private View.OnClickListener deleteFriendButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Snackbar snackbar = Snackbar.make(cLayout, "Deleted Friend", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // do nothing
                }
            });

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    if(event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        mCallback.removeFriend(memberKey);
                        setNotFriendButtonLayout();
                    }
                }
            });

            snackbar.show();
        }
    };

    private ValueEventListener memberProfileListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(!dataSnapshot.exists()) return;

            HashMap<String, String> memberData = UserProfile.getUserProperties(dataSnapshot);

            memberName = memberData.get(UserProfile.VALUE_FIRST_NAME);
            final String major = memberData.get(UserProfile.VALUE_MAJOR);
            final String onlineStatus = memberData.get(UserProfile.VALUE_USER_ONLINE);
            final String courseLoad = memberData.get(UserProfile.VALUE_COURSE_LOAD);
            final String additionalInfo = memberData.get(UserProfile.VALUE_OTHER);

            if(onlineStatus.equals(UserProfile.STATUS_ONLINE))
                firstNameText.setTextColor(ContextCompat.getColor(getContext(), R.color.online));
            else
                firstNameText.setTextColor(ContextCompat.getColor(getContext(), R.color.offline));

            getActivity().setTitle(memberName);

            firstNameText.setText(memberName);
            majorText.setText(major);
            courseLoadText.setText(courseLoad);
            additionalInfoText.setText(additionalInfo);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    public void imageFetched(boolean success, @Nullable Bitmap imageBitmap) {

    }

    @Override
    public void imageUploaded(boolean success) {

    }

    interface FriendListener {
        void sendFriendRequest(String friendKey);
        void acceptFriendRequest(String friendKey);
        void rejectFriendRequest(String friendKey);
        void removeFriend(String friendKey);
    }

    public static class AddFriendFirstDialogFragment extends android.support.v4.app.DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder
                    .setMessage("This person must be your friend before starting a chat with them.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    });

            return builder.create();
        }
    }

}