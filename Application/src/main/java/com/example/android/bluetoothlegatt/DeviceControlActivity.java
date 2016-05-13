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
    //public static final int OK_ADDRESS = 200;
    public static final String TEST_STATE = "STATE";

    private TextView mConnectionState;
    private TextView mDataField;
    private static String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeService.LocalBinder  binder;
    private static int bpHeadDataLeng=0;
    private static boolean keepReadFlag = false;
    private byte[] headerData = new byte[40];
    private static int stopIndex=0;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;
    //private BluetoothGattCharacteristic mNotifyCharacteristic;

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
    // COUNTDOWN_BR: set service time out timer.
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
                final byte[] dataStr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //nextBLEFlag = displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                nextBLEFlag = displayData(dataStr);

                if (nextBLEFlag)
                {
                    Utils.mlcDelay(600);
                    goBackDeviceScanActivity(true);
                }
            }
            else if (BluetoothLeService.COUNTDOWN_BR.equals(action))
            {
                //boolean timeOutState = false;
                boolean serviceState = serviceTimeOut(intent);
                if (serviceState)
                {
                    Log.d(TAG, "event COUNTDOWN_BR: " + serviceState);
                    //serviceTimerOut = false;
                    goBackDeviceScanActivity(false);    // test device address fail.
                    //serviceFailProcess();
                    //onBackPressed();
                    //onDestroy();
                }
            }
        }
    };

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
        keepReadFlag = false;

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress); //set device mac address to UI
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

    //private void displayData(String data)
    //private boolean displayData(String data)
    private boolean displayData(byte[] data)
    {
        //debugFunction(data);
        int csCheck = 0;

        /*
        for (int i=0; i<(data.length-1); i++)
        {
            csCheck += data[i];
        }
        Log.d(TAG, "raw data: " + data + "CS: " + csCheck);
        */
        if (data != null)
        {
            StringBuilder tmpText =  new StringBuilder();
            for (int i=0; i<data.length; i++)
                tmpText.append(String.format("%02X ", data[i]));
            mDataField.setText(tmpText);

            /*
            if ((data[0] == 'M') && (bpHeadDataLeng ==0))     // read header
            {
                bpHeadDataLeng  = headerLength(data);
                Log.d(TAG, "bpHeadDataLeng: " + bpHeadDataLeng);
                if (bpHeadDataLeng > 20)   keepReadFlag = true;
                stopIndex = 0;
                for (int i=0; i<bpHeadDataLeng; i++)
                    headerData[i] = 0;
            }
            else
            {
                keepReadFlag = false;
            }


             int i=0;
            //for (int i = 0; ((i < bpHeadDataLeng) && (stopIndex < bpHeadDataLeng)); i++)
            Log.d(TAG, "1 Stop Index: " + stopIndex);


            //while((stopIndex<bpHeadDataLeng))
            //{
            //    headerData[stopIndex++] = data[i++];
            //}

            Log.d(TAG, "2 Stop Index: " + stopIndex);

            Log.d(TAG, "Data");
            debugFunction(data, bpHeadDataLeng);
            Log.d(TAG, "header Data");
            debugFunction(headerData, bpHeadDataLeng);

            if (stopIndex==bpHeadDataLeng)  bpHeadDataLeng = 0;

            */
            // check BP reply to APP data format. form CB2, A6BT, A6BT(R8C)
            //if ((headData[4] == 0x00) && (data.matches("M0") || data.matches("M1") ||
            //     data.matches("M2") || data.matches("M3") || data.matches("M4") ||
            //     data.matches("M5") || data.matches("M6") || data.matches("M7")))
            //if ((data != null) && (data.matches("M0")));
            //if ((headData[0] == 'M') || data.matches("M0") || data.matches("M1") /*&& (headData[]*/);
            //if (((headData[4] == 0x00) && (headData[0]=='M')) && ((headData[1] == 0x30) ||
            //     (headData[1] == 0x31) || (headData[1] == 0x32) || (headData[1] == 0x33) ||
            //     (headData[1] == 0x34) || (headData[1] == 0x35) || (headData[1] == 0x36) ||
            //     (headData[1] == 0x37)))


            switch (data[4])      // BP command process
            {
                case 0x00:      //
                    if ((data[0] == 'M') && (data[data.length-1] == csCheck))
                    {
                        return true;
                    }

                case (byte) 0x81:
                case 0x03:      //
                   // debugFunction(data, data.length);

                default:
                    Log.d(TAG, "String: " + tmpText + "CS: " + csCheck);
                    break;
            }

            /*
            if ((data[4]==0x00) && (data[0]=='M') && (data[data.length-1] == csCheck))
            {
                Utils.mlcDelay(100);
                return true;
            }
            else
                Log.d(TAG, "String: " + tmpText + "CS: " + csCheck);
            */
        }
        return false;
    }

    private boolean serviceTimeOut(Intent intent)
    {
        //long millisUntilFinished = intent.getLongExtra("countdown", 0);
        long lastSecand = intent.getLongExtra("countdown", 0);

        //switch (millisUntilFinished)
        switch ((int)lastSecand)
        {
            case 0:
                Log.d(TAG, "service time out.  " + lastSecand);
                return true;

            default:
                mConnectionState.append(" " + lastSecand);
                Log.d(TAG, "Countdown seconds: " + lastSecand);
                return false;
        }

        /*
        boolean serviceTimerOut = false;

        if ((intent.getExtras() != null) && !serviceTimerOut)
        {
            long millisUntilFinished = intent.getLongExtra("countdown", 0);
            String tmpSec = String.valueOf(millisUntilFinished);
            serviceTimerOut = intent.getBooleanExtra("TimeOut", false);
            mConnectionState.append(" " + tmpSec);
            Log.i(TAG, "Countdown seconds: " + tmpSec + "s, time out:" + serviceTimerOut);
            //Log.i(TAG, "service time out:" + serviceTimerOut);
        }
        return serviceTimerOut;
        */
    }

    private void sendCommandToDevice(List<BluetoothGattService> gattServices)
    {
        BluetoothGattCharacteristic     readCharacter = null;
        BluetoothGattCharacteristic     writeCharacter = null;
        int[]  cmdFlow = {0x03, 0x00, 0x04};

        if (gattServices == null)   return;
        mConnectionState.setText(R.string.connected);   //for Service state info.

        // Loops to find available GATT Characteristic.
        for (BluetoothGattService gattService : gattServices)
        {
            readCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            writeCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);
        }

        mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
        //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);

        if (writeCharacter != null)
        {
            for (int i=0; i<cmdFlow.length; i++)
            {
                //mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
                byte[] tmpCMDResult = Utils.mlcTestFunction(cmdFlow[i]);
                writeCharacter.setValue(tmpCMDResult);
                //Log.i(TAG, cmdFlow[i]+ " cmd : " +  String.format("%02x", tmpCMDResult) );
                Log.i(TAG, " cmd : " + cmdFlow[i]);  //debug
                /*
                for (int j=0; j<tmpCMDResult.length; j++)
                {
                    Log.i(TAG, String.format("[%02d] = %02X", j, tmpCMDResult[j]));
                }
                */
                mBluetoothLeService.writeCharacteristic(writeCharacter);
                if((mDeviceName != null) && mDeviceName.matches("BP3GT1-6B") && (cmdFlow[i] == 0x03))
                    Utils.mlcDelay(1000);    //1 s
                else
                    Utils.mlcDelay(30);    // 300ms
            }


            /*
            //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);
            byte[] tmpCMDResult = Utils.mlcTestFunction(0x03);
            writeCharacter.setValue(tmpCMDResult);
            Log.i(TAG, "0x03 cmd : " + tmpCMDResult.toString());
            mBluetoothLeService.writeCharacteristic(writeCharacter);
            Utils.mlcDelay(300);    //300 ms

            //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);
            tmpCMDResult = Utils.mlcTestFunction(0x00);
            writeCharacter.setValue(tmpCMDResult);
            Log.i(TAG, "0x00 cmd : " + tmpCMDResult.toString());
            mBluetoothLeService.writeCharacteristic(writeCharacter);
            Utils.mlcDelay(200);    //200 ms

            //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);
            tmpCMDResult = Utils.mlcTestFunction(0x04);
            writeCharacter.setValue(tmpCMDResult);
            Log.i(TAG, "0x04 cmd : " + tmpCMDResult.toString());
            mBluetoothLeService.writeCharacteristic(writeCharacter);
            Utils.mlcDelay(100);    //100 ms
            */
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }

    //private void goBackDeviceScanActivity()
    private void goBackDeviceScanActivity(boolean state)
    {
        mBluetoothLeService.disconnect();
        Utils.mlcDelay(100);    //100

        Intent  intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(DeviceControlActivity.TEST_STATE, state);
        Log.d(TAG, "DeviceAddress: " + mDeviceAddress);
        Log.d(TAG,  " State:" + state);

        if (state == false)
        {
            Log.d(TAG, "Bluetooth Le service STOP!!");
            mDataField.setText("BP Bluetooth Error !!  Restart APP.");
            Utils.mlcDelay(100);
        }
        setResult(DeviceScanActivity.REQUEST_TEST_FUNCTION, intent);
        finish();
    }

    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.COUNTDOWN_BR);
        return intentFilter;
    }

    private Integer getBleRssi(HashMap<String, Integer> rssiMap, ArrayList<String> deviceAddress)
    {
        return (rssiMap.get(deviceAddress));
    }

    private void debugFunction(byte[] data, int length)
    {
        byte[] headData = new byte[data.length];
        headData = data;

        for (int i=0; i<length; i++)
        {
            Log.d(TAG, length + "   [" + i + "] = " + "0x" +
                    //Integer.toString(headData[i], 16) + "  ");
                    //Integer.toHexString(headData[i]) + "  ");
                    String.format("%02x", headData[i]) + "  ");
        }
    }

    private int headerLength(byte[] data)
    {
        int tmp =0;

        tmp |= data[2];
        Log.d(TAG, "1 tmp : " + tmp);
        tmp <<= 8;
        Log.d(TAG, "2 tmp : " + tmp);
        tmp |= data[3];
        Log.d(TAG, "3 tmp: " + tmp);

        return tmp;
    }
}
