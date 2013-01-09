package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.ModuleCoordinates;
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
	
	public NodeLogicalType getLogicalType() throws InvalidNodeDescException {
		int raw = this.getOperation().getData()[3] & 0x3; // (the first two bits)
		
		NodeLogicalType result = null;
		
		switch (raw) {
		case 0:
			result = NodeLogicalType.END_DEVICE;
			break;
		case 1:
			result = NodeLogicalType.ROUTER;
			break;
		case 2:
			result = NodeLogicalType.COORDINATOR;
			break;
		default:
			throw new InvalidNodeDescException();
		}
		
		return result;
	}
	
	public Manufacturer getManufacturerCode() {
		
		byte[] data = getOperation().getData();
		short raw = (short) ((((short)(data[7] & 0xFF)) << 8) + data[6]); 
		
		return Manufacturer.fromCode(raw);
	}
	
	public short getAddrOfInterest() {
		
		byte[] data = getOperation().getData();
		return (short) ((((short)(data[2] & 0xFF)) << 8) + data[1]); 
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
