package it.uniud.easyhome.packets.xbee.mock;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

public final class NodeDescrRspOutpkt extends XBeePacketFromNode {

	private static int APS_PAYLOAD_SIZE = 16;
	
	public NodeDescrRspOutpkt(MockXBeeNode node, byte tsn) throws InvalidMockNodeException {
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domain.MANAGEMENT.getCode();
		clusterId = ManagementContext.NODE_DESC_RSP.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = tsn;
		
		apsPayload[0] = (byte)0; // SUCCESS
		
		byte[] addrBytes = ByteUtils.getBytes(node.getCoordinates().getAddress(), Endianness.LITTLE_ENDIAN);
		apsPayload[1] = addrBytes[0];
		apsPayload[2] = addrBytes[1];
		
		switch (node.getLogicalType()) {
			case END_DEVICE:
			case ROUTER:			
			case COORDINATOR:
				apsPayload[3] = node.getLogicalType().getCode();
				break;	
			case UNDEFINED:
				throw new InvalidMockNodeException();
		}
		
		byte[] manufacturerBytes = ByteUtils.getBytes(node.getManufacturer().getCode(), Endianness.LITTLE_ENDIAN);
		apsPayload[6] = manufacturerBytes[0];
		apsPayload[7] = manufacturerBytes[1];
		
		// We ignore the other bytes for now
	}
	
}
