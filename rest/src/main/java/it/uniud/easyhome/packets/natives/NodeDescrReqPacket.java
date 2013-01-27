package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodeDescrReqPacket extends NativePacket {

	private static final long serialVersionUID = 3241227466990620831L;
	private static final int APS_PAYLOAD_LENGTH = 2;
	
	public NodeDescrReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.NODE_DESC_REQ.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeDescrReqPacket(Node destinationNode, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0),
			 new ModuleCoordinates(destinationNode.getCoordinates(),(byte)0),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.NODE_DESC_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       ByteUtils.getBytes(destinationNode.getCoordinates().getAddress(), Endianness.LITTLE_ENDIAN)));
	}
	
	public NodeDescrReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}
}
