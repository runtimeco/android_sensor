package io.runtime.sensoroic.task;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcResource;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import io.runtime.sensoroic.OicApplication;
import io.runtime.sensoroic.R;

public class DiscoveryTask extends AsyncTask<Void, Integer, Void> implements OcPlatform.OnResourceFoundListener {

    // Logging TAG
    private static final String TAG = "DiscoveryTask";

    // Application
    private OicApplication mApp;

    // Context member variable
    private Context mContext;

    // OnDiscoveryListener
    private OnDiscoveryListener mListener;

    // Progress View
    private ProgressDialog mProgressDialog;

    // Discovery transports
    private boolean mDiscoverBle;
    private boolean mDiscoverIp;


    // Bluetooth member variables
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<ScanFilter> mBleScanFilters;
    private ScanSettings mBleScanSettings;
    private Handler mHandler;

    // 128-bit OIC Service UUID
    private final UUID mOicUuid = UUID.fromString("ADE3D529-C784-4F63-A987-EB69F70EE816");

    // Scan constants
    private static final int BLE_SCAN_DURATION = 10;
    private static final int BLE_SCAN_DURATION_MILLIS = BLE_SCAN_DURATION * 1000;
    private static final int DISCOVERY_DURATION = 15;
    private static final int DISCOVERY_DURATION_MILLIS = DISCOVERY_DURATION * 1000;

    // List of found OIC Bluetooth LE Hosts
    private ArrayList<String> mScannedHosts = new ArrayList<>();
    private ArrayList<String> mConnectedHosts = new ArrayList<>();

    // List of discovered resources to be retured via the listener callback
    private ArrayList<OcResource> mDiscoveredResources = new ArrayList<>();

    // BLE device address whitelist
    private ArrayList<String> mWhiteList;

    // Whether or not the BLE cache has been cleared
    private boolean mCacheCleared = false;


    /**
     * General multicast resource discovery constructor for DiscoveryTask. This DiscoveryTask will
     * discover all OIC enabled devices over BLE and IP transport
     *
     * @param context   context
     * @param listener  the OnDiscoveryListener for discovery callbacks
     */
    public DiscoveryTask(Context context, OnDiscoveryListener listener) {
        mContext = context;
        mApp = (OicApplication) context.getApplicationContext();
        mListener = listener;
        mDiscoverBle = true;
        mDiscoverIp = true;
    }

    /**
     * Multicast resoruce discovery constructor for DiscoveryTask. This DiscoveryTask will publish
     * progress updates to the given ProgressDialog's message TextView.
     *
     * @param context           context
     * @param listener          the OnDiscoveryListener for discovery callbacks
     * @param discoverBle       whether or not to discover devices over BLE transport
     * @param discoverIp        whether or not to discover devices over IP transport
     * @param progressdialog    the progress dialog to publish progress to
     */
    public DiscoveryTask(Context context, OnDiscoveryListener listener, boolean discoverBle,
                         boolean discoverIp, ProgressDialog progressdialog) {
        mContext = context;
        mApp = (OicApplication) context.getApplicationContext();
        mProgressDialog = progressdialog;
        mListener = listener;
        mDiscoverBle = discoverBle;
        mDiscoverIp = discoverIp;
    }

    /**
     * Unicast BLE, Multicast IP  resource discovery constructor for DiscoveryTask. This
     * DiscoveryTask will multicast discovery IP devices and only discover BLE devices for hosts
     * in the whitelist.
     *
     * @param context       context
     * @param listener      the OnDiscoveryListener for discovery callbacks
     * @param discoverBle   whether or not to discover devices over BLE transport
     * @param discoverIp    whether or not to discover devices over IP transport
     * @param bleWhitelist  the list of BLE host addresses to do resource discovery on
     */
    public DiscoveryTask(Context context, OnDiscoveryListener listener, boolean discoverBle,
                         boolean discoverIp, List<String> bleWhitelist) {
        mContext = context;
        mApp = (OicApplication) context.getApplicationContext();
        mListener = listener;
        mWhiteList = (ArrayList<String>) bleWhitelist;
        mDiscoverBle = discoverBle;
        mDiscoverIp = discoverIp;
    }

