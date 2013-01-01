package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class NodeNeighReqPacket extends NativePacket {

	private static final long serialVersionUID = 3241227466990620831L;
	private static final int APS_PAYLOAD_LENGTH = 1;
	
	public NodeNeighReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {

		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.NODE_NEIGH_REQ.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeNeighReqPacket(Node destinationNode, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0),
			 new ModuleCoordinates(destinationNode.getGatewayId(),destinationNode.getId(),destinationNode.getAddress(),(byte)0),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.NODE_NEIGH_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       new byte[]{0})); // We choose offset 0 to get all neighbors
	}
	
	public NodeNeighReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
}