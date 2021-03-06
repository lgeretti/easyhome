package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class SimpleDescrReqPacket extends NativePacket {

	private static final long serialVersionUID = 8448404581483865375L;
	private static final int APS_PAYLOAD_LENGTH = 3;
	
	public SimpleDescrReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.SIMPLE_DESC_REQ.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public SimpleDescrReqPacket(GlobalCoordinates destinationCoordinates, byte endpoint, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0),
			 new ModuleCoordinates(destinationCoordinates,(byte)0),				
			 new Operation(seqNumber,Domain.MANAGEMENT.getCode(),ManagementContext.SIMPLE_DESC_REQ.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       new byte[]{
				 				(byte)(destinationCoordinates.getAddress() & 0xFF),
				 				(byte)((destinationCoordinates.getAddress() >>> 8) & 0xFF), 				 				 
				 				endpoint}));
	}
	
	public SimpleDescrReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
}
