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
    private ArrayList<String>   finalBLEList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        finalBLEList = new ArrayList<>();
        finalBLEList = null;
        Intent  intent = getIntent();
        finalBLEList = intent.getStringArrayListExtra(RESULT_LIST);

        Utils.writeTolog(finalBLEList);

        Log.d(TAG, "final BLE List: " + finalBLEList.toString());

        tvListInfo = (TextView)findViewById(R.id.textView1);
        tvListInfo.setText(" ");
        btnEmail = (Button)findViewById(R.id.btnEMail);
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     * This is a good place to begin animations, open exclusive-access devices
     * (such as the camera), etc.
     * <p/>
     * <p>Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front.  Use {@link #onWindowFocusChanged} to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestoreInstanceState
     * @see #onRestart
     * @see #onPostResume
     * @see #onPause
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        for (int i=0; i<finalBLEList.size(); i++)
        {
            Log.d(TAG, "final BLE list [" + i + "] : " + finalBLEList.get(i));
            tvListInfo.append(finalBLEList.get(i));
        }
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed.  The counterpart to
     * {@link #onResume}.
     * <p/>
     * <p>When activity B is launched in front of activity A, this callback will
     * be invoked on A.  B will not be created until A's {@link #onPause} returns,
     * so be sure to not do anything lengthy here.
     * <p/>
     * <p>This callback is mostly used for saving any persistent state the
     * activity is editing, to present a "edit in place" model to the user and
     * making sure nothing is lost if there are not enough resources to start
     * the new activity without first killing this one.  This is also a good
     * place to do things like stop animations and other things that consume a
     * noticeable amount of CPU in order to make the switch to the next activity
     * as fast as possible, or to close resources that are exclusive access
     * such as the camera.
     * <p/>
     * <p>In situations where the system needs more memory it may kill paused
     * processes to reclaim resources.  Because of this, you should be sure
     * that all of your state is saved by the time you return from
     * this function.  In general {@link #onSaveInstanceState} is used to save
     * per-instance state in the activity and this method is used to store
     * global persistent data (in content providers, files, etc.)
     * <p/>
     * <p>After receiving this call you will usually receive a following call
     * to {@link #onStop} (after the next activity has been resumed and
     * displayed), however in some cases there will be a direct call back to
     * {@link #onResume} without going through the stopped state.
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onResume
     * @see #onSaveInstanceState
     * @see #onStop
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        tvListInfo.setText("");
    }

    /**
     * Perform any final cleanup before an activity is destroyed.  This can
     * happen either because the activity is finishing (someone called
     * {@link #finish} on it, or because the system is temporarily destroying
     * this instance of the activity to save space.  You can distinguish
     * between these two scenarios with the {@link #isFinishing} method.
     * <p/>
     * <p><em>Note: do not count on this method being called as a place for
     * saving data! For example, if an activity is editing data in a content
     * provider, those edits should be committed in either {@link #onPause} or
     * {@link #onSaveInstanceState}, not here.</em> This method is usually implemented to
     * free resources like threads that are associated with an activity, so
     * that a destroyed activity does not leave such things around while the
     * rest of its application is still running.  There are situations where
     * the system will simply kill the activity's hosting process without
     * calling this method (or any others) in it, so it should not be used to
     * do things that are intended to remain around after the process goes
     * away.
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onPause
     * @see #onStop
     * @see #finish
     * @see #isFinishing
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        tvListInfo.setText("");
        finalBLEList.clear();
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key.  The default implementation simply finishes the current activity,
     * but you can override this to do whatever you want.
     */
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        //Intent intent = new Intent(ResultActivity.class, DeviceScanActivity.class);
        //startActivity(intent);
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
        it.putExtra(Intent.EXTRA_SUBJECT, "BLE test log file." + logfile);
        it.putExtra(Intent.EXTRA_TEXT, "To see attach fil is form BLE test log.");
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
