package com.utility;

public class CmdProcObj {
	static private String mWriteHeader = new String(new byte[]{35, 04});
	static private String mReadHeader = new String(new byte[]{36, 04});
	static public String proc(String cmd) {
		cmd += "\r";
		byte bl = (byte) cmd.length();
		byte sum = (byte) (35 + 4 + bl);
		byte [] cmdBArr = cmd.getBytes();
		for(byte b : cmdBArr)
			sum += b;
		byte crc = (byte) (0 - sum);
		String result = mWriteHeader + new String(new byte[]{bl}) + cmd + new String(new byte[]{crc});
		return result;
	}
}
