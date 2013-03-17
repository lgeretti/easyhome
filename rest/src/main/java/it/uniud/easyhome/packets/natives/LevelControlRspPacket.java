package it.uniud.easyhome.packets.natives;

import java.util.Arrays;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.HomeAutomationContext;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.ResponseStatus;

/**
 * Note that we added the status and the network address to the response
 *
 */
public class LevelControlRspPacket extends NativePacket {

	private static final long serialVersionUID = 1290599310233664990L;

	public LevelControlRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() == 0 || dstCoords.getEndpoint() == 0)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.HOME_AUTOMATION.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != HomeAutomationContext.LEVEL_CONTROL.getCode())
			throw new InvalidContextException();
		byte[] opData = op.getData();
		if (opData.length != 4)
			throw new InvalidPayloadLengthException();
	}
	
	public LevelControlRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public ResponseStatus getStatus() {
		return ResponseStatus.fromCode(this.getOperation().getData()[0]);
	}

	public short getAddrOfInterest() {
		return ByteUtils.getShort(this.getOperation().getData(), 1, Endianness.LITTLE_ENDIAN);
	}
	
	public int getLevelPercentage() {
		return (0xFF & this.getOperation().getData()[3])-100;
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (pkt.getSrcCoords().getEndpoint() == 0 || pkt.getDstCoords().getEndpoint() == 0)
			return false;
		if (op.getDomain() != Domain.HOME_AUTOMATION.getCode())
			return false;
		
		if (op.getContext() != HomeAutomationContext.LEVEL_CONTROL.getCode())
			return false;
		// Command id (fake)
		if (op.getCommand() != (byte)0x0)
			return false;
		if (op.getData().length != 4) 
			return false;
		
		return true;
	}
	
	
}
