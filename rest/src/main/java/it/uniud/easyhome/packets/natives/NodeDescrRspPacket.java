package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidNodeLogicalTypeException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodeDescrRspPacket extends NativePacket {

	private static final long serialVersionUID = -5541681898302354205L;

	private static final int APS_PAYLOAD_LENGTH = 16;
	
	public NodeDescrRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.NODE_DESC_RSP.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeDescrRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public NodeLogicalType getLogicalType() throws InvalidNodeLogicalTypeException {
		int raw = this.getOperation().getData()[3] & 0x3; // (the first two bits)
		
		NodeLogicalType result = null;
		
		switch (raw) {
		case 0:
			result = NodeLogicalType.COORDINATOR;
			break;
		case 1:
			result = NodeLogicalType.ROUTER;
			break;
		case 2:
			result = NodeLogicalType.END_DEVICE;
			break;
		default:
			throw new InvalidNodeLogicalTypeException();
		}
		
		return result;
	}
	
	public Manufacturer getManufacturerCode() {
		
		short raw = ByteUtils.getShort(getOperation().getData(), 6, Endianness.LITTLE_ENDIAN); 
		return Manufacturer.fromCode(raw);
	}
	
	public boolean isSuccessful() {
		return (this.getOperation().getData()[0] == 0);
	}
	
	public short getAddrOfInterest() {
		
		return ByteUtils.getShort(getOperation().getData(), 1, Endianness.LITTLE_ENDIAN); 
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.NODE_DESC_RSP.getCode()) 
			return false;
		
		return true;
	}
}
