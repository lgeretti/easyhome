package it.uniud.easyhome.gateway;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Gateway {
    
    private int id;
    
    private ProtocolType protocol;
    
    private int port;
    
    public int getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return protocol;
    }
    
    public int getPort() {
        return port;
    }
    
    private Gateway() {}
    
    public Gateway(int id, ProtocolType protocol, int port) {
        this.id = id;
        this.protocol = protocol;
        this.port = port;
    }
    
}
