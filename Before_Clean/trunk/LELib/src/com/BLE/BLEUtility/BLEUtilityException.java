package com.BLE.BLEUtility;

public class BLEUtilityException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//constant 
	static public final int CONN_BADTARGET = 0;
	static public final int CONN_ALREADY_CONN = 1;
	static public final int BT_NOTENABLE = 2;
	static public final int BT_CANT_GETDEVICE = 3;
	static public final int CHAR_NOTREADY = 4;
	static public final int CHAR_WRITEFAIL = 5;
	static public final int CHAR_READFAIL = 6;
	
	//data members
	private int mErrorCode = -1;
	
	//constructor
	BLEUtilityException(int errorCode)
	{
		mErrorCode = errorCode;
	}
	
	@Override
	public String getMessage() {
		switch (mErrorCode){
		case CONN_BADTARGET:
			return "bad connection address";
		case CONN_ALREADY_CONN:
			return "already has connection";
		case BT_NOTENABLE:
			return "not enable bluetooth";
		case BT_CANT_GETDEVICE:
			return "can`t get remote le device";
		case CHAR_NOTREADY:
			return "Characteristic is no ready";
		case CHAR_WRITEFAIL:
			return "Write Characteristic fail";
		case CHAR_READFAIL:
			return "Read Characteristic fail";
		default :
		}
		return super.getMessage();
	}
}
