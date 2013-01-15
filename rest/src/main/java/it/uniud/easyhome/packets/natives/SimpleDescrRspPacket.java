package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.InvalidPayloadLengthException;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class SimpleDescrRspPacket extends NativePacket {

	private static final long serialVersionUID = 1770063684876852615L;

	public SimpleDescrRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.SIMPLE_DESC_RSP.getCode())
			throw new InvalidPacketTypeException();
		byte[] opData = op.getData();
		if (opData[3] != opData.length-4)
			throw new InvalidPayloadLengthException();
	}
	
	public SimpleDescrRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public HomeAutomationDevice getDevice() {
		
		short deviceCode = ByteUtils.getShort(this.getOperation().getData(), 7, Endianness.LITTLE_ENDIAN);
		return HomeAutomationDevice.fromCode(deviceCode);
	}
	
	public byte getEndpoint() {
		return this.getOperation().getData()[4];
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(this.getOperation().getData(), 1, Endianness.LITTLE_ENDIAN);
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.SIMPLE_DESC_RSP.getCode())
			return false;
		
		return true;
	}
	
	
}
