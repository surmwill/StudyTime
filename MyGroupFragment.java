package com.example.st;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRouter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.st.dialog_fragments.ChatLockedDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;


public class MyGroupFragment extends Fragment {
    private static final String TITLE = "My Group";
    private static final int LOWER_LL_PADDING_DP = 20;
    private static final int MEMBER_PROFILE_VIEW_HEIGHT_DP = 75;

    private int LOWER_LL_PADDING_PX;
    private int MEMBER_PROFILE_VIEW_HEIGHT_PX;
    private boolean memberListShowing;
    private boolean memberListGenerated;

    private String groupKey;

    // Layout
    private ConstraintLayout myGroupCLayout;
    private LinearLayout memberLL;

    // Text
    private EditText myGroupName;
    private EditText myGroupDescription;
    private EditText myGroupCourseCode;
    private AutoCompleteTextView myGroupBuilding;
    private AutoCompleteTextView myGroupSubject;

    // Buttons
    private Button myGroupUpdateButton;
    private Button myGroupLeaveButton;
    private ToggleButton myGroupToggle;

    // ImageView
    private ImageView dropDownImage;
    private ImageView addMemberImage;

    public MyGroupFragment() {
        // Required empty public constructor
    }

    public static MyGroupFragment newInstance() {
        MyGroupFragment fragment = new MyGroupFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
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
        final View view = inflater.inflate(R.layout.fragment_my_group, container, false);

        LOWER_LL_PADDING_PX = MyUtils.convDpToPx(getContext(), LOWER_LL_PADDING_DP);
        MEMBER_PROFILE_VIEW_HEIGHT_PX = MyUtils.convDpToPx(getContext(), MEMBER_PROFILE_VIEW_HEIGHT_DP);

        memberListGenerated = false;
        memberListShowing = false;

        // EditText
        myGroupName = view.findViewById(R.id.myGroupName);
        myGroupDescription = view.findViewById(R.id.myGroupDescription);
        myGroupCourseCode = view.findViewById(R.id.myGroupCourseCode);

        // AutoCompleteText
        myGroupBuilding = view.findViewById(R.id.myGroupBuilding);
        myGroupSubject = view.findViewById(R.id.myGroupSubject);

        MyUtils.addUpperCaseFilter(myGroupSubject);
        MyUtils.addUpperCaseFilter(myGroupBuilding);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                GroupManager.getValidSubjects());
        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                GroupManager.getValidBuildings());

        myGroupSubject.setAdapter(subjectAdapter);
        myGroupBuilding.setAdapter(buildingAdapter);

        setAutoTextListeners();

        // Buttons
        myGroupLeaveButton = view.findViewById(R.id.myGroupLeaveButton);
        myGroupUpdateButton = view.findViewById(R.id.myGroupUpdateButton);

        // Switch
        myGroupToggle = view.findViewById(R.id.myGroupToggle);

        // ImageView
        dropDownImage = view.findViewById(R.id.myGroupDropDownImage);
        addMemberImage = view.findViewById(R.id.myGroupAddMemberImage);

        // Layouts
        myGroupCLayout = view.findViewById(R.id.myGroupCLayout);

        // Figure out what group we are apart of (stored in our user profile)
        FirebaseDatabase.getInstance().getReference(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // If the group reference doesn't exist return
                        if (!dataSnapshot.exists()) return;

                        // If we aren't in a group, there is nothing update
                        String testKey = dataSnapshot.child(UserProfile.VALUE_IN_GROUP).getValue().toString();
                        if (testKey.equals(UserProfile.STATUS_NOT_IN_GROUP)) {
                            myGroupSubject.setAdapter(null);
                            myGroupBuilding.setAdapter(null);
                            return;
                        }

                        // Display general group information (building, description, etc...)
                        // We also set the groupKey if the snapshot is non-null
                        FirebaseDatabase.getInstance().getReference().child(GroupManager.PARENT_GROUPS).child(testKey)
                                .addListenerForSingleValueEvent(groupDetailsListener);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        // Update group details button listener
        myGroupUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(groupKey == null) return;

                // Update the group's details
                FirebaseDatabase.getInstance().getReference()
                        .child(GroupManager.PARENT_GROUPS).child(groupKey).addListenerForSingleValueEvent(groupUpdateListener);
            }
        });

        // Leave current group button listener
        myGroupLeaveButton.setOnClickListener(leaveButtonListener);

        // Group open/closed switch listener
        myGroupToggle.setOnCheckedChangeListener(groupToggleListener);

        addMemberImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(QRCodeDialogFragment.isQrLoaded()) {
                    QRCodeDialogFragment popup = new QRCodeDialogFragment();
                    popup.show(getFragmentManager(), "QRCodeDialogFragment");
                }
                else {
                    MyUtils.simpleSnackBar(myGroupCLayout, "Generating QR... Try again soon");
                }
            }
        });

        // Expands / Collapse group members profiles
        dropDownImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(groupKey == null) return;

                if(!memberListShowing) {
                    if(groupKey.isEmpty()) return;

                    if(!memberListGenerated) {
                        // linear layout where the member data will go
                        memberLL = new LinearLayout(getActivity());
                        memberLL.setId(View.generateViewId());
                        memberLL.setLayoutParams(
                                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        memberLL.setOrientation(LinearLayout.VERTICAL);

                        Drawable divider = ContextCompat.getDrawable(getContext(), R.drawable.mydivider);
                        memberLL.setDividerDrawable(divider);
                        memberLL.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
                        myGroupCLayout.addView(memberLL);

                        // load up the member profiles
                        loadMemberProfiles();
                        memberListGenerated = true;
                    }
                    else memberLL.setVisibility(View.VISIBLE);

                    // The list will go after the Members header and before the buttons, readjust the constraints to reflect this
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(myGroupCLayout);
                    constraintSet.connect(memberLL.getId(), ConstraintSet.TOP, R.id.upperLL, ConstraintSet.BOTTOM, LOWER_LL_PADDING_PX);
                    constraintSet.connect(R.id.lowerLL, ConstraintSet.TOP, memberLL.getId(), ConstraintSet.BOTTOM, LOWER_LL_PADDING_PX);
                    constraintSet.applyTo(myGroupCLayout);

                    memberListShowing = true;
                }
                else {
                    // There is no member list to display, move the buttons back to under the Members header
                    if(memberListGenerated) {
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(myGroupCLayout);
                        constraintSet.connect(R.id.lowerLL, ConstraintSet.TOP, R.id.upperLL, ConstraintSet.BOTTOM, LOWER_LL_PADDING_PX);
                        constraintSet.applyTo(myGroupCLayout);
                        memberLL.setVisibility(View.GONE);
                    }

                    memberListShowing = false;
                }
            }
        });

        return view;
    }

    // Display's the group member's profile data in a list stretching down the bottom of the screen.
    void loadMemberProfiles() {
        FirebaseDatabase.getInstance().getReference().child(GroupManager.PARENT_GROUPS).child(groupKey).child(GroupManager.PARENT_MEMBERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()) return;

                        for(DataSnapshot member : dataSnapshot.getChildren()) {
                            MemberProfileView memberProfile = new MemberProfileView(getContext());

                            memberProfile.setLayoutParams(
                                    new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, MEMBER_PROFILE_VIEW_HEIGHT_PX));
                            memberProfile.loadMemberData(member.getKey());
                            memberLL.addView(memberProfile);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // Updates values in the database when we change any of the groups values (name, what they are studying, etc...)
    // Fired after the update group button is pressed
    private ValueEventListener groupUpdateListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(!dataSnapshot.exists()) return;

            HashMap<String, String> groupData = GroupManager.getGroupProperties(dataSnapshot);
            GroupManager myGroup = new GroupManager(groupData.get(GroupManager.VALUE_GROUP_KEY));

            // The new data
            String newSubject = myGroupSubject.getText().toString();
            String newCourseCode = myGroupCourseCode.getText().toString();
            String newGroupName = myGroupName.getText().toString();
            String newGroupDescription = myGroupDescription.getText().toString();
            String newGroupBuilding = myGroupBuilding.getText().toString();

            // Check if our entries are valid
            if (!GroupManager.validGroupProperties(
                    getActivity(), newSubject, newCourseCode, newGroupName, newGroupBuilding, newGroupDescription)) {
                return;
            }

            // If the subject or course code changed we need to change the subject/course code our group is studying
            if (!newSubject.equals(groupData.get(GroupManager.VALUE_SUBJECT))
                    || !newCourseCode.equals(groupData.get(GroupManager.VALUE_COURSE_CODE))) {

                CourseManager.removeGroupFromCourse(
                        groupData.get(GroupManager.VALUE_GROUP_KEY),
                        groupData.get(GroupManager.VALUE_SUBJECT),
                        groupData.get(GroupManager.VALUE_COURSE_CODE));

                CourseManager.addGroupToCourse(
                        newSubject,
                        newCourseCode,
                        groupData.get(GroupManager.VALUE_GROUP_KEY)
                );
            }

            // Change the group's properties in the database
            myGroup.setGeneralGroupProperties(
                    newGroupName,
                    newSubject,
                    newCourseCode,
                    newGroupBuilding,
                    newGroupDescription
            );

            Toast.makeText(getActivity(), "Group Updated", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    // DataSnapshot is with reference to Root -> Groups -> groupKey. Displays
    // data about the group like name, description, building... (not any of the members)
    // Fired when we load the fragment to display the group's details
    private ValueEventListener groupDetailsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot groupDataSnapshot) {
            if(!groupDataSnapshot.exists()) {
                myGroupBuilding.setAdapter(null);
                myGroupSubject.setAdapter(null);
                return;
            }

            // The group exists
            groupKey = groupDataSnapshot.child(GroupManager.VALUE_GROUP_KEY).getValue().toString();

            // Generate the group QR in another thread
            LoadGroupQRImageTask loadGroupQRImageTask = new LoadGroupQRImageTask(groupKey);
            loadGroupQRImageTask.execute();

            // Display the group's details
            HashMap <String, String> groupData = GroupManager.getGroupProperties(groupDataSnapshot);
            myGroupName.setText(groupData.get(GroupManager.VALUE_NAME));
            myGroupDescription.setText(groupData.get(GroupManager.VALUE_DESCRIPTION));
            myGroupBuilding.setText(groupData.get(GroupManager.VALUE_BUILDING));
            myGroupSubject.setText(groupData.get(GroupManager.VALUE_SUBJECT));
            myGroupCourseCode.setText(groupData.get(GroupManager.VALUE_COURSE_CODE));

            // If we are the leader we are able to modify the group's details
            if(groupData.get(GroupManager.VALUE_LEADER).equals(UserProfile.getKey())) {
                myGroupName.setFocusableInTouchMode(true);
                myGroupDescription.setFocusableInTouchMode(true);
                myGroupBuilding.setFocusableInTouchMode(true);
                myGroupSubject.setFocusableInTouchMode(true);
                myGroupCourseCode.setFocusableInTouchMode(true);
                myGroupToggle.setEnabled(true);
                myGroupUpdateButton.setEnabled(true);
            }

            // We can only leave the group if we have joined the group
            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                    .child(UserProfile.VALUE_IN_GROUP).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()) return;

                    String inGroup = dataSnapshot.getValue().toString();
                    if(!inGroup.equals(UserProfile.STATUS_NOT_IN_GROUP)) {
                        myGroupLeaveButton.setEnabled(true);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            if(groupData.get(GroupManager.VALUE_OPEN).equals(GroupManager.STATUS_GROUP_OPEN)) myGroupToggle.setChecked(true);
            else myGroupToggle.setChecked(false);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private View.OnClickListener leaveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(groupKey == null) return;
            GroupManager.leftGroup(groupKey);
            clearFragment();
        }
    };

    private CompoundButton.OnCheckedChangeListener groupToggleListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
            if(groupKey == null) return;

            DatabaseReference groupOpenDBRef = FirebaseDatabase.getInstance().getReference()
                    .child(GroupManager.PARENT_GROUPS).child(groupKey).child(GroupManager.VALUE_OPEN);

            // set the state of the group open/closed based on the state of the switch
            if(isChecked) groupOpenDBRef.setValue(GroupManager.STATUS_GROUP_OPEN);
            else groupOpenDBRef.setValue(GroupManager.STATUS_GROUP_CLOSED);
        }

    };

    private void setAutoTextListeners() {
        myGroupSubject.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                myGroupSubject.showDropDown();
                return false;
            }
        });
        myGroupBuilding.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                myGroupBuilding.showDropDown();
                return false;
            }
        });
    }

    private void clearFragment() {
        myGroupCLayout.requestFocus();

        myGroupName.setText("");
        myGroupSubject.setText("");
        myGroupCourseCode.setText("");
        myGroupBuilding.setText("");
        myGroupDescription.setText("");

        myGroupName.setFocusableInTouchMode(false);
        myGroupSubject.setFocusableInTouchMode(false);
        myGroupCourseCode.setFocusableInTouchMode(false);
        myGroupBuilding.setFocusableInTouchMode(false);
        myGroupDescription.setFocusableInTouchMode(false);

        myGroupSubject.setAdapter(null);
        myGroupBuilding.setAdapter(null);

        myGroupToggle.setChecked(false);
        myGroupToggle.setClickable(false);
        myGroupUpdateButton.setEnabled(false);
        myGroupLeaveButton.setEnabled(false);

        if(memberLL != null) memberLL.setVisibility(View.GONE);
        dropDownImage.setOnClickListener(null);
    }

    private class LoadGroupQRImageTask extends AsyncTask<Void, Void, Bitmap> {
        private final String groupKey;

        LoadGroupQRImageTask(String groupKey) {
            this.groupKey = groupKey;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            QRCodeHelper helper = QRCodeHelper.newInstance();
            helper.setContent(groupKey);

            // L, M, Q, H
            helper.setErrorCorrectionLevel(ErrorCorrectionLevel.H);

            return helper.generate();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            QRCodeDialogFragment.setGroupQRCodeImage(bitmap);
        }
    }

    public static class QRCodeDialogFragment extends DialogFragment {
        private static final float BACKGROUND_DIM_AMOUNT = 0.6f;
        public static Bitmap mGroupQRBitmap;
        private ImageView groupQRCodeImage;
        private static boolean qrLoaded = false;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Dialog dialog = new Dialog(getActivity());

            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.dialog_fragment_show_qr, null);

            groupQRCodeImage = view.findViewById(R.id.qrImage);
            groupQRCodeImage.setImageBitmap(mGroupQRBitmap);

            dialog.setContentView(view);

            setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog);
            dialog.show();

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();

            // dim everything but the dialog fragment
            WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
            lp.dimAmount = BACKGROUND_DIM_AMOUNT;
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        public static void setGroupQRCodeImage(Bitmap groupQRBitmap){
            mGroupQRBitmap = groupQRBitmap;
            qrLoaded = true;
        }

        public static boolean isQrLoaded() {
            return qrLoaded;
        }
    }

}
