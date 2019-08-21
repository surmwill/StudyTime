package com.example.st.dialog_fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class GroupJoinDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("1.) You have notified the group of your intent to join.\n\n" +
                "2.) Meet up with your group and tap phones to officially join.\n\n" +
                "3.) Check the group description for information on their location.\n\n" +
                "4.) A new chat has been opened with the group's leader if you can't find them.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
