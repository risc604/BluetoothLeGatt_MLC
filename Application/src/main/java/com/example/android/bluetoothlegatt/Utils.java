package com.example.android.bluetoothlegatt;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by tomcat on 2016/3/9.
 */
public class Utils
{
    private static final String    TAG = Utils.class.getSimpleName();
    private static String txtFileName = new String();

    private static final String   sdCardPath = "/sdcard/";
    private static int     testCmdLength=0;
    //private static String  strName = new String();

    //private static List<String>     deviceList = new ArrayList<>();
    private static List<String>     nameList = new ArrayList<>();
    private static List<Integer>    lengthList = new ArrayList<>();

    public Utils()
    {
        txtFileName = null;
        //deviceList = null;
        nameList = null;
        lengthList = null;
    }

    public static void mlcDelay(int mSecand)
    {
        try
        {
            Thread.sleep(mSecand);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public static void createDefaultFile(String fileName)
    {
        try
        {
            BufferedWriter outputText = new BufferedWriter(new FileWriter(fileName));
            outputText.write("3MW1-4B, 11");
            outputText.newLine();
            outputText.write("A6 BT, 12");
            outputText.newLine();
            outputText.write("BP3GT1-6B, 12");
            outputText.newLine();

            outputText.close();
        }
        catch (IOException ioError)
        {
            ioError.printStackTrace();
        }
    }

    //public static boolean readTextFile(String fileName, List<String> deviceList)
    public static boolean readINIFile(String fileName)
    {
        List<String>     deviceList = new ArrayList<>();

        try
        {
            BufferedReader inputText = new BufferedReader(new FileReader(fileName));
            String	tmpInfo = null;

            while((tmpInfo = inputText.readLine()) != null )
            {
                deviceList.add(tmpInfo);
            }
            inputText.close();

            Log.d(TAG, "Read ini file.");
            displayList(deviceList);    //debug
            separateNameLength(deviceList);
            return true;
        }
        catch(IOException ioError)
        {
            ioError.printStackTrace();
            //System.out.println("file not exist. to create default mlcDevice.txt by default devices !!");
            createDefaultFile(fileName);
            return false;
        }
    }

    public static boolean separateNameLength(List<String> list)
    {
        //List<String>	nameList = new ArrayList<>();
        //List<Integer>	lengthList = new ArrayList<>();
        String[]		tmpStr=null;
        if(list.isEmpty())
            return false;

        nameList.clear();
        lengthList.clear();
        for(int i=0; i<list.size(); i++)
        {
            tmpStr = list.get(i).split(",");
            //nameList.add(tmpStr[0].replaceAll("\\s+", ""));
            nameList.add(tmpStr[0]);
            lengthList.add(Integer.decode(tmpStr[1].replaceAll("\\s+", "")));
        }

        Log.d(TAG, "get Device name List.");
        //System.out.println("get Device name:  ");
        displayList(nameList);  //debug
        displayList(lengthList);
        return(true);
    }

    public static ArrayList<String> getDeviceNameList()
    {
        return (ArrayList<String>) nameList;
    }

    public static List<Integer> getLengthList()
    {
        return lengthList;
    }

    public static void setCommandIndex(int cmdIndex)
    {
        testCmdLength = lengthList.get(cmdIndex);
    }

    //public static void displayList(List<String> listData)
    public static void displayList(List<?> listData)
    {
        if(listData.isEmpty())
            return;

        for(int i=0; i<listData.size(); i++)
        {
            //System.out.println("Data[" + i + "]= " + listData.get(i));
            Log.d(TAG, "list[" + i + "]= " + listData.get(i));
        }
    }

    /*
    public static void displayInt(List<Integer> listData)
    {
        if(listData.isEmpty())
            return;

        for(int i=0; i<listData.size(); i++)
        {
            //System.out.println("Int[" + i + "]= " + listData.get(i));
            Log.d(TAG, "Int[" + i + "]= " + listData.get(i));
        }
    }
    */


    /*
    public static final byte[] makeDateTimeString()
    {
        Calendar mCalendar = Calendar.getInstance();
        byte[]  tmpBytep = {(byte)(mCalendar.get(Calendar.YEAR)-2000),
                (byte)(mCalendar.get(Calendar.MONTH)+1),
                (byte)(mCalendar.get(Calendar.DATE)),
                (byte)(mCalendar.get(Calendar.HOUR)),
                (byte)(mCalendar.get(Calendar.MINUTE)),
                (byte)(mCalendar.get(Calendar.SECOND))
        };
        return tmpBytep;

    */

    /*
    private static final void makeDefaultFile(File fileName)
    {
        try
        {
            fileName.createNewFile();
            FileWriter  iniWrite = new FileWriter(fileName);
            BufferedWriter  iniBufferWriter = new BufferedWriter(iniWrite);

            // writr CB2, A6 BT as default.
            iniBufferWriter.write("Name=3MW1-4B");
            iniBufferWriter.newLine();
            iniBufferWriter.write("Length=11");
            iniBufferWriter.newLine();

            iniBufferWriter.write("#Name=A6 BT");
            iniBufferWriter.newLine();
            iniBufferWriter.write("#Length=12");
            iniBufferWriter.newLine();

            iniBufferWriter.write("#Name=BP3GT1-6B");
            iniBufferWriter.newLine();
            iniBufferWriter.write("#Length=12");
            iniBufferWriter.newLine();
            iniBufferWriter.flush();
            iniBufferWriter.close();
            //return true;
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            Log.e(TAG, "makeDefaultFile():" + e);
        }
        //return false;
    }


    //public static final String mlcGetDeviceName(String fileName, Context context)
    public static final String mlcGetTestDevice(String fileName, Context context)
    {
        File    iniFile;    //= new File(sdCardPath + fileName);;
        String  strName = new String();

        strName = null;
        try
        {
            //iniFile = new File(sdCardPath + "mlcBleDevices.ini");
            iniFile = new File(sdCardPath + fileName);
            if (!iniFile.exists())  //file NOT exist.
            {
                makeDefaultFile(iniFile);
            }

            Properties  p = new Properties();
            p.load(new FileInputStream(iniFile));
            strName = p.getProperty("Name");
            if (strName != null)
            {
                String tmpLeng = p.getProperty("Length");
                Log.d(TAG, "command length: " + tmpLeng);
                testCmdLength = Integer.parseInt(tmpLeng);
                //testCmdLength = Integer.getInteger(p.getProperty("Length"));
            }
            else
            {
                Log.d(TAG, "read ini file fail, No command length.");
            }
        }
        catch (IOException e)
        {
            //iniFile = new File(sdCardPath + fileName);
            //makeDefaultFile(iniFile);

            Toast.makeText(context, "no " + sdCardPath + fileName, Toast.LENGTH_SHORT).show();

            Log.e(TAG, "mlcGetTestDevice():" + e);
            //e.printStackTrace();
        }

        return strName;
    }


    public static final int getCmdLength()
    {
        //if (testCmdLength != 0)
            return testCmdLength;
        //else
        //    return 0;
    }
    */

    //public static final byte[] mlcTestFunction()
    public static final byte[] mlcTestFunction(int fnCMDByte)
    {
        Calendar mCalendar = Calendar.getInstance();
        // int   cmdLength = 11;     //for CB2
        final int   cmdLength = testCmdLength;     //for BLE device

        byte[] cmdByte = { 0x4d, (byte) 0xff, 0x00, 0x08, (byte)fnCMDByte,
                (byte)(mCalendar.get(Calendar.YEAR)-2000),
                (byte)(mCalendar.get(Calendar.MONTH)+1),
                (byte)(mCalendar.get(Calendar.DATE)),
                (byte)(mCalendar.get(Calendar.HOUR)),
                (byte)(mCalendar.get(Calendar.MINUTE)),
                (byte)(mCalendar.get(Calendar.SECOND)),
                0x00, 0x00  };

        byte[] tempByte = new byte[cmdLength];
        switch (cmdLength)      //just for CB2 to shift command head length high byte remove.
        {
            case 11:
                for (int i=0; i<cmdLength; i++)
                {
                    if (i<2)
                    {
                        tempByte[i] = cmdByte[i];
                    }
                    else if (i>=2)  //reject byte 2.
                    {
                        tempByte[i] = cmdByte[i+1];
                    }
                }
                break;

            default:
                tempByte = cmdByte;
                break;
        }

        //for (int i=0; i<cmdLength; i++)
        //    tempByte[i] = cmdByte[i];
        for (int i=0; i<(cmdLength-1); i++)     // make check sum
            tempByte[cmdLength-1] += tempByte[i];

        return tempByte;
    }

    //public static void writeFile(Context context, String fileName, String content)
    //public static void writeFile(Context context, File txtFile, String content)
    public static void writeFile(File txtFile, String content)
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
                Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) +
                Integer.toString(cal.get(Calendar.MINUTE)) +
                Integer.toString(cal.get(Calendar.SECOND)) +
                fileAppendName);
        //*Integer.toString(cal.get(Calendar.MINUTE)) +*/ ".txt";)
    }

    public static void writeTolog(ArrayList<String> sourceList)
    {
        String fileName = makeFileName(".txt");
        txtFileName = sdCardPath + fileName;

        try
        {
            Log.d(TAG, txtFileName);
            FileOutputStream    fout = new FileOutputStream(new File(txtFileName), true);

            for (int i=0; i<sourceList.size(); i++)
            {
                fout.write(sourceList.get(i).getBytes());
            }
            fout.close();
            Log.d(TAG, "Write log file OK.");
        }
        catch (FileNotFoundException e)
        {
            //e.printStackTrace();
            Log.e(TAG, "Write file fail." + e.toString());
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            Log.e(TAG, "file NOT found." + e.toString());
        }
    }

    public static String getFileName()
    {
        return txtFileName;
    }

}


