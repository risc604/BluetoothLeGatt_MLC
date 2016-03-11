package com.example.android.bluetoothlegatt;

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

        return cmdByte;
    }
}
