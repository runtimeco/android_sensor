package io.runtime.sensoroic.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;
import org.iotivity.base.QualityOfService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.runtime.sensoroic.OicApplication;
import io.runtime.sensoroic.R;
import io.runtime.sensoroic.task.ObserveTask;

public class LightActivity extends AppCompatActivity implements
        OcResource.OnObserveListener,
        OcResource.OnPutListener {

    // Logging TAG
    private final static String TAG = "LightActivity";

    // Views
    private Switch mSwitch;

    // Application
    private OicApplication mApp;

    // Resource and Representation
    private OcResource mResource;

    // Map of resource values
    private HashMap<String, Object> mValueMap = new HashMap<>();

    // Flag set when the user manually sets the switch such that observe doesnt override the
    // setting and change the value back to the original
    private boolean mIsPutting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light);
        setTitle("Light");

        // Set up views
        mSwitch = (Switch) findViewById(R.id.light_switch);
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    boolean isChecked = ((Switch)v).isChecked();
                    OcRepresentation rep = new OcRepresentation();
                    rep.setValue("value", !isChecked);
                    mResource.put(rep, new HashMap<String, String>(), LightActivity.this);
                    mIsPutting = true;
                } catch (OcException e) {
                    e.printStackTrace();
                }
            }
        });

        // Get OicApplication
        mApp = (OicApplication) getApplication();

        // Get resource ID from intent
        String resId = getIntent().getStringExtra("resId");

        // Get OcResource using ID from Application
        mResource = mApp.getResource(resId);

        // Get values from resource and observe on callback
        if (mResource.isObservable()) {
            new ObserveTask(this, mResource, this).execute();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mResource.cancelObserve(QualityOfService.LOW);
        } catch (OcException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation, int i) {
        final Map<String, Object> values = ocRepresentation.getValues();
        Log.d(TAG, String.valueOf(values));

        mValueMap.putAll(values);
        final boolean value = (boolean)mValueMap.get("value");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mIsPutting) {
                    mSwitch.setChecked(!value);
                }
            }
        });
    }

    @Override
    public void onObserveFailed(Throwable throwable) {

    }

    @Override
    public void onPutCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation) {
        Log.d(TAG, "Put completed");
        mIsPutting = false;
    }

    @Override
    public void onPutFailed(Throwable throwable) {
        Log.d(TAG, "Put failed");
        mIsPutting = false;
    }
}
