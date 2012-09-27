package it.uniud.easyhome.gateway.it;

import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.gateway.XBeeGateway;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class XBeeGatewayIT {

    static int srcGwPort = 5000;
    static ProtocolType srcGwProtocol = ProtocolType.XBEE;
    static int dstGwPort = 4000;
    static ProtocolType dstGwProtocol = ProtocolType.XBEE;
    
    static int dstAddress = 20;
    static int dstPort = 1;
    static int srcEndpoint = 15;
    
    public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {
        
        Socket skt = new Socket(args[0],Integer.parseInt(args[1]));
        
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
        
        byte[] bytes = baos.toByteArray();
        StringBuilder strb = new StringBuilder();
        for (byte b: bytes) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        System.out.println(strb.toString());
        
        BufferedOutputStream os = new BufferedOutputStream(skt.getOutputStream());
        os.write(bytes);
        os.flush();
        os.close();
        
        skt.close();
        
    }
    
}
