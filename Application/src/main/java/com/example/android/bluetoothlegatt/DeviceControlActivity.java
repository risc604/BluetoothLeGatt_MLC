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
    private String mDeviceName=null;
    private String mDeviceAddress = null;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeService.LocalBinder  binder;
    private int bpHeaderLeng=0;
    //private static boolean keepReadFlag = false;
    private byte[] headerData = new byte[40];
    private int hIndex = 0;
    private byte mlcCommand;
    //private static int stopIndex=0;

    //private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
    //        new ArrayList<>();

    private ArrayList<BluetoothGattCharacteristic> mGattCharacList;

    //private ArrayList<BluetoothGattCharacteristic> bleGattCharates = new ArrayList<>();
    private boolean mConnected = false;
    private boolean waitDataFlag = false;
    //private BluetoothGattCharacteristic mNotifyCharacteristic;

    //private final String LIST_NAME = "NAME";
    //private final String LIST_UUID = "UUID";

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
            //findGattCharate(mBluetoothLeService.getSupportedGattServices());    //tomcat add
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

            //Log.d("onReceive()", " action state " + binder.getService().toString());
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
                //if (mGattCharacList == null)
                if (mGattCharacList.isEmpty())
                    findGattCharate(mBluetoothLeService.getSupportedGattServices());

                //if(!waitDataFlag)
                    sendCommandToDevice(getCommand());  // MLC test command.
                Log.d(TAG, "after sCTD().");

                    if (getCommand() == 4)
                        goBackDeviceScanActivity(true);

                    //sendCommandToDevice(mBluetoothLeService.getSupportedGattServices());  // MLC test command.
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                boolean nextBLEFlag = false;
                final byte[] dataStr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //nextBLEFlag = displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                //if (dataStr[4]==0x00)
                nextBLEFlag = displayData(dataStr);
                Log.d(TAG, "nextBLEFlag: " + nextBLEFlag);
                //else
                //    displayData(dataStr);

                if (nextBLEFlag)
                {
                    //nextBLEFlag = false;
                    //Utils.mlcDelay(0);
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
        //keepReadFlag = false;

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress); //set device mac address to UI
        mConnectionState = (TextView) findViewById(R.id.connection_state);      //set device connection state to UI
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mGattCharacList = new ArrayList<BluetoothGattCharacteristic>();
        mGattCharacList.clear();
        //mGattCharacList = null;

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    //private int everCount=0;
    @Override
    protected void onResume()
    {
        super.onResume();
        waitDataFlag = false;
        setCommand((byte) 0x03);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
            //findGattCharate(mBluetoothLeService.getSupportedGattServices());    //tomcat add.
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
                //findGattCharate(mBluetoothLeService.getSupportedGattServices());    //tomcat add
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
        int csCheck = 0;
        if (data != null)
        {
            debugFunction(data, data.length);  //debug

            StringBuilder tmpText =  new StringBuilder();
            for (int i=0; i<data.length; i++)
                tmpText.append(String.format("%02X ", data[i]));
            mDataField.setText(tmpText);

            if (data[0] == 'M')
            {
                bpHeaderLeng = headerLength(data);
                hIndex = 0; //debug
            }

            Log.d(TAG, "Header size: " + bpHeaderLeng +
                    ", hIndex: " + hIndex +
                    ", data[4]: " + String.format("0x%02X", data[4]));

            for (int i=0; (i<data.length); i++)
                headerData[hIndex++] = data[i];
            //switch (data[4])      // BP command process

            byte    command = getCommand();
            Log.d(TAG, "dD() cmd : " + command);  //debug
            switch (command)
            {
                case 0x00:      //
                    //for (int i=0; i<data.length; i++)
                    //    headerData[hIndex++] = data[i];
                    debugFunction(headerData, hIndex);

                    int adjustLength = 0;
                    if (mDeviceName.equals("3MW1-4B"))
                    {
                        adjustLength = 3;
                    }
                    else
                    {
                        adjustLength = 4;
                    }


                    Log.d(TAG, "0x00 (hIndex-adjustLength): " + (hIndex-adjustLength) +
                            ", bpHeaderLeng: " + bpHeaderLeng);

                    if ((hIndex-adjustLength) == bpHeaderLeng)
                    {
                        hIndex-=1;      // pointer last address.

                        int csSum = csSum(headerData, hIndex);
                        //for (int i=0; i<(hIndex); i++)
                        //{
                        //    csSum += headerData[i];
                        //}
                        Log.d(TAG, "csSum = " + String.format("%04XH", csSum) +
                                ", headData[" + (hIndex) + "] = " +
                                String.format("%04XH", headerData[hIndex]));

                        if ((csSum & 0x00ff) == (headerData[hIndex] & 0x00ff))
                        {
                            ///hIndex = 0;
                            setWaitBLEData(false);
                            setCommand((byte) 0x04);
                            //binder.getService().initialize();
                            ///binder.getService().broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);

                            return true;
                        }
                    }
                    break;

                case (byte) 0x81:
                case 0x03:      //
                    int csSUM = csSum(headerData, hIndex-1);
                    if (((bpHeaderLeng < 20) &&
                            (csSUM == headerData[hIndex-1])) ||
                            mDeviceName.equals("3MW1-4B"))
                    {
                        ///hIndex = 0;
                        setWaitBLEData(false);
                        setCommand((byte)0x00);
                        binder.getService().broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
                        //BluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
                    }
                    break;
                    // debugFunction(data, data.length);

                case 0x04:
                    return (true);

                default:
                    Log.d(TAG, "String: " + tmpText + "CS: " + csCheck);
                    break;
            }

            //Log.d(TAG, "0xbpHeaderLeng: " + bpHeaderLeng +
            //        ",  (hIndex-3):" + (hIndex-3));


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
        else
        {
            if (mDeviceName.equals("3MW1-4B"))
            {
                ///hIndex = 0;
                setWaitBLEData(false);
                setCommand((byte) 0x00);
                binder.getService().broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            }
            Log.d(TAG, "dD() No data");
        }
        return false;
    }

    private int csSum(byte[] data, int length)
    {
        int tmp = 0;
        for (int i=0; i<length; i++)
            tmp += data[i];
        Log.d(TAG, "csSum(): " + String.format("%04XH", tmp));
        return tmp;
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

    //find Bluetooth gatt characteristic
    private void findGattCharate(List<BluetoothGattService> gattServices)
    {
        /*
        BluetoothGattCharacteristic     readCharacter = null;
        BluetoothGattCharacteristic     writeCharacter = null;
        Log.d(TAG, " 1 fGCh(): rGatt" + readCharacter.getUuid().toString());
        Log.d(TAG, " 1 fGCh(): wGatt" + writeCharacter.getUuid().toString());
        */

        if (gattServices == null)   return;
        ///mConnectionState.setText(R.string.connected);   //for Service state info.

        Log.d(TAG, "find Gatt characteristic.");
        // Loops to find available GATT Characteristic to save global variable
        for (BluetoothGattService gattService : gattServices)
        {
            //readCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ);
            //writeCharacter = gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE);
            mGattCharacList.add(0, gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_READ));
            mGattCharacList.add(1, gattService.getCharacteristic(BluetoothLeService.UUID_MLC_BLE_SERVICE_WRITE));
        }

        Log.d(TAG, "fGCh(): rGatt" + mGattCharacList.get(0).getUuid().toString());
        Log.d(TAG, "fGCh(): wGatt" + mGattCharacList.get(1).getUuid().toString());

        /*
        Log.d(TAG, "fGCh(): rGatt" + readCharacter.getUuid().toString());
        Log.d(TAG, "fGCh(): wGatt" + writeCharacter.getUuid().toString());
        */
        //Log.d(TAG, "findGattCharate(): " + mGattCharacList); //debug
        //bleGattCharates
    }

    private void sendCommandToDevice(byte command)
    {
        Log.d(TAG, "sCTD() rGatt: " + mGattCharacList.get(0).getUuid().toString());
        Log.d(TAG, "sCTD() wGatt: " + mGattCharacList.get(1).getUuid().toString());

        BluetoothGattCharacteristic     readCharacter = mGattCharacList.get(0);
        BluetoothGattCharacteristic     writeCharacter = mGattCharacList.get(1);

        mConnectionState.setText(R.string.connected);   //for Service state info.

        mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
        //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);

        // read gatt characteristic
        if (writeCharacter != null)
        {
            byte[]  tmpCMDResult = Utils.mlcTestFunction(command);
            writeCharacter.setValue(tmpCMDResult);
            Log.d(TAG, "sCTD cmd : " + command);  //debug
            mBluetoothLeService.writeCharacteristic(writeCharacter);

            if (mDeviceName.equals("3MW1-4B"))
            {
                binder.getService().broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE);
            }
            //setWaitBLEData(true);
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");

    }

    /*
    ///private void sendCommandToDevice(List<BluetoothGattService> gattServices)
    private void sendCommandToDevice(byte command)
    {
        Log.d(TAG, "sCTD() rGatt: " + mGattCharacList.get(0).getUuid().toString());
        Log.d(TAG, "sCTD() wGatt: " + mGattCharacList.get(1).getUuid().toString());

        BluetoothGattCharacteristic     readCharacter = mGattCharacList.get(0);
        BluetoothGattCharacteristic     writeCharacter = mGattCharacList.get(1);
        int[]  cmdFlow = {0x03, 0x00, 0x04};

        mConnectionState.setText(R.string.connected);   //for Service state info.

        mBluetoothLeService.setCharacteristicNotification(readCharacter, true);
        //mBluetoothLeService.setCharacteristicNotification(writeCharacter, true);

        // read gatt characteristic
        if (writeCharacter != null)
        {
            for (int i=0; i<cmdFlow.length; i++)
            {
                byte[] tmpCMDResult = Utils.mlcTestFunction(cmdFlow[i]);
                //byte[] tmpCMDResult = Utils.mlcTestFunction(cmdFlow[cmdIdx]);
                //byte[]  tmpCMDResult = Utils.mlcTestFunction(command);
                writeCharacter.setValue(tmpCMDResult);
                //Log.i(TAG, cmdFlow[i]+ " cmd : " +  String.format("%02x", tmpCMDResult) );
                Log.d(TAG, " cmd : " + cmdFlow[i]);  //debug
                //Log.d(TAG, " cmd : " + command);  //debug
                //for (int j=0; j<tmpCMDResult.length; j++)
                //{
                //    Log.i(TAG, String.format("[%02d] = %02X", j, tmpCMDResult[j]));
                //}

                mBluetoothLeService.writeCharacteristic(writeCharacter);
                if((mDeviceName != null) && mDeviceName.matches("BP3GT1-6B") && (cmdFlow[i] == 0x03))
                    Utils.mlcDelay(1000);    //1 s
                else
                    Utils.mlcDelay(100);    // 200ms
            }
            //setWaitBLEData(true);
        }
        else
            Log.e(TAG, "Error, No mlc BP write Charactics");
    }
    */

    private void setWaitBLEData(boolean Flag)
    {
        this.waitDataFlag = Flag;
    }

    public boolean getWaitBLEData()
    {
        return this.waitDataFlag;
    }

    public void setCommand(byte command)
    {
        mlcCommand = command;
        Log.d(TAG, "setC(): " + mlcCommand);
    }

    private byte getCommand()
    {
        return mlcCommand;
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

        if (mDeviceName.equals("3MW1-4B"))
        {
            Log.d(TAG, "3MW1-4B");
            tmp |= data[3];
            Log.d(TAG, "1 tmp : " + tmp);
            tmp <<= 8;
            Log.d(TAG, "2 tmp : " + tmp);
            tmp |= data[2];
            Log.d(TAG, "3 tmp : " + tmp);
        }
        else
        {
            tmp |= data[2];
            Log.d(TAG, "1 tmp : " + tmp);
            tmp <<= 8;
            Log.d(TAG, "2 tmp : " + tmp);
            tmp |= data[3];
            Log.d(TAG, "3 tmp: " + tmp);
        }
        return tmp;
    }
}
