package it.uniud.easyhome.network;

import static org.junit.Assert.*;


import org.junit.*;

import com.google.gson.Gson;

public class NodeTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction() {
        
        new Node.Builder(1,(byte)0,0,(short)0);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction2() {
        
        new Node.Builder(0,(byte)1,0,(short)0);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction3() {
        
        new Node.Builder(0,(byte)0,0,(short)0);
    }
    
    @Test
    public void testConstruction() {
        
        Node.Builder nb1 = new Node.Builder(1,(byte)2,10L,(short)0);
        
        Node node1 = nb1.build();
        
        Node.Builder nb2 = new Node.Builder(2,(byte)2,11L,(short)0);

        Node node2 = nb2.build();
        node1.addNeighbor(node2);
        
        assertEquals(10L, node1.getCoordinates().getNuid());
        assertEquals("test", node1.getName());
        assertEquals(node1.getNeighbors().size(),1);
    }
    
}
