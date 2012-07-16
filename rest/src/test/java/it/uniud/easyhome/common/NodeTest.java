package it.uniud.easyhome.common;

import it.uniud.easyhome.network.Node;

import org.junit.*;

public class NodeTest {

    @Test(expected=IllegalStateException.class)
    public void testIncorrectConstruction() {
        
        Node.Builder nb = new Node.Builder(1);
        
        nb.build();
    }
    
}
