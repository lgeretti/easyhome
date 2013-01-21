package it.uniud.easyhome.packets.natives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.exceptions.InvalidPacketTypeException;
import it.uniud.easyhome.exceptions.InvalidPayloadLengthException;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.ModuleCoordinates;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.ResponseStatus;

public class ActiveEndpointsRspPacket extends NativePacket {
	
	private static final long serialVersionUID = 3008289461892332406L;

	public ActiveEndpointsRspPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != 0 || dstCoords.getEndpoint() != 0)
			throw new InvalidPacketTypeException();
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			throw new InvalidPacketTypeException();
		if (op.getContext() != ManagementContext.ACTIVE_EP_RSP.getCode())
			throw new InvalidPacketTypeException();
		byte[] opData = op.getData();
		if (opData[3] != opData.length-4)
			throw new InvalidPayloadLengthException();
	}
	
	public ActiveEndpointsRspPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}

	public ResponseStatus getStatus() {
		return ResponseStatus.fromCode(this.getOperation().getData()[0]);
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 1, Endianness.LITTLE_ENDIAN); 
	}
	
	public List<Short> getActiveEndpoints() {
		
		List<Short> result = new ArrayList<Short>();
		
		byte[] opData = this.getOperation().getData();
		
		int numEndpoints = opData[3];
		
		for (int i=0; i<numEndpoints;i++) 
			result.add(new Short(opData[4+i]));
		
		return result;
	}
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.MANAGEMENT.getCode())
			return false;
		if (op.getContext() != ManagementContext.ACTIVE_EP_RSP.getCode())
			return false;
		
		return true;
	}
	
	
}
