package io.runtime.sensoroic.activity;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;
import org.iotivity.base.QualityOfService;

import java.util.ArrayList;
import java.util.List;

import io.runtime.sensoroic.MynewtSensor;
import io.runtime.sensoroic.OicApplication;
import io.runtime.sensoroic.R;
import io.runtime.sensoroic.task.ObserveTask;


public class SensorActivity extends AppCompatActivity implements OcResource.OnObserveListener {

    // Logging TAG
    private static final String TAG = "SensorActivity";
    private static final int DATA_X_RANGE = 15;

    // Application
    private OicApplication mApp;

    // Resource and Resource Type
    private OcResource mResource;
    private String mResourceType;

    // Mynewt Sensor Object
    private MynewtSensor mSensor;

    // ArrayList of Sensor Value keys
    private ArrayList<String> mSensorDataKeys = new ArrayList<>();
    // ArrayList of Sensor Value values
    private ArrayList<Object> mSensorDataValues = new ArrayList<>();

    private boolean mIsObserving;

    // Chart Object
    private LineChart mChart;

    // Views
    private ListView mSensorValueListView;

    // Adapter for mSensorValueListView
    private SensorValueAdapter mSensorValueListAdapter;

    // X value for chart
    private volatile int mCurrentX = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        // Get OicApplication
        mApp = (OicApplication) getApplication();

        // Get resource ID from intent
        String resId = getIntent().getStringExtra("resId");

        // Get OcResource object using ID from Application
        mResource = mApp.getResource(resId);
        setTitle(MynewtSensor.getReadableName(mResource));

        // Get the sensor resource type
        mResourceType = MynewtSensor.getSensorResourceType(mResource.getResourceTypes());

        // Set up Views
        mSensorValueListView = (ListView) findViewById(R.id.sensor_value_list);

        //Set Adapter for mSensorValueListView
        mSensorValueListAdapter = new SensorValueAdapter(this, R.layout.list_item_sensor_value, mSensorDataValues);
        mSensorValueListView.setAdapter(mSensorValueListAdapter);

        initChart();

        // Observe the resource
        if (mResource.isObservable()) {
            new ObserveTask(this, mResource, this).execute();
            mIsObserving = true;
        } else {
            //TODO show dialog
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        try {
            //TODO bug, where closing the app manually very quickly doesnt cancel the observe
            if (mIsObserving) {
                mResource.cancelObserve(QualityOfService.LOW);
            }
        } catch (OcException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sensor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.sensor_menu_toggle_observe:
                if (mIsObserving) {
                    try {
                        mResource.cancelObserve(QualityOfService.LOW);
                        item.setTitle(R.string.start_observe);
                        mIsObserving = false;
                    } catch (OcException e) {
                        e.printStackTrace();
                    }
                } else {
                    item.setTitle(R.string.stop_observe);
                    new ObserveTask(this, mResource, this).execute();
                    mIsObserving = true;
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initChart() {
        mChart = (LineChart) findViewById(R.id.sensor_line_chart);
        Description desc = new Description();
        desc.setText("");
        mChart.setDescription(desc);
        mChart.setDrawMarkers(false);

        XAxis xl = mChart.getXAxis();
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setDrawLabels(false);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setDrawZeroLine(true);
        rightAxis.setZeroLineWidth(2f);
        rightAxis.setZeroLineColor(Color.DKGRAY);

        LineData data = new LineData();
        mChart.setData(data);
    }

    private synchronized void createDataSets(LineData data) {
        int[] colors = getResources().getIntArray(R.array.colors);
        for (int i = 0; i < mSensor.getSensorDataCount(); i++) {
            LineDataSet set = new LineDataSet(null, mSensorDataKeys.get(i).toUpperCase());
            set.setColor(colors[i]);
            set.setDrawValues(false);
            set.setCircleColor(colors[i]);
            set.setDrawCircleHole(false);
            set.setCircleRadius(2.5f);
            set.setAxisDependency(YAxis.AxisDependency.RIGHT);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(.05f);
            set.setLineWidth(2.5f);
            data.addDataSet(set);
            if (i > 2) {
                data.getDataSetByIndex(i).setVisible(false);
            }
        }
    }
    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation ocRepresentation, int i) {
        // If we have not yet received an Observe response, create a new MynewtSensor object.
        // Otherwise update the data values inside the MynewtSensor object.

        if (mSensor == null) {
            mSensor = new MynewtSensor(ocRepresentation, mResourceType);
            mSensorDataKeys = new ArrayList<>(mSensor.getSensorDataKeySet());

            Log.d(TAG, "MynewtSensor Created:");
            Log.d(TAG, "\tkeys = " + mSensor.getSensorDataKeySet());
            Log.d(TAG, "\tvalues = " + mSensor.getSensorDataValues());
        } else {
            mSensor.updateSensor(ocRepresentation);
        }
        Log.d(TAG, "Observe completed - values = " + ocRepresentation.getValues());
        if (!mIsObserving) {
            return;
        }
        mCurrentX++;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the chart
                // Get the data obj from the chart
                synchronized (SensorActivity.this) {

                    mSensorDataValues.clear();
                    mSensorDataValues.addAll(mSensor.getSensorDataValues());

                    LineData data = mChart.getData();
                    if (data != null) {
                        ILineDataSet set = data.getDataSetByIndex(0);
                        if (set == null) {
                            // If there is no data yet, create the data sets
                            createDataSets(data);
                        }
                        // Add the new data entries
                        for (int i = 0; i < mSensor.getSensorDataCount(); i++) {
                            double val = (Double) mSensorDataValues.get(i);
                            data.addEntry(new Entry(mCurrentX, (float) val), i);
                        }
                        data.notifyDataChanged();

                        // let the chart know it's data has changed
                        mChart.notifyDataSetChanged();
                        mChart.setVisibleXRangeMaximum(DATA_X_RANGE);
                        mChart.moveViewToX(data.getXMax());
                    }

                    mSensorValueListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onObserveFailed(Throwable throwable) {
        Log.i(TAG, "Observe failed");
    }

    private class SensorValueAdapter extends ArrayAdapter {

        private ArrayList<Object> mValues;

        private SensorValueAdapter(Context context, int resource, ArrayList<Object> values) {
            super(context, resource, values);
            mValues = values;
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View view, @NonNull ViewGroup parent) {
            TextView keyView;
            TextView valueView;
            Switch chartSwitch;
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item_sensor_value, parent, false);

                keyView = (TextView) view.findViewById(R.id.list_item_sensor_value_key);
                keyView.setText(mSensorDataKeys.get(position));

                chartSwitch = (Switch) view.findViewById(R.id.list_item_sensor_value_chart_switch);
                chartSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mChart != null) {
                            LineData data = mChart.getLineData();
                            ILineDataSet set = data.getDataSetByIndex(position);
                            if (set != null) {
                                set.setVisible(isChecked);
                                mChart.invalidate();
                            }
                        }
                    }
                });
                if (position < 3) {
                    chartSwitch.setChecked(true);
                }
            }
            valueView = (TextView) view.findViewById(R.id.list_item_sensor_value_value);
            synchronized(SensorActivity.this) {
                valueView.setText(shaveDoubleString(String.valueOf(mValues.get(position))));
            }
            return view;
        }
    }

    // Util
    String shaveDoubleString(String dStr) {
        if (dStr.length() < 7) return dStr;
        return dStr.startsWith("-") ? dStr.substring(0, 7) : dStr.substring(0, 6);
    }

    /* ============
     * KNOWN ISSUES
     * ============
     *
     *  o When observing is stopped then started again, the first data set (sensor value)
     *    always shows in the chart
     */
}
