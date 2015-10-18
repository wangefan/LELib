package com.BLE.BLEUtility;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

public class MyLog 
{	
	public static boolean _DEBUG = false; 
	private static PrintWriter mMsgWriter = null;
	public static void init()
    {
    	if(_DEBUG) {
    		File path = Environment.getExternalStoragePublicDirectory(
    	            Environment.DIRECTORY_DOWNLOADS);
    	
    		String fileName = "Integral" + "_" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US).format(new Date(System.currentTimeMillis()));
    		fileName += ".txt";
    		final File file = new File(path, fileName);
    		try {
				mMsgWriter = new PrintWriter(file.getAbsolutePath(), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
	public static void d(String tag, String msg) {
		if(_DEBUG) {
			Log.d("BLE", "[" + tag + "]:" + msg);	
			String msgFile = String.format("[%s]:%s", tag, msg);
			mMsgWriter.println(msgFile);
		}
    }
	
	public static void d(String tag, String msg, Throwable tr) {
		if(_DEBUG) {
			Log.d("BLE", "[" + tag + "]:" + msg, tr);
			String msgFile = String.format("[%s]:%s", tag, msg);
			mMsgWriter.println(msgFile);
		}
    }
	
	public static void e(String tag, String msg) {
		if(_DEBUG) {
			Log.e("BLE", "[" + tag + "]:" + msg);
			String msgFile = String.format("[%s]:%s", tag, msg);
			mMsgWriter.println(msgFile);
		}
    }
	
	public static void e(String tag, String msg, Throwable tr) {
		if(_DEBUG) {
			Log.e("BLE", "[" + tag + "]:" + msg, tr);
			String msgFile = String.format("[%s]:%s", tag, msg);
			mMsgWriter.println(msgFile);
		}
    }
	public static void close() {
		if(mMsgWriter != null) {
			mMsgWriter.close();
			mMsgWriter = null;
		}
    }
}
