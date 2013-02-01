package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodePowerLevelReqPacket extends NativePacket {
	
	private static final long serialVersionUID = -2130181396710201357L;
	private static final int APS_PAYLOAD_LENGTH = 2;
	
	public NodePowerLevelReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != ManagementContext.NODE_POWER_LEVEL_REQ.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public NodePowerLevelReqPacket(GlobalCoordinates destinationCoordinates, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0xEA),
			 new ModuleCoordinates(destinationCoordinates,(byte)0xEA),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.NODE_POWER_LEVEL_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       ByteUtils.getBytes(destinationCoordinates.getAddress(), Endianness.LITTLE_ENDIAN)));
	}
	
	public NodePowerLevelReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}
}
