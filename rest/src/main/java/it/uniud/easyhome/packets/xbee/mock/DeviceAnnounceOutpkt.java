package it.uniud.easyhome.packets.xbee.mock;

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
		frameControl = 0x0;
		apsPayload = new byte[11];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		apsPayload[0] = (byte)((node.getAddress() >>> 8) & 0xFF);
		apsPayload[1] = (byte)(node.getAddress() & 0xFF);
		
		for (int i=2,j=56; j>=0; i++,j-=8)
			apsPayload[i] = (byte)((node.getId() >>> j) & 0xFF);
		
		apsPayload[10] = node.getCapability();
	}
	
}
