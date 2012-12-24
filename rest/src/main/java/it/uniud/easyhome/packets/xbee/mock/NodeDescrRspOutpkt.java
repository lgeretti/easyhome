package it.uniud.easyhome.packets.xbee.mock;

import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Context;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

public final class NodeDescrRspOutpkt extends XBeePacketFromNode {

	private static int APS_PAYLOAD_SIZE = 16;
	
	public NodeDescrRspOutpkt(MockXBeeNode node) throws InvalidMockNodeException {
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domain.MANAGEMENT.getCode();
		clusterId = Context.NODE_DESC_RSP.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		frameControl = 0x0;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		apsPayload[0] = (byte)0; // SUCCESS
		apsPayload[1] = (byte)((node.getAddress() >>> 8) & 0xFF);
		apsPayload[2] = (byte)(node.getAddress() & 0xFF);
		
		switch (node.getLogicalType()) {
			case END_DEVICE:
			case ROUTER:			
			case COORDINATOR:
				apsPayload[3] = node.getLogicalType().getCode();
				break;	
			case UNDEFINED:
				throw new InvalidMockNodeException();
		}
		
		short manufacturerCode = node.getManufacturer().getCode();
		apsPayload[6] = (byte)((manufacturerCode >>> 8) & 0xFF);
		apsPayload[7] = (byte)(manufacturerCode & 0xFF);
		
		// We ignore the other bytes for now
	}
	
}
