package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidEndpointsException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.InvalidPayloadLengthException;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodeAnncePacket extends NativePacket {

	private static final long serialVersionUID = -5541681898302354205L;

	private static final int APS_PAYLOAD_LENGTH = 11;
	
	public NodeAnncePacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.NODE_ANNOUNCE.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public NodeAnncePacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAnnouncedAddress() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN);
	}
	
	public long getAnnouncedNuid() {
		return ByteUtils.getLong(getOperation().getData(), 2, Endianness.LITTLE_ENDIAN);
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.NODE_ANNOUNCE.getCode())
			return false;
		
		return true;
	}
}
