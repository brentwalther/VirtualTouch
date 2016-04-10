package io.walther.virtualtouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by brentwalther on 4/10/2016.
 */
public class DeviceTypeDialogFragment extends DialogFragment {

    private ListDialogListener mListener;

    public interface ListDialogListener {
        public void onListItemSelected(String choice);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ListDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose device type for: " + getArguments().getString("device_name"))
                .setItems(R.array.hardware_device_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        TypedArray array = getResources().obtainTypedArray(R.array.hardware_device_options);
                        String choice = array.getString(which);
                        mListener.onListItemSelected(choice);
                    }
                });
        return builder.create();
    }
}
