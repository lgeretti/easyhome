package it.uniud.easyhome.gateway;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GatewayInfo {
    
    private int port;
    private ProtocolType protocol;
    private int id;
    
    public int getId() {
        return id;
    }
    
    public ProtocolType getProtocolType() {
        return protocol;
    }
    
    public int getPort() {
        return port;
    }
    
    @SuppressWarnings("unused")
    private GatewayInfo() {}
    
    public GatewayInfo(Gateway gw) {
        id = gw.getId();
        protocol = gw.getProtocolType();
        port = gw.getPort();
    }
    
    public static List<GatewayInfo> createFromAll(List<Gateway> gws) {
        List<GatewayInfo> result = new ArrayList<GatewayInfo>(gws.size());
        for (Gateway gw : gws)
            result.add(new GatewayInfo(gw));
        
        return result;
    }
}
