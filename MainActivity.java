package com.example.st;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.FragmentManager;
import android.view.SubMenu;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.st.adapters.GroupExpandableListAdapter;
import com.example.st.adapters.MessageListAdapter;
import com.example.st.dialog_fragments.ChatLockedDialogFragment;
import com.example.st.dialog_fragments.GroupJoinDialogFragment;
import com.example.st.dialog_fragments.MessageLockedDialogFragment;
import com.example.st.dialog_fragments.NoCameraDialogFragment;
import com.example.st.dialog_fragments.QuitDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements ProfileFragment.OnProfileUpdatedListener,
        GroupExpandableListAdapter.GroupBookmarkListener, ChatManager.UpdateUserChatDataListener, MemberProfileView.LaunchMemberProfileCallback,
        MemberProfileFragment.FriendListener, ChatOverviewFragment.LockedListener, GroupManager.OnGroupStatusChangedListener {
    public static final String I_USER_KEY = "USER_KEY";
    public static final String I_START_FRAG = "START_FRAG";

    private static final int SUBJECT_ITEM_STARTING_INDEX = 5; // subjects start at the fifth item down in the drawer
    private static final int CAMERA_REQUEST_CODE = 100;

    // Use an ArrayList instead of Deque because ArrayList allows the addition of null values
    ArrayList<MenuItem> prevMenuItems;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private ConstraintLayout cLayout;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private UserProfile userProfile;

    String [] subjectsArray; // All the subjects to display in the drawer (alphabetized)
    int [] subjectStartLetterIndices; // contains the indices of the first word for each letter, this is for easy filtering of subjects

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = getIntent();

        // Constraint Layout
        cLayout = findViewById(R.id.mainCLayout);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout); // NavigationLayout contains the drawer and the fragment space we are swapping
        nvDrawer = findViewById(R.id.nvView); // NavigationView (the drawer, contains the menu)

        // Database setup
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null) {

                }
                // If we aren't signed into the database, switch to the login activity
                else {
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }
            }
        };

        // other initializations / callbacks
        ChatManager.bindListeners(this);
        FriendsAdapter.setLaunchFriendProfileCallback(this);
        GroupManager.setGroupStatusChangedListener(this);

        // Accessing resources
        subjectsArray = getResources().getStringArray(R.array.subjects);
        subjectStartLetterIndices = getResources().getIntArray(R.array.subjectStartLetterIndices);
        GroupManager.setValidGroupProperties(subjectsArray, getResources().getStringArray(R.array.buildings));

        // set up the drawer with all the subjects as menu item
        setupDrawerContent(nvDrawer, subjectsArray);

        // If we are calling onCreate from an activity switch we will need to fetch information from it's intent
        if(savedInstanceState == null) {
            // This is the only time a UserProfile object should be created
            userProfile = new UserProfile(i.getStringExtra(I_USER_KEY));

            // Set what fragment we initially land on based on whether the user has created their profile or not
            if(i.getStringExtra(I_START_FRAG).equals(ProfileFragment.class.getName()))
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, ProfileFragment.newInstance()).addToBackStack(null).commit();
            else
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MemberProfileFragment.newInstance(UserProfile.getKey())).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, homeFragment).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, CreateGroupFragment.newInstance()).commit();
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MyGroupFragment.newInstance()).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, SubjectFragment.newInstance("ACTSC")).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, ChatOverviewFragment.newInstance()).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, profileFragment).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, BookmarkedGroupsFragment.newInstance()).commit();
                //        .addToBackStack(Integer.toString(R.id.bookmarked_groups_fragment)).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MessageListFragment.newInstance()).commit();
                // getSupportFragmentManager().beginTransaction().replace(R.id.clContent, FriendsOverviewFragment.newInstance()).addToBackStack(null).commit();
        }

        checkForCameraPermission();
    }

    // Call our selectDrawerItem method when a drawer item is selected
    private void setupDrawerContent(NavigationView navigationView, String [] subjects) {
        navigationView.setItemIconTintList(null);
        final Menu menu = navigationView.getMenu();
        setupSubjectSearchView(menu);

        //searchView.setQueryHint("Search for a Subject");

        /* top part of the drawer (create group, my group, chats, etc...) */

        menu.findItem(R.id.drawer_create_group_fragment).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, CreateGroupFragment.newInstance()).addToBackStack(null).commit();
                changedItemChecked(item);
                drawerLayout.closeDrawers();
                return true;
            }
        })
        .getIcon().setColorFilter(getColor(R.color.create_group_icon_color), PorterDuff.Mode.SRC_IN);

        menu.findItem(R.id.drawer_my_group_fragment).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MyGroupFragment.newInstance()).addToBackStack(null).commit();
                changedItemChecked(item);
                drawerLayout.closeDrawers();
                return true;
            }
        })
        .getIcon().setColorFilter(getColor(R.color.my_group_icon_color), PorterDuff.Mode.SRC_IN);

        menu.findItem(R.id.drawer_bookmarked_groups_fragment).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, BookmarkedGroupsFragment.newInstance()).addToBackStack(null).commit();
                changedItemChecked(item);
                drawerLayout.closeDrawers();
                return true;
            }
        })
        .getIcon().setColorFilter(getColor(R.color.bookmarked_groups_icon_color), PorterDuff.Mode.SRC_IN);

        menu.findItem(R.id.drawer_chat_fragment).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, ChatOverviewFragment.newInstance()).addToBackStack(null).commit();
                changedItemChecked(item);
                drawerLayout.closeDrawers();
                return true;
            }
        })
        .getIcon().setColorFilter(getColor(R.color.my_chats_icon_color), PorterDuff.Mode.SRC_IN);;

        /* bottom part of the drawer (subjects) */
        for(int i = 0; i < subjects.length; i++) {
            final String subject = subjects[i];

            menu.add(R.id.drawer_submenu_subjects, i, i, subject).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.clContent, SubjectFragment.newInstance(subject)).addToBackStack(null).commit();
                    changedItemChecked(item);
                    drawerLayout.closeDrawers();
                    return true;
                }
            });
        }
    }

    // Adjust which drawer item is checked when we change fragments, null is sent
    // when we are selecting an actionbar menu item, where we don't have any item to check
    private void changedItemChecked(@Nullable MenuItem currItem) {
        if(!prevMenuItems.isEmpty()) {
            final MenuItem prevItem = prevMenuItems.get(prevMenuItems.size() - 1);
            if(prevItem != null) prevItem.setChecked(false);
        }

        if(currItem != null) currItem.setChecked(true);
        prevMenuItems.add(currItem);
    }

    // Properties of the SearchView in the drawer
    private void setupSubjectSearchView(final Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.drawerSubjectSearchView);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        final EditText searchText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);

        searchText.setTextSize(14);
        searchText.setTextColor(Color.BLACK);
        searchText.setGravity(Gravity.CENTER);
        searchText.setTypeface(searchText.getTypeface(), Typeface.BOLD);

        // Input is in all caps and has a max length of 6
        InputFilter[] editFilters = searchText.getFilters();
        InputFilter[] newFilters = new InputFilter[editFilters.length + 2];
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
        newFilters[editFilters.length] = new InputFilter.AllCaps();
        newFilters[editFilters.length + 1] = new InputFilter.LengthFilter(getResources().getInteger(R.integer.subject_length));
        searchText.setFilters(newFilters);

        // When we search for a subject code in the drawer filter the menu accordingly
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query.isEmpty()) return true;

                // 65 = A, 90 = Z
                // Courses only contain uppercase letters
                final char firstLetter = query.charAt(0);
                if(firstLetter < 65 || firstLetter > 90) {
                    return true;
                }

                // Based on the first letter we can reduce the amount of courses we have to compare
                // with the query. Figure out the location of all the words that start with the
                // first letter in subjectsArray
                final int startIndex = firstLetter - 65;
                final int wordsStart = subjectStartLetterIndices[startIndex];
                if(wordsStart == -1) return true; // -1 means no course starts with this letter

                // Find the last occurrence of the word that contains the starting letter, or the end of the array
                int wordsEnd = -1;
                for(int i = startIndex + 1; i <= subjectStartLetterIndices.length; i++) {
                    if(i == subjectStartLetterIndices.length) wordsEnd = subjectsArray.length - 1;
                    else if(subjectStartLetterIndices[i] != -1) {
                        wordsEnd = subjectStartLetterIndices[i];
                        break;
                    }
                }

                // Set all menu items invisible except the ones which match our query
                for(int i = subjectsArray.length - 1; i >= 0; i--) {
                    if(i >= wordsStart && i <= wordsEnd) {
                        final String subject = subjectsArray[i];
                        if(subject.length() >= query.length() && subject.substring(0, query.length()).equals(query)) {
                            menu.getItem(SUBJECT_ITEM_STARTING_INDEX + i).setVisible(true);
                            continue;
                        }
                    }

                    menu.getItem(SUBJECT_ITEM_STARTING_INDEX + i).setVisible(false);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Set all the subjects visible once we click the X
                if(newText.isEmpty()) {
                    for(int i = subjectsArray.length - 1; i >= 0; i--) {
                        menu.getItem(SUBJECT_ITEM_STARTING_INDEX + i).setVisible(true);
                    }
                }
                return false;
            }
        });
    }

    // What to do when an actionbar item is selected (not a drawer item)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_friends:
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, FriendsOverviewFragment.newInstance()).addToBackStack(null).commit();
                break;
            case R.id.action_open_profile:
                getSupportFragmentManager().beginTransaction().replace(R.id.clContent, ProfileFragment.newInstance()).addToBackStack(null).commit();
                break;
            case R.id.action_sign_out:
                mAuth.signOut();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return true;
        }

        changedItemChecked(null);
        return true;
    }

    // Initial setup of the action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        // stack size will always be >= 1
        final int stackSize = getSupportFragmentManager().getBackStackEntryCount();

        // No fragment to pop back to, ask them if they want to exit
        if(stackSize <= 1) {
            QuitDialogFragment popup = new QuitDialogFragment();
            popup.show(getFragmentManager(), "quitDialog");
            return;
        }

        // A stack size >= 2 means we are interacting with current fragment, and have visited at least one previous fragment.
        // It follows that prevMenuItems will have size >= 1
        getSupportFragmentManager().popBackStack();
        final MenuItem poppedItem = prevMenuItems.remove(prevMenuItems.size() - 1);
        if(poppedItem != null) poppedItem.setChecked(false);

        final MenuItem newTopItem = prevMenuItems.get(prevMenuItems.size() - 1);
        if(newTopItem != null) newTopItem.setChecked(true);
    }

    private void checkForCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == CAMERA_REQUEST_CODE) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                NoCameraDialogFragment noCameraDialogFragment = new NoCameraDialogFragment();
                noCameraDialogFragment.show(getSupportFragmentManager(), "NoCameraDialogFragment");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        prevMenuItems = new ArrayList<>();

        // TODO: maybe change, as of now the first fragment is one in the action bar
        changedItemChecked(null);

        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        mAuth.removeAuthStateListener(mAuthListener);
        prevMenuItems.clear();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // *************************************************************** INTERFACE IMPLEMENTATIONS ********************************************************************* //

    // Implements ProfileFragment.OnProfileUpdateListener
    @Override
    public void updateProfile(@Nullable String firstName, @Nullable String major, @Nullable String courseLoad, @Nullable String addInfo) {
        userProfile.updateUserProfile(firstName, major, courseLoad, addInfo);
    }

    // Implements MemberProfileFragment.FriendListener
    @Override
    public void sendFriendRequest(String friendKey) {
        userProfile.sendFriendRequest(friendKey);
    }

    // Implements MemberProfileFragment.FriendListener
    @Override
    public void acceptFriendRequest(String friendKey) {
        userProfile.acceptFriendRequest(friendKey);
    }

    // Implements MemberProfileFragment.FriendListener
    @Override
    public void rejectFriendRequest(String friendKey) {
        userProfile.rejectFriendRequest(friendKey);
    }

    // Implements MemberProfileFragment.FriendListener
    @Override
    public void removeFriend(String friendKey) {
        userProfile.removeFriend(friendKey);
    }

    // Implements GroupExpandableListAdapter.GroupWantedListener
    @Override
    public void changeBookmarkedStatus(String groupKey, boolean bookmarked) {
        if(bookmarked) {
            userProfile.bookmarkGroup(groupKey);
            MyUtils.simpleSnackBar(cLayout, "Group Bookmarked");
        }
        else userProfile.unBookmarkGroup(groupKey);
    }

    // Implements ChatManager.UpdateUserChatDataListener
    @Override
    public void launchNewChat(String chatKey, String gfKey) {
        userProfile.newChat(chatKey, gfKey);
        launchExistingChat(chatKey);
    }

    // Implements ChatManager.UpdateUserChatDataListener
    @Override
    public void launchExistingChat(String chatKey) {
        getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MessageListFragment.newInstance(chatKey))
                .addToBackStack(Integer.toString(R.id.drawer_create_group_fragment)).commit();
        changedItemChecked(nvDrawer.getMenu().findItem(R.id.drawer_chat_fragment));
    }

    // Implements ChatManager.UpdateUserChatDataListener
    @Override
    public void deleteChat(String chatKey, String otherKey) {
        userProfile.deleteChat(chatKey, otherKey);
    }

    // Implements ChatManager.UpdateUserChatDataListener
    @Override
    public void sentMessageTo(String chatKey, String otherKey) {
        userProfile.sentMessageTo(chatKey, otherKey);
    }

    // Implements MemberProfileView.LaunchMemberProfileCallback
    @Override
    public void launchMemberProfileFrag(String memberKey, @Nullable Bitmap profileBitmap, boolean groupTicked) {
        getSupportFragmentManager().beginTransaction().replace(R.id.clContent, MemberProfileFragment.newInstance(memberKey, profileBitmap))
                .addToBackStack(null).commit();

        if(groupTicked) changedItemChecked(nvDrawer.getMenu().findItem(R.id.drawer_my_group_fragment));
        else changedItemChecked(null);
    }

    // Implements ChatOverviewFragment.LockedListener
    @Override
    public void messageLocked(boolean messageLocked) {
        if(messageLocked) {
            userProfile.setMessageLocked(true);
            // Already shows in message frag, kind of annoying to pop up in chat overview frag too
            // MessageLockedDialogFragment popup = new MessageLockedDialogFragment();
            // popup.show(getSupportFragmentManager(), "messageLockedDialog");
        }
        else userProfile.setMessageLocked(false);
    }

    // Implements ChatOverviewFragment.LockedListener
    @Override
    public void chatLocked(boolean chatLocked) {
        if(chatLocked) {
            userProfile.setChatLocked(true);
            ChatLockedDialogFragment popup = new ChatLockedDialogFragment();
            popup.show(getSupportFragmentManager(), "chatLockedDialog");
        }
        else userProfile.setChatLocked(false);
    }

    // Implements GroupManager.OnGroupStatusChanged
    @Override
    public void joinedGroup(String groupKey, boolean success) {
        if(success) {
            userProfile.joinGroup(groupKey);
            MyUtils.simpleSnackBar(cLayout, "Successfully joined group");
        }
        else MyUtils.simpleSnackBar(cLayout, "Could not join group");
    }

    // Implements GroupManager.OnGroupStatusChanged
    @Override
    public void startedGroup(String groupKey) {
        userProfile.startGroup(groupKey);
        MyUtils.simpleSnackBar(cLayout, "Successfully created group");
    }

    // Implements GroupManager.OnGroupStatusChanged
    @Override
    public void leftGroup() {
        userProfile.leaveGroup();
        MyUtils.simpleSnackBar(cLayout, "Successfully left group");
    }
}
