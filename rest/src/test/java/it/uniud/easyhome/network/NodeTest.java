package it.uniud.easyhome.network;

import it.uniud.easyhome.network.Node;

import org.junit.*;

public class NodeTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectConstruction() {
        
        Node.Builder nb = new Node.Builder(-2);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncorrectName() {
        
        Node.Builder nb = new Node.Builder(1);
        
        nb.setName(null);
    }    
    
}
