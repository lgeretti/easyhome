package it.uniud.easyhome.packets.xbee.mock;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

public final class DeviceAnnounceOutpkt extends XBeePacketFromNode {

	public DeviceAnnounceOutpkt(MockXBeeNode node) {
		
		dstAddr64 = 0x0000FFFFL;
		dstAddr16 = (short)0xFFFE;
		profileId = Domain.MANAGEMENT.getCode();
		clusterId = ManagementContext.NODE_ANNOUNCE.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		apsPayload = new byte[11];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		byte[] addrBytes = ByteUtils.getBytes(node.getCoordinates().getAddress(), Endianness.LITTLE_ENDIAN);
		apsPayload[0] = addrBytes[0];
		apsPayload[1] = addrBytes[1];
		
		byte[] idBytes = ByteUtils.getBytes(node.getCoordinates().getNuid(), Endianness.LITTLE_ENDIAN);
		for (int i=0; i<8; i++)
			apsPayload[i+2] = idBytes[i];
		
		// The capability byte is ignored
	}
	
}
