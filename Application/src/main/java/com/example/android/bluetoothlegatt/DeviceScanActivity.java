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

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity
{
    private final String    TAG = DeviceScanActivity.class.getSimpleName();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int    REQUEST_ENABLE_BT = 1;
    private static final long   SCAN_PERIOD = 10000;
    private static final String mlcDeviceName = "3MW1-4B";
    private boolean stopFlag = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning)
        {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            stopFlag = true;
        }
        else
        {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
            stopFlag = false;
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

    @Override
    protected void onResume()
    {
        super.onResume();

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

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);

        //tomcat adds
        /*
        bleDevices = mLeDeviceListAdapter.getTotalInfo();
        final Intent    intent = new Intent(this, MLCIntentService.class);
        intent.putExtra("ble", bleDevices);
        startService(intent);
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    ///*
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        toBLEServiceStart(position);
    }

    // MLC make data to Passing.
    private void toBLEServiceStart(int position)
    {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(this, DeviceControlActivity.class);

        HashMap<String, Integer> bleDevicesInfo = new HashMap<String, Integer>();
        List<String>    deviceAddressList = new ArrayList<>();

        ///intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        ///intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        ///ArrayList<HashMap<BluetoothDevice, Integer>>  tmpDevicesRssi= new ArrayList<HashMap<BluetoothDevice, Integer>>();


        for (int i=0; i<mLeDeviceListAdapter.getCount(); i++)
        {
            deviceAddressList.add(mLeDeviceListAdapter.getDevice(i).getAddress());
            Log.i(TAG, "List[" + i + "]" + deviceAddressList.get(i));
        }
        intent.putExtra("BLE_ADDRESS", (Serializable)deviceAddressList);

        ///bleDevicesInfo = mLeDeviceListAdapter.getTotalInfo();
        ///intent.putExtra("BLE_DEVICE", (Serializable) bleDevicesInfo);
        intent.putExtra("BLE_DEVICE", (Serializable) mLeDeviceListAdapter.getTotalInfo());

        /*
        ArrayList<String>  tmpDevicesName = new ArrayList<String>();
        ArrayList<String>  tmpDevicesAddress = new ArrayList<String>();
        for (int i=0; i<mLeDeviceListAdapter.getCount(); i++)
        {
            tmpDevicesName.add(i, mLeDeviceListAdapter.getDevice(i).getName());
            tmpDevicesAddress.add(i, mLeDeviceListAdapter.getDevice(i).getAddress());
        }
        //tmpDevicesRssi = mLeDeviceListAdapter.getTotalInfo();

        //tmpDevices.add(i, mLeDeviceListAdapter.getDevice(i));
        ///intent.putExtra("BLE_DEVICE_NAME", (Serializable)tmpDevicesName);
        ///intent.putExtra("BLE_DEVICE_ADDRESS", (Serializable)tmpDevicesAddress);
        //intent.putExtra("BLE_DEVICE_RSSI", (Serializable) tmpDevicesRssi);
        tmpDevicesRssi.add(mLeDeviceListAdapter.getTotalInfo());
        intent.putExtra("BLE_DEVICE_RSSI", (Serializable)tmpDevicesRssi);
        */


        if (mScanning)
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
    //*/

    private void scanLeDevice(final boolean enable)
    {
        //boolean mOK=true;

        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
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
        }
        else
        {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //Toast.makeText(this, "Scan BLE device OK", Toast.LENGTH_SHORT).show();  //debug.
        }


        //if (stopFlag)
        //    Toast.makeText(this, "Scan BLE device OK", Toast.LENGTH_SHORT).show();  //debug.
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private final ArrayList<BluetoothDevice>  mLeDevices;
        private final HashMap<BluetoothDevice, Integer>    rssiMap;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            rssiMap = new HashMap<BluetoothDevice, Integer>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        //public void addDevice(BluetoothDevice device)
        public void addDevice(BluetoothDevice device, int rssi)
        {
            if (!mLeDevices.contains(device) )  // for MLC BP
            {
                mLeDevices.add(device);
                rssiMap.put(device, rssi);
            }
        }

        public BluetoothDevice getDevice(int position)
        {
            return mLeDevices.get(position);
        }

        public void clear()
        {
            mLeDevices.clear();
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

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
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
            viewHolder.deviceRssi.setText("" + rssiMap.get(device) + " dBm");
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

        public int getRssi(BluetoothDevice device)
        {
            return rssiMap.get(device);
        }

        public HashMap<String, Integer> getTotalInfo()
        {
            HashMap<String, Integer>    rssiMapInfo = new HashMap<>();

            for (int i=0; i<mLeDeviceListAdapter.getCount(); i++)
            {
                rssiMapInfo.put(mLeDeviceListAdapter.getDevice(i).getAddress(),
                                mLeDeviceListAdapter.getRssi(mLeDeviceListAdapter.getDevice(i)));
            }
            return rssiMapInfo;
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
                        @Override
                        public void run()
                        {
                            //if(device.getName().equalsIgnoreCase("3MW1-4B"))
                            if(device.getName().equalsIgnoreCase(mlcDeviceName))
                            //if(device.getName().equals(mlcDeviceName))
                            {
                                //mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.addDevice(device, rssi);
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