    /**
     * Unicast BLE, Multicast IP  resource discovery constructor for DiscoveryTask. This
     * DiscoveryTask will publish progress updates to the given ProgressDialog's message TextView.
     * This DiscoveryTask will multicast discovery IP devices and only discover BLE devices for
     * hosts in the whitelist.
     *
     * @param context           context
     * @param listener          the OnDiscoveryListener for discovery callbacks
     * @param discoverBle       whether or not to discover devices over BLE transport
     * @param discoverIp        whether or not to discover devices over IP transport
     * @param progressdialog    the progress dialog to publish progress to
     * @param bleWhitelist      the list of BLE host addresses to do resource discovery on
     */
    public DiscoveryTask(Context context, OnDiscoveryListener listener, boolean discoverBle,
                         boolean discoverIp, ProgressDialog progressdialog, List<String> bleWhitelist) {
        mContext = context;
        mApp = (OicApplication) context.getApplicationContext();
        mListener = listener;
        mProgressDialog = progressdialog;
        mWhiteList = (ArrayList<String>) bleWhitelist;
        mDiscoverBle = discoverBle;
        mDiscoverIp = discoverIp;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
        // Initializes Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        //Set up Bluetooth LE Scanner and do a preliminary scan
        if (bluetoothAdapter == null) {
            //TODO handle case
            cancel(true);
            Log.w(TAG, "Device does not support Bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) {
            //TODO handle case
            cancel(true);
            Log.w(TAG, "Bluetooth is not currently enabled");
        } else {
            // If Bluetooth is enabled, set up the LE scanner and scan for 5 seconds
            mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            mBleScanFilters = new ArrayList<>(1);
            mBleScanFilters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(mOicUuid)).build());
            mBleScanSettings = new ScanSettings.Builder().build();
            mHandler = new Handler();
        }

