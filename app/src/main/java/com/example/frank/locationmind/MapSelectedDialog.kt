package com.example.frank.locationmind

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog

/**
 * Created by frank on 18/1/13.
 */

class MapSelectedDialog : DialogFragment() {


    interface MapSelectedDialogInterface {
        fun onMapSelected(isConfirmed: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val message = arguments.getString("MESSAGE")
        // Use the Builder class for convenient dialog construction
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(message)
                .setPositiveButton(R.string.dialog_ok) { dialog, id ->
                    val a = activity as MapSelectedDialogInterface
                    a.onMapSelected(true)
                    // FIRE ZE MISSILES!
                }
                .setNegativeButton(R.string.dialog_cancel) { dialog, id ->
                    // User cancelled the dialog
                    val a = activity as MapSelectedDialogInterface
                    a.onMapSelected(false)
                }
        // Create the AlertDialog object and return it
        return builder.create()
    }

    companion object {
        fun newInstance(message: String): MapSelectedDialog {
            val frag = MapSelectedDialog()
            val args = Bundle()
            args.putString("MESSAGE", message)
            frag.arguments = args
            return frag
        }
    }
}
