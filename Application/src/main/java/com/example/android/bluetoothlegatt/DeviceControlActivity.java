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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity
{
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final int OK_ADDRESS = 200;

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private int     bleDevices=0;
    //private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeService.LocalBinder  binder;
    //private List<BluetoothLeService>    mBLEServiceList;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private List<String>    devicesAddrList = new ArrayList<String>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    //private boolean flagOK = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            binder = (BluetoothLeService.LocalBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    //int reciveCount = 0;    //debug
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                sendCommandToDevice(mBluetoothLeService.getSupportedGattServices());  // MLC test command.
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                boolean nextBLEFlag = false;
                //String  data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                nextBLEFlag = displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                if (nextBLEFlag)
                    goBackDeviceScanActivity();

                //displayData(data);

                ///if ((data == null) && (mBluetoothLeService != null))
                ///sendNextCommand();
            }

            //Log.i(TAG, "Rev: " + reciveCount++);
        }
    };


    /*
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener()
            {
                @Override
                public boolean onChildClick(ExpandableListView parent,
                                            View v,
                                            int groupPosition,
                                            int childPosition,
                                            long id)
                {
                    if (mGattCharacteristics != null)
                    {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
                        {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null)
                            {
                                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                        {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }
                        return true;
                    }

                    //Log.i(TAG, "View Count: " + viewCount++);
                    return false;
                }
            };

    */
    private void clearUI()
    {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress); //set device mac address to UI
        mConnectionState = (TextView) findViewById(R.id.connection_state);      //set device connection state to UI
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        //AlarmManager    alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        //intent.putExtra("ALARM_EVENT", true);
        //PendingIntent alarmIntenet = PendingIntent.getBroadcast(this, 0, intent, 0);
        //alarmManager.set(AlarmManager.RTC_WAKEUP, 20 * 60 * 1000, alarmIntenet);
        //bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    //private int everCount=0;
    @Override
    protected void onResume()
    {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        //Log.i(TAG, "Service Count: " + binder.getCount());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected)
        {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;

            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mConnectionState.setText(resourceId);
            }
        });
    }

    //private void displayData(String data)
    private boolean displayData(String data)
    {
        if (data != null)
        {
            mDataField.setText(data);
            if (data.matches("M0"));
            {
                Utils.mlcDelay(200);
                return true;
            }
        }
        return false;
    }

    private void sendCommandToDevice(List<BluetoothGattService> gattServices)
    {
        BluetoothGattCharacteristic     readCharacter = null;
        BluetoothGattCharacteristic     writeCharacter = null;

        if (gattServices == null)   return;
        // Loops to find available GATT Characteristic.
        for (BluetoothGattService gattService : gattServices)
        {
            readCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            writeCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);
        }

        mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
        if (writeCharacter != null)
        {
            writeCharacter.setValue(Utils.mlcTestFunction());
            Log.i(TAG, Utils.mlcTestFunction().toString());
            mBluetoothLeService.writeCharacteristic(writeCharacter);
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }

    private void goBackDeviceScanActivity()
    {
        mBluetoothLeService.disconnect();
        Utils.mlcDelay(200);

        Intent  intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        Log.d(TAG, "DeviceAddress: " + mDeviceAddress);

        setResult(DeviceScanActivity.REQUEST_TEST_FUNCTION, intent);
        finish();
        //startActivity(intent);
    }

    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private Integer getBleRssi(HashMap<String, Integer> rssiMap, ArrayList<String> deviceAddress)
    {
        return (rssiMap.get(deviceAddress));
    }

    /*
    // Parser BLE RSSI by Hash map info.
    private ArrayList<String> getBleAddress(HashMap<String, Integer> rssiMap)
    {
        if (rssiMap == null)
            return null;

        String[]    AddressList = rssiMap.toString().split("[,]+");
        ArrayList<String> realAddress = new ArrayList<>();
        ArrayList<Integer> deviceRssi = new ArrayList<>();

        for (int i=0; i<AddressList.length; i++)
        {
            AddressList[i] = AddressList[i].replace("{", "");
            AddressList[i] = AddressList[i].replace("}", "");
            AddressList[i] = AddressList[i].replace(" ", "");
            //AddressList[i] = AddressList[i].
            Log.i(TAG, "split MAP[" + i + "]: " + AddressList[i]);

            String[] tmpString =  AddressList[i].split("[=]");
            for (String item : tmpString)
            {
                if (item.matches("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$"))
                    realAddress.add(item);
                else
                    deviceRssi.add(Integer.parseInt(item));
            }
        }
        return (realAddress);
    }
    */
}
