package it.uniud.easyhome.packets.natives;

import java.util.ArrayList;
import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketLengthException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.LocalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class NodeNeighRspPacket extends NativePacket {

	private static final long serialVersionUID = 460097305274024896L;
	
	public NodeNeighRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.NODE_NEIGH_RSP.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData()[3] != (op.getData().length-4)/22)
			throw new InvalidPacketLengthException();
	}
	
	public NodeNeighRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public boolean isSuccessful() {
		return (this.getOperation().getData()[0] == 0);
	}

	public List<LocalCoordinates> getNeighbors() {
		
		List<LocalCoordinates> result = new ArrayList<LocalCoordinates>();
		
		byte[] opData = this.getOperation().getData();
		
		int numNeighbors = opData[3];
		
		for (int n=0;n<numNeighbors;n++) {
			result.add(new LocalCoordinates(ByteUtils.getLong(opData, 12+n*22, Endianness.LITTLE_ENDIAN),
											   ByteUtils.getShort(opData, 20+n*22, Endianness.LITTLE_ENDIAN)));
		}
		
		return result;
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.NODE_NEIGH_RSP.getCode())
			return false;
		
		return true;
	}
}
