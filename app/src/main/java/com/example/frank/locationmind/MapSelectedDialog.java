package com.example.frank.locationmind;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by frank on 18/1/13.
 */

public class MapSelectedDialog extends DialogFragment {
    public static MapSelectedDialog newInstance(String message){
        MapSelectedDialog frag = new MapSelectedDialog();
        Bundle args = new Bundle();
        args.putString("MESSAGE", message);
        frag.setArguments(args);
        return frag;
    }


    public interface MapSelectedDialogInterface {
        void onMapSelected(boolean isConfirmed);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String message = getArguments().getString("MESSAGE");
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MapSelectedDialogInterface a =(MapSelectedDialogInterface)getActivity();
                        a.onMapSelected(true);
                        // FIRE ZE MISSILES!
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        MapSelectedDialogInterface a =(MapSelectedDialogInterface)getActivity();
                        a.onMapSelected(false);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
