package it.uniud.easyhome;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Node {

    private int id;
    
    private String name;
    
    public Node() {
    }
    
    public Node(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setId(int id) { 
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
