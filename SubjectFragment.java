package com.example.st;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.st.adapters.GroupExpandableListAdapter;
import com.example.st.adapters.GroupInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class SubjectFragment extends Fragment {
    private static final String ARG_SUBJECT_NAME = "arg_subject_name";

    private String subjectName;
    private String title = null;

    private EditText subjectCourseCode;
    private EditText subjectGroupSize;
    private EditText subjectGroupName;
    private AutoCompleteTextView subjectBuilding;
    private ProgressBar progressBar;
    private ImageView searchIcon;

    // ListView
    GroupExpandableListAdapter expListAdapter;
    ExpandableListView expListView;

    // Dynamic database data
    private final List<String> courseNumHeaders = new ArrayList<>();
    private final HashMap<String, List<String>> groupsUnderNum = new HashMap<>();


    public SubjectFragment() {
        // Required empty public constructor
    }

    public static SubjectFragment newInstance(String subjectName) {
        SubjectFragment fragment = new SubjectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_NAME, subjectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) subjectName = getArguments().getString(ARG_SUBJECT_NAME);
        title = subjectName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subject, container, false);

        courseNumHeaders.clear();
        groupsUnderNum.clear();

        // ExpandableListView
        expListView = view.findViewById(R.id.lvExp);

        // EditText
        subjectCourseCode = view.findViewById(R.id.subjectCourseCode);
        subjectGroupName = view.findViewById(R.id.subjectGroupName);
        subjectBuilding = view.findViewById(R.id.subjectBuilding);
        subjectGroupSize = view.findViewById(R.id.subjectGroupSize);

        // ProgressBar
        progressBar = view.findViewById(R.id.subjectProgressBar);

        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                GroupManager.getValidBuildings());
        subjectBuilding.setAdapter(buildingAdapter);

        MyUtils.addUpperCaseFilter(subjectBuilding);
        setSubjectBuildingOnTouchListener();

        final Query groupsQuery = FirebaseDatabase.getInstance().getReference().child(CourseManager.PARENT_COURSES).child(subjectName);
        groupsQuery.orderByValue().addListenerForSingleValueEvent(fetchGroupsListener);

        return view;
    }

    private ValueEventListener fetchGroupsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            // Iterate through all the course numbers
            for(DataSnapshot courseNumData : dataSnapshot.getChildren()) {
                final List <String> groups = new ArrayList<>();
                final String courseNum = courseNumData.getKey();
                courseNumHeaders.add(courseNum);

                // Iterate through all the groups under each course num
                for(DataSnapshot groupData : courseNumData.getChildren()) {
                    final String groupKey = groupData.getKey();
                    groups.add(groupKey);
                }
                groupsUnderNum.put(courseNum, groups);
            }

            // Figure out which groups we have bookmarked
            final HashSet<String> bookmarkedGroups = new HashSet<>();
            FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(UserProfile.getKey())
                    .child(UserProfile.PARENT_BOOKMARKED_GROUPS).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot bookmarkedData : dataSnapshot.getChildren()) {
                        bookmarkedGroups.add(bookmarkedData.getKey());
                    }

                    expListAdapter = new GroupExpandableListAdapter(getContext(), courseNumHeaders, groupsUnderNum, bookmarkedGroups);
                    progressBar.setVisibility(View.GONE);
                    expListView.setAdapter(expListAdapter);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void setSubjectBuildingOnTouchListener() {
        subjectBuilding.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                subjectBuilding.showDropDown();
                return false;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        if(title != null) getActivity().setTitle(title);
    }

}
