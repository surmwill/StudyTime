package com.example.st;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

public class LoginActivity extends AppCompatActivity {
    private Button createUser, loginUser;
    private EditText userEmail, userPassword;
    private boolean skipAutoAuthentication = false;

    private static final String TITLE = "Log In";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle(TITLE);

        // Buttons
        createUser = findViewById(R.id.createUserButton);
        loginUser = findViewById(R.id.loginUserButton);

        // EditText
        userEmail = findViewById(R.id.userEmail);
        userPassword = findViewById(R.id.userPassword);

        // Database
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Automatically called after registering, signing in, signing out, user change, or token changed.
        // Starts the main activity, passing in their profile's identification key in the database
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null) {
                    // When registering we don't want this to run, as the profile in the database is missing fields
                    // that we will try to read
                    if(!skipAutoAuthentication) {
                        final String email = user.getEmail();
                        mDatabaseRef.child(UserProfile.PARENT_USERS).addListenerForSingleValueEvent(new ValueEventListener() {
                            // Get a snapshot of the database so we can retrieve the user's key in order
                            // to further customize their profile
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) return;

                                switchToMainActivity(dataSnapshot, email);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // do nothing
                            }
                        });
                    }
                    // else do nothing, let onComplete do the activity switching
                }
                else {
                    // They are not logged in, do nothing
                }
            }
        };

        // Attempt to create the new user
        createUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = userEmail.getText().toString();
                final String pass = userPassword.getText().toString();

                if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)) {
                    // Skip onAuthStateListener call as their profile is incomplete at this point and we will crash
                    skipAutoAuthentication = true;

                    mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Creation successful?
                            if(task.isSuccessful()) {
                                DatabaseReference mChildDatabase = mDatabaseRef.child(UserProfile.PARENT_USERS).push();
                                String userKey = mChildDatabase.getKey();
                                UserProfile.createNewProfile(userKey, email);

                                Toast.makeText(LoginActivity.this, "User Account Created", Toast.LENGTH_LONG).show();

                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                i.putExtra(MainActivity.I_USER_KEY, userKey);
                                i.putExtra(MainActivity.I_START_FRAG, ProfileFragment.class.getName());
                                startActivity(i);
                            }
                            else
                                Toast.makeText(LoginActivity.this, "Failed to create User Account", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        // Login an existing user
        loginUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = userEmail.getText().toString();
                final String pass = userPassword.getText().toString();

                if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)) {
                    // OnAuthStateChanged will fire once we successfully sign in,
                    // this will handle the activity switch
                    mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Incorrect username or password", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    // Fetch the user's profile from the database and swap to MainActivity,
    private void switchToMainActivity(DataSnapshot dataSnapshot, String email) {
        Iterator iterator = dataSnapshot.getChildren().iterator();

        while(iterator.hasNext()) {
            DataSnapshot dataUser = (DataSnapshot) iterator.next();

            // In case we have an incomplete profile, we don't want to crash,
            // finish the loop and handle the error after
            if(!dataUser.hasChild(UserProfile.VALUE_EMAIL)) continue;

            if(dataUser.child(UserProfile.VALUE_EMAIL).getValue().toString().equals(email)) {
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                i.putExtra(MainActivity.I_USER_KEY, dataUser.child(UserProfile.VALUE_USER_KEY).getValue().toString());
                if(dataUser.child(UserProfile.VALUE_PROFILE_COMPLETED).getValue().toString().equals(UserProfile.STATUS_PROFILE_INCOMPLETE))
                    i.putExtra(MainActivity.I_START_FRAG, ProfileFragment.class.getName());
                else
                    i.putExtra(MainActivity.I_START_FRAG, HomeFragment.class.getName());

                startActivity(i);
            }
        }

        // If we've reached this point, their profile has not been properly registered,
        // handle this error
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Authenticate the user if they are already logged into the database
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mAuth.removeAuthStateListener(mAuthListener);
    }

}