        //Configure the Iotivity Platform
        PlatformConfig platformConfig = new PlatformConfig(mContext, ServiceType.IN_PROC,
                ModeType.CLIENT, "0.0.0.0", 0, QualityOfService.LOW);
        OcPlatform.Configure(platformConfig);
    }


    /**
     * This is the main work method for DiscoveryTask. There are three sections in this method:
     *   - BLE Scan: If the whitelist is not null, scan nearby BLE devices which advertise the
     *               Iotivity UUID.
     *   - BLE Discovery: If the mDiscoverBle flag is enabled, discover BLE devices from the
     *                    list of scanned devices or the whitelist.
     *   - IP Discovery: If the mDiscoverIp flag is enabled, discover IP devices.
     * @param param void
     * @return void
     */
    @Override
    protected synchronized Void doInBackground(Void... param) {
        //Scan for Ble Devices and wait for 10 seconds
        try {
            if (mWhiteList == null && mDiscoverBle) {
                publishProgress(R.string.scan_progress_ble_scan);
                // Scan for BLE devices advertising the Iotivity UUID
                scanBleDevices();
            } else if (mDiscoverBle) {
                // Uncomment to clear BLE cache for the first device in the whitelist
                //connectAndClearCache(mWhiteList.get(0));
                //wait();
                mScannedHosts.addAll(mWhiteList);
            }
            if (mDiscoverBle) {
                if(mWhiteList == null) {
                    publishProgress(R.string.scan_progress_ble_discovery);
                } else {
                    publishProgress(R.string.scan_progress_ble_discovery_whitelist);
                }
                // Discover BLE devices
                discoverBle();
            }
            if (mDiscoverIp) {
                publishProgress(R.string.scan_progress_ip_discovery);
                // Discover IP devices
                discoverIp();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (OcException e) {
            Log.e(TAG, "Error calling findResource.");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void o) {
        super.onPostExecute(o);
        if (mDiscoveredResources.isEmpty()) {
            mListener.OnDiscoveryFailed();
        } else {
            mListener.OnDiscoveryCompleted(mDiscoveredResources);
        }

    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mDiscoveredResources.isEmpty()) {
            mListener.OnDiscoveryFailed();
        } else {
            mListener.OnDiscoveryCompleted(mDiscoveredResources);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... msg) {
        super.onProgressUpdate(msg);
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(mApp.getResources().getString(msg[0]));
        }
    }

    /**
     * Scans for BLE devices for a set duration.
     * @throws InterruptedException
     */
    private synchronized void scanBleDevices() throws InterruptedException {
        Log.d(TAG, "Scanning for BLE devices...");
        // Stops scanning and notify the waiting thread after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "BLE scan stopped after " + BLE_SCAN_DURATION + " seconds.");
                mBluetoothLeScanner.stopScan(mScanCallback);
                synchronized (DiscoveryTask.this) {
                    DiscoveryTask.this.notify();
                }
            }
        }, BLE_SCAN_DURATION_MILLIS);
        mBluetoothLeScanner.startScan(mBleScanFilters, mBleScanSettings, mScanCallback);
        wait();
    }

    /**
     * Performs the discovery for BLE devices which have been scanned previously or whitelisted.
     * Since Iotivity resource discovery over BLE cannot be multicasted, devices must be scanned
     * one at a time. This method loops through the list of scanned hosts and does resource
     * discovery for a set duration before moving on to the next device in the list.
     * @throws InterruptedException
     * @throws OcException
     */
    private synchronized void discoverBle() throws InterruptedException, OcException {
        for (String hostAddr : mScannedHosts) {
            Log.d(TAG, "Discovering device (" + hostAddr + ") over BLE...");
            OcPlatform.findResource(
                    hostAddr,
                    OcPlatform.WELL_KNOWN_QUERY,
                    EnumSet.of(OcConnectivityType.CT_ADAPTER_GATT_BTLE),
                    this, QualityOfService.LOW);
            wait(DISCOVERY_DURATION_MILLIS);
            wait(1000); //wait for the other resources to be found before continuing
        }
        wait(1000);
    }

    /**
     * Performs the multicast IP resource discovery for a set duration
     * @throws InterruptedException
     * @throws OcException
     */
    private synchronized void discoverIp() throws InterruptedException, OcException {
        Log.d(TAG, "Discovering devices over IP...");
        OcPlatform.findResource("",
                OcPlatform.WELL_KNOWN_QUERY,
                EnumSet.of(OcConnectivityType.CT_ADAPTER_IP),
                this, QualityOfService.LOW);
        wait(DISCOVERY_DURATION_MILLIS);
    }

    //********************************************************
    // Callbacks
    //********************************************************

    /**
     * Callback for the BLE scan.
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final String addr = result.getDevice().getAddress();
            Log.d(TAG, "Scanned device: " + result.getDevice().getName() + ", " + result.getDevice().getAddress());
            if (!mScannedHosts.contains(addr)) {
                mScannedHosts.add(addr);
            }
            if (!mCacheCleared) {
                mCacheCleared = true;
                result.getDevice().connectGatt(mContext, false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        refreshDeviceCache(gatt);
                    }
                });
            }
        }
    };

    /*
     * Callback from the Iotivity library when a resource has been found
     */
    @Override
    public synchronized void onResourceFound(OcResource ocResource) {
        if (null == ocResource) {
            Log.e(TAG, "Found resource is invalid");
            return;
        }
        Log.d(TAG, "Resource found: " + ocResource.getHost() + ocResource.getUri());
        Log.d(TAG, "\t Resource Types: " + ocResource.getResourceTypes());
        // Add the resource to the list of discovered resources
        mDiscoveredResources.add(ocResource);

        // Add the host address to the list of found hosts if not already and notify
        if (!mConnectedHosts.contains(ocResource.getHost())) {
            mConnectedHosts.add(ocResource.getHost());
            // Only notify for ble gatt transports
            if (ocResource.getConnectivityTypeSet().contains(OcConnectivityType.CT_ADAPTER_GATT_BTLE)) {
                // Notify the waiting thread to move on to the next host
                notify();
            }
        }
    }

    /**
     * The listener for DiscoveryTask.
     */
    public interface OnDiscoveryListener {
        /**
         * Called when resources were found from DiscoveryTask
         * @param resources the list of found resources
         */
        void OnDiscoveryCompleted(List<OcResource> resources);

        /**
         * Called when no resources were found from DiscoveryTask
         */
        void OnDiscoveryFailed();
    }

    //Connects to the device and clears the BLE cache
    private void connectAndClearCache(final String address) {
        mBluetoothLeScanner.startScan(mBleScanFilters, mBleScanSettings, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d(TAG, "Scanned device: " + result.getDevice().getName() + ", " + result.getDevice().getAddress());
                if (!result.getDevice().getAddress().equals(address)) {
                    return;
                }
                Log.d(TAG, "Scanned Device");
                super.onScanResult(callbackType, result);
                mBluetoothLeScanner.stopScan(mScanCallback);
                result.getDevice().connectGatt(mContext, false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (newState != 2) {
                            return;
                        }
                        Log.d(TAG, "Gatt connection state: " + newState);
                        refreshDeviceCache(gatt);
                        Log.d(TAG, "refreshed device cache");
                        gatt.disconnect();
                        Log.d(TAG, "disconnected gatt");
                        synchronized (DiscoveryTask.this) {
                            DiscoveryTask.this.notify();
                        }
                    }
                });
            }
        });
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }
}
