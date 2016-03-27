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
            //mBLEServiceList.add(0, mBluetoothLeService);
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
                //String  data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
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
        final Bundle bundle = intent.getExtras();
        mDeviceName = bundle.getString(this.EXTRAS_DEVICE_NAME);
        mDeviceAddress = bundle.getString(this.EXTRAS_DEVICE_ADDRESS);

        //mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        //mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        /*
        //List<String>                devicesAddrList =
        //        (ArrayList<String>) intent.getSerializableExtra("BLE_ADDRESS");
        ///devicesAddrList = (ArrayList<String>) intent.getSerializableExtra("BLE_ADDRESS");
        ///HashMap<String, Integer>    bleDeviceInfoMap =
        ///        (HashMap<String, Integer>)intent.getSerializableExtra("BLE_DEVICE");
        ///bleDevices = devicesAddrList.size();

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
        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);      //set device connection state to UI
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    //private int everCount=0;
    @Override
    protected void onResume()
    {
        super.onResume();

        //goBackDeviceScanActivity();
        //MLC_TestFunction(0);


        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        //Log.i(TAG, "Resume: " + everCount++);
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

    /*
    //private void MLC_TestFunction(int state)
    //{
    //    // make mulit device ble service intent & start service to connection.
    //    //ArrayList<Intent>   intentsList = null;
    //    int devIndex = 0;
    //    int step = state;
    //    Intent gattServiceIntent = null;
    //
    //    Log.i(TAG, "BLE: " + devicesAddrList.size());
    //    while (step < 10)
    //    {
    //        Log.i(TAG, "Step: " + step);
    //        switch (step)
    //        {
    //            case 0:     // make test ble mac address
    //                //flagOK = false;
    //                mDeviceAddress = devicesAddrList.get(devIndex);
    //                ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
    //                mConnectionState = (TextView) findViewById(R.id.connection_state);      //set device connection state to UI
    //                mDataField = (TextView) findViewById(R.id.data_value);
    //                mDeviceName = mDeviceAddress;
    //
    //                getActionBar().setTitle(mDeviceName);
    //                getActionBar().setDisplayHomeAsUpEnabled(true);
    //                // make ble service receiver
    //                // intentsList.add(devIndex, new Intent(this, BluetoothLeService.class));
    //                gattServiceIntent = new Intent(this, BluetoothLeService.class);
    //                // check data or receive data ok
    //                //bindService(intentsList.get(devIndex), mServiceConnection, BIND_AUTO_CREATE);
    //                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    //                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    //
    //            case 1:
    //                if (mBluetoothLeService != null)
    //                {
    //                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
    //                    Log.d(TAG, "Connect request result=" + result);
    //                    //intentsList.remove(devIndex);
    //                }
    //
    //                if (flagOK)
    //                {
    //                    flagOK = false;
    //                    step = 2;
    //                }
    //                //else if (mBluetoothLeService == null)
    //                //    step = 2;
    //                //break;
    //
    //            case 2:
    //                unbindService(mServiceConnection);
    //                mBluetoothLeService = null;
    //                step = 3;
    //                break;
    //
    //            case 3:
    //                /*
    //                if (intentsList != null)
    //                {
    //                    step = 0;
    //                }
    //                else if (intentsList == null)
    //                {
    //                    step = 11;  // over to exist MLC test.
    //                }
    //                else
    //                {
    //                    Log.e(TAG, "MLC test function STEP Error.");
    //                }
    //                */
    //                devIndex++;
    //                if (devIndex < devicesAddrList.size())
    //                    step = 0;
    //                else if (devIndex >= devicesAddrList.size())
    //                    step = 11;
    //                else
    //                {
    //                    Log.e(TAG, "MLC test function STEP Error.");
    //                }
    //                break;
    //
    //            default:
    //                break;
    //        }
    //    }
    //    /*
    //    for (int i=0; i<bleDevices; i++)
    //    {
    //        // check data or receive data ok
    //        bindService(intentsList.get(i), mServiceConnection, BIND_AUTO_CREATE);
    //        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    //    }
    //    */
    //
        //disconnect ble service or gatt server
        // to restart.
    //}


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
            if (data.matches("M0"));
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                //onBackPressed();
                //onDestroy();
                goBackDeviceScanActivity();
                //Toast.makeText(this, "Get BP ML string.", Toast.LENGTH_SHORT).show();

            }
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
            writeCharacter.setValue(Utils.mlcTestFunction());
            Log.i(TAG, Utils.mlcTestFunction().toString());
            mBluetoothLeService.writeCharacteristic(writeCharacter);
            /*   try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
            }
            */
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }


    private void goBackDeviceScanActivity()
    {
        //unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        //mBluetoothLeService.disconnect();
        //mBluetoothLeService = null;


        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        Intent  intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
        //intent.putExtra(this.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        Bundle bundle = new Bundle();

        bundle.putString(this.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        Log.d(TAG, "DeviceAddress: " + mDeviceAddress);
        //intent.putExtra(this.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);

        intent.putExtras(bundle);
        setResult(OK_ADDRESS, intent);
        finish();

        //startActivity(intent);

    }


    /*
    private void sendNextCommand()
    {
        mDeviceAddress = devicesAddrList.get(1);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "2 result=" + result);
        }
    }

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
