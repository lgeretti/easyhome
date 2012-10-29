package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.network.mock.MockXBeeNode;

public final class DeviceAnnounceOutpkt extends XBeeOutboundPacket {

	public DeviceAnnounceOutpkt(MockXBeeNode node) {
		
		dstAddr64 = 0x0000FFFFL;
		dstAddr16 = (short)0xFFFE;
		profileId = (short)0xEA50;
		clusterId = (short)0x0013;
		srcEndpoint = 0x01;
		dstEndpoint = 0x01;
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
