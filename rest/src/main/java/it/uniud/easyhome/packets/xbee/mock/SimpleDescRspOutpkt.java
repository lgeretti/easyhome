package it.uniud.easyhome.packets.xbee.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.contexts.ManagementContext;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.mock.InvalidMockNodeException;
import it.uniud.easyhome.network.mock.MockXBeeNode;
import it.uniud.easyhome.network.mock.MockXBeeNodeNotFoundException;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.xbee.XBeePacketFromNode;

public final class SimpleDescRspOutpkt extends XBeePacketFromNode {

	private static int APS_PAYLOAD_SIZE_FIXED = 4;
	
	public SimpleDescRspOutpkt(MockXBeeNode node, byte endpoint) throws InvalidMockNodeException, MockXBeeNodeNotFoundException {
		
		Map<Short,HomeAutomationDevice> devices = node.getDevices();
		
		HomeAutomationDevice dev = devices.get(new Short(endpoint));
		
		// We do not send the actual clusters used
		final int APS_PAYLOAD_SIZE = APS_PAYLOAD_SIZE_FIXED + 12;
		
		dstAddr64 = 0x0L;
		dstAddr16 = (short)0x0;
		profileId = Domain.MANAGEMENT.getCode();
		clusterId = ManagementContext.SIMPLE_DESC_RSP.getCode();
		srcEndpoint = 0x00;
		dstEndpoint = 0x00;
		apsPayload = new byte[APS_PAYLOAD_SIZE];
		
		transactionSeqNumber = node.nextSeqNumber();
		
		apsPayload[0] = 0; // SUCCESS
		byte[] nwkAddr = ByteUtils.getBytes(node.getAddress());
		apsPayload[1] = nwkAddr[1];
		apsPayload[2] = nwkAddr[0];
		
		apsPayload[3] = 12;
		
		// Endpoint
		apsPayload[4] = endpoint;
		// Profile
		apsPayload[5] = (byte)(Domain.HOME_AUTOMATION.getCode() & 0xFF);
		apsPayload[6] = (byte)((Domain.HOME_AUTOMATION.getCode() >>> 8) & 0xFF);
		// Device
		apsPayload[7] = (byte)(dev.getCode() & 0xFF);
		apsPayload[8] = (byte)((dev.getCode() >>> 8) & 0xFF);
		// The rest is ignored (implicitly setting to zero the input/output cluster counts)
	}
	
}
