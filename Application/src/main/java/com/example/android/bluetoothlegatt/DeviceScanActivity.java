/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
//public class DeviceScanActivity extends Activity
public class DeviceScanActivity extends ListActivity
{
    private final String    TAG = DeviceScanActivity.class.getSimpleName();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter    mBluetoothAdapter;
    private ArrayList<String>   testDeviceList;
    private ArrayList<String>   okDeviceList;
    private HashMap<String, Integer>    rssiMapList;

    private boolean mScanning;
    private Handler mHandler;
    //private TextView    finalTextView;

    private static final int    REQUEST_ENABLE_BT = 1;
    public static final int     REQUEST_TEST_FUNCTION = 10;
    public static final int     REQUEST_FINAL_LIST = 200;
    private static final long   SCAN_PERIOD = 10000;    // Stop scanning after 10s.
    //private final String DEVICEFILENAME = "/sdcard/mlcDevices.ini";
    private final String DEVICEFILENAME = Environment.getExternalStorageDirectory().getPath() +
                                            "/mlcDevices.ini";
    ArrayList<String>    deviceNameList = new ArrayList<>();
    private String  mlcDeviceName = "";//"3MW1-4B";
    //private int     versionCode=0;
    private String  titleString= "";    //appVersion();

    //private static final String mlcDeviceName = "3MW1-4B";
    //private static HashMap<String, Integer>    rssiMapAddr;
    //private static int          ActivityCount=0;

    //private boolean stopFlag = false;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        titleString = appVersion();
        Log.d(TAG, "file path: " + DEVICEFILENAME);
        //read mlcDevices.ini
        if(!Utils.readINIFile(DEVICEFILENAME))
            Utils.readINIFile(DEVICEFILENAME);   //read default file

