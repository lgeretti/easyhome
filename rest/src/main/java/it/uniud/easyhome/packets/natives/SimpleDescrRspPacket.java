package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.ManagementContext;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class SimpleDescrRspPacket extends NativePacket {

	private static final long serialVersionUID = 1266890985693018451L;
	private static final int APS_PAYLOAD_LENGTH = 16;
	
	public SimpleDescrRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.SIMPLE_DESC_RSP.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public SimpleDescrRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
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
