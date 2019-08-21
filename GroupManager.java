package com.example.st;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.util.NumberUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class GroupManager {
    public static final String VALUE_NAME = "name";
    public static final String VALUE_DESCRIPTION = "description";
    public static final String VALUE_BUILDING = "building";
    public static final String VALUE_OPEN = "open";
    public static final String VALUE_SUBJECT = "subject";
    public static final String VALUE_COURSE_CODE = "courseCode";
    public static final String VALUE_LEADER = "leader";
    public static final String VALUE_GROUP_KEY = "key";
    public static final String VALUE_GROUP_SIZE = "groupSize";

    public static final String PARENT_GROUPS = "Groups";
    public static final String PARENT_MEMBERS = "Members";

    public static final String STATUS_GROUP_OPEN = "true";
    public static final String STATUS_GROUP_CLOSED = "false";

    public static final int MAX_DESCRIPTION_LENGTH = 180;

    private static String[] subjectArray;
    private static String[] buildingArray;

    private static Set<String> subjectSet;
    private static Set<String> buildingSet;

    private String groupKey;
    private DatabaseReference groupDBReference;
    private static OnGroupStatusChangedListener groupStatusChangedListener;

    public static void setValidGroupProperties(String [] subjectArray, String [] buildingArray) {
        GroupManager.subjectArray = subjectArray;
        GroupManager.buildingArray = buildingArray;

        // For easy searching
        subjectSet = new HashSet<>(Arrays.asList(subjectArray));
        buildingSet = new HashSet<>(Arrays.asList(buildingArray));
    }

    public static String[] getValidSubjects() {
        return subjectArray;
    }

    public static String[] getValidBuildings() {
        return buildingArray;
    }

    public static void setGroupStatusChangedListener(OnGroupStatusChangedListener listener) {
        groupStatusChangedListener = listener;
    }

    public static void createGroup(String name, String subject, String courseCode, String building, String description) {
        DatabaseReference newGroupDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_GROUPS).push();
        String newGroupKey = newGroupDBRef.getKey();

        newGroupDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).setValue(ServerValue.TIMESTAMP);

        newGroupDBRef.child(VALUE_NAME).setValue(name);
        newGroupDBRef.child(VALUE_SUBJECT).setValue(subject);
        newGroupDBRef.child(VALUE_COURSE_CODE).setValue(courseCode);
        newGroupDBRef.child(VALUE_BUILDING).setValue(building);
        newGroupDBRef.child(VALUE_DESCRIPTION).setValue(description);
        newGroupDBRef.child(VALUE_LEADER).setValue(UserProfile.getKey());
        newGroupDBRef.child(VALUE_OPEN).setValue(STATUS_GROUP_OPEN);
        newGroupDBRef.child(VALUE_GROUP_KEY).setValue(newGroupKey);
        newGroupDBRef.child(VALUE_GROUP_SIZE).setValue(1);

        groupStatusChangedListener.startedGroup(newGroupKey);
        CourseManager.addGroupToCourse(subject, courseCode, newGroupKey);
    }

    // Call for an existing group with an established groupKey
    GroupManager(String groupKey) {
        this.groupKey = groupKey;
        groupDBReference = FirebaseDatabase.getInstance().getReference().child(PARENT_GROUPS).child(groupKey);
    }

    // Various checks to see if the group we are starting has a name (non-empty), has a valid subject name (not a swear) etc...
    public static boolean validGroupProperties(
            Context context, String newSubject, String newCourseCode, String newGroupName, String newBuilding, String newDescription) {

        // Course code
        if(newCourseCode.isEmpty()) {
            Toast.makeText(context, "Error: The course code cannot be empty", Toast.LENGTH_LONG).show();
            return false;
        }
        if(!NumberUtils.isNumeric(newCourseCode)) {
            Toast.makeText(context, "Error: The course code must be numeric", Toast.LENGTH_LONG).show();
            return false;
        }

        // Subject
        if(!subjectSet.contains(newSubject)) {
            Toast.makeText(context, "Error: subject doesn't exist", Toast.LENGTH_LONG).show();
            return false;
        }

        // Name
        if(newGroupName.isEmpty()) {
            Toast.makeText(context, "Error: group name cannot be empty", Toast.LENGTH_LONG).show();
            return false;
        }

        // Building
        if(!buildingSet.contains(newBuilding)) {
            Toast.makeText(context, "Error: building doesn't exist", Toast.LENGTH_LONG).show();
            return false;
        }

        if(newDescription.length() > MAX_DESCRIPTION_LENGTH) {
            Toast.makeText(context, "Error: description is too long", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public void setGeneralGroupProperties(String name, String subject, String courseCode, String building, String description) {
        groupDBReference.child(VALUE_NAME).setValue(name);
        groupDBReference.child(VALUE_DESCRIPTION).setValue(description);
        groupDBReference.child(VALUE_BUILDING).setValue(building);
        groupDBReference.child(VALUE_OPEN).setValue(STATUS_GROUP_OPEN);
        groupDBReference.child(VALUE_SUBJECT).setValue(subject);
        groupDBReference.child(VALUE_COURSE_CODE).setValue(courseCode);
    }

    // Assumes DataSnapshot is at Groups -> groupKey
    public static HashMap<String, String> getGroupProperties(DataSnapshot groupDataSnapshot) {
        HashMap <String, String> groupData = new HashMap<>();

        groupData.put(VALUE_NAME, groupDataSnapshot.child(VALUE_NAME).getValue().toString());
        groupData.put(VALUE_DESCRIPTION, groupDataSnapshot.child(VALUE_DESCRIPTION).getValue().toString());
        groupData.put(VALUE_BUILDING, groupDataSnapshot.child(VALUE_BUILDING).getValue().toString());
        groupData.put(VALUE_OPEN, groupDataSnapshot.child(VALUE_OPEN).getValue().toString());
        groupData.put(VALUE_SUBJECT, groupDataSnapshot.child(VALUE_SUBJECT).getValue().toString());
        groupData.put(VALUE_COURSE_CODE, groupDataSnapshot.child(VALUE_COURSE_CODE).getValue().toString());
        groupData.put(VALUE_LEADER, groupDataSnapshot.child(VALUE_LEADER).getValue().toString());
        groupData.put(VALUE_GROUP_KEY, groupDataSnapshot.child(VALUE_GROUP_KEY).getValue().toString());
        groupData.put(VALUE_GROUP_SIZE, groupDataSnapshot.child(VALUE_GROUP_SIZE).getValue().toString());

        return groupData;
    }

    // Remove the user from the Members list. Delete the group if we are the only member,
    // if not, choose a new leader based on time joined
    public static void leftGroup(final String groupKey) {
        final DatabaseReference groupDBReference = FirebaseDatabase.getInstance().getReference().child(PARENT_GROUPS).child(groupKey);
        groupDBReference.child(PARENT_MEMBERS).child(UserProfile.getKey()).removeValue();

        MyUtils.shitDebug(groupKey);

        groupDBReference.child(VALUE_GROUP_SIZE).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null) {
                    final int remainingMembers = mutableData.getValue(Integer.class) - 1;
                    mutableData.setValue(remainingMembers);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(final DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                groupStatusChangedListener.leftGroup();
                if(!b) return;

                final int remainingMembers = dataSnapshot.getValue(Integer.class);
                FirebaseDatabase.getInstance().getReference().child(PARENT_GROUPS).child(groupKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) return;

                                // We are the last member, remove the group from the DB
                                if(remainingMembers <= 0) {
                                    final String subject = dataSnapshot.child(VALUE_SUBJECT).getValue().toString();
                                    final String courseNum = dataSnapshot.child(VALUE_COURSE_CODE).getValue().toString();

                                    CourseManager.removeGroupFromCourse(subject, courseNum, groupKey);
                                    groupDBReference.removeValue();
                                }
                                else {
                                    // At least one other member remains, check to see if we need to assign a new leader
                                    final String leaderKey = dataSnapshot.child(VALUE_LEADER).getValue().toString();

                                    // Give leadership to the person first person to join after us
                                    if (leaderKey.equals(UserProfile.getKey())) {
                                        long latestJoin = -1;
                                        String newLeaderKey = "";

                                        for (DataSnapshot memberData : dataSnapshot.child(PARENT_MEMBERS).getChildren()) {
                                            final long memberJoin = memberData.getValue(Long.class);
                                            if (latestJoin == -1 || memberJoin < latestJoin) {
                                                latestJoin = memberJoin;
                                                newLeaderKey = memberData.getKey();
                                            }
                                        }

                                        // ensure we actually found a new leader before writing
                                        if (!newLeaderKey.isEmpty())
                                            groupDBReference.child(VALUE_LEADER).setValue(newLeaderKey);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    public static void joinGroup(final String groupKey) {
        final DatabaseReference groupDBRef = FirebaseDatabase.getInstance().getReference().child(PARENT_GROUPS).child(groupKey);

        groupDBRef.child(VALUE_GROUP_SIZE).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null) {
                    final int size = mutableData.getValue(Integer.class) + 1;
                    mutableData.setValue(size);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                // Ensure the group is in a valid state before joining
                if(!b || dataSnapshot.getValue(Integer.class) <= 1) {
                    groupDBRef.removeValue();
                    groupStatusChangedListener.joinedGroup(groupKey, false);
                    return;
                }

                groupDBRef.child(PARENT_MEMBERS).child(UserProfile.getKey()).setValue(ServerValue.TIMESTAMP);
                groupStatusChangedListener.joinedGroup(groupKey, true);
            }
        });
    }

    public void addGroupMember(String memberKey) {
        groupDBReference.child(PARENT_MEMBERS).child(memberKey).setValue(memberKey);
    }

    public String getGroupKey() {
        return groupKey;
    }

    interface OnGroupStatusChangedListener {
        void joinedGroup(String groupKey, boolean success);
        void startedGroup(String groupKey);
        void leftGroup();
    }

}
