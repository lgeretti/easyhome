package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.EasyHomeContext;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class AlarmStateReqPacket extends NativePacket {
	
	private static final long serialVersionUID = 5765317536508570657L;
	private static final int APS_PAYLOAD_LENGTH = 2;
	
	public AlarmStateReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() == (byte)0x0 || dstCoords.getEndpoint() == (byte)0x0)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.EASYHOME.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != EasyHomeContext.ALARM.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public AlarmStateReqPacket(GlobalCoordinates destinationCoordinates, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0x1),
			 new ModuleCoordinates(destinationCoordinates,(byte)0x1),				
			 new Operation(seqNumber,Domain.EASYHOME.getCode(),EasyHomeContext.ALARM.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,
					       ByteUtils.getBytes(destinationCoordinates.getAddress(), Endianness.LITTLE_ENDIAN)));
	}
	
	public AlarmStateReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.EASYHOME.getCode())
			return false;
		if (op.getContext() != EasyHomeContext.ALARM.getCode())
			return false;
		
		return true;
	}
}
