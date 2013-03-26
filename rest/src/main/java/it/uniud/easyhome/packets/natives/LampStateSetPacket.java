package it.uniud.easyhome.packets.natives;

import java.util.Arrays;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.EasyHomeContext;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.states.LampState;
import it.uniud.easyhome.exceptions.*;
import it.uniud.easyhome.network.GlobalCoordinates;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;

public class LampStateSetPacket extends NativePacket {

	private static final long serialVersionUID = 1658106643423371562L;
	
	public LampStateSetPacket(ModuleCoordinates srcCoords, ModuleCoordinates dstCoords, Operation op) {
		
		super(srcCoords,dstCoords,op);
		
		if (srcCoords.getEndpoint() != (byte)0xEA || dstCoords.getEndpoint() != (byte)0xEA)
			throw new InvalidEndpointsException();
		if (op.getDomain() != Domain.EASYHOME.getCode())
			throw new InvalidDomainException();
		if (op.getContext() != EasyHomeContext.LAMP_UPDATE.getCode())
			throw new InvalidContextException();
	}
	
	public LampStateSetPacket(LampState lampState, GlobalCoordinates destination) {
		this(new ModuleCoordinates((byte)1,0L,(short)0,(byte)0xEA),
			 new ModuleCoordinates(destination,(byte)0xEA),				
			 new Operation((byte)0,Domain.EASYHOME.getCode(),EasyHomeContext.LAMP_UPDATE.getCode(),
					       (byte)0x0/*Context invariant*/,(byte)0x0/*Irrelevant*/,getBytes(lampState,destination)));
	}
	
	public LampStateSetPacket(NativePacket pkt) {
		this(pkt.getSrcCoords(),pkt.getDstCoords(),pkt.getOperation());
	}
	
	public short getAddrOfInterest() {
		return ByteUtils.getShort(getOperation().getData(), 0, Endianness.LITTLE_ENDIAN); 
	}

	private static byte[] getBytes(LampState lampState, GlobalCoordinates destination) {

		byte[] result = new byte[8];
		
		byte[] nuidBytes = ByteUtils.getBytes(destination.getNuid(),Endianness.BIG_ENDIAN);

		result[0] = nuidBytes[5];
		result[1] = nuidBytes[6];
		result[2] = nuidBytes[7];
		result[3] = lampState.getRed();
		result[4] = lampState.getGreen();
		result[5] = lampState.getBlue();
		result[6] = lampState.getWhite();
		result[7] = lampState.getAlarm().getCode();
		
		return result;
	}
	
	public String getSeparatedParameters() {
		byte[] data = this.getOperation().getData();
		
		StringBuilder resultBuilder = new StringBuilder();
		for (byte b: data)
			resultBuilder.append(";").append(Integer.toHexString(b));
			
		return resultBuilder.toString();
	}	
	
	public static boolean validates(NativePacket pkt) {
		
		if (pkt == null)
			return false;
		
		Operation op = pkt.getOperation();
		
		if (op.getDomain() != Domain.EASYHOME.getCode())
			return false;
		if (op.getContext() != EasyHomeContext.LAMP_UPDATE.getCode())
			return false;
		
		return true;
	}
}
