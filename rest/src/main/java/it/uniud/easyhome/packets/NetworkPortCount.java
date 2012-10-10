package it.uniud.easyhome.packets;

public class NetworkPortCount {

    int network;
    
    int portCount;
    
    public NetworkPortCount(int network) {
        
        this.network = network;
        
        portCount = 0;
    }
    
    public int getNetwork() {
        return network;
    }
    
    public int getPortCount() {
        return portCount;
    }
    
    /**
     * Increases the port count.
     * 
     * @return The new port count.
     */
    public int increasePortCount() {
        return ++portCount;
    }
    
}
