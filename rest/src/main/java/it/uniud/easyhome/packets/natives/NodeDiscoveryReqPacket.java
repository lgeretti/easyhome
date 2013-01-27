package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidContextException;
import it.uniud.easyhome.exceptions.InvalidDomainException;
import it.uniud.easyhome.exceptions.InvalidEndpointsException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.InvalidPayloadLengthException;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodeDiscoveryReqPacket extends NativePacket {

	private static final long serialVersionUID = 713737630597150888L;
	
	private static final int APS_PAYLOAD_LENGTH = 2;
	
	public NodeDiscoveryReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != ManagementContext.NODE_DISCOVERY_REQ.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public NodeDiscoveryReqPacket(Node destinationNode, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0xEA),
			 new ModuleCoordinates(destinationNode.getCoordinates(),(byte)0xEA),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.NODE_DISCOVERY_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       ByteUtils.getBytes(destinationNode.getCoordinates().getAddress(), Endianness.LITTLE_ENDIAN)));
	}
	
	public NodeDiscoveryReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}
}
