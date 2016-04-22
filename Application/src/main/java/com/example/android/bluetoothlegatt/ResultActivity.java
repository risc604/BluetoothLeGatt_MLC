package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ResultActivity extends Activity
{
    private final String    TAG = ResultActivity.class.getSimpleName();
    public static final String     RESULT_LIST = "RESULT_LIST";

    TextView    tvListInfo;
    Button      btnEmail;
    String      testDeviceName=null;
    private ArrayList<String>   finalBLEList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        finalBLEList = new ArrayList<>();
        finalBLEList = null;
        Intent  intent = getIntent();

        testDeviceName = intent.getStringExtra("DeviceName");
        getActionBar().setTitle(testDeviceName);
        finalBLEList = intent.getStringArrayListExtra(RESULT_LIST);

        Utils.writeTolog(finalBLEList);
        Log.d(TAG, "final BLE List: " + finalBLEList.size());

        tvListInfo = (TextView)findViewById(R.id.textView1);
        //tvListInfo.setText("");
        btnEmail = (Button)findViewById(R.id.btnEMail);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String  tmpItem;
        for (int i=0; i<finalBLEList.size(); i++)
        {
            tmpItem = finalBLEList.get(i);
            //Log.d(TAG, "final BLE list [" + Integer.toString(i) + "] : " + finalBLEList.get(i));
            Log.d(TAG, "final_list [" + i + "] : " + tmpItem);
            tvListInfo.append(tmpItem);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        tvListInfo.setText("");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        tvListInfo.setText("");
        finalBLEList.clear();
    }

    public void onBtnClick(View view)
    {
        Log.d(TAG, "onBtnClick()");
        sendEMail();
    }

    private void sendEMail()
    {
        String logfile = Utils.getFileName();
        Log.i(TAG, "sendEMail()");
        Log.i(TAG, "log file path: " + logfile);
        Intent it = new Intent(Intent.ACTION_SEND);
        it.putExtra(Intent.EXTRA_SUBJECT,  testDeviceName + "  BLE test log file." + logfile);
        it.putExtra(Intent.EXTRA_TEXT, Uri.parse("file://" + logfile));
        it.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + logfile));
        it.setType("text/plain");

        Log.i(TAG, "send intent data OK");
        try
        {
            startActivity(Intent.createChooser(it, "Choose Email Client"));
        }
        catch (android.content.ActivityNotFoundException ext)
        {
            Toast.makeText(getBaseContext(), "An Error Happened ", Toast.LENGTH_SHORT).show();
        }
    }
}
