package it.uniud.easyhome.packets.natives;

import java.util.ArrayList;
import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.exceptions.InvalidNodeDescException;
import it.uniud.easyhome.exceptions.InvalidPacketLengthException;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;

public class NodeNeighRspPacket extends NativePacket {

	private static final long serialVersionUID = 460097305274024896L;
	
	public NodeNeighRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContexts.NODE_NEIGH_RSP.getCode())
			throw new InvalidPacketTypeException();
		if (op.getData()[3] != (op.getData().length-4)/22)
			throw new InvalidPacketLengthException();
	}
	
	public NodeNeighRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}

	public List<Long> getNeighborIds() {
		
		List<Long> result = new ArrayList<Long>();
		
		byte[] opData = this.getOperation().getData();
		
		int numNeighbors = opData[3];
		
		for (int n=0;n<numNeighbors;n++) {
			long val = 0;
			for (int i=0, j=0; i<=56; i+=8, j++)
				val += ((long)(opData[12+j+n*22] & 0xFF))<<i;
			result.add(val);
		}
		
		return result;
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domains.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContexts.NODE_NEIGH_RSP.getCode())
			return false;
		
		return true;
	}
}
