package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class NodeDescrRspPacket extends NativePacket {

	private static final long serialVersionUID = -5541681898302354205L;

	private static final int APS_PAYLOAD_LENGTH = 16;
	
	public NodeDescrRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContexts.NODE_DESC_RSP.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeDescrRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public NodeLogicalType getLogicalType() throws InvalidNodeDescException {
		int raw = (this.getOperation().getData()[3] >>> 5) & 0xFF;
		
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
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(),1);
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContexts.NODE_DESC_RSP.getCode())
			return false;
		
		return true;
	}
}
