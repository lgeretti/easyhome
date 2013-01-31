package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.ResponseStatus;

public class NodeDiscoveryRspPacket extends NativePacket {

	private static final long serialVersionUID = 5949441541386198411L;
	
	private static final int APS_PAYLOAD_LENGTH = 16;
	
	public NodeDiscoveryRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != ManagementContext.NODE_DISCOVERY_RSP.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public NodeDiscoveryRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public NodeLogicalType getLogicalType() throws InvalidNodeLogicalTypeException {
		byte raw = this.getOperation().getData()[13];
		
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
	
	public Manufacturer getManufacturer() {
		
		short raw = ByteUtils.getShort(getOperation().getData(), 14, Endianness.LITTLE_ENDIAN); 
		return Manufacturer.fromCode(raw);
	}
	
	public ResponseStatus getStatus() {
		return ResponseStatus.fromCode(this.getOperation().getData()[0]);
	}
	
	public short getSenderAddress() {
		return ByteUtils.getShort(getOperation().getData(), 1, Endianness.LITTLE_ENDIAN);
	}

	public long getNuid() { 
		return ByteUtils.getLong(getOperation().getData(), 3, Endianness.LITTLE_ENDIAN);
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 11, Endianness.LITTLE_ENDIAN); 
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.NODE_DISCOVERY_RSP.getCode()) 
			return false;
		
		return true;
	}
}
