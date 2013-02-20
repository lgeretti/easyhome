package it.uniud.easyhome.packets.xbee.mock;

import java.util.List;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.network.mock.MockXBeeNodeNotFoundException;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

public final class ActiveEpRspOutpkt extends XBeePacketFromNode {

	private static int APS_PAYLOAD_SIZE_FIXED = 4;
	
	public ActiveEpRspOutpkt(MockXBeeNode node, byte tsn) throws InvalidMockNodeException, MockXBeeNodeNotFoundException {
		
		List<Byte> endpoints = node.getEndpoints();
		
		final int APS_PAYLOAD_SIZE = APS_PAYLOAD_SIZE_FIXED+endpoints.size();
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domain.MANAGEMENT.getCode();
		clusterId = ManagementContext.ACTIVE_EP_RSP.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = tsn;
		
		apsPayload[0] = 0; // SUCCESS
		byte[] nwkAddr = ByteUtils.getBytes(node.getCoordinates().getAddress(),Endianness.LITTLE_ENDIAN);
		apsPayload[1] = nwkAddr[0];
		apsPayload[2] = nwkAddr[1];
		apsPayload[3] = (byte)(endpoints.size() & 0xFF);
		
		int idx = 4;
		for (Byte endpoint : endpoints)
			apsPayload[idx++] = endpoint;
	}
	
}
