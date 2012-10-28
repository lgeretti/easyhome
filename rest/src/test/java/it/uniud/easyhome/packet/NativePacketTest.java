package it.uniud.easyhome.packet;


import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import it.uniud.easyhome.packets.NativePacket;

import org.junit.*;

public class NativePacketTest {

    @Test
    public void checkConstruction() throws IOException {
        
        byte[] sampleData = new byte[]
                       {0x00,       // source network
        				0x00, 0x22, 0x33, 0x44, (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF, // source uuid
                        0x0A, 0x31, // source address
                        0x00, 0x10, // source port
                        0x02,       // destination network
                        0x00, 0x22, 0x33, 0x44, 0x01, 0x23, 0x45, 0x67, // destination uuid
                        0x00, 0x0F, // destination address
                        0x00, 0x01, // destination port
                        0x01,       // operation flags
                        0x00, 0x10, // operation domain
                        0x00, 0x03, // operation context
                        (byte)0xA1, // operation command
                        0x05, 0x01, 0x01, (byte)0xFF // operation data
                       };
        
        assertTrue(sampleData.length < 65536);
        
        byte[] packetBytes = new byte[sampleData.length+3];
        
        packetBytes[0] = (byte)0xEA;
        packetBytes[1] = (byte)((sampleData.length >>> 8) & 0xFF);
        packetBytes[2] = (byte)(sampleData.length & 0xFF);
        
        int i=3;
        for (byte b : sampleData)
            packetBytes[i++] = b;
        
        StringBuilder strb = new StringBuilder();
        for (byte b: packetBytes) {
            if ((0xFF & b) < 0x10)
                strb.append("0");
            strb.append(Integer.toHexString(0xFF & b).toUpperCase()).append(" ");
        }
        
        System.out.println("Original bytes: " + strb.toString());
        
        NativePacket pkt = new NativePacket(new ByteArrayInputStream(packetBytes));
        
        System.out.println("Recovered bytes: " + pkt.printBytes());
        
        assertTrue(Arrays.equals(pkt.getBytes(), packetBytes));
    }
    
}
