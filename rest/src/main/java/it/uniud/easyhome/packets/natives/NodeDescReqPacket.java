package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class NodeDescReqPacket extends NativePacket {

	private static final long serialVersionUID = 3241227466990620831L;
	private static final int APS_PAYLOAD_LENGTH = 2;
	
	public NodeDescReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContexts.NODE_ANNOUNCE.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeDescReqPacket(Node destinationNode, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)1),
			 new ModuleCoordinates(destinationNode.getGatewayId(),destinationNode.getId(),destinationNode.getAddress(),(byte)1),				
			 new Operation(seqNumber,Domains.EASYHOME_MANAGEMENT.getCode(),ManagementContexts.NODE_DESC_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       new byte[]{(byte)((destinationNode.getAddress() >>> 8) & 0xFF), (byte)(destinationNode.getAddress() & 0xFF)}));
	}
	
	public NodeDescReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		byte[] data = getOperation().getData();
		return (short) ((((short)data[0]) << 8) + data[1]); 
	}
}
