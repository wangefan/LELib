package com.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import com.BLE.BLEUtility.MyLog;

//Cmd structure
// HEADER[2 byte] + cmd length[1 byte] + cmds(include 0d)[xx bytes] + crc[1 byte] 
public class CmdProcObj {
	static private final String mTag = "CmdProcObj";
	static private byte[] mWriteHeader = new byte[]{35, 04};	//0x23, 0x04
	static private byte[] mReadHeader = new byte[]{35, 05};		//0x23, 0x05
	static private byte getCRC(byte [] cmdBArr) {
		byte sum = 0;
		for(byte b : cmdBArr)
			sum += b;
		byte crc = (byte) (0 - sum);
		return crc;
	}
	
	static public byte [] addCRC(String cmd, boolean bWrite) {
		String strHex = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if(bWrite)
				baos.write(mWriteHeader);
			else 
				baos.write(mReadHeader);
			baos.write((byte) (cmd.length() + 1));
			baos.write(cmd.getBytes());
			baos.write((byte) '\r');
			byte crc = getCRC(baos.toByteArray());
			baos.write(crc);
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		strHex = String.format("%x", new BigInteger(1, baos.toByteArray()));
		MyLog.d(mTag, "strCmdFull hex = " + strHex);
		return baos.toByteArray();
	}
	
	//calculate crc and extract command response.
	static public byte[] calCRC(byte[] cmdRes, boolean bWrite) {
		if(cmdRes == null || cmdRes.length <= 3)
			return null;
		
		if(bWrite) {
			if(cmdRes[0] != mWriteHeader[0] || 
			   cmdRes[1] != mWriteHeader[1])
				return null;
		}
		else { 
			if(cmdRes[0] != mReadHeader[0] || 
			   cmdRes[1] != mReadHeader[1])
				return null;
		}
		byte cmdLength = cmdRes[2];
		//header(2) + length(1) + cmdlength + crc(1) - 1<= get index 
		byte crcIdx = (byte) (2 + 1 + cmdLength + 1 - 1);
		byte crcSrc = cmdRes[crcIdx];
		ByteArrayOutputStream cmdNoCRC = new ByteArrayOutputStream(); 
		cmdNoCRC.write(cmdRes, 0, crcIdx);
		if(crcSrc == getCRC(cmdNoCRC.toByteArray()))
		{
			cmdNoCRC.reset();
			//Extract read string, remove header (first 2 byte) and CRC(last byte)
			cmdNoCRC.write(cmdRes, 3, cmdLength - 1);
			return cmdNoCRC.toByteArray();
		}
		
		return null;
	}
}
