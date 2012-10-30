package it.uniud.easyhome.common;

public class ByteUtils {

	public static short getShort(byte[] bytes, int start) {
		if (start < 0 || start > bytes.length-2)
			throw new IndexOutOfBoundsException();
		return (short)((short)(bytes[start] << 8) + bytes[start+1]);
	}
	public static short getShort(byte[] bytes) {
		if (bytes.length != 2)
			throw new IndexOutOfBoundsException();
		return getShort(bytes,0);
	}
	public static byte[] getBytes(short val) {
		byte[] result = new byte[2];
		result[0] = (byte)((val >>> 8) & 0xFF);
		result[1] = (byte)(val & 0xFF);
		return result;
	}
	
	public static long getLong(byte[] bytes, int start) {
		if (start < 0 || start > bytes.length-8)
			throw new IndexOutOfBoundsException();
		long result = 0;
		for (int i=56, j=0; i>=0; i-=8, j++)
			result += ((long)(bytes[j] & 0xFF))<<i;
		return result;
	}
	public static long getLong(byte[] bytes) {
		if (bytes.length != 8)
			throw new IndexOutOfBoundsException();
		return getLong(bytes,0);
	}
	public static byte[] getBytes(long val) {
		byte[] result = new byte[8];
		for (int i=56, j=0; i>=0; i-=8, j++) {
			result[j] = (byte)((val >>> i) & 0xFF);
		}
		return result;
	}
}
