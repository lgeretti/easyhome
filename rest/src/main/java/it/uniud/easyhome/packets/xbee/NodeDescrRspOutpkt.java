package it.uniud.easyhome.packets.xbee;

import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ManagementContexts;

public final class NodeDescrRspOutpkt extends XBeeOutboundPacket {

	private static int APS_PAYLOAD_SIZE = 16;
	
	public NodeDescrRspOutpkt(MockXBeeNode node) throws InvalidMockNodeException {
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domains.EASYHOME_MANAGEMENT.getCode();
		clusterId = ManagementContexts.NODE_DESC_RSP.getCode();
		srcEndpoint = 0x01;
		dstEndpoint = 0x02; // This is the endpoint that addresses the management port for the domotic controller
		frameControl = 0x0;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		apsPayload[0] = (byte)0; // SUCCESS
		apsPayload[1] = (byte)((node.getAddress() >>> 8) & 0xFF);
		apsPayload[2] = (byte)(node.getAddress() & 0xFF);
		
		switch (node.getLogicalType()) {
			case END_DEVICE:
				apsPayload[3] = (byte)0x00;
				break;
			case ROUTER:
				apsPayload[3] = (byte)0x20;
				break;				
			case COORDINATOR:
				apsPayload[3] = (byte)0x40;
				break;	
			case UNDEFINED:
				throw new InvalidMockNodeException();
		}
		
		// We ignore the other bytes for now
	}
	
}
