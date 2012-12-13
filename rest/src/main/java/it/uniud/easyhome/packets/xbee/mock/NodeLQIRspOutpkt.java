package it.uniud.easyhome.packets.xbee.mock;

import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.network.mock.MockXBeeNodeNotFoundException;
import it.uniud.easyhome.packets.Domains;
import it.uniud.easyhome.packets.ManagementContexts;
import it.uniud.easyhome.packets.xbee.XBeeOutboundPacket;

public final class NodeLQIRspOutpkt extends XBeeOutboundPacket {

	private static int APS_PAYLOAD_SIZE_FIXED = 4;
	private static int APS_PAYLOAD_SIZE_PER_ELEMENT = 22;
	
	public NodeLQIRspOutpkt(MockXBeeNode node) throws InvalidMockNodeException, MockXBeeNodeNotFoundException {
		
		if (node.getLogicalType() == NodeLogicalType.END_DEVICE)
			throw new InvalidMockNodeException();
		
		List<MockXBeeNode> neighbors = node.getNeighbors();
		
		final int APS_PAYLOAD_SIZE = APS_PAYLOAD_SIZE_FIXED+APS_PAYLOAD_SIZE_PER_ELEMENT*neighbors.size();
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domains.EASYHOME_MANAGEMENT.getCode();
		clusterId = ManagementContexts.NODE_NEIGH_RSP.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		frameControl = 0x0;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		apsPayload[0] = 0; // SUCCESS
		apsPayload[1] = (byte)(neighbors.size() & 0xFF);
		apsPayload[2] = 0; // We start from the first element
		apsPayload[3] = (byte)(neighbors.size() & 0xFF);
		
		int idx = 4;
		for (MockXBeeNode neighbor: neighbors) {
			
			// Data in little-endian format
			
			// Extended PAN address (plainly using 16 bit network address)
			byte[] nwkAddr = ByteUtils.getBytes(neighbor.getAddress());
			apsPayload[idx++] = nwkAddr[1];
			apsPayload[idx++] = nwkAddr[0];
			for (int i=0;i<6;i++)
				apsPayload[idx++] = (byte)0;
			// MAC address
			byte[] macAddr = ByteUtils.getBytes(neighbor.getId());
			for (int i=0;i<8;i++)
				apsPayload[idx++] = macAddr[7-i];
			// NWK address
			apsPayload[idx++] = nwkAddr[1];
			apsPayload[idx++] = nwkAddr[0];
			// Device type, RxOnWhenIdle (0x1), Relationship (0x3), reserved bit (0x0) -> 0b001101XX -> 52 | 0xXX
			apsPayload[idx++] = (byte) (((byte)52) | neighbor.getLogicalType().getCode());
			// Permit joining (0x2: unknown) and reserved bits (set to zero)
			apsPayload[idx++] = 2;
			// Depth (ignored)
			apsPayload[idx++] = 0;
			// LQI (ignored)
			apsPayload[idx++] = 0;
		}
	}
	
}
