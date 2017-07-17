package io.runtime.sensoroic.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.iotivity.base.OcResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import io.runtime.sensoroic.MynewtSensor;
import io.runtime.sensoroic.OicApplication;
import io.runtime.sensoroic.R;
import io.runtime.sensoroic.task.DiscoveryTask;

import static io.runtime.sensoroic.R.id.fab;

public class DeviceActivity extends AppCompatActivity implements DiscoveryTask.OnDiscoveryListener {

    // Logging TAG
    private final static String TAG = "DeviceActivity";

    // Standard OIC resource type strings
    private final static String RT_BINARY_SWITCH = "oic.r.switch.binary";

    // Application
    private OicApplication mApp;

    // List of smart devices
    private ArrayList<OcResource> mSmartDevices = new ArrayList<>();

    // List of sensors
    private ArrayList<OcResource> mSensors = new ArrayList<>();

    // Views
    private ListView mSmartDeviceList;
    private ListView mSensorList;
    private FloatingActionButton mDiscoverButton;

    // Discovery Dialog
    private ProgressDialog mDiscoveryDialog;

    // The AsyncTask which handles the Resource Discovery Process
    private DiscoveryTask mDiscoveryTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        setTitle(R.string.device_activity_title);

        // Get application
        mApp = (OicApplication) getApplication();

        // Initialize Views
        initViews();

        // Initialize Listeners
        initListeners();

