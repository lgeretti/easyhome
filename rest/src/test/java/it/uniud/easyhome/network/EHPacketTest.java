package it.uniud.easyhome.network;


import static org.junit.Assert.*;

import java.util.Arrays;

import it.uniud.easyhome.network.EHPacket;

import org.junit.*;

public class EHPacketTest {

    @Test
    public void checkConstruction() {
        
        byte[] sampleData = new byte[]
                       {0x00,       // source network
                        0x0A, 0x31, // source address
                        0x00, 0x10, // source port
                        0x02,       // destination network
                        0x00, 0x0F, // destination address
                        0x00, 0x01, // destination port
                        0x00, 0x03, // operation context
                        0x00, (byte)0xA1, // operation method
                        0x05, 0x01, 0x01, (byte)0xFF // operation data
                       };
        
        assertTrue(sampleData.length < 65536);
        
        int checksum = 0;
        for (byte b : sampleData)
            checksum += b;
        checksum = 0xFF - (checksum & 0xFF);
        
        byte[] packetBytes = new byte[sampleData.length+4];
        
        packetBytes[0] = (byte)0xEA;
        packetBytes[1] = (byte)((sampleData.length >>> 8) & 0xFF);
        packetBytes[2] = (byte)(sampleData.length & 0xFF);
        
        int i=3;
        for (byte b : sampleData)
            packetBytes[i++] = b;
        packetBytes[i] = (byte)checksum;
        
        EHPacket pkt = new EHPacket(packetBytes);
        
        StringBuilder strb = new StringBuilder();
        for (byte b: pkt.getBytes()) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        System.out.println("Packet bytes: " + strb.toString());
        
        assertTrue(Arrays.equals(pkt.getBytes(), packetBytes));
    }
    
}
