package it.uniud.easyhome.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.FunctionalityContainerType;
import it.uniud.easyhome.network.FunctionalityType;
import it.uniud.easyhome.network.LocationType;
import it.uniud.easyhome.network.Manufacturer;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.network.NodePersistentInfo;
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
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Path(RestPaths.ADMIN)
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
    	ClientResponse response = client.resource(TARGET).path(RestPaths.GATEWAYS)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind, LogLevel logLevel) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	if (logLevel != null)
    		formData.add("logLevel", logLevel.toString());
    	ClientResponse response = client.resource(TARGET).path(RestPaths.PROCESSES)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
    	return insertProcess(kind, null);
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/ui/changePower -H "Content-Type: application/x-www-form-urlencoded" --data-binary "gatewayId=2&address=0&powerLevel=3"
    @Path("/changePower") 
    @POST
    public Response changePower(@FormParam("gatewayId") byte gatewayId, 
    						   @FormParam("address") short address,
    						   @FormParam("powerLevel") byte powerLevel) throws JSONException, JMSException, NamingException {
    	
    	if (powerLevel < 0 || powerLevel > 4)
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
    	ClientResponse nodeResponse = client.resource(TARGET).path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("address",Short.toString(address));
        formData.add("tsn",Byte.toString((byte)0));
        formData.add("payload",Byte.toString(powerLevel));       
        client.resource(TARGET).path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
        NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_POWER_LEVEL_SET_ISSUE,gatewayId,address,new byte[]{powerLevel});
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
    
    @Path("/up")
    @POST
    public Response up() {
    	
    	insertLocationsAndDevices();
		insertGateway(XBEE_GATEWAY_ID, XBEE_GATEWAY_PORT, ProtocolType.XBEE);
		insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
		insertProcess(ProcessKind.NODE_DESCR_REQUEST);
		insertProcess(ProcessKind.NODE_DESCR_REGISTRATION);	
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REQUEST);
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REGISTRATION);
		insertProcess(ProcessKind.SIMPLE_DESCR_REQUEST);
		insertProcess(ProcessKind.SIMPLE_DESCR_REGISTRATION);
		insertProcess(ProcessKind.NODE_DISCOVERY_REQUEST);
		insertProcess(ProcessKind.NODE_DISCOVERY_REGISTRATION);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REQUEST,LogLevel.DEBUG);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REGISTRATION,LogLevel.DEBUG);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ISSUE);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ACKNOWLEDGMENT);
		insertProcess(ProcessKind.NETWORK_GRAPH_MINIMIZATION);
		insertProcess(ProcessKind.NETWORK_UPDATE);
		
		insertProcess(ProcessKind.OCCUPANCY_REQUEST);
		insertProcess(ProcessKind.OCCUPANCY_REGISTRATION);
		
		insertProcess(ProcessKind.LIGHT_LEVEL_CONTROL,LogLevel.DEBUG);
		
        return Response.ok().build();
    }
    
    private void insertLocationsAndDevices() {
    	
    	try {
	    	
	    	String loc0 = "Salotto";
	    	String loc1 = "Cucina";
	    	String loc2 = "Camera da letto doppia";
	    	String loc3 = "Camera da letto singola";
	    	String loc4 = "Bagno piccolo";
	    	String loc5 = "Bagno grande";
	    	
	        MultivaluedMap<String,String> formData;
	        ClientResponse response;
	        
	        // Locations
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc0);
	        formData.add("type",LocationType.LIVINGROOM.toString());
	        formData.add("imgPath","img/livingroom.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc1);
	        formData.add("type",LocationType.KITCHEN.toString());
	        formData.add("imgPath","img/kitchen.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc2);
	        formData.add("type",LocationType.BEDROOM.toString());
	        formData.add("imgPath","img/doublebedroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	 
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc3);
	        formData.add("type",LocationType.BEDROOM.toString());
	        formData.add("imgPath","img/bedroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc4);
	        formData.add("type",LocationType.BATHROOM.toString());
	        formData.add("imgPath","img/wc.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",loc5);
	        formData.add("type",LocationType.BATHROOM.toString());
	        formData.add("imgPath","img/bathroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        // Devices
	        
	        // ZigBee
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Gateway ZigBee");
	        formData.add("locationName",loc0);
	        formData.add("imgPath","img/accesspoint.svg");
	        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146521827785L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146521827785L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long zigbeeGatewayId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	    	
	    	/*
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Interfaccia gestuale");
	        formData.add("locationName",loc2);
	        formData.add("deviceType", FunctionalityContainerType.HAND_CONTROLLER.toString());
	        formData.add("imgPath","img/hand.svg");
	        formData.add("help", "E' possibile cambiare la lampada associata all'interfaccia e spostare il dispositivo in un'altra stanza");
	        client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928337L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928337L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long gestualId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();     
	    	*/
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Interfaccia gestuale (test)");
	        formData.add("locationName",loc2);
	        formData.add("deviceType", FunctionalityContainerType.HAND_CONTROLLER.toString());
	        formData.add("imgPath","img/hand.svg");
	        formData.add("help", "E' possibile cambiare la lampada associata all'interfaccia e spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928181L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928181L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long testGestualId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	    	
	    	// PowerLine
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Gateway Powerline");
	        formData.add("locationName",loc0);
	        formData.add("imgPath","img/accesspoint.svg");
	        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(100L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(100L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long plGatewayId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Lampada");
	        formData.add("locationName",loc0);
	        formData.add("deviceType", FunctionalityContainerType.COLORED_LAMP.toString());
	        formData.add("imgPath","img/colorlight.svg");
	        formData.add("help", "E' possibile cambiare luce o colore della lampada");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(1L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(1L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long lampadaSalottoId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Lampada");
	        formData.add("locationName",loc2);
	        formData.add("deviceType", FunctionalityContainerType.COLORED_LAMP.toString());
	        formData.add("imgPath","img/colorlight.svg");
	        formData.add("help", "E' possibile cambiare luce o colore della lampada");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(2L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(2L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long lampadaCameraId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Frigo");
	        formData.add("locationName",loc1);
	        formData.add("imgPath","img/fridge.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(3L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	// Fake
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Forno");
	        formData.add("locationName",loc1);
	        formData.add("imgPath","img/oven.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(1L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Macchina del caffè");
	        formData.add("locationName",loc1);
	        formData.add("imgPath","img/coffee.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(2L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Tostapane");
	        formData.add("locationName",loc1);
	        formData.add("imgPath","img/bread.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(3L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Televisore");
	        formData.add("locationName",loc0);
	        formData.add("imgPath","img/tv.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(4L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","HiFi");
	        formData.add("locationName",loc0);
	        formData.add("imgPath","img/hifi.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(5L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Termostato");
	        formData.add("locationName",loc0);
	        formData.add("imgPath","img/thermostat.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(6L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Sveglia");
	        formData.add("locationName",loc3);
	        formData.add("imgPath","img/timer.svg");
	        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(7L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(7L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long timerId = JsonUtils.getFrom(response, NodePersistentInfo.class).getId();
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Lavatrice");
	        formData.add("locationName",loc5);
	        formData.add("imgPath","img/washer.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(8L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	// Functionalities
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Associazione a lampada");
	        formData.add("type",FunctionalityType.PAIRING.toString());
	        formData.add("imgPath","img/link.svg");
	        formData.add("help", "Scegli una lampada a cui associare l'interfaccia gestuale. La precedente associazione verrà annullata");
	        formData.putSingle("deviceId",Long.toString(testGestualId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	//formData.putSingle("deviceId",Long.toString(gestualId));
	    	//client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Luminosità");
	        formData.add("type",FunctionalityType.LUMINOSITY_CONTROL.toString());
	        formData.add("imgPath","img/sun.svg");
	        formData.add("help", "Seleziona il livello di luminosità preferito");
	        formData.putSingle("deviceId",Long.toString(lampadaCameraId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	formData.putSingle("deviceId",Long.toString(lampadaSalottoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Colore");
	        formData.add("type",FunctionalityType.COLOR_CONTROL.toString());
	        formData.add("imgPath","img/colorpalette.svg");
	        formData.add("help", "Seleziona il bilanciamento colore preferito per ognuno dei tre colori (crescente da sinistra verso destra)");
	        formData.putSingle("deviceId",Long.toString(lampadaCameraId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("deviceId",Long.toString(lampadaSalottoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Spostamento");
	        formData.add("type",FunctionalityType.MOVE_ROOM.toString());
	        formData.add("imgPath","img/move.svg");
	        formData.add("help", "Seleziona la nuova stanza in cui spostare il dispositivo");
	        formData.add("deviceId",Long.toString(zigbeeGatewayId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("deviceId",Long.toString(plGatewayId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("deviceId",Long.toString(testGestualId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        //formData.putSingle("deviceId",Long.toString(gestualId));
	        //client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("deviceId",Long.toString(timerId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	    	
    	} catch (JSONException ex) {
    		down();
    		throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    	}
    }
    
    private void stopEventBasedJSFProcesses() throws NamingException, JMSException {
    	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
 	    ObjectMessage nullMessage = jmsSession.createObjectMessage(null);
    	producer.send(nullMessage);    
    	
    	jmsConnection.close();
    }

    @Path("/down")
    @POST
    public Response down() {
    	
    	client.resource(TARGET).path(RestPaths.GATEWAYS).delete();
    	client.resource(TARGET).path(RestPaths.PROCESSES).delete();
    	client.resource(TARGET).path(RestPaths.NODES).delete();
    	client.resource(TARGET).path(RestPaths.JOBS).delete();
    	client.resource(TARGET).path(RestPaths.LINKS).delete();
    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).delete();
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).delete();
    	client.resource(TARGET).path(RestPaths.LOCATIONS).delete();
    	
    	try {
    		stopEventBasedJSFProcesses();
    	} catch (Exception ex) { }
    	
        return Response.ok().build();
    }
    
}
