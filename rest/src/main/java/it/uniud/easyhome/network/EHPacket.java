package it.uniud.easyhome.network;

public interface EHPacket extends Packet {

    /** A node identifier is global and is resolved by the domotic hub, by identifying
     *  the proper gateway and network address of the corresponding module. 
     *  The domotic controller is identified by id 0, broadcast by 0xFFFF. **/
    public int getSourceId();
    
    public int getDestinationId();

    public int getSourcePort();
    
    public int getDestinationPort();
    
    public int getOperationContext();
    
    public int getOperation();
    
    public byte[] getOperationData();
}
