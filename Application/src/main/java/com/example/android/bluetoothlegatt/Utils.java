package com.example.android.bluetoothlegatt;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by tomcat on 2016/3/9.
 */
public class Utils
{
    public static final byte[] mlcTestFunction()
    {
        Calendar mCalendar = Calendar.getInstance();
        final int   cmdLength = 11;     //for CB2
        final int   fnCMDByte = 0x00;   // for CB2 to read all data & send data/time.

        byte[] cmdByte = { 0x4d, (byte) 0xff, 0x08, (byte)fnCMDByte,
                (byte)(mCalendar.get(Calendar.YEAR)-2000),
                (byte)(mCalendar.get(Calendar.MONTH)+1),
                (byte)(mCalendar.get(Calendar.DATE)),
                (byte)(mCalendar.get(Calendar.HOUR)),
                (byte)(mCalendar.get(Calendar.MINUTE)),
                (byte)(mCalendar.get(Calendar.SECOND)),
                0x00, 0x00  };

        for (int i=0; i<(cmdLength-1); i++)     // check sum
            cmdByte[cmdLength-1] += cmdByte[i];

        byte[] tempByte = new byte[cmdLength];

        //for (int i=0; i<cmdLength; i++)
        //    tempByte[i] = cmdByte[i];
        tempByte = cmdByte;
        return tempByte;
    }

    //public static void writeFile(Context context, String fileName, String content)
    public static void writeFile(Context context, File txtFile, String content)
    {
        try
        {
            //Environment.getDataDirectory()
            //FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_APPEND);
            FileOutputStream fos = new FileOutputStream(txtFile, true);
            fos.write(content.getBytes());
            //fos.flush();
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //public static File openFile(File fileName, String fileAppendName)
    public static String makeFileName(String fileAppendName)
    {
        Calendar cal = Calendar.getInstance();
        //String txtFileName = Integer.toString(cal.get(Calendar.YEAR)) +
        //        Integer.toString((cal.get(Calendar.MONTH))+1) +
        //        Integer.toString(cal.get(Calendar.DATE)) +
        //        Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + fileAppendName;
        //       //*Integer.toString(cal.get(Calendar.MINUTE)) +*/ ".txt";

        return( Integer.toString(cal.get(Calendar.YEAR)) +
                Integer.toString((cal.get(Calendar.MONTH))+1) +
                Integer.toString(cal.get(Calendar.DATE)) +
                Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + fileAppendName);
        //*Integer.toString(cal.get(Calendar.MINUTE)) +*/ ".txt";)
    }
}
