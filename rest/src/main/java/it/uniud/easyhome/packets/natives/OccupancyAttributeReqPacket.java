package it.uniud.easyhome.packets.natives;

import it.uniud.easyhome.contexts.HomeAutomationContext;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class OccupancyAttributeReqPacket extends NativePacket {

	private static final long serialVersionUID = 3703209876955134178L;
	private static final int APS_PAYLOAD_LENGTH = 1;
	
	public OccupancyAttributeReqPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);

		if (op.getDomain() != Domain.HOME_AUTOMATION.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != HomeAutomationContext.OCCUPANCY_SENSING.getCode())
			throw new InvalidContextException();
		if (op.getData().length != APS_PAYLOAD_LENGTH)
			throw new InvalidPayloadLengthException();
	}
	
	public OccupancyAttributeReqPacket(GlobalCoordinates destinationCoordinates, byte endpoint, byte seqNumber) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,endpoint),
			 new ModuleCoordinates(destinationCoordinates,endpoint),				
			 new Operation(seqNumber,Domain.HOME_AUTOMATION.getCode(),HomeAutomationContext.OCCUPANCY_SENSING.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Read attribute(s)*/,
					       new byte[]{0x0} /*Occupancy attribute*/));
	}
	
	public OccupancyAttributeReqPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}

	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.HOME_AUTOMATION.getCode())
			return false;
		if (op.getContext() != HomeAutomationContext.OCCUPANCY_SENSING.getCode())
			return false;
		
		return true;
	}
}
