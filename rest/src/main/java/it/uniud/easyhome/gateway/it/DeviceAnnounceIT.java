package it.uniud.easyhome.gateway.it;

import it.uniud.easyhome.xbee.XBeeConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.codehaus.jackson.util.ByteArrayBuilder;

public class DeviceAnnounceIT {
    
    public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {
        
        Socket xbeeSkt1 = new Socket(args[0],Integer.parseInt(args[1]));
        
        Socket xbeeSkt2 = new Socket(args[0],6060);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int sum = 0;
        // Delimiter
        baos.write(XBeeConstants.START_DELIMITER);
        // Length (32)
        baos.write(0x00);
        baos.write(0x20);
        // Frame type
        baos.write(XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE);
        sum += XBeeConstants.EXPLICIT_RX_INDICATOR_FRAME_TYPE;
        // Source 64 bit address (arbitrary)
        baos.write(new byte[7]);
        baos.write(0x77);
        sum += 0x77;
        // Source 16 bit address (arbitrary)
        baos.write(new byte[]{(byte)0x7D,(byte)0xB3});
        sum += 0x7D;
        sum += 0xB3;
        // Source endpoint
        baos.write(0x00);
        sum += 0x00;
        // Destination endpoint
        baos.write(0x00);
        sum += 0x00;
        // Cluster Id (DeviceAnnce)
        baos.write(new byte[]{0x00,0x13});
        sum += 0x13;
        // Profile Id (ZDP)
        baos.write(new byte[]{0x00,0x00});
        sum += 0x00;
        sum += 0x00;
        // Receive options (0x02: packet was a broadcast; 0x00 otherwise)
        baos.write(0x02);
        sum += 0x02;
        // Frame control
        baos.write(0x00);
        sum += 0x00;
        // Transaction sequence number (arbitrary)
        baos.write(0x71);
        sum += 0x71;
        // Command (toggle)
        baos.write(0x02);
        sum += 0x02;
        // Device announce data
        // NWK addr
        baos.write(0x5F);
        sum += 0x5F;
        baos.write(0x34);
        sum += 0x34;
        // IEEE addr
        for (int i=0;i<8;i++) {
	        baos.write(0x55);
	        sum += 0x55;        
        }
        // Capability (random)
        baos.write(0x7A);
        sum += 0x7A;
        // Checksum
        baos.write(0xFF - (sum & 0xFF));
        
        byte[] bytesToSend = baos.toByteArray();
        printBytes(bytesToSend);
        
        BufferedOutputStream os = new BufferedOutputStream(xbeeSkt1.getOutputStream());
        BufferedInputStream is = new BufferedInputStream(xbeeSkt2.getInputStream());
        
        os.write(bytesToSend);
        os.flush();
        os.close();

        
        ByteArrayBuilder ba = new ByteArrayBuilder();
        ba.append(is.read());
        int highLength = is.read();
        ba.append(highLength);
        int lowLength = is.read();
        ba.append(lowLength);
        int length = (highLength << 8) + lowLength;
        int receivedSum = 0;
        for (int i=0; i<length+1; i++) {
        	int byteRead = is.read();
        	ba.append(byteRead);
        	receivedSum += byteRead;
        }
        if ((receivedSum & 0xFF) != 0xFF)
        	System.out.println("Checksum failed");
        
        printBytes(ba.toByteArray());
        ba.close();
        
        xbeeSkt1.close();
        xbeeSkt2.close();
    }
    
    private static void printBytes(byte[] bytes) {
        StringBuilder strb = new StringBuilder();
        for (byte b: bytes) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        System.out.println(strb.toString());    	
    }
    
}
