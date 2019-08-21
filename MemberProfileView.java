package com.example.st;


import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MemberProfileView extends ConstraintLayout implements ImageStore.ImageOperationListener {
    private TextView firstNameText;
    private TextView majorText;
    private ImageView profileImage;
    //private ConstraintLayout constraintLayout;
    private LaunchMemberProfileCallback mCallback;
    private Bitmap profileBitmap;

    public MemberProfileView(Context context) {
        super(context);
        init(context);
    }

    public MemberProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MemberProfileView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void loadMemberData(final String memberKey) {
        FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(memberKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String name = dataSnapshot.child(UserProfile.VALUE_FIRST_NAME).getValue().toString();
                        final String major = "- " + dataSnapshot.child(UserProfile.VALUE_MAJOR).getValue().toString();

                        firstNameText.setText(name);
                        majorText.setText(major);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        ImageStore memberImageStore = new ImageStore(memberKey, this);
        memberImageStore.fetchProfileImage(getContext());

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.launchMemberProfileFrag(memberKey, profileBitmap, true);
            }
        });
    }

    private void init(Context context) {
        inflate(getContext(), R.layout.view_member_profile, this);

        try{
            mCallback = (LaunchMemberProfileCallback) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement LaunchMemberProfileCallback");
        }

        // TextView
        firstNameText = findViewById(R.id.memberProfileFirstName);
        majorText = findViewById(R.id.memberProfileMajor);

        // ImageView
        profileImage = findViewById(R.id.memberProfileImage);
    }

    @Override
    public void imageFetched(boolean success, @Nullable Bitmap imageBitmap) {
        if(success) {
            profileBitmap = imageBitmap;
            profileImage.setImageBitmap(profileBitmap);
        }
    }

    @Override
    public void imageUploaded(boolean success) {

    }

    interface LaunchMemberProfileCallback {
        void launchMemberProfileFrag(String memberKey, @Nullable Bitmap profileBitmap, boolean groupTicked);
    }
}
