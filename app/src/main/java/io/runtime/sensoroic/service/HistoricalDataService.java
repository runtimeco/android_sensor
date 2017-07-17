package io.runtime.sensoroic.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.HashMap;
import java.util.List;

import io.runtime.sensoroic.OicApplication;

public class HistoricalDataService extends Service implements OcResource.OnGetListener {

    private final static String TAG = "HistoricalDataService";

    private OicApplication mApp;
    private Handler mHandler =  new Handler();
    private Runnable mRunnable;
    private OcResource mTempResource;
    private int mValueCount = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mApp = (OicApplication)getApplication();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mTempResource != null) {
                        mTempResource.get(new HashMap<String, String>(), HistoricalDataService.this);
                    }
                } catch (OcException e) {
                    e.printStackTrace();
                }
                getData();
            }
        };
        mTempResource = mApp.getDiscovered().get("coap+tcp://C0:FA:AC:CF:FA:0A/bme280_0/ambtmp");
        getData();
        return Service.START_NOT_STICKY;
    }

    private void getData() {
        mHandler.postDelayed(mRunnable, 20000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onGetCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation) {
        Log.d(TAG, "Get Completed");
        HashMap<String, Object> values = (HashMap<String, Object>)ocRepresentation.getValues();
        double temp = (Double) values.get("temp");
        mApp.addHistoricalData(new Entry((float)mValueCount, (float)temp));
        mValueCount++;
    }

    @Override
    public void onGetFailed(Throwable throwable) {
        Log.d(TAG, "Get Failed");
    }
}
