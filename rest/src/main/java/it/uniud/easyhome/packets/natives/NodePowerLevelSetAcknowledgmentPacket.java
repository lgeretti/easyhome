package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.ResponseStatus;

public class NodePowerLevelSetAcknowledgmentPacket extends NativePacket {

	private static final long serialVersionUID = -2244804877909060044L;
	private static final int APS_PAYLOAD_LENGTH = 3;
	
	public NodePowerLevelSetAcknowledgmentPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		validationBarrier(srcCoords, dstCoords, op);
	}
	
	public NodePowerLevelSetAcknowledgmentPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}

	public ResponseStatus getStatus() {
		return ResponseStatus.fromCode(this.getOperation().getData()[0]);
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 1, Endianness.LITTLE_ENDIAN); 
	}
	
	private static void validationBarrier(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != ManagementContext.NODE_POWER_LEVEL_SET_ACK.getCode())
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
