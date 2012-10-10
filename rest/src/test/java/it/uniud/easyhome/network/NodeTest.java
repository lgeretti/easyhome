package it.uniud.easyhome.network;

import static org.junit.Assert.*;

import it.uniud.easyhome.packets.Node;

import org.junit.*;

public class NodeTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction() {
        
        new Node.Builder(-2);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectName() {
        
        Node.Builder nb = new Node.Builder(1);
        
        nb.setName(null);
    }   
    
    @Test
    public void testConstruction() {
        
        Node.Builder nb = new Node.Builder(10);
        
        nb.setName("test");
        nb.setGatewayId((byte)2);
        nb.setAddress((short)15);
        
        Node node = nb.build();
        
        assertEquals(10, node.getId());
        assertEquals("test", node.getName());
    }
    
}
