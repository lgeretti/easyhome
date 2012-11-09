package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class NodeAnncePacket extends NativePacket {

	private static final long serialVersionUID = -5541681898302354205L;

	private static final int APS_PAYLOAD_LENGTH = 11;
	
	public NodeAnncePacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContexts.NODE_ANNOUNCE.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPacketTypeException();
	}
	
	public NodeAnncePacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAnnouncedAddress() {
		byte[] data = getOperation().getData();
		return (short) ((((short)(data[0] & 0xFF)) << 8) + data[1]); 
	}
	
	public long getAnnouncedNuid() {
		byte[] data = getOperation().getData();
		long result = 0;
		for (int i=56,j=2; i>=0; i-=8,j+=1)
			result += ((long)(data[j] & 0xFF))<<i;
		return result;
	}
	
	public byte getAnnouncedCapability() {
		return getOperation().getData()[APS_PAYLOAD_LENGTH-1];
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContexts.NODE_ANNOUNCE.getCode())
			return false;
		
		return true;
	}
}
