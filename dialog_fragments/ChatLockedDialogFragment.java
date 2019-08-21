package com.example.st.dialog_fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.example.st.ChatManager;

public class ChatLockedDialogFragment extends android.support.v4.app.DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder
                .setTitle("Chat Locked")
                .setMessage("You have met the maximum amount of active chats(" + Integer.toString(ChatManager.MAX_LIVE_CHATS) + "). " +
                "You will be unable to start new chats until old ones are deleted.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });
        return builder.create();
    }
}
