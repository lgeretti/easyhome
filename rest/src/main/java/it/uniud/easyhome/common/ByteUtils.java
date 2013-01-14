package it.uniud.easyhome.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteUtils {
	
	public static short getShort(ByteArrayInputStream is, Endianness endianness) {
		if (endianness == Endianness.BIG_ENDIAN)
			return (short)((short)(is.read() << 8) + (((short)is.read()) & 0xFF));
		else
			return (short)((((short)is.read()) & 0xFF) + (short)(is.read() << 8));
	}
	
	public static short getShort(byte[] bytes, int start, Endianness endianness) {
		if (start < 0 || start > bytes.length-2)
			throw new IndexOutOfBoundsException();
		if (endianness == Endianness.BIG_ENDIAN)
			return (short)((short)(bytes[start] << 8) + (((short)bytes[start+1]) & 0xFF));
		else
			return (short)((short)(bytes[start+1] << 8) + (((short)bytes[start]) & 0xFF));
	}
	public static short getShort(byte[] bytes, Endianness endianness) {
		if (bytes.length != 2)
			throw new IndexOutOfBoundsException();
		return getShort(bytes,0,endianness);
	}
	public static byte[] getBytes(short val, Endianness endianness) {
		byte[] result = new byte[2];
		if (endianness == Endianness.BIG_ENDIAN) {
			result[0] = (byte)((val >>> 8) & 0xFF);
			result[1] = (byte)(val & 0xFF);
		} else {
			result[1] = (byte)((val >>> 8) & 0xFF);
			result[0] = (byte)(val & 0xFF);
		}
		return result;
	}
	
	public static long getLong(ByteArrayInputStream is, Endianness endianness) {
		long result = 0;
		if (endianness == Endianness.BIG_ENDIAN) {
			for (int i=56; i>=0; i-=8)
				result += ((long)(is.read() & 0xFF))<<i;
		} else {
			for (int i=0; i<=56; i+=8)
				result += ((long)(is.read() & 0xFF))<<i;
		}
		return result;
	}	
	public static long getLong(byte[] bytes, int start, Endianness endianness) {
		if (start < 0 || start > bytes.length-8)
			throw new IndexOutOfBoundsException();
		long result = 0;
		if (endianness == Endianness.BIG_ENDIAN) {
			for (int i=56, j=0; i>=0; i-=8, j++)
				result += ((long)(bytes[j] & 0xFF))<<i;
		} else {
			for (int i=56, j=7; i>=0; i-=8, j--)
				result += ((long)(bytes[j] & 0xFF))<<i;
		}
		return result;
	}
	public static long getLong(byte[] bytes, Endianness endianness) {
		if (bytes.length != 8)
			throw new IndexOutOfBoundsException();
		return getLong(bytes,0,endianness);
	}
	public static byte[] getBytes(long val, Endianness endianness) {
		byte[] result = new byte[8];
		if (endianness == Endianness.BIG_ENDIAN) {
			for (int i=56, j=0; i>=0; i-=8, j++) {
				result[j] = (byte)((val >>> i) & 0xFF);
			}
		} else {
			for (int i=56, j=7; i>=0; i-=8, j--) {
				result[j] = (byte)((val >>> i) & 0xFF);
			}			
		}
		return result;
	}
	
	public static String printBytes(byte[] bytes) {
		
		StringBuilder strb = new StringBuilder();
		
		for (byte b : bytes) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
		}
		strb.deleteCharAt(strb.length()-1);
		
		return strb.toString();
	}
	
	public static String printBytes(short val) {
		return printBytes(getBytes(val,Endianness.BIG_ENDIAN));
	}
	
	public static String printBytes(long val) {
		return printBytes(getBytes(val,Endianness.BIG_ENDIAN));
	}
}
