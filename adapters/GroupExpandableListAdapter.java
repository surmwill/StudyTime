package com.example.st.adapters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.constraint.ConstraintLayout;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.st.MyUtils;
import com.example.st.dialog_fragments.GroupJoinDialogFragment;
import com.example.st.GroupManager;
import com.example.st.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

public class GroupExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private GroupBookmarkListener mCallback;
    private List<String> courseNumsHeader;
    private HashMap<String, List<String>> groupsUnderNum;
    private HashSet<String> bookmarkedGroups;

    // When we want to use a convertView but have already fetched that groups data, we can
    // save a database call by storing the fetched data in GroupInfo
    private HashMap<String, GroupInfo> groupKeyToInfo;

    public GroupExpandableListAdapter(Context context, List<String> courseNumsHeader, HashMap<String, List<String>>  groupsUnderNum,
                                      HashSet<String> bookmarkedGroups) {
        this.context = context;
        this.courseNumsHeader = courseNumsHeader;
        this.groupsUnderNum = groupsUnderNum;
        this.groupKeyToInfo = new HashMap<>();
        this.bookmarkedGroups = bookmarkedGroups;

        try{
            mCallback = (GroupBookmarkListener) this.context;
        }
        catch(ClassCastException e) {
            throw new ClassCastException(this.context.toString() + " must implement GroupWantedListener");
        }
    }

    @Override
    public String getChild(int groupPosition, int childPosition) {
        return groupsUnderNum.get(courseNumsHeader.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final String groupKey = getChild(groupPosition, childPosition);
        boolean wasNull = false;

        // we don't have a view we can reuse, create a new one
        if (convertView == null) {
            wasNull = true;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_group_child, parent, false);
        }

        // bind views
        final TextView groupNameText = convertView.findViewById(R.id.groupChildName);
        final TextView groupStudyingText = convertView.findViewById(R.id.groupChildStudying);
        final TextView groupBuildingText = convertView.findViewById(R.id.groupChildBuilding);
        final TextView groupSizeText = convertView.findViewById(R.id.groupChildNumMembers);
        final TextView groupDescription = convertView.findViewById(R.id.groupChildDescription);
        final ConstraintLayout cLayout = convertView.findViewById(R.id.groupChildCLayout);
        final CheckBox groupBookmark = convertView.findViewById(R.id.groupChildBookmarkCheckbox);

        final View.OnClickListener tapToShowCheckmarkListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(groupBookmark.getVisibility() == View.INVISIBLE) groupBookmark.setVisibility(View.VISIBLE);
                else groupBookmark.setVisibility(View.INVISIBLE);
            }
        };

        if(!wasNull) {
            // clear the recycled info while we are fetching the new info
            groupNameText.setText("");
            groupStudyingText.setText("");
            groupBuildingText.setText("");
            groupSizeText.setText("");
            groupDescription.setText("");

            // ensure we don't bind multiple listeners to the same views
            groupBookmark.setOnCheckedChangeListener(null);
            cLayout.setOnClickListener(null);
        }

        if(bookmarkedGroups.contains(groupKey)) {
            groupBookmark.setVisibility(View.VISIBLE);
            groupBookmark.setChecked(true);
        }
        else {
            groupBookmark.setVisibility(View.INVISIBLE);
            groupBookmark.setChecked(false);
            cLayout.setOnClickListener(tapToShowCheckmarkListener);
        }

        groupBookmark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    bookmarkedGroups.add(groupKey);
                    mCallback.changeBookmarkedStatus(groupKey, true);
                    cLayout.setOnClickListener(null); // cannot dismiss checked CheckMark
                }
                else {
                    bookmarkedGroups.remove(groupKey);
                    mCallback.changeBookmarkedStatus(groupKey, false);
                    cLayout.setOnClickListener(tapToShowCheckmarkListener); // can dismiss unchecked CheckMark
                }
            }
        });

        if(groupKeyToInfo.containsKey(groupKey)) {
            final GroupInfo groupInfo = groupKeyToInfo.get(groupKey);
            final String groupSize = Long.toString(groupInfo.getSize());

            groupNameText.setText(groupInfo.getName());
            groupStudyingText.setText(groupInfo.getStudying());
            groupBuildingText.setText(groupInfo.getBuilding());
            groupDescription.setText(groupInfo.getDescription());
            groupSizeText.setText(groupSize);
        }
        else {
            FirebaseDatabase.getInstance().getReference().child(GroupManager.PARENT_GROUPS).child(groupKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final String name = dataSnapshot.child(GroupManager.VALUE_NAME).getValue().toString();
                            final String studying = dataSnapshot.child(GroupManager.VALUE_SUBJECT).getValue().toString() +
                                    dataSnapshot.child(GroupManager.VALUE_COURSE_CODE).getValue().toString();
                            final String building = dataSnapshot.child(GroupManager.VALUE_BUILDING).getValue().toString();
                            final String description = dataSnapshot.child(GroupManager.VALUE_BUILDING).getValue().toString();
                            final String status = dataSnapshot.child(GroupManager.VALUE_OPEN).getValue().toString();
                            final String leaderKey = dataSnapshot.child(GroupManager.VALUE_LEADER).getValue().toString();
                            final long size = dataSnapshot.child(GroupManager.VALUE_GROUP_SIZE).getValue(Long.class);
                            final String sizeString = Long.toString(size);

                            groupKeyToInfo.put(groupKey,
                                    new GroupInfo(groupKey, name, studying, building, description, status, leaderKey, size, -1));

                            groupNameText.setText(name);
                            groupStudyingText.setText(studying);
                            groupBuildingText.setText(building);
                            groupDescription.setText(description);
                            groupSizeText.setText(sizeString);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }


        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return groupsUnderNum.get(courseNumsHeader.get(groupPosition)).size();
    }

    @Override
    public String getGroup(int groupPosition) {
        return courseNumsHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return courseNumsHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final String courseNum = getGroup(groupPosition);

        // If the old view doesn't exist or is non-usable create a new view for the group
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_group_parent, null);
        }

        final TextView courseNumText =  convertView.findViewById(R.id.groupParentCourseNum);
        courseNumText.setTypeface(null, Typeface.BOLD);
        courseNumText.setText(courseNum);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        // Since list id dynamically changing
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface GroupBookmarkListener {
        void changeBookmarkedStatus(String groupKey, boolean bookmarked);
    }
}
