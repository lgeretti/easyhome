package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.EasyHomeContext;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.ResponseStatus;

public class AlarmStateRspPacket extends NativePacket {

	private static final long serialVersionUID = 7065101909528022359L;
	private static final int APS_PAYLOAD_LENGTH = 5;
	
	public AlarmStateRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		validationBarrier(srcCoords, dstCoords, op);
	}
	
	public AlarmStateRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}

	public ResponseStatus getStatus() {
		return ResponseStatus.fromCode(this.getOperation().getData()[0]);
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 1, Endianness.LITTLE_ENDIAN); 
	}
	
	public short getAlarmCode() {
		return ByteUtils.getShort(getOperation().getData(), 3, Endianness.LITTLE_ENDIAN);
	}
	
	private static void validationBarrier(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		if (srcCoords.getEndpoint() == (byte)0x0 || dstCoords.getEndpoint() == (byte)0x0)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.EASYHOME.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != EasyHomeContext.ALARM.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public static boolean validates(NativePacket pkt) {
		
		boolean result = true;
		
		if (pkt == null) {
			result = false;
		} else {
			try {
				
				validationBarrier(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
				result = true;
				
			} catch (InvalidEndpointsException | InvalidDomainException | InvalidContextException | InvalidPayloadLengthException ex) {
				result = false;
			}
		}
		
		return result;
	}
	
	
}
