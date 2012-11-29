package it.uniud.easyhome.network;

import static org.junit.Assert.*;


import org.junit.*;

import com.google.gson.Gson;

public class NodeTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction() {
        
        new Node.Builder(0);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectName() {
        
        Node.Builder nb = new Node.Builder(1);
        
        nb.setName(null);
    }
    
    @Test
    public void testConstruction() {
        
        Node.Builder nb1 = new Node.Builder(10L);
        
        nb1.setName("test");
        nb1.setGatewayId((byte)2);
        nb1.setAddress((short)15);
        nb1.setCapability((byte)14);
        
        Node node1 = nb1.build();
        
        Node.Builder nb2 = new Node.Builder(11L);
        
        nb2.setName("test2");
        nb2.setGatewayId((byte)2);
        nb2.setAddress((short)24);
        nb2.setCapability((byte)14);
        
        Node node2 = nb2.build();
        node1.addNeighbor(node2);
        
        assertEquals(10L, node1.getId());
        assertEquals("test", node1.getName());
        assertEquals(node1.getNeighbors().size(),1);
    }
    
}
