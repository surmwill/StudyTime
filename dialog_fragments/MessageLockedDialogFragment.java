package com.example.st.dialog_fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.example.st.ChatManager;

public class MessageLockedDialogFragment extends android.support.v4.app.DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder
                .setTitle("Message Locked")
                .setMessage("You have met the maximum amount of messages(" + Integer.toString(ChatManager.MAX_MESSAGES) + "). You will be " +
                "unable to send any more messages until some of your chats are deleted.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });

        return builder.create();
    }
}
