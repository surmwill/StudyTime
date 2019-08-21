package com.example.st;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.util.NumberUtils;
import com.google.android.gms.flags.impl.DataUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.lang.reflect.Array;
import java.security.acl.Group;
import java.util.Iterator;

import static android.app.Activity.RESULT_OK;

public class CreateGroupFragment extends Fragment {
    private static final String TITLE = "New Group";

    private AutoCompleteTextView createGroupSubject;
    private AutoCompleteTextView createGroupBuilding;
    private EditText createGroupName;
    private EditText createGroupDescription;
    private EditText createGroupCourseCode;
    private Button createGroupButton;
    private Button scanQRCodeButton;
    private LinearLayout linearLayout;
    private Boolean inGroup;

    public CreateGroupFragment() {
        // Required empty public constructor
    }

    public static CreateGroupFragment newInstance() {
        CreateGroupFragment fragment = new CreateGroupFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(TITLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_group, container, false);

        // EditText
        createGroupName = view.findViewById(R.id.createGroupName);
        createGroupDescription = view.findViewById(R.id.createGroupDescription);
        createGroupCourseCode = view.findViewById(R.id.createGroupCourseCode);

        // AutoCompleteText
        createGroupSubject = view.findViewById(R.id.createGroupSubject);
        createGroupBuilding = view.findViewById(R.id.createGroupBuilding);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                GroupManager.getValidSubjects());
        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                GroupManager.getValidBuildings());

        createGroupSubject.setAdapter(subjectAdapter);
        createGroupBuilding.setAdapter(buildingAdapter);

        MyUtils.addUpperCaseFilter(createGroupSubject);
        MyUtils.addUpperCaseFilter(createGroupBuilding);

        setAutoTextListeners();

        // Buttons
        createGroupButton = view.findViewById(R.id.createGroupButton);
        scanQRCodeButton = view.findViewById(R.id.scanQRCodeButton);

        // LinearLayout
        linearLayout = view.findViewById(R.id.createGroupLLayout);

        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey()).child(UserProfile.VALUE_IN_GROUP)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()) return;

                        if(dataSnapshot.getValue().toString().equals(UserProfile.STATUS_NOT_IN_GROUP)) inGroup = Boolean.FALSE;
                        else inGroup = Boolean.TRUE;
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        createGroupButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(inGroup == null) {
                    MyUtils.simpleSnackBar(linearLayout, "Fetching data... Try again soon");
                    return;
                }
                if(inGroup) {
                    MyUtils.simpleSnackBar(linearLayout, "Cannot join a new group while you are still apart of an old one");
                    return;
                }

                final String groupSubject = createGroupSubject.getText().toString().toUpperCase();
                final String courseCode = createGroupCourseCode.getText().toString();
                final String groupName = createGroupName.getText().toString();
                final String groupBuilding = createGroupBuilding.getText().toString();
                final String groupDescription = createGroupDescription.getText().toString();

                // First check that the input is valid
                if(!GroupManager.validGroupProperties(getActivity(), groupSubject, courseCode, groupName, groupBuilding, groupDescription)) {
                    return;
                }

                // Create the new group in the database
                GroupManager.createGroup(groupName, groupSubject, courseCode, groupBuilding, groupDescription);
                inGroup = Boolean.TRUE;
                clearText();
            }
        });

        scanQRCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inGroup == null) {
                    MyUtils.simpleSnackBar(linearLayout, "Fetching data... Try again soon");
                    return;
                }
                if(inGroup) {
                    MyUtils.simpleSnackBar(linearLayout, "Cannot start a new group while you are still apart of an old one");
                    return;
                }

                IntentIntegrator.forSupportFragment(CreateGroupFragment.this).initiateScan(IntentIntegrator.QR_CODE_TYPES);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) MyUtils.simpleSnackBar(linearLayout, "Scan Cancelled");
            else {
                final String groupKey = result.getContents();
                GroupManager.joinGroup(groupKey);
                inGroup = Boolean.TRUE;
            }
        }
    }

    private void clearText() {
        createGroupSubject.setText("");
        createGroupCourseCode.setText("");
        createGroupName.setText("");
        createGroupBuilding.setText("");
        createGroupDescription.setText("");
    }

    private void setAutoTextListeners() {
        createGroupSubject.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                createGroupSubject.showDropDown();
                return false;
            }
        });
        createGroupBuilding.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                createGroupBuilding.showDropDown();
                return false;
            }
        });
    }
}
