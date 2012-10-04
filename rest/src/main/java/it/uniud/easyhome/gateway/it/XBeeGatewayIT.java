package it.uniud.easyhome.gateway.it;

import it.uniud.easyhome.gateway.XBeeGateway;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.codehaus.jackson.util.ByteArrayBuilder;

public class XBeeGatewayIT {

    static int srcEndpoint = 15;
    
    public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {
        
        Socket skt = new Socket(args[0],Integer.parseInt(args[1]));
        
        Socket skt2 = new Socket(args[0],6060);
        
        int mappedDstEndpoint = Integer.parseInt(args[2]);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int sum = 0;
        // Delimiter
        baos.write(XBeeGateway.START_DELIMITER);
        // Length (21)
        baos.write(0x00);
        baos.write(0x15);
        // Frame type
        baos.write(XBeeGateway.EXPLICIT_RX_INDICATOR_FRAME_TYPE);
        sum += XBeeGateway.EXPLICIT_RX_INDICATOR_FRAME_TYPE;
        // Source 64 bit address (arbitrary)
        baos.write(new byte[8]);
        // Source 16 bit address (arbitrary)
        baos.write(new byte[]{(byte)0xA2,(byte)0xB3});
        sum += 0xA2;
        sum += 0xB3;
        // Source endpoint
        baos.write(srcEndpoint);
        sum += srcEndpoint;
        // Destination endpoint (mapped by the hub)
        baos.write(mappedDstEndpoint);
        sum += mappedDstEndpoint;
        // Cluster Id (On/Off)
        baos.write(new byte[]{0x00,0x06});
        sum += 0x06;
        // Profile Id (Home Automation)
        baos.write(new byte[]{0x01,0x04});
        sum += 0x01;
        sum += 0x04;
        // Receive options (0x02: packet was a broadcast; 0x00 otherwise)
        baos.write(0x00);
        sum += 0x00;
        // Frame control (Cluster specific)
        baos.write(0x01);
        sum += 0x01;
        // Transaction sequence number (arbitrary)
        baos.write(0x71);
        sum += 0x71;
        // Command (toggle)
        baos.write(0x02);
        sum += 0x02;
        // (empty data)
        // Checksum
        baos.write(0xFF - (sum & 0xFF));
        
        byte[] bytesToSend = baos.toByteArray();
        printBytes(bytesToSend);
        
        BufferedOutputStream os = new BufferedOutputStream(skt.getOutputStream());
        BufferedInputStream is = new BufferedInputStream(skt2.getInputStream());
        
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
        
        skt.close();
        skt2.close();
        
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
