package com.example.st;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CourseManager {
    public static final String PARENT_COURSES = "Courses";
    public static final String PARENT_GROUPS = "Groups";

    public static void addGroupToCourse(String subject, String courseCode, String groupKey) {
        FirebaseDatabase.getInstance().getReference()
                .child(PARENT_COURSES).child(subject).child(courseCode).child(groupKey).setValue(ServerValue.TIMESTAMP);
    }

    public static void removeGroupFromCourse(String subject, String courseCode, String groupKey) {
        FirebaseDatabase.getInstance().getReference()
                .child(PARENT_COURSES).child(subject).child(courseCode).child(groupKey).removeValue();

    }
}
