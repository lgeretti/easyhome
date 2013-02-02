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

public class NodePowerLevelSetIssuePacket extends NativePacket {

	private static final long serialVersionUID = 1031759605922543318L;
	private static final int APS_PAYLOAD_LENGTH = 3;
	
	public NodePowerLevelSetIssuePacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != ManagementContext.NODE_POWER_LEVEL_SET_ISS.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public NodePowerLevelSetIssuePacket(GlobalCoordinates destinationCoordinates, byte level, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0xEA),
			 new ModuleCoordinates(destinationCoordinates,(byte)0xEA),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.NODE_POWER_LEVEL_SET_ISS.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       new byte[]{ByteUtils.getBytes(destinationCoordinates.getAddress(), Endianness.LITTLE_ENDIAN)[0],
				 					  ByteUtils.getBytes(destinationCoordinates.getAddress(), Endianness.LITTLE_ENDIAN)[1],
				 					  level}));
	}
	
	public NodePowerLevelSetIssuePacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}
	
	public short getPowerLevel() {
		return getOperation().getData()[2]; 
	}
}
