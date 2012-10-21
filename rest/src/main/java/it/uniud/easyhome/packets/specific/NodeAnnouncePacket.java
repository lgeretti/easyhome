package it.uniud.easyhome.packets.specific;

import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.packets.Contexts;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.NativePacket;
import it.uniud.easyhome.packets.Operation;

public class NodeAnnouncePacket extends NativePacket {

	private static final long serialVersionUID = -5541681898302354205L;

	public NodeAnnouncePacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		System.out.println(srcCoords + " " + dstCoords + " " + op + " length = " + op.getData().length);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != Contexts.NODE_ANNOUNCE.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData().length != 11)
			throw new InvalidPacketTypeException();
	}
	
	public NodeAnnouncePacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAnnouncedAddress() {
		byte[] data = getOperation().getData();
		return (short) ((((short)data[0]) << 8) + data[1]); 
	}
	
	public long getAnnouncedNuid() {
		byte[] data = getOperation().getData();
		long result = 0;
		for (int i=56,j=2; i>=0; i-=8,j+=1)
			result += ((long)data[j])<<i;
		return result; 
	}
	
	public byte getAnnouncedCapability() {
		return getOperation().getData()[10];
	}
}