        // Initial Discover
        if (mApp.getDiscovered().isEmpty()) {
            discover();
        } else {
            populateListViews();
        }
    }

    /**
     * Initializes views for the sensors and smart devices lists. Also sets up the adapters for the
     * ListViews as well as creating the discovery progress dialog to be shown during discovery.
     */
    private void initViews() {
        // Init the discover button
        mDiscoverButton = (FloatingActionButton) findViewById(fab);

        // Set up TextView for empty Lists
        TextView smartDevicesEmpty = (TextView) findViewById(R.id.dev_smart_devices_empty_tv);
        TextView sensorsEmpty = (TextView) findViewById(R.id.dev_sensors_empty_tv);

        // Set up ListViews
        mSmartDeviceList = (ListView) findViewById(R.id.dev_smart_device_list);
        mSmartDeviceList.setEmptyView(smartDevicesEmpty);
        mSensorList = (ListView) findViewById(R.id.dev_sensor_list);
        mSensorList.setEmptyView(sensorsEmpty);

        // Create and set adapters for ListViews
        DeviceListAdapter smartDeviceAdapter = new DeviceListAdapter(this, R.layout.list_item_device, mSmartDevices);
        DeviceListAdapter sensorAdapter = new DeviceListAdapter(this, R.layout.list_item_device, mSensors);
        mSmartDeviceList.setAdapter(smartDeviceAdapter);
        mSensorList.setAdapter(sensorAdapter);

        // Set up DiscoveryDialog
        mDiscoveryDialog = new ProgressDialog(this, R.style.ProgressDialog);
        mDiscoveryDialog.setTitle("Discovering OIC Devices");
        mDiscoveryDialog.setIcon(R.drawable.ic_discover_dark);
        mDiscoveryDialog.setCancelable(false);
        mDiscoveryDialog.setMessage("");
    }

    /**
     * Initializes listeners for DeviceActivity's views, specifically the OnItemClickListeners for
     * the sensor and smart device lists. This method should be called after initViews to avoid
     * NullPointerExceptions.
     */
    private void initListeners() {
        // Set up the discover button's OnClickListener
        mDiscoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discover();
            }
        });

        // Set up the Sensor List OnItemClickListener
        mSensorList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OcResource res = mSensors.get(position);
                Intent i = new Intent(getApplicationContext(), SensorActivity.class);
                i.putExtra("resId", OicApplication.createUniqueId(res.getHost(), res.getUri()));
                startActivity(i);
            }
        });

        // Set up the Smart Device List OnItemClickListener
        mSmartDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OcResource res = mSmartDevices.get(position);
                ArrayList<String> rts = new ArrayList<>(res.getResourceTypes());
                if (rts.contains(RT_BINARY_SWITCH)) {
                    Intent i = new Intent(getApplicationContext(), LightActivity.class);
                    i.putExtra("resId", OicApplication.createUniqueId(res.getHost(), res.getUri()));
                    startActivity(i);
                }
            }
        });

        // Set up the listener for the Discovery Dialog's cancel button
        mDiscoveryDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mDiscoveryDialog != null) {
                    mDiscoveryTask.cancel(true);
                }
                dialog.dismiss();
            }
        });
    }

    /**
     * Displays the discover dialog, starts the discovery task, and starts the waiting thread.
     */
    private void discover() {
        // Example of using a whitelist to discover a single, known ble device.
//        ArrayList<String> whitelist = new ArrayList<>(Arrays.asList("BB:F2:5A:03:98:1C"));
//        mDiscoveryTask = new DiscoveryTask(this, this, true, false, mDiscoveryDialog, whitelist);

        // General, multi-transport, resource discovery with dialog to report progress.
        mDiscoveryTask = new DiscoveryTask(this, this, true, true, mDiscoveryDialog);
        mDiscoveryTask.execute();
    }

    private void populateListViews() {
        // Loop through the list of discovered resources and sort by whether the resource
        // is a Mynewt sensor or smart device (i.e. binary switch)
        Set<String> keys = mApp.getDiscovered().keySet();
        mSensors.clear();
        mSmartDevices.clear();
        for (String key : keys) {
            Log.d(TAG, key);
            OcResource res = mApp.getResource(key);
            if (MynewtSensor.isMynewtSensor(res)) {
                mSensors.add(res);
            } else if (res.getResourceTypes().contains((RT_BINARY_SWITCH))) {
                mSmartDevices.add(res);
            }
        }
        ((DeviceListAdapter) mSmartDeviceList.getAdapter()).notifyDataSetChanged();
        ((DeviceListAdapter) mSensorList.getAdapter()).notifyDataSetChanged();
        mDiscoveryDialog.dismiss();
        // Start Historical Data Service
//            Intent i = new Intent(this, HistoricalDataService.class);
//            startService(i);
    }

    @Override
    public void OnDiscoveryCompleted(List<OcResource> resources) {
        // Populate discovered table
        for (OcResource resource : resources) {
            mApp.putResource(resource);
        }
        populateListViews();
    }

    @Override
    public void OnDiscoveryFailed() {
        // If the table of discovered resources is empty, show a dialog
        new AlertDialog.Builder(DeviceActivity.this)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        discover();
                    }
                })
                .setTitle("No Devices Found")
                .setMessage("No OIC Devices have been found. Make sure that you are in range and your devices are on.")
                .create().show();
        // Dismiss the discovery dialog
        mDiscoveryDialog.dismiss();

    }

    /**
     * Array Adapter for displaying devices found by discovery.
     */
    private class DeviceListAdapter extends ArrayAdapter<OcResource> {
        ArrayList<OcResource> mResources = new ArrayList<>();

        public DeviceListAdapter(@NonNull Context context, int resource, @NonNull List<OcResource> objects) {
            super(context, resource, objects);
            mResources = (ArrayList<OcResource>) objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item_device, parent, false);
            }

            ImageView img = (ImageView) view.findViewById(R.id.list_item_device_img);
            TextView title = (TextView) view.findViewById(R.id.list_item_device_name);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_device_subtitle);

            OcResource res = mResources.get(position);

            // Get the resource URI
            String uri = res.getUri();

            // Get the resource types from the resource at the given position
            ArrayList<String> rts = new ArrayList<>(res.getResourceTypes());

            title.setText(MynewtSensor.getReadableName(res));
            subtitle.setText(uri.substring(1, uri.lastIndexOf('/')));

            if (rts.contains(RT_BINARY_SWITCH)) {
                title.setText(R.string.binary_switch);
                subtitle.setText("LED");
            }

            if (rts.contains(RT_BINARY_SWITCH)) {
                img.setImageResource(R.drawable.ic_light_mynewt);
            } else {
                img.setImageResource(R.drawable.ic_device_mynewt);
            }

            return view;
        }
    }

}
