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
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
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

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

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
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

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
                    return false;
                }
            };

    private void clearUI()
    {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();

        List<String>    devicesAddrList = (ArrayList<String>) intent.getSerializableExtra("BLE_ADDRESS");
       HashMap<String, Integer> tempMap =
                (HashMap<String, Integer>)intent.getSerializableExtra("BLE_DEVICE");

        ArrayList<String>   bleDeviceAddress = new ArrayList<String>();
        //(HashMap<BluetoothDevice, Integer>)intent.getSerializableExtra("BLE_DEVICE");
        Log.i(TAG, "HashMAP: " + tempMap.toString() + ",  total: " + tempMap.size());

        for (int i=0; i<tempMap.size(); i++)
        {
            Log.i(TAG, "Rssi["+ i +"]: " + tempMap.get(devicesAddrList.get(i)));
        }

        bleDeviceAddress = getBleAddress2(tempMap);
        bleDeviceAddress = getBleAddress(tempMap);
        for (int i=0; i<bleDeviceAddress.size(); i++)
        {
            Log.i(TAG, "Real Addr[" + i + "]: " + bleDeviceAddress.get(i));
            //Log.i(TAG, "Rssi[" + i + "]: " + bleDeviceAddress.get(i));
            mDeviceAddress = bleDeviceAddress.get(i);

            /*
            try
            {
                Thread.sleep(200);
                //wait(5000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            */
        }

        /*
        ///mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        ///mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        //mBluetoothTotalDevice = (ArrayList<HashMap<BluetoothDevice, Integer>>) intent.getSerializableExtra("BLE_DEVICE");
        //Map<BluetoothDevice, Integer> rssMap = new Map<BluetoothDevice, Integer>();
        ArrayList<String>  mBleDevicesName = new ArrayList<String>();
        ArrayList<String>  mBleDevicesAddress = new ArrayList<String>();
        ArrayList<HashMap<BluetoothDevice, Integer>>   totalDevicesRssi = new ArrayList<HashMap<BluetoothDevice, Integer>>();
        final int   deviceCounts= mBleDevicesName.size();
        //String[] mDevicesName = new String[deviceCounts];
        //String[] mDevicesAddress = new String[deviceCounts];
        int[]     mDeviceRssi = new int[deviceCounts];

        //Log.i(TAG, "HashMAP: " + totalDevicesRssi.toString());

        mBleDevicesName = (ArrayList<String>) intent.getSerializableExtra("BLE_DEVICE_NAME");
        mBleDevicesAddress = (ArrayList<String>) intent.getSerializableExtra("BLE_DEVICE_ADDRESS");
        totalDevicesRssi = (ArrayList<HashMap < BluetoothDevice, Integer>>) intent.getSerializableExtra("BLE_DEVICE_RSSI");

        for (int i=0; i<deviceCounts; i++)
        {
            //mDevicesName[i] = mBleDevicesName.get(i);
            //mDevicesAddress[i] = mBleDevicesAddress.get(i);
            mDeviceRssi[i] =  totalDevicesRssi.get(i).get(mBleDevicesAddress);

            Log.i(TAG, "Device[" + Integer.toString(i) + "]: " + mBleDevicesName.get(i).toString() + ": "
                    + mBleDevicesAddress.get(i).toString() + ": "
                    + Integer.toString(mDeviceRssi[i]));
        }
        mDeviceName = mBleDevicesName.get(0);
        mDeviceAddress = mBleDevicesAddress.get(0);

        //*
        for (int i=0; i<mBluetoothTotalDevice.size(); i++)
        {
            Map tmp = mBluetoothTotalDevice.get(i);
            //mDeviceName = tmp.get();
        }
        /*
        //test debug
        for (int i=0; i<mBluetoothTotalDevice.size(); i++)
        {
            Log.i(TAG, "Device[" + Integer.toString(i) + "]: " + mBluetoothTotalDevice.get(i).toString());
            //Toast.makeText(this, Integer.toString(i)+ "  mac: "
            //        + mBluetoothAdapter.getDevice(i).getAddress().toString(), Toast.LENGTH_SHORT).show();
        }
        */
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress); //set device mac address to UI
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);      //set device connection state to UI
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

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

    private void displayData(String data)
    {
        if (data != null)
        {
            mDataField.setText(data);
        }
    }

    private void sendCommandToDevice(List<BluetoothGattService> gattServices)
    {
        BluetoothGattCharacteristic     readCharacter = null;
        BluetoothGattCharacteristic     writeCharacter = null;

        if (gattServices == null) return;
        // Loops to find available GATT Characteristic.
        for (BluetoothGattService gattService : gattServices)
        {
            readCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            writeCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);
        }

        mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
        if (writeCharacter != null)
        {
            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
            }
            writeCharacter.setValue(Utils.mlcTestFunction());
            Log.i(TAG, Utils.mlcTestFunction().toString());

            mBluetoothLeService.writeCharacteristic(writeCharacter);
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }


    /*
    private void sendCommandToDevice(List<BluetoothGattService> gattServices)
    {
        if (gattServices == null) return;
        // Loops to find available GATT Characteristic.
        for (BluetoothGattService gattService : gattServices)
        {
            mlcBPReadChar = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            mlcBPWriteChar = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);
        }

        mBluetoothLeService.setCharacteristicNotification(mlcBPReadChar, true);
        if (mlcBPWriteChar != null)
        {
            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
            }
            mlcBPWriteChar.setValue(Utils.mlcTestFunction());
            Log.i(TAG, Utils.mlcTestFunction().toString());

            mBluetoothLeService.writeCharacteristic(mlcBPWriteChar);
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }

    */

/*
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.

    private void displayGattServices(List<BluetoothGattService> gattServices)
    {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices)
        {
            //set service to ListArray.
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            mlcBPReadChar = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            mlcBPWriteChar = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
                {
                    if (gattCharacteristic.getUuid().equals(SampleGattAttributes.MLC_BLE_SEVICE_WRITE))
                    {
                        mlcBPWriteChar = gattCharacteristic;
                        Log.i(TAG, "Write:" + mlcBPWriteChar.toString());
                    }
                }

                if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                {
                    if (gattCharacteristic.getUuid().equals(SampleGattAttributes.MLC_BLE_SEVICE_READ))
                    {
                        mlcBPReadChar = gattCharacteristic;
                        Log.i(TAG, "Read:" + mlcBPReadChar.toString());
                    }
                }

                charas.add(gattCharacteristic);
                //set characteristic to ListArray.
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                //debug
                if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(SampleGattAttributes.MLC_BLE_SEVICE_WRITE))
                    Log.i(TAG, "uuid:" + gattCharacteristic.getUuid().toString());
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }
*/
    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private ArrayList<String> getBleAddress2(HashMap<String, Integer> rssiMap)
    {
        ArrayList<String>   address = new ArrayList<String>();
        ArrayList<Integer>  rssi    = new ArrayList<Integer>();

        return address;
    }

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
}
