package it.uniud.easyhome.rest;

import java.net.URI;
import java.util.HashMap;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetPacket;
import it.uniud.easyhome.processing.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Path("/ui")
public class UserInterfaceResource {
    
	private static final String TARGET = "http://localhost:8080/easyhome/rest/";
	private static Client client = Client.create();
	private static final byte XBEE_GATEWAY_ID = (byte)2;
	private static final int XBEE_GATEWAY_PORT = 5100;
	
	private Connection jmsConnection = null;
	protected Context jndiContext = null;
	protected Session jmsSession = null;
    
    private ClientResponse insertGateway(byte id, int port, ProtocolType protocol) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("id", Byte.toString(id));
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	ClientResponse response = client.resource(TARGET).path("hub").path("gateways")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	ClientResponse response = client.resource(TARGET).path("processes")
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    } 
    
    // curl -X POST http://localhost:8080/easyhome/rest/ui/changePower -H "Content-Type: application/x-www-form-urlencoded" --data-binary "gid=2&address=0&powerLevel=3"
    @Path("/changePower") 
    @POST
    public Response changePower(@FormParam("gid") byte gid, 
    						   @FormParam("address") short address,
    						   @FormParam("powerLevel") byte powerLevel) throws JSONException, JMSException, NamingException {
    	
    	ClientResponse nodeResponse = client.resource(TARGET).path("network").path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node node = JsonUtils.getFrom(nodeResponse, Node.class);
    	
    	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
    		throw new WebApplicationException(Response.Status.NOT_FOUND); 
        	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic outboundPacketsTopic = (Topic) jndiContext.lookup(JMSConstants.OUTBOUND_PACKETS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(outboundPacketsTopic);
            
        jmsConnection.start();
        
        NodePowerLevelSetPacket packet = new NodePowerLevelSetPacket(node.getCoordinates(),powerLevel,(byte)0);
 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(packet);
    	producer.send(outboundMessage);    
    	
    	jmsConnection.close();
    	
        return Response.ok().build();    	
    }
    
    @Path("/up")
    @POST
    public Response up() {
    	
		insertGateway(XBEE_GATEWAY_ID,XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
		insertProcess(ProcessKind.NODE_DESCR_REQUEST);
		insertProcess(ProcessKind.NODE_DESCR_REGISTRATION);	
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REQUEST);
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REGISTRATION);
		insertProcess(ProcessKind.SIMPLE_DESCR_REQUEST);
		insertProcess(ProcessKind.SIMPLE_DESCR_REGISTRATION);
		insertProcess(ProcessKind.NODE_DISCOVERY_REQUEST);
		insertProcess(ProcessKind.NODE_DISCOVERY_REGISTRATION);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REQUEST);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_ACKNOWLEDGMENT);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET);
		insertProcess(ProcessKind.NETWORK_UPDATE);
		
        return Response.ok().build();
    }

    @Path("/down")
    @POST
    public Response down() {
    	
    	client.resource(TARGET).path("hub").path("gateways").delete();
    	client.resource(TARGET).path("processes").delete();
    	client.resource(TARGET).path("network").delete();
    	client.resource(TARGET).path("network").path("jobs").delete();
    	client.resource(TARGET).path("network").path("links").delete();
    	
        return Response.ok().build();
    }
    
}