        deviceNameList.clear();
        deviceNameList = Utils.getDeviceNameList();
        showDeviceAlertDialog(deviceNameList);    // mlc

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, LENGTH_SHORT).show();
            finish();
            return;
        }

        testDeviceList = new ArrayList<>(); //make test Ok device quent.
        okDeviceList = new ArrayList<>();
        rssiMapList = new HashMap<>();
        //rssiMapAddr = new HashMap<>();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning)
        {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        else
        {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onResume()
    {
        super.onResume();
        //getActionBar().setTitle(R.string.title_devices);

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled())
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        Log.i(TAG, "onResume...");
        getActionBar().setTitle(titleString);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        //Toast.makeText(this, requestCode, Toast.LENGTH_SHORT ).show();
        // User chose not to enable Bluetooth.

        if ((requestCode == REQUEST_ENABLE_BT) && (resultCode == Activity.RESULT_CANCELED) )
        {
            finish();
            return;
        }

        Log.d(TAG, "result code: " + resultCode + ", requestCode code: " + requestCode);
        //Toast.makeText(this, requestCode, Toast.LENGTH_SHORT ).show();

        if (resultCode == this.REQUEST_TEST_FUNCTION)
        {
            if (mScanning)
            {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }

            //Log.d(TAG, "okDeviceAddress: " + bundle.getString(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS));
            String testAddress = data.getStringExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS);
            boolean testState = data.getBooleanExtra(DeviceControlActivity.TEST_STATE, true);
            Log.d(TAG, testAddress + ", state: " + testState);

            if (testAddress != null)
            {
                if (testState)
                {
                    //int tmpRssi= rssiMapAddr.get(testAddress);
                    //int tmpRssi = mLeDeviceListAdapter.getRssi(testAddress);
                    int tmpRssi = rssiMapList.get(testAddress);

                    Log.d(TAG, mlcDeviceName + " :" + testAddress + " :" +
                            //mLeDeviceListAdapter.getRssi(testAddress)+ " dBm" );
                            tmpRssi + " dBm" );
                    //make test ok address & rssi mapping.
                    okDeviceList.add( mlcDeviceName + " \t" +
                            //testAddress + "  \t" + mLeDeviceListAdapter.getRssi(testAddress) + " dBm " +
                            testAddress + " \t" + tmpRssi + " dBm " +
                            "  => PASS \r\n");

                    Log.d(TAG, "add OK list: " + testAddress + ": " + testDeviceList.size());
                }
                else
                    Utils.mlcDelay(100);

                //check OK address then remove test quenu item.
                for (int i=0; i<testDeviceList.size(); i++)
                {
                    if (testDeviceList.get(i).equals(testAddress))
                    {
                        Log.d(TAG, "Remove Item: " + testDeviceList.get(i));
                        testDeviceList.remove(i);
                        Log.d(TAG, "after remove list items: " + testDeviceList.size());
                    }
                }
            }

            //Toast.makeText(this, okDeviceAddress, Toast.LENGTH_LONG ).show();
            if ((testDeviceList.size()>0))  //go to BLE test screen
            {
                //Log.d(TAG, "testOKDEviceList is Not Empty: " +  ActivityCount++);
                Intent intent = new Intent(this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mlcDeviceName);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, testDeviceList.get(0));

                startActivityForResult(intent, REQUEST_TEST_FUNCTION);
            }
            else if ((testDeviceList.size() < 1) && (okDeviceList.size() > 0))//test final to display resultActivity.
            {
                Log.d(TAG, "Test List : " + testDeviceList.size());
                if (okDeviceList != null)
                    gotoResultActivity(okDeviceList);
            }
            else
            {
                Log.d(TAG, "Error: " + testAddress + " :" + testState);
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //rssiMapAddr.clear();
    }

    ///*
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(0);
        if (device == null) return;

        Log.d(TAG, "Devices: " + mLeDeviceListAdapter.mLeDevices.size() +
                "Counts: " + mLeDeviceListAdapter.getCount());

        testDeviceList.clear();
        okDeviceList.clear();
        //rssiMapAddr.clear();
        rssiMapList.clear();

        //tomcat add for check list item.
        for (int i=0; i<mLeDeviceListAdapter.getCount(); i++)
        {
            String tmpAddr = mLeDeviceListAdapter.getDevice(i).getAddress();
            ///int tmpRssi = mLeDeviceListAdapter.getRssi(tmpAddr);
            //Log.d(TAG, "Rssi:" + tmpRssi);
            testDeviceList.add(i, tmpAddr);
            rssiMapList.put(tmpAddr, mLeDeviceListAdapter.getRssi(tmpAddr));
            ///rssiMapAddr.put(tmpAddr, tmpRssi) ;
            //Log.d(TAG, tmpAddr + ":" + tmpRssi);
        }
        final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivityForResult(intent, REQUEST_TEST_FUNCTION);
    }

    private void gotoResultActivity(ArrayList<String> resultBLEList)
    {
        Log.d(TAG, "Result BLE device: " + resultBLEList.size());
        final Intent intent = new Intent(DeviceScanActivity.this, ResultActivity.class);
        intent.putExtra("DeviceName", mlcDeviceName);
        intent.putStringArrayListExtra(ResultActivity.RESULT_LIST, resultBLEList);
        startActivityForResult(intent, REQUEST_FINAL_LIST);
        //startActivity(intent);
        //finish();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable)
    {
        //boolean mOK=true;

        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run()
                {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            //okDeviceList.clear();   // clear test device quenu.
        }
        else
        {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //Toast.makeText(this, "Scan BLE device OK", Toast.LENGTH_SHORT).show();  //debug.
        }
        invalidateOptionsMenu();
    }

    // MCL make function.
    // AlertDialog function.
    protected void showDeviceAlertDialog(ArrayList<String> nameList)
    {
        String[] tmpList = new String[nameList.size()];
        for (int i=0; i<nameList.size(); i++)
            tmpList[i] = nameList.get(i);
        final String[]  DataList = tmpList.clone();
        AlertDialog adBuilder = new AlertDialog.Builder(this)
                .setTitle("Select a device for test")
                .setSingleChoiceItems(DataList, 0, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        //Toast.makeText(this, tmp, LENGTH_SHORT).show();
                        mlcDeviceName = DataList[which];
                        Utils.setCommandIndex(which);
                        titleString = titleString + " \tScan " + mlcDeviceName;
                        getActionBar().setTitle(titleString);
                        Toast.makeText(getApplicationContext(), mlcDeviceName, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        //adBuilder.setPositiveButton("OK", ));
                    }
                }).show();
    }

    private String appVersion()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return ("v" + packageInfo.versionName + "." + packageInfo.versionCode);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /*
    private final List<String> getDataInfo()
    {
        List<String>    deviceList = new ArrayList<>();

        deviceList.add("3MW1-4B");
        deviceList.add("A6 BT");
        deviceList.add("BP3GT1-6B");
        deviceList.add("BP3GU1-7B");

        return deviceList;
    }
    */

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private final ArrayList<BluetoothDevice>  mLeDevices;
        private final HashMap<String, Integer>    rssiMap;
        private LayoutInflater mInflator;
        private ViewHolder viewHolder;

        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            rssiMap = new HashMap<String, Integer>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();

            clear();
            /*
            TextView devName = (TextView)mInflator.inflate(R.layout.listitem_device, null).
                    findViewById(R.id.device_name);
            devName.setText("");
            TextView devAddr= (TextView) mInflator.inflate(R.layout.listitem_device, null).
                    findViewById(R.id.device_address);
            devAddr.setText("");
            TextView devRssi= (TextView)mInflator.inflate(R.layout.listitem_device, null).
                    findViewById(R.id.device_rssi);
            devRssi.setText("");
            */
        }

        //public void addDevice(BluetoothDevice device)
        @TargetApi(Build.VERSION_CODES.ECLAIR)
        public void addDevice(BluetoothDevice device, int rssi)
        {
            if (!mLeDevices.contains(device) )  // for MLC BP
            {
                mLeDevices.add(device);
                //if (device.getAddress() != null)
                rssiMap.put(device.getAddress(), rssi);
            }
        }

        public BluetoothDevice getDevice(int position)
        {
            return mLeDevices.get(position);
        }

        public void clear()
        {
            mLeDevices.clear();
            TextView devName = (TextView)mInflator.inflate(
                    R.layout.listitem_device, null).findViewById(R.id.device_name);
            devName.setText("");
            TextView devAddr= (TextView)mInflator.inflate(
                    R.layout.listitem_device, null).findViewById(R.id.device_address);
            devAddr.setText("");
            TextView devRssi= (TextView)mInflator.inflate(
                    R.layout.listitem_device, null).findViewById(R.id.device_rssi);
            devRssi.setText("");
        }

        @Override
        public int getCount()
        {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @TargetApi(Build.VERSION_CODES.ECLAIR)
        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            //ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null)
            {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                //viewHolder.totalDevices = (TextView) view.findViewById(R.id.textView);
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRssi.setText("" + rssiMap.get(device.getAddress()) + " dBm");
            //viewHolder.totalDevices.setText(Integer.toString(getCount())+ " devices");
            return view;
        }

        // disable touch screen to trigger ListView Item.
        @Override
        public boolean areAllItemsEnabled()
        {
            return super.areAllItemsEnabled();
            //return false;
        }

        @Override
        public boolean isEnabled(int position)
        {
            return super.isEnabled(position);
            //return false;
        }

        public int getRssi(String deviceAddr)
        {
            //int tmpRssi = new Integer(rssiMap.get(deviceAddr));
            //Log.d(TAG, "getRssi(): " + tmpRssi);    //rssiMap.get(deviceAddr));
            Log.d(TAG, "getRssi(): " + rssiMap.get(deviceAddr));

            //return tmpRssi;
            return rssiMap.get(deviceAddr).intValue();
            //return rssiMap.get(deviceAddr);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
                {
                    runOnUiThread(new Runnable()
                    {
                        @TargetApi(Build.VERSION_CODES.ECLAIR)
                        @Override
                        public void run()
                        {
                            if ((device.getName() != null) && device.getName().equals(mlcDeviceName))
                            {
                                //mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.addDevice(device, rssi);
                                //testOKDeviceList.add(device.getAddress());
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
       // TextView totalDevices;
    }
}


