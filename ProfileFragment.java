package com.example.st;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment implements ImageStore.ImageOperationListener {
    private static final String ARG_USER_KEY = "USER_KEY";
    private static final String TITLE = "Profile";
    private static final int PICK_IMAGE_REQUEST = 71;

    private boolean imageFetched = false;
    private boolean infoFetched = false;
    private boolean profileImageChanged = false;

    private final String userKey = UserProfile.getKey();

    private String firstName;
    private String major;
    private String courseLoad;
    private String addInfo;

    // Database
    private DatabaseReference profileDBReference;

    // ImageView
    private ImageView profileImage;
    private Uri filePath;

    // EditText
    private EditText profileFirstName;
    private EditText profileMajor;
    private EditText profileCourseLoad;
    private EditText profileAddInfo;

    // ProgressBar
    private ProgressBar profileUpdatePBar;

    // Buttons
    private Button updateProfileButton;

    // Relative Layout
    private RelativeLayout profileRelLayout;

    private OnProfileUpdatedListener mCallback;

    private ImageStore userImageStore;

    public ProfileFragment() {
        // Required empty public constructor
    }

    // Pass in the user key as an argument to establish a
    // future database reference
    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnProfileUpdatedListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement OnProfileUpdateListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    // Create a UserProfile object based on the user's key so
    // we can make changes to their profile in the database
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        profileDBReference = FirebaseDatabase.getInstance().getReference().child(UserProfile.PARENT_USERS).child(userKey);
        userImageStore = new ImageStore(userKey, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(TITLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // EditText
        profileFirstName = view.findViewById(R.id.profileFirstName);
        profileMajor = view.findViewById(R.id.profileMajor);
        profileCourseLoad = view.findViewById(R.id.profileCourseLoad);
        profileAddInfo = view.findViewById(R.id.profileAddInfo);

        // Buttons
        updateProfileButton = view.findViewById(R.id.updateProfileButton);

        // Scrollview
        profileRelLayout = view.findViewById(R.id.profileRelativeLayout);

        // ImageView
        profileImage = view.findViewById(R.id.profileImage);

        // ProgressBar
        profileUpdatePBar = view.findViewById(R.id.profileUpdatePBar);

        // Fetch the profile image, we set the bitmap in the image fetched listener
        // userImageStore.fetchProfileImage(profileImage);
        userImageStore.fetchProfileImage(getContext());

        updateProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileUpdatePBar.setVisibility(View.VISIBLE);

                // wait until we have retrieved their profile from the DB
                if(!infoFetched) {
                    Snackbar.make(profileRelLayout, "Loading your profile, please wait", Snackbar.LENGTH_LONG).setActionTextColor(Color.YELLOW).show();
                    return;
                }

                String newName = profileFirstName.getText().toString();
                String newMajor = profileMajor.getText().toString().toUpperCase();
                String newCourseLoad = profileCourseLoad.getText().toString();
                String newAddInfo = profileAddInfo.getText().toString();

                // null means we won't update whats in the DB because no change has occurred
                if(newName.equals(firstName))
                    newName = null;
                if(newMajor.equals(major))
                    newMajor = null;
                if(newCourseLoad.equals(courseLoad))
                    newCourseLoad = null;
                if(newAddInfo.equals(addInfo))
                    newAddInfo = null;

                mCallback.updateProfile(newName, newMajor, newCourseLoad, newAddInfo);

                // Only change their profile image in the DB if it's a different picture
                if(profileImageChanged) userImageStore.uploadImage(filePath);
                else {
                    Snackbar.make(profileRelLayout, "Profile Updated", Snackbar.LENGTH_LONG)
                            .setActionTextColor(Color.YELLOW).show();
                    profileUpdatePBar.setVisibility(View.INVISIBLE);
                }
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseProfileImage();
            }
        });

        // Update the text fields based on the user's information in the DB
        profileDBReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) return;

                HashMap <String, String> userData = UserProfile.getUserProperties(dataSnapshot);

                firstName = userData.get(UserProfile.VALUE_FIRST_NAME);
                major = userData.get(UserProfile.VALUE_MAJOR);
                courseLoad = userData.get(UserProfile.VALUE_COURSE_LOAD);
                addInfo = userData.get(UserProfile.VALUE_OTHER);

                // Whatever finishes last (loading the profile pic or their text data) will trigger the loading symbol to go away
                infoFetched = true;
                if(imageFetched) profileUpdatePBar.setVisibility(View.INVISIBLE);

                if(!firstName.isEmpty()) profileFirstName.setText(firstName);
                if(!major.isEmpty()) profileMajor.setText(major);
                if(!courseLoad.isEmpty()) profileCourseLoad.setText(courseLoad);
                if(!addInfo.isEmpty()) profileAddInfo.setText(addInfo);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        profileRelLayout.requestFocus();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    public interface OnProfileUpdatedListener {
        void updateProfile(@Nullable String firstName, @Nullable String major, @Nullable String courseLoad, @Nullable String addInfo);
    }

    // Let the user browse their albums/downloads for a profile picture, when they have
    // selected a picture, the OnActivityResult callback is fired
    private void chooseProfileImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Callback for when we return from startActivityForResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the callback is for returning from the intent of choosing a profile picture, retrieve the image data
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri testFilePath = data.getData();

            if(testFilePath.equals(filePath)) return;

            filePath = testFilePath;
            profileImageChanged = true;

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), filePath);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void imageFetched(boolean success, @Nullable Bitmap imageBitmap) {
        if(success) {
            imageFetched = true;
            profileImage.setImageBitmap(imageBitmap);
        }
        else
            Snackbar.make(profileRelLayout, "ERROR: Could not fetch profile image", Snackbar.LENGTH_LONG)
                .setActionTextColor(Color.YELLOW).show();

        // Whatever finishes last (loading the profile pic or their text data) will trigger the loading symbol to go away
        if(infoFetched) profileUpdatePBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void imageUploaded(boolean success) {
        if(success) {
            profileImageChanged = false;
            Snackbar.make(profileRelLayout, "SUCCESS: profile updated", Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.YELLOW).show();
        }
        else
            Snackbar.make(profileRelLayout, "ERROR: Could not upload profile image", Snackbar.LENGTH_LONG)
            .setActionTextColor(Color.YELLOW).show();

        profileUpdatePBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        userImageStore.removeOnImageOpCompleteListener();
    }
}
