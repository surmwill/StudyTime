package com.example.st;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.st.adapters.BookmarkedGroupsAdapter;
import com.example.st.adapters.GroupInfo;
import com.example.st.dialog_fragments.ChatLockedDialogFragment;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BookmarkedGroupsFragment extends Fragment {
    private static final String TITLE = "Bookmarks";

    int totalGroups;
    int groupsFetched;

    private RecyclerView bookmarkedGroupsRecycler;
    private BookmarkedGroupsAdapter adapter;
    private ConstraintLayout cLayout;

    private ArrayList<GroupInfo> groupInfos;

    public BookmarkedGroupsFragment() {
        // Required empty public constructor
    }

    public static BookmarkedGroupsFragment newInstance() {
        BookmarkedGroupsFragment fragment = new BookmarkedGroupsFragment();
        return fragment;
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
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bookmarked_groups, container, false);

        groupsFetched = 0;
        groupInfos = new ArrayList<>();

        // ConstraintLayout
        cLayout = view.findViewById(R.id.bookmarkedGroupsCLayout);

        // RecyclerView
        bookmarkedGroupsRecycler = view.findViewById(R.id.bookmarkedGroupsRecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        bookmarkedGroupsRecycler.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(bookmarkedGroupsRecycler.getContext(), layoutManager.getOrientation());
        bookmarkedGroupsRecycler.addItemDecoration(dividerItemDecoration);

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(bookmarkedGroupsRecycler);

        Query userBookmarkedGroups = FirebaseDatabase.getInstance().getReference(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                .child(UserProfile.PARENT_BOOKMARKED_GROUPS);
        userBookmarkedGroups.orderByKey().addListenerForSingleValueEvent(bookmarkedGroupsListener);

        return view;
    }

    // Initially show all the groups in the order we added them
    private ValueEventListener bookmarkedGroupsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            totalGroups = (int)dataSnapshot.getChildrenCount();

            for(final DataSnapshot groupData : dataSnapshot.getChildren()) {
                final String groupKey = groupData.getKey();
                final long timestamp = groupData.getValue(Long.class);

                FirebaseDatabase.getInstance().getReference(GroupManager.PARENT_GROUPS).child(groupKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // no need to keep a bookmarked group that no longer exists
                                if(!dataSnapshot.exists()) groupData.getRef().removeValue();
                                else {
                                    final String groupName = dataSnapshot.child(GroupManager.VALUE_NAME).getValue().toString();
                                    final String studying = dataSnapshot.child(GroupManager.VALUE_SUBJECT).getValue().toString() +
                                            dataSnapshot.child(GroupManager.VALUE_COURSE_CODE).getValue().toString();
                                    final String building = dataSnapshot.child(GroupManager.VALUE_BUILDING).getValue().toString();
                                    final String description = dataSnapshot.child(GroupManager.VALUE_DESCRIPTION).getValue().toString();
                                    final String groupStatus = dataSnapshot.child(GroupManager.VALUE_OPEN).getValue().toString();
                                    final String leaderKey = dataSnapshot.child(GroupManager.VALUE_LEADER).getValue().toString();
                                    final long size = dataSnapshot.child(GroupManager.VALUE_GROUP_SIZE).getValue(Long.class);

                                    GroupInfo groupInfo = new GroupInfo(
                                            groupKey, groupName, studying, building,
                                            description, groupStatus, leaderKey, size, timestamp);

                                    groupInfos.add(groupInfo);
                                }

                                increaseGroupsFetched();
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

    public void increaseGroupsFetched() {
        groupsFetched++;

        if (groupsFetched == totalGroups) {
            Collections.sort(groupInfos);

            adapter = new BookmarkedGroupsAdapter(getContext(), groupInfos);
            bookmarkedGroupsRecycler.setAdapter(adapter);
        }
    }

    private ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        private Bitmap deleteIcon;
        private Bitmap startChatIcon;

        // In DP (they get converted to PX in init())
        private int iconWidth = 30;
        private int iconHeight = 30;

        private int iconPadding = 30;
        private int minDragLengthForIcon;
        private int deleteBackgroundColor;
        private int startChatBackgroundColor;

        private boolean initialized = false;

        private Rect backgroundRect;
        private Paint deleteBackgroundPaint;
        private Paint startChatBackgroundPaint;

        int scRed, scGreen, scBlue;
        int dRed, dGreen, dBlue;


        public void init() {
            initialized = true;

            Resources r = getResources();

            startChatBackgroundColor = ContextCompat.getColor(getContext(), R.color.bookmark_start_chat_target);
            deleteBackgroundColor = ContextCompat.getColor(getContext(), R.color.bookmark_delete_chat_target);

            // We will be changing these RGB values as the user slides the view further across the screen
            scRed = Color.red(startChatBackgroundColor);
            scGreen = Color.green(startChatBackgroundColor);
            scBlue = Color.blue(startChatBackgroundColor);

            dRed = Color.red(deleteBackgroundColor);
            dGreen = Color.green(deleteBackgroundColor);
            dBlue = Color.blue(deleteBackgroundColor);

            // DP to PX
            iconWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconWidth, r.getDisplayMetrics());
            iconHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconHeight, r.getDisplayMetrics());
            iconPadding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconPadding, r.getDisplayMetrics());

            minDragLengthForIcon = iconPadding * 2 + iconWidth;

            deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete);
            deleteIcon = MyUtils.getResizedBitmap(deleteIcon, iconWidth, iconHeight);

            startChatIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_start_chat);
            startChatIcon = MyUtils.getResizedBitmap(startChatIcon, iconWidth, iconHeight);

            deleteBackgroundPaint = new Paint();
            deleteBackgroundPaint.setColor(deleteBackgroundColor);
            deleteBackgroundPaint.setAntiAlias(true);
            deleteBackgroundPaint.setStyle(Paint.Style.FILL);

            startChatBackgroundPaint = new Paint();
            startChatBackgroundPaint.setColor(startChatBackgroundColor);
            startChatBackgroundPaint.setAntiAlias(true);
            startChatBackgroundPaint.setStyle(Paint.Style.FILL);

            backgroundRect = new Rect();
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            BookmarkedGroupsAdapter.ViewHolder vh = ((BookmarkedGroupsAdapter.ViewHolder)viewHolder);
            final int vhPosition = vh.getAdapterPosition();

            final GroupInfo groupInfo = adapter.getGroupInfo(vhPosition);
            final String groupTitle = groupInfo.getName();
            final String groupKey = groupInfo.getKey();

            if(direction == ItemTouchHelper.LEFT) { // delete
                adapter.removeGroup(vhPosition);

                Snackbar snackbar = Snackbar.make(cLayout, "Group Removed: " + groupTitle, Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adapter.restoreGroupKey(vhPosition, groupInfo);
                    }
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        if(event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                                    .child(UserProfile.PARENT_BOOKMARKED_GROUPS).child(groupKey).removeValue();
                        }
                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }
            else if(direction == ItemTouchHelper.RIGHT) { // start chat
                final String groupLeaderKey = groupInfo.getLeaderKey();
                final String groupStudying = groupInfo.getStudying();

                if(groupLeaderKey.equals(UserProfile.getKey())) {
                    MyUtils.simpleSnackBar(cLayout, "You cannot start a chat with yourself");
                    return;
                }

                FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                final String chatLocked = dataSnapshot.child(UserProfile.VALUE_CHAT_LOCKED).getValue().toString();

                                if(chatLocked.equals(UserProfile.STATUS_LOCKED)) {
                                    ChatLockedDialogFragment popup = new ChatLockedDialogFragment();
                                    popup.show(getChildFragmentManager(), "chatLockedPopup");
                                    adapter.notifyItemChanged(vhPosition);
                                }
                                else {
                                    final DataSnapshot userChatData = dataSnapshot.child(UserProfile.PARENT_CHATS);
                                    ChatManager.launchGroupChat(userChatData, groupKey, groupLeaderKey, groupTitle, groupStudying);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            // view the background view
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            if(!initialized) {
                init();
            }

            // left drag
            if(dX < 0){
                drawDelete(c, viewHolder, dX);
            }
            // right drag
            else {
                drawStartChat(c, viewHolder, dX);
            }
        }

        // left drag
        void drawDelete(Canvas c, RecyclerView.ViewHolder viewHolder, float dX) {
            View itemView = viewHolder.itemView;

            // The further we drag the brighter the background, this stops once we drag past
            // half to signal that that's the distance we need to trigger an events
            float halfScreen = itemView.getRight() / 2.0f;
            float dragPct = Math.min(Math.abs(dX) / halfScreen, 1.0f);

            String red = Integer.toHexString((int)(dRed * dragPct));
            if(red.length() < 2) red = "0" + red;

            String green = Integer.toHexString((int)(dGreen * dragPct));
            if(green.length() < 2) green = "0" + green;

            String blue = Integer.toHexString((int)(dBlue * dragPct));
            if(blue.length() < 2) blue = "0" + blue;

            String colorString = "#" + red + green + blue;
            deleteBackgroundPaint.setColor(Color.parseColor(colorString));

            backgroundRect.left = itemView.getRight() + (int)dX;
            backgroundRect.top = itemView.getTop();
            backgroundRect.right = itemView.getRight();
            backgroundRect.bottom = itemView.getBottom();

            c.drawRect(backgroundRect, deleteBackgroundPaint);

            int dragLength = (int)Math.abs(dX);

            // Once we have room for the icon and equal padding on either side, draw the icon in the rightmost (with padding)
            // center position of the ViewHolder
            if(dragLength > minDragLengthForIcon) {
                c.drawBitmap(
                        deleteIcon,
                        itemView.getRight() - (iconPadding + iconWidth),
                        (itemView.getBottom() + itemView.getTop()) / 2 - iconHeight / 2,
                        null
                );
            }
        }

        // right drag
        void drawStartChat(Canvas c, RecyclerView.ViewHolder viewHolder, float dX) {
            View itemView = viewHolder.itemView;

            // The further we drag the brighter the background, this stops once we drag past
            // half to signal that that's the distance we need to trigger an events
            float halfScreen = itemView.getRight() / 2.0f;
            float dragPct = Math.min(dX / halfScreen, 1.0f);

            String red = Integer.toHexString((int)(scRed * dragPct));
            if(red.length() < 2) red = "0" + red;

            String green = Integer.toHexString((int)(scGreen * dragPct));
            if(green.length() < 2) green = "0" + green;

            String blue = Integer.toHexString((int)(scBlue * dragPct));
            if(blue.length() < 2) blue = "0" + blue;

            String colorString = "#" + red + green + blue;
            startChatBackgroundPaint.setColor(Color.parseColor(colorString));

            int dragLength = (int)dX;

            backgroundRect.left = itemView.getLeft();
            backgroundRect.top = itemView.getTop();
            backgroundRect.right = itemView.getLeft() + dragLength;
            backgroundRect.bottom = itemView.getBottom();

            c.drawRect(backgroundRect, startChatBackgroundPaint);

            // Once we have room for the icon and equal padding on either side, draw the icon in the rightmost (with padding)
            // center position of the ViewHolder
            if(dragLength > minDragLengthForIcon) {
                c.drawBitmap(
                        startChatIcon,
                        itemView.getLeft() + iconPadding,
                        (itemView.getBottom() + itemView.getTop()) / 2 - iconHeight / 2,
                        null
                );
            }
        }
    };

}