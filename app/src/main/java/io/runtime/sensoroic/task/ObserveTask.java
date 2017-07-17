package io.runtime.sensoroic.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import org.iotivity.base.ObserveType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.HashMap;
import java.util.List;

import io.runtime.sensoroic.R;

public class ObserveTask extends AsyncTask<Void, Void, Void> implements OcResource.OnObserveListener {

    private Context mContext;
    private OcResource mResource;
    private OcResource.OnObserveListener mListener;
    private ProgressDialog mProgressDialog;
    private boolean mResponseReceived = false;

    public ObserveTask(Context context, OcResource resource, OcResource.OnObserveListener listener) {
        mContext = context;
        mResource = resource;
        mListener = listener;
        mProgressDialog = new ProgressDialog(context, R.style.ProgressDialog);
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setMessage("Wait while loading values from device...");
        mProgressDialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected synchronized Void doInBackground(Void[] params) {
        try {

            mResource.observe(ObserveType.OBSERVE, new HashMap<String, String>(), this);
            wait(10000);
        } catch (OcException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mProgressDialog.dismiss();
        if (!mResponseReceived) {
            new AlertDialog.Builder(mContext).
                    setCancelable(false)
                    .setTitle("Response Failure")
                    .setMessage("The device failed to respond.")
                    .setNegativeButton("Go Back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((AppCompatActivity) mContext).finish();
                        }
                    }).create().show();
        }
    }

    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation, int i) {
        mResponseReceived = true;
        notify();
        mListener.onObserveCompleted(list, ocRepresentation, i);
    }

    @Override
    public synchronized void onObserveFailed(Throwable throwable) {
        mResponseReceived = true;
        notify();
        mListener.onObserveFailed(throwable);
    }
}
