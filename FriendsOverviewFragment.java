package com.example.st;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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

public class FriendsOverviewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private int fetchedFriends;
    private int totalFriends;

    private RecyclerView friendsRecycler;
    private SearchView friendsSearchView;
    private SearchView.SearchAutoComplete searchAutoComplete;
    private FloatingActionButton fab;
    private ConstraintLayout constraintLayout;
    private PopupMenu filterPopupMenu;
    private SwipeRefreshLayout friendsRefreshLayout;

    private FriendsAdapter adapter;
    private FriendsAdapter filteredAdapter;

    // We want to show incoming friend requests first, this will be appened to the front of friendInfoLost
    private ArrayList <FriendsAdapter.FriendInfo> friendRequestInfoList;
    private ArrayList <FriendsAdapter.FriendInfo> friendInfoList;
    private ArrayList <FriendsAdapter.FriendInfo> filteredFriendInfoList; // if we want to filter "in group" or not "in group", etc..
    private ArrayList <String> friendNames;

    private MenuItem prevMenuItem;

    private static final String TITLE = "Friends";

    public FriendsOverviewFragment() {
        // Required empty public constructor
    }

    public static FriendsOverviewFragment newInstance() {
        FriendsOverviewFragment fragment = new FriendsOverviewFragment();
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

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friends_overview, container, false);

        fetchedFriends = 0;
        friendInfoList = new ArrayList<>();
        friendRequestInfoList = new ArrayList<>();
        filteredFriendInfoList = new ArrayList<>();

        // For the SearchView
        friendNames = new ArrayList<>();

        // RefreshLayout for the RecyclerView
        friendsRefreshLayout = view.findViewById(R.id.friendsOverviewRefresh);
        friendsRefreshLayout.setOnRefreshListener(this);

        // ConstraintLayout
        constraintLayout = view.findViewById(R.id.friendsOverviewCLayout);

        // FloatingActionButton
        fab = view.findViewById(R.id.friendsOverviewFloatingActionButton);

        // PopupMenu
        filterPopupMenu = new PopupMenu(getContext(), fab);
        filterPopupMenu.getMenuInflater().inflate(R.menu.friends_popup_filter_menu, filterPopupMenu.getMenu());
        filterPopupMenu.setOnMenuItemClickListener(filterPopupMenuListener);

        // SearchView
        friendsSearchView = view.findViewById(R.id.friendsOverviewSearchView);
        searchAutoComplete = friendsSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        //searchAutoComplete.setThreshold(0);

        // Recycler
        friendsRecycler = view.findViewById(R.id.friendsOverviewRecycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        friendsRecycler.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        friendsRecycler.addItemDecoration(dividerItemDecoration);

        Query fetchFriends = FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey());
        fetchFriends.orderByValue().addListenerForSingleValueEvent(retrieveFriendsListener);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterPopupMenu.show();
            }
        });

        friendsSearchView.setOnQueryTextListener(queryTextListener);

        friendsSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(adapter != null) friendsRecycler.setAdapter(adapter);
                return true;
            }
        });

        searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(adapter != null) { // if the adapter isn't null we must have fetched all the names
                    final String name = friendNames.get(position);
                    filterByQuery(name);
                }
            }
        });

        return view;
    }

    private SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if(adapter != null) filterByQuery(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if(newText.isEmpty()) if(adapter != null) friendsRecycler.setAdapter(adapter);
            return false;
        }
    };

    private PopupMenu.OnMenuItemClickListener filterPopupMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            // Haven't fetched the profiles yet so we have nothing to filter
            if(item.equals(prevMenuItem) || friendsRecycler.getAdapter() == null) return true;

            if(prevMenuItem != null) prevMenuItem.setChecked(false);
            item.setChecked(true);
            prevMenuItem = item;

            switch (item.getItemId()) {
                case R.id.friendsFilterInGroup: {
                    filterInGroup();
                    break;
                }
                case R.id.friendsFilterNotInGroup: {
                    filterNotInGroup();
                    break;
                }
                case R.id.friendsFilterNone: {
                    friendsRecycler.setAdapter(adapter);
                    break;
                }
            }
            return true;
        }
    };

    private void filterByQuery(String query) {
        query = query.trim();
        if(query.isEmpty()) {
            friendsRecycler.setAdapter(adapter);
            return;
        }

        filteredFriendInfoList.clear();

        for(FriendsAdapter.FriendInfo friendInfo : friendInfoList) {
            final String friendName = friendInfo.getName();

            final int queryLength = query.length();
            if(queryLength <= friendName.length()) {
                if(query.equals(friendName.substring(0, queryLength))) filteredFriendInfoList.add(friendInfo);
            }
        }

        filteredAdapter = new FriendsAdapter(getContext(), filteredFriendInfoList);
        friendsRecycler.setAdapter(filteredAdapter);
    }

    private void filterInGroup() {
        filteredFriendInfoList.clear();

        for(FriendsAdapter.FriendInfo friendInfo : friendInfoList) {
            if(!friendInfo.getGroupStatus().equals(UserProfile.STATUS_NOT_IN_GROUP))
                filteredFriendInfoList.add(friendInfo);
        }

        filteredAdapter = new FriendsAdapter(getContext(), filteredFriendInfoList);
        friendsRecycler.setAdapter(filteredAdapter);
    }

    private void filterNotInGroup() {
        filteredFriendInfoList.clear();

        for(FriendsAdapter.FriendInfo friendInfo : friendInfoList) {
            if(friendInfo.getGroupStatus().equals(UserProfile.STATUS_NOT_IN_GROUP))
                filteredFriendInfoList.add(friendInfo);
        }

        filteredAdapter = new FriendsAdapter(getContext(), filteredFriendInfoList);
        friendsRecycler.setAdapter(filteredAdapter);
    }

    // Users -> UserKey
    // Retrieves all friend data for our actual friends as well as friend requests
    private ValueEventListener retrieveFriendsListener = new ValueEventListener() {
        @Override
        public void onDataChange(final DataSnapshot userDataSnapshot) {
            final DataSnapshot friendRequestDataSnap = userDataSnapshot.child(UserProfile.PARENT_FRIEND_REQUESTS);
            final DataSnapshot friendDataSnap = userDataSnapshot.child(UserProfile.PARENT_FRIENDS);
            totalFriends = (int)friendRequestDataSnap.getChildrenCount() + (int)friendDataSnap.getChildrenCount();

            // cycle through friend requests
            for(final DataSnapshot friendRequestData : friendRequestDataSnap.getChildren()) {
                final String friendKey = friendRequestData.getKey();

                // fetch friend request's user data
                FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(friendKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) friendRequestData.getRef().removeValue();

                                // It's possible they send a friend request as we add them as a friend. We will be friends
                                // and have a friend request from them, when only one is allowed. This cleans that up
                                if(friendDataSnap.child(friendKey).exists()) friendRequestData.getRef().removeValue();
                                else friendRequestInfoList.add(newFriendInfo(friendKey, dataSnapshot, true));

                                increaseFriendsFetched();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }

            // cycle through friends
            for(final DataSnapshot friendData : friendDataSnap.getChildren()) {
                final String friendKey = friendData.getKey();

                // fetch friend's user data
                FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(friendKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) friendData.getRef().removeValue();
                                else friendInfoList.add(newFriendInfo(friendKey, dataSnapshot, false));
                                increaseFriendsFetched();
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

    // Create a friendInfo object and add it to the friendInfoList to display in the adapter
    // Also adds the friends names to the name list which is used by the SearchView
    // Users -> friendKey
    private FriendsAdapter.FriendInfo newFriendInfo(String friendKey, DataSnapshot friendData, boolean isFriendRequest) {
        final String name  = friendData.child(UserProfile.VALUE_FIRST_NAME).getValue().toString();
        final String major = friendData.child(UserProfile.VALUE_MAJOR).getValue().toString();
        final String groupStatus = friendData.child(UserProfile.VALUE_IN_GROUP).getValue().toString();
        final String onlineStatus = friendData.child(UserProfile.VALUE_USER_ONLINE).getValue().toString();

        friendNames.add(name);
        return new FriendsAdapter.FriendInfo(friendKey, name, major, groupStatus, onlineStatus, isFriendRequest);
    }

    // Once we have gone fetched all of our friend / friend request data, set the adapter
    public void increaseFriendsFetched() {
        fetchedFriends++;

        if(fetchedFriends == totalFriends) {
            Collections.sort(friendInfoList);
            Collections.sort(friendRequestInfoList);
            friendInfoList.addAll(0, friendRequestInfoList);

            adapter = new FriendsAdapter(getContext(), friendInfoList);
            friendsRecycler.setAdapter(adapter);

            ArrayAdapter <String> nameAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, friendNames);
            searchAutoComplete.setAdapter(nameAdapter);
        }
    }

    @Override
    public void onRefresh() {
        if(adapter != null) friendsRecycler.setAdapter(adapter);
        if(prevMenuItem != null) prevMenuItem.setChecked(false);
        friendsRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
