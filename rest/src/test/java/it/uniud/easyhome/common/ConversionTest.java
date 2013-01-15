package it.uniud.easyhome.common;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;


import org.junit.*;

import com.google.gson.Gson;

public class ConversionTest {

    @Test
    public void testShortConversion() {

    	byte[] originalBytes = new byte[2];
    	
    	originalBytes[0] = (byte)0x80;
    	originalBytes[1] = (byte)0x7F;
    	    	
    	checkShort(originalBytes, Endianness.BIG_ENDIAN);
    	checkShort(originalBytes, Endianness.LITTLE_ENDIAN);

    	originalBytes[0] = (byte)0x7F;
    	originalBytes[0] = (byte)0x80;
    	
    	checkShort(originalBytes, Endianness.BIG_ENDIAN);
    	checkShort(originalBytes, Endianness.LITTLE_ENDIAN);
    	
    	originalBytes[0] = (byte)0x80;
    	originalBytes[0] = (byte)0x80;

    	checkShort(originalBytes, Endianness.BIG_ENDIAN);
    	checkShort(originalBytes, Endianness.LITTLE_ENDIAN);
    	
    	originalBytes[0] = (byte)0x7F;
    	originalBytes[0] = (byte)0x7F;
    	
    	checkShort(originalBytes, Endianness.BIG_ENDIAN);
    	checkShort(originalBytes, Endianness.LITTLE_ENDIAN);
    }
    
    @Test
    public void testLongConversion() {

    	byte[] originalBytes = new byte[8];
    	
    	originalBytes[0] = (byte)0x80;
    	originalBytes[1] = (byte)0x7F;
    	originalBytes[2] = (byte)0x80;
    	originalBytes[3] = (byte)0x7F;
    	originalBytes[4] = (byte)0x80;
    	originalBytes[5] = (byte)0x7F;
    	originalBytes[6] = (byte)0x80;
    	originalBytes[7] = (byte)0x7F;
    	    	
    	checkLong(originalBytes, Endianness.BIG_ENDIAN);
    	checkLong(originalBytes, Endianness.LITTLE_ENDIAN);

    	originalBytes[0] = (byte)0x7F;
    	originalBytes[1] = (byte)0x80;
    	originalBytes[2] = (byte)0x7F;
    	originalBytes[3] = (byte)0x80;
    	originalBytes[4] = (byte)0x7F;
    	originalBytes[5] = (byte)0x80;
    	originalBytes[6] = (byte)0x7F;
    	originalBytes[7] = (byte)0x80;
    	
    	checkLong(originalBytes, Endianness.BIG_ENDIAN);
    	checkLong(originalBytes, Endianness.LITTLE_ENDIAN);
    	
    	originalBytes[0] = (byte)0x80;
    	originalBytes[1] = (byte)0x80;
    	originalBytes[2] = (byte)0x80;
    	originalBytes[3] = (byte)0x80;
    	originalBytes[4] = (byte)0x80;
    	originalBytes[5] = (byte)0x80;
    	originalBytes[6] = (byte)0x80;
    	originalBytes[7] = (byte)0x80;
    	
    	checkLong(originalBytes, Endianness.BIG_ENDIAN);
    	checkLong(originalBytes, Endianness.LITTLE_ENDIAN);
    	
    	originalBytes[0] = (byte)0x7F;
    	originalBytes[1] = (byte)0x7F;
    	originalBytes[2] = (byte)0x7F;
    	originalBytes[3] = (byte)0x7F;
    	originalBytes[4] = (byte)0x7F;
    	originalBytes[5] = (byte)0x7F;
    	originalBytes[6] = (byte)0x7F;
    	originalBytes[7] = (byte)0x7F;
    	
    	checkLong(originalBytes, Endianness.BIG_ENDIAN);
    	checkLong(originalBytes, Endianness.LITTLE_ENDIAN);
    }
    
    private void checkLong(byte[] originalBytes, Endianness endianness) {

    	byte[] convertedBytes = new byte[8];
    	long conversion;
    	
    	conversion = ByteUtils.getLong(originalBytes, endianness);
    	convertedBytes = ByteUtils.getBytes(conversion, endianness);
    	
    	assertTrue(Arrays.equals(originalBytes,convertedBytes));
    	
    	ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
    	conversion = ByteUtils.getLong(bais, endianness);
    	convertedBytes = ByteUtils.getBytes(conversion, endianness);
    	
    	assertTrue(Arrays.equals(originalBytes,convertedBytes));
    }
    
    private void checkShort(byte[] originalBytes, Endianness endianness) {

    	byte[] convertedBytes = new byte[2];
    	short conversion;
    	
    	conversion = ByteUtils.getShort(originalBytes, endianness);
    	convertedBytes = ByteUtils.getBytes(conversion, endianness);
    	
    	assertTrue(Arrays.equals(originalBytes,convertedBytes));
    	
    	ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
    	conversion = ByteUtils.getShort(bais, endianness);
    	convertedBytes = ByteUtils.getBytes(conversion, endianness);
    	
    	assertTrue(Arrays.equals(originalBytes,convertedBytes));
    }
    
}
