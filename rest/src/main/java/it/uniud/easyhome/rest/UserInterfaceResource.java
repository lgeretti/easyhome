package it.uniud.easyhome.rest;

import java.net.URI;
import java.util.HashMap;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.LocationType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetIssuePacket;
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

@Path("/admin")
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
    	
    	if (powerLevel < 0 || powerLevel > 4)
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
    	ClientResponse nodeResponse = client.resource(TARGET).path("network").path(Byte.toString(gid)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	Node node = JsonUtils.getFrom(nodeResponse, Node.class);
    	
    	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
        formData.add("gid",Byte.toString(gid));
        formData.add("address",Short.toString(address));
        formData.add("tsn",Byte.toString((byte)0));
        formData.add("payload",Byte.toString(powerLevel));       
        client.resource(TARGET).path("network").path("jobs").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
        NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_POWER_LEVEL_SET_ISSUE,gid,address,new byte[]{powerLevel});
 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(event);
    	producer.send(outboundMessage);    
    	
    	jmsConnection.close();
    	
        return Response.ok().build();    	
    }
    
    @Path("/minimizeGraph") 
    @POST
    public Response minimizeGraph() throws JSONException, JMSException, NamingException {
        	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
        NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NETWORK_GRAPH_MINIMIZATION,(byte)0,(short)0);
 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(event);
    	producer.send(outboundMessage);    
    	
    	jmsConnection.close();
    	
        return Response.ok().build();    	
    }
    
    @Path("/persistence/name")
    @POST
    public Response changeNodeName() throws JSONException {
    	
    	
    	
    	return Response.ok().build();
    }
    
    @Path("/up")
    @POST
    public Response up() {
    	
    	insertPersistentInfo();
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
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REGISTRATION);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ISSUE);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ACKNOWLEDGMENT);
		insertProcess(ProcessKind.NETWORK_UPDATE);
		insertProcess(ProcessKind.NETWORK_GRAPH_MINIMIZATION);
		
        return Response.ok().build();
    }
    
    private void insertPersistentInfo() {
    	
        MultivaluedMap<String,String> formData;
        
        formData = new MultivaluedMapImpl();
        formData.add("name","Gateway");
        formData.add("locationName","CameraLuca");
        formData.add("locationType",LocationType.BEDROOM.toString());
    	client.resource(TARGET).path("persistentinfo").path(Byte.toString((byte)2)).path(Long.toString(5526146521827785L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        formData = new MultivaluedMapImpl();
        formData.add("name","RouterStudio");
        formData.add("locationName","Studio");
        formData.add("locationType",LocationType.STUDYROOM.toString());
    	client.resource(TARGET).path("persistentinfo").path(Byte.toString((byte)2)).path(Long.toString(5526146521326115L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);    	
        formData = new MultivaluedMapImpl();
        formData.add("name","RouterScarpiera");
        formData.add("locationName","ZonaScarpiera");
        formData.add("locationType",LocationType.LIVINGROOM.toString());
    	client.resource(TARGET).path("persistentinfo").path(Byte.toString((byte)2)).path(Long.toString(5526146523928181L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        formData = new MultivaluedMapImpl();
        formData.add("name","OnOffSwitch");
    	client.resource(TARGET).path("persistentinfo").path(Byte.toString((byte)2)).path(Long.toString(5526146521326185L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
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
